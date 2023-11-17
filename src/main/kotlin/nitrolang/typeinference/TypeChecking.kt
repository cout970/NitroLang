package nitrolang.typeinference

import nitrolang.ast.*
import nitrolang.parsing.ParserCtx
import nitrolang.parsing.SELF_NAME
import nitrolang.util.Prof
import nitrolang.util.Span
import kotlin.math.min

fun ParserCtx.doAllTypeChecking() {
    Prof.start("check_type_alias")
    program.typeAliases.forEach { alias ->
        alias.type = getTypeFromUsage(alias.typeUsage)
    }

    Prof.next("check_const")
    program.consts.forEach { const ->
        if (const.checked) return@forEach
        const.checked = true
        const.typeBox = typeEnv.box(getTypeFromUsage(const.typeUsage), const.span)
        const.body.returnTypeBox = const.typeBox
    }

    Prof.next("check_structs")
    program.structs.forEach { struct ->
        if (struct.checked) return@forEach
        struct.checked = true

        struct.fields.values.forEach { field ->
            field.typeBox = typeEnv.box(getTypeFromUsage(field.typeUsage), field.span)
        }

        struct.typeParameters.forEach { tp ->
            tp.requiredTags += tp.bounds.mapNotNull { findTag(it) }
        }
    }

    Prof.next("check_lambdas")
    program.lambdaFunctions.forEach { lambda ->
        if (lambda.checked) return@forEach
        lambda.checked = true

        lambda.params.forEach { param ->
            param.typeBox = typeEnv.box(getTypeFromUsage(param.typeUsage), param.span)
            param.variable!!.typeBox = param.typeBox
        }
        lambda.returnTypeBox = typeEnv.box(getTypeFromUsage(lambda.returnTypeUsage), lambda.span)
        lambda.body.returnTypeBox = lambda.returnTypeBox

        lambda.typeBox = typeEnv.box(typeEnv.typeLambda(lambda), lambda.span)
    }

    Prof.next("check_functions")
    program.functions.forEach { func ->
        if (func.checked) return@forEach
        func.checked = true

        currentTag = func.tag
        func.params.forEach { param ->
            param.typeBox = typeEnv.box(getTypeFromUsage(param.typeUsage), param.span)
            param.variable!!.typeBox = param.typeBox
        }
        func.returnTypeBox = typeEnv.box(getTypeFromUsage(func.returnTypeUsage), func.span)
        func.body.returnTypeBox = func.returnTypeBox
        func.typeParameters.forEach { tp ->
            tp.requiredTags += tp.bounds.mapNotNull { findTag(it) }
        }
        currentTag = null
    }

    Prof.next("check_tags")
    val matchMap = mutableMapOf<String, MutableList<Pair<LstTag, LstFunction>>>()

    program.tags.forEach { tag ->
        if (tag.checked) return@forEach
        tag.checked = true

        for (func in tag.headers.values) {
            if (func.params.isEmpty()) {
                collector.report("Missing required receiver type", func.span)
                continue
            }

            if (func.typeParameters.count() != 1) {
                collector.report("Tag function must have exactly 1 type parameter", func.span)
                continue
            }

            val param = func.typeParameters.first()

            if (param.bounds.isEmpty() || param.bounds.none { it.fullName == tag.fullName }) {
                collector.report("Type param must contain the tag, ej. `#${param.name}: ${tag.fullName}`", func.span)
                continue
            }

            val matchEntry = matchMap.getOrPut(func.name) { mutableListOf() }
            matchEntry += tag to func
        }
    }

    program.functions.forEach { func ->
        if (func.tag != null) return@forEach

        val possibleTags = matchMap[func.name] ?: emptyList()

        possibleTags.forEach inner@{ (tag, header) ->
            val matchType = functionMatchesTagHeader(func, header) ?: return@inner
            tag.addPosibleImpl(func.name, matchType, func)
        }
    }

    program.tags.forEach i@{ tag ->
        tag.posibleImplementation.forEach { (implementer, impls) ->
            if (tag.headers.size != impls.size) return@forEach

            // Check all headers have an implementation
            for (name in tag.headers.keys) {
                if (name !in impls) return@forEach
            }

            for (name in tag.headers.keys) {
                tag.addImpl(name, implementer, impls[name]!!)
            }

            tag.implementers += implementer
        }
    }

    Prof.next("check_func_bodies")
    program.functions.forEach { func ->
        if (func.codeChecked) return@forEach
        func.codeChecked = true

        // Extern functions have empty body
        if (func.omitBody) {
            if (func.body.nodes.isNotEmpty()) {
                collector.report("Extern function must have empty body", func.span)
            }
            return@forEach
        }

        currentTag = func.tag
        visitCode(func.body)
        currentTag = null

        if (func.tag != null) {
            return@forEach
        }

        finishCode(func.body, func.returnType, "Function '${func.name}'", func.span)

        // `fun test() = 42` is valid, but clearly an error, it returns Nothing instead of 42
        if (func.returnType.isNothing() && func.hasExpressionBody && func.returnTypeUsage.span.isInternal()) {
            val lastInst = code.lastExpression?.let { code.getInst(it) }

            if ((lastInst as? LstExpression)?.type?.isNothing() != true) {
                val err =
                    "Function '${func.name}' has an expression body and implicit return type Nothing, please specify the correct return type"

                collector.report(err, func.span)
            }
        }
    }

    Prof.next("check_lambda_bodies")
    program.lambdaFunctions.forEach { lambda ->
        if (lambda.codeChecked) return@forEach
        lambda.codeChecked = true

        currentTag = null
        visitCode(lambda.body)
        finishCode(lambda.body, lambda.returnType, "Lambda", lambda.span)
    }

    Prof.next("check_const_bodies")
    program.consts.forEach { const ->
        if (const.codeChecked) return@forEach
        const.codeChecked = true

        const.body.returnTypeBox = const.typeBox

        visitCode(const.body)
        finishCode(const.body, const.type, "Const '${const.name}'", const.span)
    }

    Prof.next("solve_inference")
    typeEnv.solveConstraints()
    Prof.next("finish")
    typeEnv.finish()
    Prof.end()
}

