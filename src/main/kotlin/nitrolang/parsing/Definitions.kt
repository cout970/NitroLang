package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.backend.ConstString
import nitrolang.gen.MainParser

fun ParserCtx.processStructDefinition(ctx: MainParser.StructDefinitionContext) {
    startTypeParams(ctx.typeParamsDef())

    var index = 0
    val fields = ctx.structBody().structField().map { fieldCtx ->
        LstStructField(
            span = fieldCtx.anyName().span(),
            name = fieldCtx.anyName().text,
            index = index++,
            typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
            ref = program.nextFieldRef()
        )
    }

    val typeParameters = endTypeParams()

    val struct = LstStruct(
        span = ctx.upperName().span(),
        name = ctx.upperName().text,
        path = currentPath(ctx),
        fields = fields.associateBy { it.ref },
        typeParameters = typeParameters,
        annotations = resolveAnnotations(ctx),
        ref = program.nextStructRef(),
    )

    if (struct.fullName in program.definedNames) {
        val prev = program.definedNames[struct.fullName]
        collector.report("Redeclaration of ${struct.fullName}, previously defined at $prev", struct.span)
        return
    }

    program.definedNames[struct.fullName] = struct.span
    program.structs += struct
}

fun ParserCtx.processOptionDefinition(ctx: MainParser.OptionDefinitionContext) {
    startTypeParams(ctx.typeParamsDef())

    val mutableTypeParametersList = mutableListOf<LstTypeParameter>()
    val options = mutableListOf<LstStruct>()

    ctx.optionDefinitionItem().forEach { opt ->

        var index = 0
        val fields = mutableListOf<LstStructField>()
        // Field to discriminate between the option's items
        fields += LstStructField(
            span = opt.upperName().span(),
            name = VARIANT_FIELD_NAME,
            index = index++,
            typeUsage = LstTypeUsage.int(),
            ref = program.nextFieldRef()
        )

        opt.structBody()?.structField()?.forEach { fieldCtx ->
            fields += LstStructField(
                span = fieldCtx.anyName().span(),
                name = fieldCtx.anyName().text,
                index = index++,
                typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
                ref = program.nextFieldRef()
            )
        }

        var path = currentPath(ctx)

        if (path != "") {
            path += MODULE_SEPARATOR
        }

        val struct = LstStruct(
            span = opt.upperName().span(),
            name = opt.upperName().text,
            path = path + ctx.upperName().text,
            fields = fields.associateBy { it.ref },
            typeParameters = mutableTypeParametersList,
            annotations = emptyList(),
            ref = program.nextStructRef(),
        )

        if (struct.fullName in program.definedNames) {
            val prev = program.definedNames[struct.fullName]
            collector.report("Redeclaration of ${struct.fullName}, previously defined at $prev", struct.span)
            return
        }

        program.definedNames[struct.fullName] = struct.span
        program.structs += struct
        options += struct
    }

    // Type parameters are shared between the option's items and the option type
    mutableTypeParametersList.addAll(endTypeParams())

    val option = LstOption(
        span = ctx.upperName().span(),
        name = ctx.upperName().text,
        path = currentPath(ctx),
        itemsRef = options.map { it.ref },
        items = options,
        typeParameters = mutableTypeParametersList,
        annotations = resolveAnnotations(ctx),
        ref = program.nextOptionRef(),
    )

    options.forEach { it.parentOption = option }

    if (option.fullName in program.definedNames) {
        val prev = program.definedNames[option.fullName]
        collector.report("Redeclaration of ${option.fullName}, previously defined at $prev", option.span)
        return
    }

    program.definedNames[option.fullName] = option.span
    program.options += option
}

