package nitrolang.types

import nitrolang.ast.*
import nitrolang.parsing.ParserCtx
import nitrolang.util.Span

fun ParserCtx.doTypeChecking() {

    program.consts.values.forEach { const ->
        const.type = typeUsageToTypeTree(const.typeUsage)
        const.body.returnType = const.type
    }
    program.structs.values.forEach { struct ->
        struct.fields.values.forEach { field ->
            field.type = typeUsageToTypeTree(field.typeUsage)
        }
    }
    program.functions.values.forEach { func ->
        func.params.forEach { param ->
            param.type = typeUsageToTypeTree(param.typeUsage)
            param.variable!!.type = param.type
        }
        func.returnType = typeUsageToTypeTree(func.returnTypeUsage)
        func.body.returnType = func.returnType
    }

    program.functions.values.forEach { func ->
        // Extern functions have empty body
        if (func.isExternal) {
            if (func.body.nodes.isNotEmpty()) {
                collector.report("Extern function must have empty body", func.span)
            }
            return@forEach
        }

        typeCheckCode(func.body)

        // Check that the function actually returns something
        val defined = func.body.returnType!!
        if (!defined.isNothing()) {
            when (val last = func.body.lastExpression?.let { func.body.getNode(it) }) {
                null -> {
                    collector.report(
                        "Function '${func.name}' must return '$defined' but has empty body",
                        func.span
                    )
                }

                // Ok, already checked in processCode()
                is LstReturn -> Unit

                is LstExpression -> {
                    if (!last.type!!.isNever() && !canBeAssignedTo(defined, last.type!!)) {
                        collector.report(
                            "Type mismatch, attempt to return '${last.type}' on a function that must return '$defined'",
                            last.span
                        )
                    }
                }

                else -> {
                    collector.report(
                        "Function '${func.name}' must return '$defined'",
                        func.span
                    )
                }
            }
        }
    }
    program.consts.values.forEach { const ->
        typeCheckCode(const.body)
    }
}

fun ParserCtx.typeUsageToTypeTree(tu: TypeUsage): TypeTree {
    val params = tu.sub.map { typeUsageToTypeTree(it) }

    if (tu.unresolvedTypeRef != null) {
        return TypeTree(
            base = UnresolvedType(tu.unresolvedTypeRef),
            params = params,
        )
    }

    if (tu.typeParameter != null) {
        return TypeTree(
            base = ParamType(tu.typeParameter),
            params = params,
        )
    }

    val segments = createPathSegments(tu.currentPath, tu.fullName)

    for (segment in segments) {
        val option = program.options.values.find { it.fullName == segment }

        if (option != null) {
            return TypeTree(
                base = OptionType(option),
                params = params,
            )
        }

        val struct = program.structs.values.find { it.fullName == segment }

        if (struct != null) {
            return TypeTree(
                base = StructType(struct),
                params = params,
            )
        }
    }

    collector.report("Type '${tu.fullName}' not found", tu.span)
    return TypeTree(InvalidType, params)
}

fun ParserCtx.typeOf(name: String): TypeTree {
    val struct = program.structs.values.find { it.name == name } ?: error("Invalid type: $name")
    return TypeTree(base = StructType(struct))
}

fun <T> ParserCtx.linkVariable(node: T, code: LstCode) where T : LstExpression, T : HasVarRef {
    var found: LstVar? = null
    var block: LstNodeBlock? = node.block

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

    val found2 = program.consts.values.find { it.name == node.name }

    if (found2 != null) {
        node.varRef = found2.ref
        node.constant = found2
        found2.referencedBy.add(node)
        return
    }

    collector.report("Variable not found: ${node.name}", node.span)
}