private fun ParserCtx.finishCode(code: LstCode, returnType: TType, name: String, span: Span) {
    if (returnType.isNothing()) {
        return
    }

    val lastInst = code.lastExpression?.let { code.getInst(it) }

    if (lastInst == null) {
        collector.report("$name must return a value but has empty body", span)
        return
    }

    if (lastInst is LstReturn) {
        return
    }

    if (lastInst !is LstExpression) {
        collector.report("$name must return '${returnType}'", span)
        return
    }

    typeEnv.addAssignableConstraint(returnType, lastInst.type, lastInst.span)
}

private fun ParserCtx.getTypeFromUsage(tu: LstTypeUsage): TType {
    if (tu.resolvedType != null) {
        return tu.resolvedType!!.type
    }

    if (tu.unresolvedTypeRef != null) {
        if (tu.sub.isNotEmpty()) {
            collector.report("Type parameters not allowed here", tu.span)
        }
        val ty = typeEnv.unresolved(tu.span)
        tu.resolvedType = typeEnv.box(ty, tu.span)
        return ty
    }

    if (tu.typeParameter != null) {
        if (tu.sub.isNotEmpty()) {
            collector.report("Type parameters not allowed here", tu.span)
        }
        val ty = typeEnv.generic(tu.typeParameter)
        tu.resolvedType = typeEnv.box(ty, tu.span)
        return ty
    }

    if (tu.lambda != null) {
        if (tu.sub.isNotEmpty()) {
            collector.report("Type parameters not allowed here", tu.span)
        }
        val ty = typeEnv.typeLambda(tu.lambda)
        tu.resolvedType = typeEnv.box(ty, tu.span)
        return ty
    }

    val params = tu.sub.map { getTypeFromUsage(it) }

    // Search for struct/option/tag/lambda/etc

    val segments = createPathSegments(tu.currentPath, tu.fullName)

    for (segment in segments) {
        val alias = program.typeAliases.find { it.fullName == segment }

        if (alias != null) {
            if (alias.typeParameters.size != params.size) {
                collector.report(
                    "Incorrect number of type parameters, expected ${alias.typeParameters.size}, found ${params.size}",
                    tu.span
                )
            }

            val map = mutableMapOf<LstTypeParameter, TType>()

            repeat(min(alias.typeParameters.size, params.size)) {
                map[alias.typeParameters[it]] = params[it]
            }

            return program.replaceGenerics(alias.type!!, map)
        }

        val option = program.options.find { it.fullName == segment }

        if (option != null) {
            val base = typeEnv.typeBaseOption(option)

            if (option.typeParameters.size != params.size) {
                collector.report(
                    "Incorrect number of type parameters, expected ${option.typeParameters.size}, found ${params.size}",
                    tu.span
                )
            }

            return typeEnv.composite(base, params)
        }

        val struct = program.structs.find { it.fullName == segment }

        if (struct != null) {

            if (!struct.isIntrinsic && struct.typeParameters.size != params.size) {
                collector.report(
                    "Incorrect number of type parameters, expected ${struct.typeParameters.size}, found ${params.size}",
                    tu.span
                )
            }

            val base = if (struct.parentOption != null)
                typeEnv.typeBaseOptionItem(struct, struct.parentOption!!)
            else
                typeEnv.typeBaseStruct(struct)

            val ty = typeEnv.composite(base, params)
            tu.resolvedType = typeEnv.box(ty, tu.span)
            return ty
        }
    }

    collector.report("Type '${tu.fullName}' not found", tu.span)
    val ty = typeEnv.invalid(tu.span)
    tu.resolvedType = typeEnv.box(ty, tu.span)
    return ty
}

