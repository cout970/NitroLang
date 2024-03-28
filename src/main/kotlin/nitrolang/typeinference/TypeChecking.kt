package nitrolang.typeinference

import nitrolang.ast.*
import nitrolang.parsing.LAMBDA_CALL_FUNCTION
import nitrolang.parsing.ParserCtx
import nitrolang.parsing.SELF_NAME
import nitrolang.parsing.VARIANT_FIELD_NAME
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

        // Detect getters
        if (func.name.startsWith("get_") && func.params.size == 1 && func.hasReceiver && !func.returnType.isNothing() && !func.returnType.isNever()) {
            val propName = func.name.substring(4)
            program.propertyGetters += Triple(func.params.first().type, propName, func)
        }

        // Detect setters
        if (func.name.startsWith("set_") && func.params.size == 2 && func.hasReceiver && (func.returnType.isNothing() || func.returnType == func.params.first().type)) {
            val propName = func.name.substring(4)
            program.propertySetters += Triple(func.params.first().type, propName, func)
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

        if (func.tag == null) {
            val signature = func.toSignatureWithoutReturn()

            if (signature != null) {
                if (signature in program.allFunctionSignatures) {
                    val dupFunc = program.allFunctionSignatures[signature]!!
                    collector.report(
                        "Duplicated function signature: $signature\nDup at ${dupFunc.span}",
                        func.span
                    )
                }
                program.allFunctionSignatures[signature] = func
            }
        }

        // Extern functions have empty body
        if (func.omitBody) {
            if (func.body.hasAnyCode()) {
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

            if (lastInst is LstExpression && !lastInst.type.isNothing()) {
                val err =
                    "Function '${func.name}' has an expression body and implicit return type Nothing, please specify the correct return type"

                collector.report(err, func.span)
            }
        }
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

    if (returnType is TUnresolved) {
        collector.report("Unable to resolve return type, not enough information", span)
        return
    }

    val lastInst = code.lastExpression?.let { code.getInst(it) }

    if (lastInst == null) {
        collector.report("$name must return a value but the last line is not an expression", span)
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

private fun ParserCtx.getTypeFromUsage(tu: LstTypeUsage, validateParams: Boolean = true): TType {
    if (tu.resolvedType != null) {
        return tu.resolvedType!!.type
    }

    if (tu.isUnresolved) {
        if (tu.sub.isNotEmpty()) {
            collector.report("Type parameters not allowed here", tu.span)
        }
        val ty = typeEnv.unresolved(tu.span, tu.debugName)
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

    val params = tu.sub.map { getTypeFromUsage(it) }

    // Search for struct/option/tag/lambda/etc

    val segments = createPathSegments(tu.currentPath, tu.fullName)

    for (segment in segments) {
        val alias = program.typeAliases.find { it.fullName == segment }

        if (alias != null) {
            if (validateParams && alias.typeParameters.size != params.size) {
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

            if (validateParams && option.typeParameters.size != params.size) {
                collector.report(
                    "Incorrect number of type parameters, expected ${option.typeParameters.size}, found ${params.size}",
                    tu.span
                )
            }

            return typeEnv.composite(base, params)
        }

        val struct = program.structs.find { it.fullName == segment }

        if (struct != null) {

            if (validateParams && !struct.isIntrinsic && struct.typeParameters.size != params.size) {
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
    code.variables.forEach { variable ->
        variable.typeUsage?.let {
            variable.typeBox = typeEnv.box(getTypeFromUsage(it), variable.span)
        }
    }

    Prof.next("bind_variables")
    // Link variable usage with the corresponding variable in scope
    bindVariables(code)

    Prof.next("nodes")
    // Resolve the type of every expression and check other requirements
    code.nodes.toList().forEach { node ->
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
        val matches = findBestFunctionMatch(funcName, call.concreteArgTypes.map { it.type }, null)
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

    Prof.next("check_uninitialized")
    val uninitialized = mutableSetOf<LstVar>()
    var j = 0

    while (j < code.nodes.size) {
        val inst = code.nodes[j]
        j++

        // Variable is defined
        if (inst is LstLetVar && !inst.variable.isParam) {
            uninitialized += inst.variable
            continue
        }

        // Variable is initialized
        // Note: We do not check if the variable is initialized inside an inner block, like an if statement,
        // because there is a chance that the variable is not initialized in all branches
        if (inst is LstStoreVar && inst.variable != null && inst.block.depth == inst.variable!!.block.depth) {
            uninitialized -= inst.variable!!
            continue
        }

        // Variable is used before initialization
        if (inst is LstLoadVar && inst.variable != null && !inst.variable!!.isParam && inst.variable in uninitialized) {
            collector.report(
                "Variable '${inst.name}' is used, but variable initialization may not happen yet",
                inst.span
            )
            continue
        }
    }

    Prof.end()
}

fun ParserCtx.bindVariables(code: LstCode) {

    code.nodes.forEach { node ->
        when (node) {
            is LstLetVar -> {
                node.block.variableStack += node.variable
            }

            is LstLoadVar -> {
                if (node.isUnbound) linkVariable(node, code)
            }

            is LstStoreVar -> {
                if (node.isUnbound) linkVariable(node, code)
            }

            else -> Unit
        }
    }

    // Replace missing variables with field access/store on 'this'
    val thisVar = code.variables.find { it.name == SELF_NAME }
    if (thisVar != null) {
        addImplicitThis(thisVar)
    }

    // Report errors
    code.nodes.filterIsInstance<LstLoadVar>().forEach { node ->
        if (node.isUnbound) {
            val kind = if (node.path.isNotEmpty()) "Constant" else "Variable"
            collector.report("$kind not found: ${node.name}", node.span)
        }
    }
    code.nodes.filterIsInstance<LstStoreVar>().forEach { node ->
        if (node.isUnbound) {
            val kind = if (node.path.isNotEmpty()) "Constant" else "Variable"
            collector.report("$kind not found: ${node.name}", node.span)
        }
    }
}

fun ParserCtx.addImplicitThis(thisVar: LstVar) {
    var i = 0

    while (i < code.nodes.size) {
        val node = code.nodes[i]

        // Replace load_var with missing variable with load_field on `this`
        if (node is LstLoadVar && node.isUnbound) {
            // Try to find the field in the struct
            val found = findField(thisVar.type, node.name)

            // Replace with LstLoadField
            if (found != null) {
                val loadThis = LstLoadVar(
                    ref = code.nextRef(),
                    span = node.span,
                    block = node.block,
                    name = SELF_NAME,
                    path = "",
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

            // Try to find a property getter
            val getter = program.propertyGetters
                .find { (propType, propName, _) -> propType == thisVar.type && propName == node.name }
                ?.let { (_, _, propGetter) -> propGetter }

            if (getter != null) {
                val loadThis = LstLoadVar(
                    ref = code.nextRef(),
                    span = node.span,
                    block = node.block,
                    name = SELF_NAME,
                    path = "",
                    variable = thisVar,
                )
                code.nodes.add(i, loadThis)
                i++

                code.nodes[i] = LstFunCall(
                    ref = node.ref,
                    span = node.span,
                    block = node.block,
                    name = getter.name,
                    path = getter.path,
                    arguments = listOf(loadThis.ref),
                    funRef = getter.ref,
                    function = getter,
                    explicitTypeParams = emptyList(),
                )
                continue
            }
        }

        // Replace store_var with missing variable with store_field on `this`
        if (node is LstStoreVar && node.isUnbound) {
            // Try to find the field in the struct
            val found = findField(thisVar.type, node.name)

            // Replace with LstStoreField
            if (found != null) {
                val loadThis = LstLoadVar(
                    ref = code.nextRef(),
                    span = node.span,
                    block = node.block,
                    name = SELF_NAME,
                    path = "",
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

            // Try to find a property setter
            val setter = program.propertySetters
                .find { (propType, propName, _) -> propType == thisVar.type && propName == node.name }
                ?.let { (_, _, propSetter) -> propSetter }

            if (setter != null) {
                val loadThis = LstLoadVar(
                    ref = code.nextRef(),
                    span = node.span,
                    block = node.block,
                    name = SELF_NAME,
                    path = "",
                    variable = thisVar,
                )
                code.nodes.add(i, loadThis)
                i++

                code.nodes[i] = LstFunCall(
                    ref = node.ref,
                    span = node.span,
                    block = node.block,
                    name = setter.name,
                    path = setter.path,
                    arguments = listOf(loadThis.ref, node.expr),
                    funRef = setter.ref,
                    function = setter,
                    explicitTypeParams = emptyList(),
                )
                continue
            }
        }

        i++
    }
}

fun ParserCtx.findVariable(name: String, block: LstBlock, ref: Ref, code: LstCode): LstVar? {
    var found: LstVar? = null
    var currBlock: LstBlock? = block

    outer@ while (currBlock != null) {
        var index = currBlock.variableStack.lastIndex

        while (index >= 0) {
            val variable = currBlock.variableStack[index]

            if (variable.name == name) {
                // Check that the let definition happened before the lambda definition
                val letIndex = code.nodes.indexOfFirst { it.ref == variable.definedBy }
                val refIndex = code.nodes.indexOfFirst { it.ref == ref }

                if (letIndex < refIndex) {
                    found = variable
                    break@outer
                }
            }

            index--
        }

        currBlock = currBlock.parent
    }

    if (found == null && code.parent != null) {
        return findVariable(name, code.parentBlock!!, code.parentRef!!, code.parent!!)
    }

    return found
}

fun <T> ParserCtx.linkVariable(node: T, code: LstCode) where T : LstExpression, T : HasVarRef {
    if (node.path == "") {
        val found: LstVar? = findVariable(node.name, node.block, node.ref, code)

        if (found != null) {
            node.variable = found
            found.referencedBy.add(node)
            if (found.definedIn != code) {
                found.isUpValue = true
                code.outerVariables += found
            }
            return
        }
    }

    val fullName = createPath(node.path, node.name)

    createPathSegments(code.currentPath, fullName).forEach { segment ->
        val found = program.consts.find { it.fullName == segment }

        if (found != null) {
            node.constant = found
            found.referencedBy.add(node)
            return
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

        is LstLong -> {
            node.typeBox = typeEnv.box(typeEnv.find("Long"), node.span)
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
            val definedType = getTypeFromUsage(node.typeUsage, validateParams = false)
            node.typeUsageBox = typeEnv.box(definedType, node.span)

            val (type, struct) = typeCheckAlloc(node.span, definedType, node.isEnumInstanceInit)

            if (struct != null) {
                val requiredNames = struct.fields.values.map { it.name }.toMutableList()

                // Campos de variantes no son requeridos
                if (struct.parentOption != null) {
                    requiredNames -= VARIANT_FIELD_NAME
                }

                requiredNames.forEach { name ->
                    if (name !in node.initFieldNames) {
                        collector.report("Missing field '$name' of struct '${struct.fullName}'", node.span)
                    }
                }
            }

            node.typeBox = typeEnv.box(type, node.span)
        }

        is LstLambdaInit -> {
            val type = typeEnv.unresolvedFunction(node.span)

            node.typeBox = typeEnv.box(type, node.span)
            node.lambda.typeBox = node.typeBox

            typeEnv.addInitLambdaConstraint(type, node.span) { inferredType ->
                if (inferredType !is TComposite || !inferredType.isFunction()) {
                    collector.report("Inferred invalid type for lambda: $inferredType", node.span)
                    return@addInitLambdaConstraint
                }
                visitLambda(node.lambda, inferredType)
            }
        }

        is LstIfChoose -> {
            val ifTrue = code.getInst(node.ifTrue).asExpr(node)
            val ifFalse = code.getInst(node.ifFalse).asExpr(node)

            if (ifTrue == null || ifFalse == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            val commonType = typeEnv.unresolved(node.span, "If branches common type")

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
            if (node.isUnbound) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            if (node.constant != null) {
                val const = node.constant!!

                node.typeBox = typeEnv.box(const.type, node.span)
                return
            }

            val variable = node.variable ?: error("Variable is not set")

            if (variable.typeBox == null) {
                collector.report("Attempt to read a variable before its first write", node.span)
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            node.typeBox = typeEnv.box(variable.type, node.span)
        }

        is LstStoreVar -> {
            val value = code.getInst(node.expr).asExpr(node)

            if (node.isUnbound || value == null) {
                node.typeBox = typeEnv.box(typeEnv.invalid(node.span), node.span)
                return
            }

            if (node.constant != null) {
                val const = node.constant!!
                node.varTypeBox = const.typeBox
                typeEnv.addAssignableConstraint(const.type, value.type, node.span)
            } else {
                val variable = node.variable ?: error("VarRef not found!")

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

            val unresolved = typeEnv.unresolved(node.span, "Struct field type")
            node.typeBox = typeEnv.box(unresolved, node.span)

            typeEnv.addFindFieldConstraint(instance.type, node.span) { ty ->
                var found: Pair<LstStruct, LstStructField>? = null

                if (ty is TComposite && (ty.base is TStruct || ty.base is TOptionItem)) {
                    found = findField(ty, node.name)
                }

                if (found == null) {
                    val getter = program.propertyGetters
                        .find { (propType, propName, _) -> propType == ty && propName == node.name }
                        ?.let { (_, _, propGetter) -> propGetter }

                    // Replace with function call to getter
                    if (getter != null) {
                        val lstFunCall = LstFunCall(
                            ref = node.ref,
                            span = node.span,
                            block = node.block,
                            name = getter.name,
                            path = getter.path,
                            arguments = listOf(node.instance),
                            funRef = getter.ref,
                            function = getter,
                            explicitTypeParams = emptyList(),
                        )
                        lstFunCall.typeBox = node.typeBox
                        val index = code.nodes.indexOf(node)
                        code.nodes[index] = lstFunCall
                        visitExpression(lstFunCall, code)
                        return@addFindFieldConstraint
                    }

                    // Error no field nor getter
                    collector.report("Type '${ty}' has no field named '${node.name}' nor getter name 'get_${node.name}'", node.span)
                    typeEnv.addEqualConstraint(node.type, typeEnv.invalid(node.span), node.span)
                    return@addFindFieldConstraint
                }

                // This can only be reached if found is not null, so ty is definitely a TComposite
                ty as TComposite

                val (struct, field) = found

                node.struct = struct
                node.field = field
                var fieldType = field.type

                repeat(min(struct.typeParameters.size, ty.params.size)) { i ->
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

            node.typeBox = typeEnv.box(typeEnv.find("Nothing"), node.span)

            typeEnv.addFindFieldConstraint(instance.type, node.span) { ty ->
                if (ty !is TComposite || (ty.base !is TStruct && ty.base !is TOptionItem)) {
                    collector.report("Type '$ty' has no fields", node.span)
                    typeEnv.addEqualConstraint(ty, typeEnv.invalid(node.span), node.span)
                    return@addFindFieldConstraint
                }

                val found = findField(ty, node.name)

                if (found == null) {
                    val setter = program.propertySetters
                        .find { (propType, propName, _) -> propType == ty && propName == node.name }
                        ?.let { (_, _, propSetter) -> propSetter }

                    // Replace with function call to setter
                    if (setter != null) {
                        val lstFunCall = LstFunCall(
                            ref = node.ref,
                            span = node.span,
                            block = node.block,
                            name = setter.name,
                            path = setter.path,
                            arguments = listOf(node.instance, node.expr),
                            funRef = setter.ref,
                            function = setter,
                            explicitTypeParams = emptyList(),
                        )
                        val index = code.nodes.indexOf(node)
                        code.nodes[index] = lstFunCall
                        visitExpression(lstFunCall, code)
                        return@addFindFieldConstraint
                    }

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

                typeEnv.addAssignableConstraint(fieldType, value.type, node.span)
            }
        }

        is LstWhenEnd -> {
            val resultType = if (node.isStatement) {
                typeEnv.find("Nothing")
            } else {
                typeEnv.unresolved(node.span, "When result type")
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
            if (node.typeBox == null) {
                val unresolved = typeEnv.unresolved(node.span, "Function call return type")
                node.typeBox = typeEnv.box(unresolved, node.span)
            }

            val argTypes = mutableListOf<TType>()

            for (arg in node.arguments) {
                val expr = code.getInst(arg).asExpr(node) ?: error("Missing argument expression")
                argTypes += expr.type
            }

            if (node.path == "" && node.explicitTypeParams.isEmpty()) {
                val variable = findVariable(node.name, node.block, node.ref, code)

                if (variable?.typeBox != null && variable.type.isFunction()) {
                    val index = code.nodes.indexOf(node)
                    val variableLoad = LstLoadVar(
                        ref = code.nextRef(),
                        span = node.span,
                        block = node.block,
                        name = node.name,
                        path = "",
                        variable = variable,
                    )

                    val funcCall = LstFunCall(
                        ref = node.ref,
                        span = node.span,
                        block = node.block,
                        name = LAMBDA_CALL_FUNCTION,
                        path = "",
                        arguments = listOf(variableLoad.ref) + node.arguments,
                    )

                    // Share return type
                    funcCall.typeBox = node.typeBox

                    code.nodes[index] = funcCall
                    // Add in the same slot, so the function call is shifted to the next slot
                    code.nodes.add(index, variableLoad)

                    visitExpression(variableLoad, code)
                    visitExpression(funcCall, code)
                    return
                }
            }

            typeEnv.addFindFunctionConstraint(argTypes, node.span) { matchArgs ->
                val thisVar = code.variables.find { it.name == SELF_NAME }
                val scopeThis = thisVar?.type
                val options = findBestFunctionMatch(node.fullName, matchArgs, scopeThis)

                if (options.size != 1) {
                    val returnType = node.type

                    if (options.isEmpty()) {
                        collector.report("Function '${node.fullName}' not found", node.span)
                        node.type = typeEnv.invalid(node.span, "Function not found '${node.fullName}'")
                    } else {

                        // If one of the input types is invalid, don't report the ambiguity
                        if (!matchArgs.any { it is TInvalid }) {
                            if (options.size < 4) {
                                val list = "\n  - ${
                                    options.joinToString("\n  - ") {
                                        it.toSignature() + " at " + it.span.toLinkString()
                                    }
                                }"
                                collector.report("Function resolution ambiguity '${node.fullName}':$list", node.span)
                            } else {
                                collector.report(
                                    "Function resolution ambiguity '${node.fullName}' with ${options.size} posible matches (${matchArgs.firstOrNull() ?: scopeThis})",
                                    node.span
                                )
                            }
                        }

                        node.type = typeEnv.invalid(node.span, "Function resolution ambiguity '${node.fullName}'")
                    }

                    matchArgs.forEach { arg ->
                        node.concreteArgTypes += node.typeBox!!
                        typeEnv.addAssignableConstraint(node.type, arg, node.span)
                    }

                    typeEnv.addEqualConstraint(node.type, returnType, node.span)
                    return@addFindFunctionConstraint
                }

                val func = options.first()
                val args = if (scopeThis != null && func.params.size == matchArgs.size + 1) {
                    node.usesImplicitThis = thisVar
                    listOf(scopeThis) + matchArgs
                } else {
                    matchArgs
                }

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
                    val paramUnresolved = typeEnv.unresolved(node.span, "Generic type param $typeParam")
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

                    typeEnv.addEqualConstraint(
                        getTypeFromUsage(typeUsage),
                        node.typeParamsTypes[index].type,
                        typeUsage.span
                    )
                }

                paramTypes.zip(args).forEach { (param, arg) ->
                    typeEnv.addAssignableConstraint(param, arg, node.span)
                }

                typeEnv.addEqualConstraint(node.type, returnType, node.span)
            }
        }
    }
}

private fun ParserCtx.visitLambda(lambda: LstLambdaFunction, type: TType) {
    if (type !is TComposite || !type.isFunction()) {
        error("Lambda type is not a TLambda: $type")
    }
    val realType = typeEnv.typeLambda(lambda, type.params)

    lambda.typeBox!!.type = realType

    if (realType.params.size - 1 > lambda.params.size) {
        lambda.params.forEach { it.index++ }
        val tu = LstTypeUsage(
            span = lambda.span,
            name = SELF_NAME,
            path = "",
            sub = listOf(),
            typeParameter = null,
            currentPath = "",
            resolvedType = typeEnv.box(realType.params[0], lambda.span),
        )
        val newParam = LstFunctionParam(
            span = lambda.span,
            name = SELF_NAME,
            index = 0,
            typeUsage = tu,
        ).also { it.createVariable(lambda.body) }
        lambda.params.add(0, newParam)

        // Move the `this` var to the beginning of the code
        val last = lambda.body.nodes.removeLast()
        lambda.body.nodes.add(0, last)
    }

    lambda.params.forEachIndexed { index, param ->
        param.typeBox = typeEnv.box(getTypeFromUsage(param.typeUsage), param.span)
        param.variable!!.typeBox = param.typeBox
        typeEnv.addEqualConstraint(param.typeBox!!.type, realType.params[index], lambda.span)
    }

    lambda.returnTypeBox = typeEnv.box(getTypeFromUsage(lambda.returnTypeUsage), lambda.span)
    lambda.body.returnTypeBox = lambda.returnTypeBox
    typeEnv.addEqualConstraint(lambda.returnType, realType.params.last(), lambda.span)

    currentTag = null
    visitCode(lambda.body)

    if (!lambda.returnType.isNothing()) {
        val lastInst = code.lastExpression?.let { code.getInst(it) }

        lastInst?.asExpr(lastInst)?.let { expr ->
            typeEnv.addAssignableConstraint(lambda.returnType, expr.type, expr.span)
        }
    }

    typeEnv.solveConstraints()
    finishCode(lambda.body, lambda.returnType, "Lambda", lambda.span)
}

private fun ParserCtx.typeCheckAlloc(
    span: Span,
    definedType: TType,
    isEnumInstanceInit: Boolean
): Pair<TType, LstStruct?> {

    if (definedType !is TComposite) {
        val err = "Type '${definedType}' cannot be instantiated"
        collector.report(err, span)
        return typeEnv.invalid(span, err) to null
    }

    // Only structs can be allocated, options and traits are only for the type system
    if (definedType.base !is TStruct && definedType.base !is TOptionItem) {
        val err = "Type '${definedType}' is not an struct/option item"
        collector.report(err, span)
        return typeEnv.invalid(span, err) to null
    }

    val struct: LstStruct = when (definedType.base) {
        is TStruct -> definedType.base.instance
        is TOptionItem -> definedType.base.instance
        else -> error("Invalid option: ${definedType.base}")
    }

    if (struct.isEnum && !isEnumInstanceInit) {
        val err = "Enum '${struct.fullName}' instance cannot be created, use and enum value instead"
        collector.report(err, span)
    }

    // The real type is unknown until we resolve al unresolved types
    // and can replace the struct type template with concrete types
    val params = List(struct.typeParameters.size) {
        definedType.params.getOrNull(it) ?: typeEnv.unresolved(span, "Struct type param $it")
    }

    val base = if (struct.parentOption != null)
        typeEnv.typeBaseOptionItem(struct, struct.parentOption!!)
    else
        typeEnv.typeBaseStruct(struct)

    return typeEnv.composite(base, params) to struct
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

private fun findField(ty: TType, name: String): Pair<LstStruct, LstStructField>? {
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