fun ParserCtx.processFunctionHeader(ctx: MainParser.FunctionHeaderContext): LstFunction {
    startTypeParams(ctx.typeParamsDef())

    val params = mutableListOf<LstFunctionParam>()
    val body = LstCode()
    var hasReceiver = false
    var index = 0

    if (ctx.functionReceiver() != null) {
        params += LstFunctionParam(
            span = ctx.functionReceiver().typeUsage().span(),
            name = SELF_NAME,
            index = index++,
            typeUsage = resolveTypeUsage(ctx.functionReceiver().typeUsage()),
        ).apply { createVariable(body) }
        hasReceiver = true
    }

    val returnTypeUsage: LstTypeUsage = if (ctx.functionReturnType() != null) {
        resolveTypeUsage(ctx.functionReturnType().typeUsage())
    } else {
        LstTypeUsage.nothing()
    }

    ctx.functionParameter().forEach { rawParam ->
        params += LstFunctionParam(
            span = rawParam.anyName().span(),
            name = rawParam.anyName().text,
            index = index++,
            typeUsage = resolveTypeUsage(rawParam.typeUsage()),
        ).apply { createVariable(body) }
    }

    val typeParameters = endTypeParams()
    val annotations = resolveAnnotations(ctx)
    var path = currentPath(ctx)

    if (ctx.modulePath() != null) {
        val subPath = ctx.modulePath()
            .anyName()
            .joinToString(MODULE_SEPARATOR) { it.text }

        path = if (path.isNotEmpty()) path + MODULE_SEPARATOR + subPath else subPath
    }

    val func = LstFunction(
        span = ctx.anyName().span(),
        name = ctx.anyName().text,
        path = path,
        hasReceiver = hasReceiver,
        params = params,
        returnTypeUsage = returnTypeUsage,
        typeParameters = typeParameters,
        body = body,
        annotations = annotations,
        ref = program.nextFunctionRef()
    )

    program.functions += func
    return func
}