private fun ParserCtx.processTypePattern(tp: LstTypePattern) {
    if (tp.typeParameter != null) {
        if (tp.sub.isNotEmpty()) {
            collector.report("Type parameters not allowed here", tp.span)
        }
        tp.generic = typeEnv.generic(tp.typeParameter)
        return
    }

    if (tp.isAny) {
        return
    }

    tp.sub.forEach { processTypePattern(it) }

    // Search for struct/option/tag/lambda/etc

    val segments = createPathSegments(tp.currentPath, tp.fullName)

    for (segment in segments) {
        val option = program.options.find { it.fullName == segment }

        if (option != null) {
            val base = typeEnv.typeBaseOption(option)

            if (option.typeParameters.size != tp.sub.size) {
                collector.report(
                    "Incorrect number of type parameters, expected ${option.typeParameters.size}, found ${tp.sub.size}",
                    tp.span
                )
            }

            tp.base = base
            return
        }

        val struct = program.structs.find { it.fullName == segment }

        if (struct != null) {

            if (!struct.isIntrinsic && struct.typeParameters.size != tp.sub.size) {
                collector.report(
                    "Incorrect number of type parameters, expected ${struct.typeParameters.size}, found ${tp.sub.size}",
                    tp.span
                )
            }

            val base = if (struct.parentOption != null)
                typeEnv.typeBaseOptionItem(struct, struct.parentOption!!)
            else
                typeEnv.typeBaseStruct(struct)

            tp.base = base
            return
        }
    }

    collector.report("Type '${tp.fullName}' not found", tp.span)
    return
}