fun ParserCtx.typeCheckCode(code: LstCode) {
    // Resolve specified types in let expressions
    code.variables.values.forEach { variable ->
        variable.type = variable.typeUsage?.let { typeUsageToTypeTree(it) }
    }

    // Link variable usage with the corresponding variable in scope
    code.nodes.forEach { node ->
        when (node) {
            is LstLoadVar -> {
                if (node.varRef == null) {
                    linkVariable(node, code)
                }
            }

            is LstStoreVar -> {
                if (node.varRef == null) {
                    linkVariable(node, code)
                }
            }

            else -> Unit
        }
    }

    // Resolve the type of every expression and check other requirements
    code.nodes.forEach { node ->
        when (node) {
            is LstIfStart -> {
                val cond = code.getNode(node.cond).asExpr(node) ?: return

                if (!cond.hasTypeError && cond.type != null && !cond.type!!.isBoolean()) {
                    collector.report("Condition must be a boolean, '${cond.type}' found", cond.span)
                }
            }

            is LstExpression -> {
                if (node.type == null && !node.hasTypeError) typeCheckNodeExpression(node, code)
            }

            else -> Unit
        }
    }

    // Flag errors
    code.variables.values.forEach { variable ->

        fun reportNumMismatch(typeParameters: List<TypeParameter>, type: TypeTree) {
            val actual = type.params.size
            val expected = typeParameters.size

            if (expected > actual) {
                val missing = typeParameters
                    .takeLast(expected - actual)
                    .joinToString(", ") { it.name }

                collector.report(
                    "Variable '${variable.name}' has invalid type: missing type parameters ($missing)",
                    variable.span
                )
            }

            if (expected < actual) {
                collector.report(
                    "Variable '${variable.name}' has invalid type: too many type parameters, " +
                            "expecting $expected",
                    variable.span
                )
            }
        }

        when (val base = variable.type?.base) {
            InvalidType -> {
                // Already reported in typeUsageToTypeTree()
            }

            is OptionType -> {
                reportNumMismatch(base.option.typeParameters, variable.type!!)
            }

            is StructType -> {
                reportNumMismatch(base.struct.typeParameters, variable.type!!)
            }

            is ParamType -> Unit

            is UnresolvedType -> {
                collector.report("Variable '${variable.name}' has a type that cannot be inferred", variable.span)
            }

            null -> {
                collector.report("Variable '${variable.name}' has no type", variable.span)
            }
        }
    }
}