fun ParserCtx.processFunctionDefinition(ctx: MainParser.FunctionDefinitionContext): LstFunction {
    val header = ctx.functionHeader()
    val func = processFunctionHeader(header)

    code = func.body
    when {
        ctx.functionBody() == null -> Unit

        ctx.functionBody().statementBlock() != null -> {
            processStatementBlock(ctx.functionBody().statementBlock())
        }

        ctx.functionBody().expression() != null -> {
            func.hasExpressionBody = true

            val expr = processExpression(ctx.functionBody().expression())
            if (func.returnTypeUsage.fullName != "Nothing") {
                func.body.lastExpression = func.body.returnExpr(ctx.functionBody().expression().span(), expr)
            }
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
    if (code.blockStack.isNotEmpty()) {
        error("Block stack is not empty, function: ${func.fullName}")
    }
    code.executeDeferredActions()
    return func
}

fun ParserCtx.processConstDefinition(ctx: MainParser.ConstDefinitionContext) {
    val body = LstCode()
    code = body
    body.lastExpression = processExpression(ctx.expression())
    code.executeDeferredActions()

    var path = currentPath(ctx)

    if (ctx.modulePath() != null) {
        val subPath = ctx.modulePath()
            .anyName()
            .joinToString(MODULE_SEPARATOR) { it.text }

        path = if (path.isNotEmpty()) path + MODULE_SEPARATOR + subPath else subPath
    }

    val const = LstConst(
        span = ctx.anyName().span(),
        name = ctx.anyName().text,
        path = path,
        typeUsage = resolveTypeUsage(ctx.typeUsage()),
        body = body,
        annotations = resolveAnnotations(ctx),
        ref = program.nextConstRef(),
    )

    if (const.fullName in program.definedNames) {
        val prev = program.definedNames[const.fullName]
        collector.report("Redeclaration of ${const.fullName}, previously defined at $prev", const.span)
        return
    }

    program.definedNames[const.fullName] = const.span
    program.consts += const
}

fun ParserCtx.processIncludeDefinition(ctx: MainParser.IncludeDefinitionContext) {
    val location = processPlainString(ctx.PLAIN_STRING())
    val namespace = if (location.contains(':')) location.substringBefore(':') else ""
    val path = location.substringAfter(':')

    AstParser.includeFile(namespace, path, program, source)
}

fun ParserCtx.processTagDefinition(ctx: MainParser.TagDefinitionContext) {
    val annotations = resolveAnnotations(ctx)
    val headers = mutableMapOf<String, LstFunction>()
    val tagName = ctx.upperName().text

    currentTagName = tagName

    ctx.tagDefinitionFunction().forEach { funHeader ->
        val header = funHeader.functionHeader()
        val name = header.anyName().text

        if (name in headers) {
            collector.report("Duplicated function name: '$name'", header.anyName().span())
        }

        val func = processFunctionHeader(header)
        headers[name] = func
    }

    currentTagName = null

    val tag = LstTag(
        span = ctx.upperName().span(),
        name = tagName,
        path = currentPath(ctx),
        annotations = annotations,
        headers = headers,
        ref = program.nextTagRef()
    )

    headers.values.forEach { it.tag = tag }

    if (tag.fullName in program.definedNames) {
        val prev = program.definedNames[tag.fullName]
        collector.report("Redeclaration of ${tag.fullName}, previously defined at $prev", tag.span)
        return
    }

    program.tags += tag
}

fun ParserCtx.processTypeAliasDefinition(ctx: MainParser.TypeAliasDefinitionContext) {

    startTypeParams(ctx.typeParamsDef())
    val typeUsage = resolveTypeUsage(ctx.typeUsage())
    val typeParameters = endTypeParams()

    val alias = LstTypeAlias(
        span = ctx.upperName().span(),
        name = ctx.upperName().text,
        path = currentPath(ctx),
        typeParameters = typeParameters,
        typeUsage = typeUsage,
        annotations = resolveAnnotations(ctx)
    )

    if (alias.fullName in program.definedNames) {
        val prev = program.definedNames[alias.fullName]
        collector.report("Redeclaration of ${alias.fullName}, previously defined at $prev", alias.span)
        return
    }

    program.definedNames[alias.fullName] = alias.span
    program.typeAliases += alias
}

fun ParserCtx.processEnumDefinition(ctx: MainParser.EnumDefinitionContext) {
    // enum Direction {
    //    Up      $[name: "up"]
    //    Down    $[name: "down"]
    //    Left    $[name: "left"]
    //    Right   $[name: "right"]
    //    Front   $[name: "front"]
    //    Back    $[name: "back"]
    //
    //    let name: String
    // }
    // to
    // struct Direction {
    //    let name: String
    // }
    // mod Direction {
    //    let Up: Direction = Direction $[name: "up"]
    //    let Down: Direction = Direction $[name: "down"]
    //    let Left: Direction = Direction $[name: "left"]
    //    let Right: Direction = Direction $[name: "right"]
    //    let Front: Direction = Direction $[name: "front"]
    //    let Back: Direction = Direction $[name: "back"]
    //
    //    // Autogenerated:
    //    fun values(): List<Direction> = #[Up, Down, Left, Right, Front, Back]
    //    fun from_variant(index: Int): Optional<Direction> {}
    //    fun Direction.to_string(): String {}
    // }

    val name = ctx.upperName().text
    val path = currentPath(ctx)
    val enumFullName = createPath(path, name)

    // Check if the enum has already been defined
    val span = ctx.upperName().span()
    if (enumFullName in program.definedNames) {
        val prev = program.definedNames[enumFullName]
        collector.report("Redeclaration of ${enumFullName}, previously defined at $prev", span)
        return
    }

    // Create struct
    var index = 0
    val fields = mutableListOf<LstStructField>()
    fields += LstStructField(
        span = span,
        name = VARIANT_FIELD_NAME,
        index = index++,
        typeUsage = LstTypeUsage.int(),
        ref = program.nextFieldRef()
    )

    ctx.enumFields()?.enumField()?.forEach { fieldCtx ->
        fields += LstStructField(
            span = fieldCtx.span(),
            name = fieldCtx.anyName().text,
            index = index++,
            typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
            ref = program.nextFieldRef()
        )
    }

    val struct = LstStruct(
        span = span,
        name = name,
        path = path,
        fields = fields.associateBy { it.ref },
        typeParameters = emptyList(),
        annotations = emptyList(),
        ref = program.nextStructRef(),
    )

    program.definedNames[struct.fullName] = struct.span
    program.structs += struct

    // Create constants for enum values
    val enumConstants = mutableMapOf<String, LstConst>()
    val tu = LstTypeUsage(
        span = span,
        name = name,
        path = path,
        sub = mutableListOf(),
        typeParameter = null,
        currentPath = path
    )

    ctx.enumValue().forEachIndexed { valueIndex, constCtx ->
        val constSpan = constCtx.span()
        val constName = constCtx.anyName().text
        val const = LstConst(
            span = constCtx.span(),
            name = constName,
            path = enumFullName,
            typeUsage = tu,
            body = LstCode(),
            annotations = emptyList(),
            ref = program.nextConstRef(),
        )

        const.enumVariant = valueIndex
        this.code = const.body

        val instance = code.alloc(constCtx.span(), tu, struct = struct) {
            isEnumInstanceInit = true
        }

        val allFields = struct.fields.values.map { it.name }.toMutableSet()

        // Variant field
        run {
            val expr = code.int(constSpan, const.enumVariant!!)

            val field = struct.fields.values.find { it.name == VARIANT_FIELD_NAME }!!
            allFields.remove(VARIANT_FIELD_NAME)

            code.storeField(
                constSpan,
                instance,
                VARIANT_FIELD_NAME,
                expr,
                struct = struct,
                field = field,
            )
        }

        // Rest of the fields
        constCtx.enumValueInit()?.forEach { initCtx ->
            val expr = processExpression(initCtx.expression())
            val fieldName = initCtx.anyName().text
            val field = struct.fields.values.find { it.name == fieldName }

            if (field == null) {
                collector.report("Field $fieldName not found in enum $name", initCtx.span())
                return
            }

            allFields.remove(fieldName)

            code.storeField(
                initCtx.span(),
                instance,
                fieldName,
                expr,
                struct = struct,
                field = field,
            )
        }

        if (allFields.isNotEmpty()) {
            collector.report("Missing fields in enum $name: ${allFields.joinToString(", ")}", constCtx.span())
            return
        }

        code.lastExpression = code.returnExpr(constCtx.span(), instance)

        program.definedNames[const.fullName] = const.span
        program.consts += const
        enumConstants[constName] = const
    }

    struct.enumConstants = enumConstants

    // Create auto-generated functions

    // fun values(): List<Direction> = #[Up, Down, Left, Right, Front, Back]
    val valuesFunc = LstFunction(
        span = span,
        name = "values",
        path = enumFullName,
        hasReceiver = false,
        params = emptyList(),
        returnTypeUsage = LstTypeUsage.list(tu),
        typeParameters = emptyList(),
        body = LstCode(),
        annotations = mutableListOf(
            LstAnnotation(
                span = span,
                name = ANNOTATION_AUTO_GENERATED
            )
        ),
        ref = program.nextFunctionRef()
    )
    program.functions += valuesFunc

    val values = valuesFunc.body
    val newList = values.call(span, "List", "new", explicitTypeParams = listOf(tu))

    ctx.enumValue().forEach { valueCtx ->
        val constName = valueCtx.anyName().text
        val const = enumConstants[constName]!!
        val loadConst = values.loadConst(span, enumFullName, constName, const)

        values.call(span, "", "add", listOf(newList, loadConst))
    }

    values.lastExpression = values.returnExpr(span, newList)

    // fun Direction.to_string(): String {
    //     if (this.variant == Direction::Up.variant) {
    //         return "Up"
    //     }
    //     if (this.variant == Direction::Down.variant) {
    //         return "Down"
    //     }
    //     // ... Rest of cases
    //     unreachable()
    // }
    val toString = LstCode()
    program.functions += LstFunction(
        span = span,
        name = "to_string",
        path = enumFullName,
        hasReceiver = true,
        params = listOf(
            LstFunctionParam(
                span = span,
                name = SELF_NAME,
                index = 0,
                typeUsage = tu,
            ).apply { createVariable(toString) }
        ),
        returnTypeUsage = LstTypeUsage.string(),
        typeParameters = emptyList(),
        body = toString,
        annotations = mutableListOf(
            LstAnnotation(
                span = span,
                name = ANNOTATION_AUTO_GENERATED
            )
        ),
        ref = program.nextFunctionRef()
    )

    enumConstants.forEach { (variantName, const) ->
        val loadSelf = toString.loadVar(span, "", SELF_NAME)
        val loadSelfVariant = toString.loadField(span, loadSelf, VARIANT_FIELD_NAME)
        val loadConst = toString.loadConst(span, enumFullName, variantName, const)
        val loadConstVariant = toString.loadField(span, loadConst, VARIANT_FIELD_NAME)

        val cond = toString.call(
            span, "", "is_equal",
            listOf(loadSelfVariant, loadConstVariant)
        )

        toString.ifStart(span, cond)
        toString.returnExpr(span, toString.string(span, variantName))
        toString.ifEnd(span)
    }

    toString.lastExpression = toString.call(span, "", "unreachable")


    // fun from_variant(index: Int): Optional<Direction> {
    //    if (index == 1) {
    //       return Optional::Some(Up)
    //    }
    //    ...
    //    return Optional::None
    //  }
    val fromVariant = LstCode()
    program.functions += LstFunction(
        span = span,
        name = "from_variant",
        path = enumFullName,
        hasReceiver = false,
        params = listOf(
            LstFunctionParam(
                span = span,
                name = "index",
                index = 0,
                typeUsage = LstTypeUsage.int(),
            ).apply { createVariable(fromVariant) }
        ),
        returnTypeUsage = LstTypeUsage.optional(tu),
        typeParameters = emptyList(),
        body = fromVariant,
        annotations = mutableListOf(
            LstAnnotation(
                span = span,
                name = ANNOTATION_AUTO_GENERATED
            )
        ),
        ref = program.nextFunctionRef()
    )

    enumConstants.forEach { (variantName, const) ->
        val loadIndex = fromVariant.loadVar(span, "", "index")
        val variantValue = fromVariant.int(span, const.enumVariant!!)
        val cond = fromVariant.call(span, "", "is_equal", listOf(loadIndex, variantValue))

        fromVariant.ifStart(span, cond)
        val loadConst = fromVariant.loadConst(span, enumFullName, variantName, const)
        val call = fromVariant.call(span, "", "Some", listOf(loadConst))
        fromVariant.returnExpr(span, call)
        fromVariant.ifEnd(span)
    }

    val none = fromVariant.call(span, "", "None")
    fromVariant.lastExpression = fromVariant.returnExpr(span, none)
}

fun ParserCtx.processTestDefinition(ctx: MainParser.TestDefinitionContext) {
    val span = ctx.span()
    val testName = processPlainString(ctx.PLAIN_STRING())

    val body = LstCode()
    code = body
    processStatementBlock(ctx.statementBlock())
    if (code.blockStack.isNotEmpty()) {
        error("Block stack is not empty, test: $testName")
    }
    code.executeDeferredActions()

    val testAnnotation = LstAnnotation(
        span = span,
        name = ANNOTATION_TEST,
        args = mapOf("name" to ConstString(testName))
    )

    val annotations = mutableListOf(testAnnotation)
    annotations.addAll(resolveAnnotations(ctx))

    val ref = program.nextFunctionRef()
    val testFunction = LstFunction(
        span = span,
        name = "test_${ref.id}",
        path = currentPath(ctx),
        body = body,
        hasReceiver = false,
        params = emptyList(),
        returnTypeUsage = LstTypeUsage.nothing(),
        typeParameters = emptyList(),
        annotations = annotations,
        ref = ref,
    )

    program.functions += testFunction
}