private fun ParserCtx.visitCode(code: LstCode) {
    this.code = code

    Prof.start("variables")
    // Resolve specified types in let expressions
    code.variables.values.forEach { variable ->
        variable.typeUsage?.let {
            variable.typeBox = typeEnv.box(getTypeFromUsage(it), variable.span)
        }
    }

    Prof.next("bind_variables")
    // Link variable usage with the corresponding variable in scope
    bindVariables(code)

    Prof.next("nodes")
    // Resolve the type of every expression and check other requirements
    code.nodes.forEach { node ->
        when (node) {
            is LstIfStart -> {
                val cond = code.getInst(node.cond).asExpr(node) ?: return

                typeEnv.addEqualConstraint(cond.type, typeEnv.find("Boolean"), cond.span)
            }

            is LstExpression -> {
                Prof.start("visit_expr")
                visitExpression(node, code)
                Prof.next("solve")
                typeEnv.solveConstraints()
                Prof.end()
            }

            else -> Unit
        }
    }

    Prof.next("opt")
    // Optimice binary operators if possible
    var i = 0
    while (i < code.nodes.size) {
        val call = code.nodes[i]
        i++

        if (call !is LstFunCall) continue
        if (call.posibleOptimizations.isEmpty()) continue
        if (call.concreteArgTypes.size != 2) continue
        if (call.concreteArgTypes[0].type != call.concreteArgTypes[1].type) continue

        val nextCall = code.nodes[i] as LstFunCall

        val ty = call.concreteArgTypes[0].type
        val tyName = ((ty as? TComposite)?.base as? TStruct)?.instance?.fullName ?: continue

        val funcName = call.posibleOptimizations[tyName] ?: continue
        val matches = findBestFunctionMatch(funcName, call.concreteArgTypes.map { it.type })
        if (matches.size != 1) continue
        val match = matches[0]

        // Marge 2 function calls into 1
        val newInst = LstFunCall(
            ref = nextCall.ref,
            span = nextCall.span,
            block = nextCall.block,
            name = funcName,
            path = "",
            arguments = call.arguments,
            funRef = match.ref,
            function = match,
            explicitTypeParams = call.explicitTypeParams,
        )

        newInst.concreteArgTypes += call.concreteArgTypes
        newInst.typeParamsTypes += call.typeParamsTypes
        newInst.typeBox = nextCall.typeBox

        // 'i' was moved to the next node in the beginning of the loop
        code.nodes[i - 1] = newInst
        code.nodes.removeAt(i)
    }

    Prof.end()
}

fun ParserCtx.bindVariables(code: LstCode) {
    fun <T> ParserCtx.linkVariable(node: T, code: LstCode) where T : LstExpression, T : HasVarRef {
        var found: LstVar? = null
        var block: LstBlock? = node.block

        while (block != null && found == null) {
            found = code.variables.values
                .filter { it.block == block }
                .sortedByDescending { it.validAfter.id }
                .filter { it.validAfter.id <= node.ref.id }
                .find { it.name == node.name }

            block = block.parent
        }

        if (found != null) {
            node.varRef = found.ref
            node.variable = found
            found.referencedBy.add(node)
            return
        }

        val found2 = program.consts.find { it.name == node.name }

        if (found2 != null) {
            node.varRef = found2.ref
            node.constant = found2
            found2.referencedBy.add(node)
            return
        }
    }

    code.nodes.filterIsInstance<LstLoadVar>().forEach { if (it.varRef == null) linkVariable(it, code) }
    code.nodes.filterIsInstance<LstStoreVar>().forEach { if (it.varRef == null) linkVariable(it, code) }

    // Replace missing variables with field access/store on 'this'
    val thisVar = code.variables.values.find { it.name == SELF_NAME }
    if (thisVar != null) {
        var i = 0
        while (i < code.nodes.size) {
            val node = code.nodes[i]

            // Replace load_var with missing variable with load_field on `this`
            if (node is LstLoadVar && node.varRef == null) {
                val found = findField(thisVar.type, node.name)

                // Replace with LstLoadField
                if (found != null) {
                    val loadThis = LstLoadVar(
                        ref = code.nextRef(),
                        span = node.span,
                        block = node.block,
                        name = SELF_NAME,
                        path = "",
                        varRef = thisVar.ref,
                        variable = thisVar,
                    )
                    code.nodes.add(i, loadThis)
                    i++

                    code.nodes[i] = LstLoadField(
                        ref = node.ref,
                        span = node.span,
                        block = node.block,
                        instance = loadThis.ref,
                        name = node.name,
                    )
                    continue
                }
            }

            // Replace store_var with missing variable with store_field on `this`
            if (node is LstStoreVar && node.varRef == null) {
                val found = findField(thisVar.type, node.name)

                // Replace with LstStoreField
                if (found != null) {
                    val loadThis = LstLoadVar(
                        ref = code.nextRef(),
                        span = node.span,
                        block = node.block,
                        name = SELF_NAME,
                        path = "",
                        varRef = thisVar.ref,
                        variable = thisVar,
                    )
                    code.nodes.add(i, loadThis)
                    i++

                    code.nodes[i] = LstStoreField(
                        ref = node.ref,
                        span = node.span,
                        block = node.block,
                        instance = loadThis.ref,
                        name = node.name,
                        expr = node.expr,
                    )
                    continue
                }
            }

            i++
        }
    }

    // Report errors
    code.nodes.filterIsInstance<LstLoadVar>().forEach { node ->
        if (node.varRef == null) {
            val kind = if (node.path.isNotEmpty()) "Constant" else "Variable"
            collector.report("$kind not found: ${node.name}", node.span)
        }
    }
    code.nodes.filterIsInstance<LstStoreVar>().forEach { node ->
        if (node.varRef == null) {
            val kind = if (node.path.isNotEmpty()) "Constant" else "Variable"
            collector.report("$kind not found: ${node.name}", node.span)
        }
    }
}