fun ParserCtx.typeCheckNodeExpression(node: LstExpression, code: LstCode) {
    when (node) {
        is LstNothing -> {
            node.type = typeOf("Nothing")
        }

        is LstBoolean -> {
            node.type = typeOf("Boolean")
        }

        is LstInt -> {
            node.type = typeOf("Int")
        }

        is LstFloat -> {
            node.type = typeOf("Float")
        }

        is LstString -> {
            node.type = typeOf("String")
        }

        is LstIsType -> {
            node.typeTree = typeUsageToTypeTree(node.typeUsage)
            node.hasTypeError = node.typeTree.isInvalid()
            node.type = typeOf("Boolean")
        }

        is LstAsType -> {
            node.type = typeUsageToTypeTree(node.typeUsage)
            node.hasTypeError = node.type.isInvalid()
        }

        is LstSizeOf -> {
            node.typeTree = typeUsageToTypeTree(node.typeUsage)
            node.hasTypeError = node.typeTree.isInvalid()
            node.type = typeOf("Int")
        }

        is LstAlloc -> {
            // Declared type, ej. List<*>, Map<*, *>, Struct<Int>
            val type = typeUsageToTypeTree(node.typeUsage)

            // Only structs can be allocated, options and traits are only for the type system
            if (type.base !is StructType) {
                collector.report(
                    "Type '${type}' is not an struct",
                    node.span
                )
                node.propagateTypeError()
                return
            }

            val struct: LstStruct = type.base.struct

            // The real type is unknown until we resolve al unresolved types
            // and can replace the struct type template with concrete types
            val realParams = List(struct.typeParameters.size) {
                type.params.getOrNull(it) ?: TypeTree(base = UnresolvedType(program.nextUnresolvedTypeRef()))
            }

            val finalType = TypeTree(base = type.base, params = realParams)

            node.typeTree = finalType
            node.struct = struct
            node.hasTypeError = finalType.isInvalid()
            node.type = finalType
        }

        is LstIfChoose -> {
            val ifTrue = code.getNode(node.ifTrue).asExpr(node) ?: return
            val ifFalse = code.getNode(node.ifFalse).asExpr(node) ?: return

            // Propagate errors
            if (ifTrue.hasTypeError || ifFalse.hasTypeError) {
                node.propagateTypeError()
                return
            }

            if (ifTrue.type == null || ifFalse.type == null) {
                node.propagateTypeError()
                return
            }

            // Attempt to replace unsolved types
            attemptResolveUnresolvedTypes(
                node.span,
                listOf(ifTrue.type!!),
                listOf(ifFalse.type!!),
                code
            )
            attemptResolveUnresolvedTypes(
                node.span,
                listOf(ifFalse.type!!),
                listOf(ifTrue.type!!),
                code
            )

            node.type = commonType(ifTrue.type!!, ifFalse.type!!)

            if (node.type!!.isInvalid()) {
                collector.report(
                    "Choice has different types for each choice: " +
                            "${ifTrue.type} vs ${ifFalse.type}",
                    node.span
                )
                node.propagateTypeError()
            }
        }

        is LstLoadVar -> {
            if (node.varRef == null) {
                node.propagateTypeError()
                return
            }

            val type = if (node.varRef in program.consts) {
                val const = program.consts[node.varRef] ?: error("ConstRef not found!")
                const.type
            } else {
                val variable = code.variables[node.varRef] ?: error("VarRef not found!")
                variable.type
            }

            node.type = type

            if (type == null) {
                node.propagateTypeError()
            }
        }

        is LstStoreVar -> {
            val value = code.getNode(node.expr).asExpr(node) ?: return

            // Propagate errors
            if (value.hasTypeError || value.type == null) {
                node.propagateTypeError()
                return
            }

            if (node.varRef == null) {
                node.propagateTypeError()
                return
            }

            when (node.varRef) {
                in program.consts -> {
                    val const = program.consts[node.varRef] ?: error("ConstRef not found!")
                    node.varType = const.type
                }

                else -> {
                    val variable = code.variables[node.varRef] ?: error("VarRef not found!")

                    // Variable without defined type, first assigned here
                    if (variable.type == null) {
                        variable.type = value.type
                    }

                    node.varType = variable.type
                }
            }

            if (node.varType == null) {
                node.propagateTypeError()
                return
            }

            // Attempt to replace unsolved types
            // For example:
            // `let a: Optional<Int> = Optional::None $[]`
            // the value `Optional::None $[]` has type `Optional::None<unresolved#1>`
            // but, with `a` we can infer that `unresolved#1` is `Int`
            attemptResolveUnresolvedTypes(
                node.span,
                listOf(node.varType!!),
                listOf(value.type!!),
                code
            )

            if (!canBeAssignedTo(node.varType!!, value.type!!)) {
                collector.report(
                    "Type mismatch, variable '${node.name}' " +
                            "expects a value of type '${node.varType}', " +
                            "but found '${value.type}' ($value)",
                    node.span
                )
                node.propagateTypeError()
                return
            }

            node.type = typeOf("Nothing")
        }

        is LstWhenEnd -> {
            val types = mutableListOf<TypeTree>()

            node.branchStores.forEach {
                val value = code.getNode(it.expr).asExpr(node) ?: return

                // Propagate errors
                if (value.hasTypeError || value.type == null) {
                    node.propagateTypeError()
                    return
                }

                types += value.type!!
            }

            node.type = multiCommonType(types)

            if (node.type.isInvalid()) {
                // Unable to find a common type, use the first type
                node.type = types.firstOrNull()
            }

            if (node.type == null) {
                node.type = TypeTree(InvalidType)
            }

            // Copy values to WhenStart and all WhenBranch
            node.start.type = node.type

            node.branchStores.forEach { branch ->
                branch.type = node.type

                // Check the type of each branch
                val value = code.getNode(branch.expr).asExpr(node) ?: return

                if (value.hasTypeError || value.type == null) {
                    return@forEach
                }

                // Attempt to replace unsolved types
                attemptResolveUnresolvedTypes(
                    value.span,
                    listOf(node.type!!),
                    listOf(value.type!!),
                    code
                )
                attemptResolveUnresolvedTypes(
                    value.span,
                    listOf(value.type!!),
                    listOf(node.type!!),
                    code
                )

                // Check if the final type if valid for the 'when' result
                if (!canBeAssignedTo(node.type!!, value.type!!)) {
                    collector.report(
                        "Type mismatch, when " +
                                "expects a value of type '${node.type}', " +
                                "but found '${value.type}' ($value)",
                        value.span
                    )
                    return
                }
            }

        }

        is LstLoadField -> {
            val instance = code.getNode(node.instance).asExpr(node) ?: return

            // Propagate errors
            if (instance.hasTypeError) {
                node.propagateTypeError()
                return
            }

            val instanceType = instance.type

            if (instanceType == null) {
                node.propagateTypeError()
                return
            }

            // Find the field
            val struct: LstStruct
            val field: LstStructureField

            if (instanceType.base is StructType) {
                struct = instanceType.base.struct
                val found = struct.fields.values.find { it.name == node.name }

                if (found == null) {
                    collector.report(
                        "Type '${instanceType}' ha no field named '${node.name}'",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                node.fieldRef = found.ref
                node.field = found
                field = found
            } else {
                collector.report(
                    "Type '${instanceType}' has no fields",
                    node.span
                )
                node.propagateTypeError()
                return
            }

            val fieldType = field.type
            if (fieldType == null) {
                node.propagateTypeError()
                return
            }

            // Replace parametric types with concrete types
            val replacements = findStructReplacements(node.span, struct, instanceType)

            val concreteStructType = replaceTypeParams(struct.templateType, replacements)
            val concreteFieldType = replaceTypeParams(fieldType, replacements)

            node.concreteStructType = concreteStructType
            node.concreteFieldType = concreteFieldType

            node.type = node.concreteFieldType
        }

        is LstStoreField -> {
            val instance = code.getNode(node.instance).asExpr(node) ?: return
            val value = code.getNode(node.expr).asExpr(node) ?: return

            // Propagate errors
            if (instance.hasTypeError || value.hasTypeError) {
                node.propagateTypeError()
                return
            }

            val instanceType = instance.type
            val valueType = value.type

            // Process later
            if (instanceType == null || valueType == null) {
                node.propagateTypeError()
                return
            }

            // Find the field
            val struct: LstStruct
            val field: LstStructureField

            if (instanceType.base is StructType) {
                struct = instanceType.base.struct
                val found = struct.fields.values.find { it.name == node.name }

                if (found == null) {
                    collector.report(
                        "Type '${instanceType}' ha no field named '${node.name}'",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                node.fieldRef = found.ref
                node.field = found
                field = found
            } else {
                collector.report(
                    "Type '${instanceType}' has no fields",
                    node.span
                )
                node.propagateTypeError()
                return
            }

            val fieldType = field.type
            if (fieldType == null) {
                node.propagateTypeError()
                return
            }

            // Replace parametric types with concrete types
            val replacements = findStructReplacements(node.span, struct, instanceType)

            val concreteStructType = replaceTypeParams(struct.templateType, replacements)
            val concreteFieldType = replaceTypeParams(fieldType, replacements)

            node.concreteStructType = concreteStructType
            node.concreteFieldType = concreteFieldType

            attemptResolveUnresolvedTypes(
                node.span,
                listOf(value.type!!),
                listOf(node.concreteFieldType!!),
                code
            )

            // By directional type resolution
            attemptResolveUnresolvedTypes(
                node.span,
                listOf(node.concreteFieldType!!),
                listOf(value.type!!),
                code
            )

            if (!canBeAssignedTo(node.concreteFieldType!!, value.type!!)) {
                collector.report(
                    "Type mismatch, field '${field.name}' " +
                            "expects a value of type '${node.concreteFieldType}', " +
                            "but found '${value.type!!}' ($value)",
                    node.span
                )
            }

            node.type = typeOf("Nothing")
        }

        is LstFunCall -> {
            val args = node.arguments.map { argRef ->
                val argNode = code.getNode(argRef)
                argNode as? LstExpression ?: error("Expression expected: $argNode")
            }

            // Propagate errors
            if (args.any { it.hasTypeError || it.type == null }) {
                node.propagateTypeError()
                return
            }

            val choices = program.functions.values.filter { it.name == node.name }

            if (choices.isEmpty()) {
                if (args.isNotEmpty() && args.first().type != null) {
                    collector.report(
                        "Function '${node.name}' not found " +
                                "for type '${args.first().type}'", node.span
                    )
                } else {
                    collector.report("Function '${node.name}' not found", node.span)
                }

                node.propagateTypeError()
                return
            }

            val tmpArgsTypes = args.map { it.type!! }

            val sortedChoices = choices.sortedByDescending {
                val paramsTypes = it.params.map { param -> param.type!! }
                functionParamsSimilarity(paramsTypes, tmpArgsTypes)
            }

            // Best match
            val function = sortedChoices.first()

            node.funRef = function.ref
            node.function = function

            if (function.returnType == null || function.params.any { it.type == null }) {
                node.propagateTypeError()
                return
            }

            if (function.params.size != args.size) {
                collector.report(
                    "Function '${function.name}' expects ${function.params.size}, " +
                            "but ${args.size} arguments where provided",
                    node.span
                )
            }

            // Replace parametric types with concrete types
            val replacements = findFuncReplacements(node, function, args)
            val allTypeParams = mutableSetOf<TypeParamRef>()

            collectAllTypeParams(function.returnType!!, allTypeParams)

            function.params.forEach {
                collectAllTypeParams(it.type!!, allTypeParams)
            }

            // All unbound type parameters are replaced by unresolved types, so they can be inferred by usage
            // For example: `let a: Int = func()` where `fun func(): #T = ...`, #T is inferred to be Int
            allTypeParams.forEach {
                if (it !in replacements) {
                    replacements[it] = TypeTree(base = UnresolvedType(program.nextUnresolvedTypeRef()))
                }
            }

            val concreteTypeParams = function.params.map { replaceTypeParams(it.type!!, replacements) }
            val concreteArgTypes = args.map { replaceTypeParams(it.type!!, replacements) }

            node.concreteTypeParams = concreteTypeParams

            // Attempt to infer the types of unresolved type-values, for example:
            // `let a = #[]` and `a.add(0)` => `a` is List<Int>
            attemptResolveUnresolvedTypes(node.span, concreteTypeParams, concreteArgTypes, code)

            // Replace type params with concrete types

            val paramsTypes = function.params.map { param ->
                replaceTypeParams(param.type!!, replacements)
            }

            val argsTypes = args.map {
                replaceTypeParams(it.type!!, replacements)
            }

            node.concreteParamTypes = paramsTypes

            paramsTypes.zip(argsTypes).forEachIndexed { index, (param, arg) ->
                if (!canBeAssignedTo(param, arg)) {
                    collector.report(
                        "Type mismatch calling function '${function.name}', param ${index + 1} expects '${param}', " +
                                "but '${arg}' was found instead",
                        args[index].span
                    )
                }
            }

            node.type = replaceTypeParams(function.returnType!!, replacements)
        }

        is LstReturn -> {
            node.type = typeOf("Nothing")
            val value = code.getNode(node.expr).asExpr(node) ?: return

            // Propagate errors
            if (value.hasTypeError) {
                node.propagateTypeError()
                return
            }

            if (value.type == null) {
                node.propagateTypeError()
                return
            }

            // If the return type is not defined, we infer from the first return
            if (code.returnType == null) {
                code.returnType = value.type
            }

            attemptResolveUnresolvedTypes(
                node.span,
                listOf(code.returnType!!),
                listOf(value.type!!),
                code
            )

            if (!canBeAssignedTo(code.returnType!!, value.type!!)) {
                collector.report(
                    "Type mismatch, attempt to return '${value.type}' on a function that must return '${code.returnType}'",
                    node.span
                )
            }
        }
    }
}

fun ParserCtx.commonType(a: TypeTree, b: TypeTree): TypeTree {
    val base = when {
        // Same type
        a.base == b.base -> a.base
        a.isUnresolved() -> b.base
        b.isUnresolved() -> a.base
        a.isInvalid() -> b.base
        b.isInvalid() -> a.base

        // Option and Option Item
        a.base is OptionType && b.base is StructType
                && b.base.struct.ref in a.base.option.items -> a.base

        // Option Item and Option
        b.base is OptionType && a.base is StructType
                && a.base.struct.ref in b.base.option.items -> b.base

        // Option Item and Option Item
        b.base is StructType && a.base is StructType
                && a.base.struct.parentOption != null
                && a.base.struct.parentOption == b.base.struct.parentOption -> {

            OptionType(program.options[b.base.struct.parentOption]!!)
        }

        else -> InvalidType
    }

    val params = a.params.zip(b.params).map { commonType(it.first, it.second) }

    return TypeTree(base, params)
}

fun ParserCtx.multiCommonType(types: List<TypeTree>): TypeTree {
    return when (types.size) {
        0 -> TypeTree(InvalidType)
        1 -> types[0]
        2 -> commonType(types[0], types[1])
        else -> {
            var base = types[0]

            repeat(types.size - 1) {
                base = commonType(base, types[it + 1])
            }

            base
        }
    }
}

fun ParserCtx.canBeAssignedTo(defined: TypeTree, used: TypeTree): Boolean {
    if (defined == used) {
        return true
    }

    // Check base
    if (!baseCanBeAssignedTo(defined.base, used.base)) {
        return false
    }

    // Check params
    if (defined.params.size != used.params.size) {
        return false
    }

    repeat(defined.params.size) {
        if (!canBeAssignedTo(defined.params[it], used.params[it])) {
            return false
        }
    }

    // Everything ok
    return true
}

private fun baseCanBeAssignedTo(defined: TypeBase, used: TypeBase): Boolean = when {
    // Common case, same type
    defined == used -> true

    // Expected Option, found Struct
    // let a: Optional<Int> = Optional::None $[]
    defined is OptionType && used is StructType && used.struct.ref in defined.option.items -> true

    // No match
    else -> false
}

fun ParserCtx.findStructReplacements(
    span: Span,
    struct: LstStruct,
    instanceType: TypeTree
): Map<TypeParamRef, TypeTree> {
    val replacements = mutableMapOf<TypeParamRef, TypeTree>()

    extractTypeReplacements(
        span,
        struct.typeParameters.map { it.toTypeTree() },
        instanceType.params,
        replacements
    )

    return replacements
}

/**
 * Attempt to solve unresolved types
 *
 * First argument is the one being modified, the second is used to determine what value should be used
 *
 * Example:
 *  $1 = alloc List<Unresolved(1)>
 *  $2 = 1 as Int
 *  $3   FunArg($1)     // => List<Unresolved(1)>
 *  $3   FunArg($2)     // => Int
 *  $3 = FunCall("add") // => List<#Item> -> #Item -> Nothing
 *  :
 *  List<Unresolved(1)> vs List<#Item>1
 *  Int vs #Item
 *  =>
 *  Unresolved(1) => Int
 */
fun ParserCtx.attemptResolveUnresolvedTypes(
    span: Span,
    concreteTypeParams: List<TypeTree>,
    concreteArgTypes: List<TypeTree>,
    code: LstCode
) {
    val resolvedTypes = mutableMapOf<UnresolvedTypeRef, TypeTree>()

    extractUnresolvedReplacements(span, concreteArgTypes, concreteTypeParams, resolvedTypes)

    if (resolvedTypes.isNotEmpty()) {
        replaceAllUnresolved(code, resolvedTypes)
    }
}

fun ParserCtx.extractUnresolvedReplacements(
    span: Span,
    defined: List<TypeTree>,
    used: List<TypeTree>,
    replacements: MutableMap<UnresolvedTypeRef, TypeTree>
) {
    for ((defParam, usedParam) in defined.zip(used)) {
        if (defParam.base !is UnresolvedType) {
            extractUnresolvedReplacements(span, defParam.params, usedParam.params, replacements)
            continue
        }

        val ref = defParam.base.ref
        val prev = replacements[ref]

        if (prev != null && prev != usedParam) {
            collector.report("Type conflict, '$ref': '$prev' vs '$usedParam'", span)
        }

        replacements[ref] = usedParam
    }
}

fun ParserCtx.findFuncReplacements(
    node: LstFunCall,
    function: LstFunction,
    args: List<LstExpression>
): MutableMap<TypeParamRef, TypeTree> {
    val replacements = mutableMapOf<TypeParamRef, TypeTree>()

    if (node.specifiedTypeParams.size > function.typeParameters.size) {
        collector.report(
            "Function '${function.name}' expects ${function.typeParameters.size} type parameters, " +
                    "but ${node.specifiedTypeParams.size} where provided",
            node.span
        )
    }

    node.specifiedTypeParams.zip(function.typeParameters).forEach { (usage, funTyParam) ->
        val type = typeUsageToTypeTree(usage)

        replacements[funTyParam.ref] = type
    }

    val argsTypes = args.map { it.type!! }

    extractTypeReplacements(
        node.span,
        function.params.map { it.type!! },
        argsTypes,
        replacements
    )

    return replacements
}

fun ParserCtx.extractTypeReplacements(
    span: Span,
    defined: List<TypeTree>,
    used: List<TypeTree>,
    replacements: MutableMap<TypeParamRef, TypeTree>
) {

    for ((defParam, usedParam) in defined.zip(used)) {
        if (defParam.base !is ParamType) {
            extractTypeReplacements(span, defParam.params, usedParam.params, replacements)
            continue
        }

        var newReplacement = usedParam
        val ref = defParam.base.param.ref
        val prev = replacements[ref]

        if (prev != null && !prev.hasUnresolved() && !newReplacement.hasUnresolved()) {
            // TODO: BUG #[Ordering::Equals $[], Ordering::Greater $[]]
            val common = commonType(prev, newReplacement)

            if (common.isInvalid() || !canBeAssignedTo(common, newReplacement)) {
                collector.report(
                    "Type conflict, multiple conflicting alternatives to resolve '${defParam.base.param}', " +
                            "'$prev' vs '$newReplacement'", span
                )
                continue
            } else {
                newReplacement = common
            }
        }

        replacements[ref] = newReplacement
    }
}

fun ParserCtx.collectAllTypeParams(type: TypeTree, result: MutableSet<TypeParamRef>) {
    if (type.base is ParamType) {
        result += type.base.param.ref
    }

    type.params.forEach { collectAllTypeParams(it, result) }
}

fun ParserCtx.replaceTypeParams(type: TypeTree, replacements: Map<TypeParamRef, TypeTree>): TypeTree {
    if (type.base is ParamType) {
        return replacements[type.base.param.ref] ?: type
    }

    return TypeTree(
        base = type.base,
        params = type.params.map { replaceTypeParams(it, replacements) }
    )
}

fun ParserCtx.replaceAllUnresolved(code: LstCode, replacements: Map<UnresolvedTypeRef, TypeTree>) {
    code.variables.values.forEach { variable ->
        variable.type = variable.type?.let { replaceUnresolved(it, replacements) }
    }

    code.nodes.filterIsInstance<LstExpression>().forEach { expr ->
        expr.mapTypes { replaceUnresolved(it, replacements) }
    }
}

fun ParserCtx.replaceUnresolved(type: TypeTree, replacements: Map<UnresolvedTypeRef, TypeTree>): TypeTree {
    if (type.base is UnresolvedType) {
        return replacements[type.base.ref] ?: type
    }

    return TypeTree(
        base = type.base,
        params = type.params.map { replaceUnresolved(it, replacements) }
    )
}

fun ParserCtx.functionParamsSimilarity(params: List<TypeTree>, args: List<TypeTree>): Float {
    var total = 1000000f

    if (params.size != args.size) {
        total -= 100000f
    }

    for ((param, arg) in params.zip(args)) {
        total += typeSimilarity(param, arg) - 10000f
    }

    return total
}

fun ParserCtx.typeSimilarity(param: TypeTree, arg: TypeTree): Float {
    var total = 10000f

    if (param.base != arg.base) {
        total -= 10000f
    }

    if (param.params.size != arg.params.size) {
        total -= 1000f
    }

    param.params.zip(arg.params).forEach { (p, a) ->
        total += (typeSimilarity(p, a) - 10000f) / 100f
    }

    return total
}