fun ParserCtx.visitExpression(node: LstExpression, code: LstCode) {
    when (node) {
        is LstBoolean -> {
            node.typeBox = typeEnv.box(typeEnv.find("Boolean"), node.span)
        }

        is LstFloat -> {
            node.typeBox = typeEnv.box(typeEnv.find("Float"), node.span)
        }

        is LstInt -> {
            node.typeBox = typeEnv.box(typeEnv.find("Int"), node.span)
        }

        is LstNothing -> {
            node.typeBox = typeEnv.box(typeEnv.find("Nothing"), node.span)
        }

        is LstString -> {
            node.typeBox = typeEnv.box(typeEnv.find("String"), node.span)
        }

        is LstSizeOf -> {
            node.typeUsageBox = typeEnv.box(getTypeFromUsage(node.typeUsage), node.span)
            node.typeBox = typeEnv.box(typeEnv.find("Int"), node.span)
        }

        is LstReturn -> {
            node.typeBox = typeEnv.box(typeEnv.find("Never"), node.span)

            val expr = code.getInst(node.expr).asExpr(node) ?: error("Return requires an expression")

            typeEnv.addEqualConstraint(code.returnTypeBox!!.type, expr.type, expr.span)
        }

        is LstAlloc -> {
            // Declared type, ej. List<*>, Map<*, *>, Struct<Int>
            val definedType = getTypeFromUsage(node.typeUsage)
            node.typeUsageBox = typeEnv.box(definedType, node.span)

            if (definedType !is TComposite) {
                val err = "Type '${definedType}' cannot be instantiated"
                collector.report(err, node.span)
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span, err), node.span)
                return
            }

            // Lambda types have a variable number of parameters
            if (definedType.base is TLambda) {
                val lambda = definedType.base.instance
                node.typeBox = typeEnv.box(typeEnv.typeLambda(lambda), node.span)
                return
            }

            // Only structs can be allocated, options and traits are only for the type system
            if (definedType.base !is TStruct && definedType.base !is TOptionItem) {
                val err = "Type '${definedType}' is not an struct/option item"
                collector.report(err, node.span)
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span, err), node.span)
                return
            }

            val struct: LstStruct = when (definedType.base) {
                is TStruct -> definedType.base.instance
                is TOptionItem -> definedType.base.instance
                else -> error("Invalid option: ${definedType.base}")
            }

            // The real type is unknown until we resolve al unresolved types
            // and can replace the struct type template with concrete types
            val params = List(struct.typeParameters.size) {
                definedType.params.getOrNull(it) ?: typeEnv.unresolved(node.span)
            }

            val base = if (struct.parentOption != null)
                typeEnv.typeBaseOptionItem(struct, struct.parentOption!!)
            else
                typeEnv.typeBaseStruct(struct)

            node.typeBox = typeEnv.box(typeEnv.composite(base, params), node.span)
        }

        is LstLambdaInit -> {
            val alloc = code.getInst(node.alloc).asExpr(node) ?: error("Missing lambda alloc")
            node.typeBox = alloc.typeBox
        }

        is LstIfChoose -> {
            val ifTrue = code.getInst(node.ifTrue).asExpr(node)
            val ifFalse = code.getInst(node.ifFalse).asExpr(node)

            if (ifTrue == null || ifFalse == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            val commonType = typeEnv.unresolved(node.span)

            typeEnv.addAssignableConstraint(commonType, ifTrue.type, ifTrue.span)
            typeEnv.addAssignableConstraint(commonType, ifFalse.type, ifFalse.span)

            node.typeBox = typeEnv.box(commonType, node.span)
        }

        is LstIsType -> {
            node.typeBox = typeEnv.box(typeEnv.find("Boolean"), node.span)
            processTypePattern(node.typePattern)
        }

        is LstAsType -> {
            node.typeUsageBox = typeEnv.box(getTypeFromUsage(node.typeUsage), node.span)
            node.typeBox = node.typeUsageBox
        }

        is LstLoadVar -> {
            if (node.varRef == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            if (node.constant != null) {
                val const = node.constant!!

                node.typeBox = typeEnv.box(const.type, node.span)
                return
            }

            val variable = code.variables[node.varRef] ?: error("VarRef not found!")

            if (variable.typeBox == null) {
                collector.report("Attempt to read a variable before its first write", node.span)
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            node.typeBox = typeEnv.box(variable.type, node.span)
        }

        is LstStoreVar -> {
            val value = code.getInst(node.expr).asExpr(node)

            if (node.varRef == null || value == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            if (node.constant != null) {
                val const = node.constant!!
                node.varTypeBox = const.typeBox
                typeEnv.addAssignableConstraint(const.type, value.type, node.span)
            } else {
                val variable = code.variables[node.varRef] ?: error("VarRef not found!")

                // First use of the variable assigns the type
                if (variable.typeBox == null) {
                    variable.typeBox = value.typeBox
                }

                node.varTypeBox = value.typeBox
                typeEnv.addAssignableConstraint(variable.type, value.type, node.span)
            }

            node.typeBox = typeEnv.box(typeEnv.find("Nothing"), node.span)
        }

        is LstLoadField -> {
            val instance = code.getInst(node.instance).asExpr(node)

            if (instance == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            val unresolved = typeEnv.unresolved(node.span)
            node.typeBox = typeEnv.box(unresolved, node.span)

            typeEnv.addFindFieldConstraint(instance.type, node.span) { ty ->
                if (ty !is TComposite || (ty.base !is TStruct && ty.base !is TOptionItem)) {
                    collector.report("Type '$ty' has no fields", node.span)
                    typeEnv.addEqualConstraint(unresolved, typeEnv.invalid(node.span), node.span)
                    return@addFindFieldConstraint
                }

                val found = findField(ty, node.name)

                if (found == null) {
                    collector.report("Type '${ty}' ha no field named '${node.name}'", node.span)
                    typeEnv.addEqualConstraint(node.type, typeEnv.invalid(node.span), node.span)
                    return@addFindFieldConstraint
                }

                val (struct, field) = found

                node.struct = struct
                node.field = field
                var fieldType = field.type

                repeat(struct.typeParameters.size) { i ->
                    val generic = typeEnv.generic(struct.typeParameters[i])

                    typeEnv.apply {
                        fieldType = fieldType.replace(generic, ty.params[i])
                    }
                }

                typeEnv.addEqualConstraint(node.type, fieldType, node.span)
            }
        }

        is LstStoreField -> {
            val instance = code.getInst(node.instance).asExpr(node)
            val value = code.getInst(node.expr).asExpr(node)

            if (instance == null || value == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            node.typeBox = typeEnv.box(typeEnv.find("Never"), node.span)

            typeEnv.addFindFieldConstraint(instance.type, node.span) { ty ->
                if (ty !is TComposite || (ty.base !is TStruct && ty.base !is TOptionItem)) {
                    collector.report("Type '$ty' has no fields", node.span)
                    typeEnv.addEqualConstraint(ty, typeEnv.invalid(node.span), node.span)
                    return@addFindFieldConstraint
                }

                val struct = when (ty.base) {
                    is TStruct -> ty.base.instance
                    is TOptionItem -> ty.base.instance
                    else -> error("Invalid type base: ${ty.base}")
                }
                val field = struct.fields.values.find { it.name == node.name }

                if (field == null) {
                    collector.report("Type '${ty}' has no field named '${node.name}'", node.span)
                    typeEnv.addEqualConstraint(ty, typeEnv.invalid(node.span), node.span)
                    return@addFindFieldConstraint
                }

                node.struct = struct
                node.field = field
                var fieldType = field.type

                repeat(struct.typeParameters.size) { i ->
                    val generic = typeEnv.generic(struct.typeParameters[i])

                    typeEnv.apply {
                        fieldType = fieldType.replace(generic, ty.params[i])
                    }
                }

                typeEnv.addAssignableConstraint(fieldType, value.type, node.span)
            }
        }

        is LstWhenEnd -> {
            val resultType = if (node.isStatement) {
                typeEnv.find("Nothing")
            } else {
                typeEnv.unresolved(node.span)
            }

            node.typeBox = typeEnv.box(resultType, node.span)
            node.start.typeBox = node.typeBox

            node.branchStores.forEach { branch ->
                branch.typeBox = node.typeBox
                val value = code.getInst(branch.expr).asExpr(node) ?: return@forEach

                typeEnv.addAssignableConstraint(resultType, value.type, branch.span)
            }
        }

        is LstFunCall -> {
            val unresolved = typeEnv.unresolved(node.span)
            node.typeBox = typeEnv.box(unresolved, node.span)

            val argTypes = mutableListOf<TType>()

            for (arg in node.arguments) {
                val expr = code.getInst(arg).asExpr(node) ?: error("Missing argument expression")
                argTypes += expr.type
            }

            typeEnv.addFindFunctionConstraint(argTypes, node.span) { args ->
                val options = findBestFunctionMatch(node.fullName, args)

                if (options.size != 1) {
                    val returnType = node.type

                    if (options.isEmpty()) {
                        collector.report("Function '${node.fullName}' not found", node.span)
                        node.type = typeEnv.invalid(node.span, "Function not found '${node.fullName}'")
                    } else {
                        collector.report(
                            "Function resolution ambiguity '${node.fullName}':\n  - ${options.joinToString("\n  - ") { it.span.toString() }}",
                            node.span
                        )
                        node.type = typeEnv.invalid(node.span, "Function resolution ambiguity '${node.fullName}'")
                    }

                    args.forEach { arg ->
                        node.concreteArgTypes += node.typeBox!!
                        typeEnv.addAssignableConstraint(node.type, arg, node.span)
                    }

                    typeEnv.addEqualConstraint(node.type, returnType, node.span)
                    return@addFindFunctionConstraint
                }

                val func = options.first()

                if (func.params.size != args.size) {
                    collector.report(
                        "Function '${func.name}' expects ${func.params.size}, but ${args.size} arguments where provided",
                        node.span
                    )
                }

                node.funRef = func.ref
                node.function = func

                for (arg in args) {
                    node.concreteArgTypes += typeEnv.box(arg, node.span)
                }

                // Replace generics with unresolved
                var paramTypes = func.params.map { it.type }
                var returnType = func.returnType

                func.typeParameters.forEach { typeParam ->
                    val generic = typeEnv.generic(typeParam)
                    val paramUnresolved = typeEnv.unresolved(node.span)
                    node.typeParamsTypes += typeEnv.box(paramUnresolved, node.span)

                    typeEnv.addBoundsConstraint(paramUnresolved, typeParam.requiredTags, node.span)

                    typeEnv.apply {
                        paramTypes = paramTypes.map { it.replace(generic, paramUnresolved) }
                        returnType = returnType.replace(generic, paramUnresolved)
                    }
                }

                // Type params explicitly defined
                node.explicitTypeParams.forEachIndexed { index, typeUsage ->
                    if (index >= node.typeParamsTypes.size) {
                        collector.report("Incorrect number of type parameters supplied", typeUsage.span)
                        return@forEachIndexed
                    }

                    typeEnv.addEqualConstraint(getTypeFromUsage(typeUsage), node.typeParamsTypes[index].type, typeUsage.span)
                }

                paramTypes.zip(args).forEach { (param, arg) ->
                    typeEnv.addAssignableConstraint(param, arg, node.span)
                }

                typeEnv.addEqualConstraint(node.type, returnType, node.span)
            }
        }
    }
}

private fun ParserCtx.findTag(tu: LstTypeUsage): LstTag? {
    if (tu.sub.isNotEmpty()) {
        collector.report("Type parameters not allowed here", tu.span)
    }

    // Search for tag
    val segments = createPathSegments(tu.currentPath, tu.fullName)

    for (segment in segments) {
        val tag = program.tags.find { it.fullName == segment }

        if (tag != null) {
            return tag
        }
    }

    collector.report("Tag '${tu.fullName}' not found", tu.span)
    return null
}

private fun ParserCtx.findField(ty: TType, name: String): Pair<LstStruct, LstStructField>? {
    if (ty !is TComposite || (ty.base !is TStruct && ty.base !is TOptionItem)) {
        return null
    }

    val struct = when (ty.base) {
        is TStruct -> ty.base.instance
        is TOptionItem -> ty.base.instance
        else -> error("Invalid type base: ${ty.base}")
    }
    val field = struct.fields.values.find { it.name == name } ?: return null

    return struct to field
}

private fun ParserCtx.functionMatchesTagHeader(origFunc: LstFunction, headerFunc: LstFunction): TType? {
    if (origFunc.tag != null) return null
    if (origFunc.name != headerFunc.name) return null
    if (origFunc.params.size != headerFunc.params.size) return null
    var tagImplementer: TType? = null
    val typeParam = headerFunc.typeParameters.first()

    val result = mutableListOf<Pair<TType, TType>>()

    val origTypes = mutableListOf(origFunc.returnType)
    origFunc.params.forEach { origTypes += it.type }

    val headerTypes = mutableListOf(headerFunc.returnType)
    headerFunc.params.forEach { headerTypes += it.type }

    deepCompareTypes(origTypes, headerTypes, result)

    for ((base, header) in result) {
        if (header is TGeneric && header.instance == typeParam) {
            tagImplementer = base
            break
        }
    }

    if (tagImplementer == null) return null

    val replacements = mutableMapOf(typeParam to tagImplementer)

    for ((baseParam, headerParam) in origFunc.params.zip(headerFunc.params)) {
        val baseT = baseParam.type
        val headerT = program.replaceGenerics(headerParam.type, replacements)

        if (baseT != headerT) return null
    }

    val baseT = origFunc.returnType
    val headerT = program.replaceGenerics(headerFunc.returnType, replacements)

    if (baseT != headerT) return null

    return tagImplementer
}
