package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.gen.MainParser

fun ParserCtx.processStructDefinition(ctx: MainParser.StructDefinitionContext) {
    startTypeParams(ctx.typeParamsDef())

    var index = 0
    val fields = ctx.structBody().structField().map { fieldCtx ->
        LstStructField(
            span = fieldCtx.nameToken().span(),
            name = fieldCtx.nameToken().text,
            index = index++,
            typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
            ref = program.nextFieldRef()
        )
    }

    val typeParameters = endTypeParams()

    val struct = LstStruct(
        span = ctx.declaredNameToken().span(),
        name = ctx.declaredNameToken().text,
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
            span = opt.declaredNameToken().span(),
            name = "variant",
            index = index++,
            typeUsage = LstTypeUsage.int(),
            ref = program.nextFieldRef()
        )

        opt.structBody()?.structField()?.forEach { fieldCtx ->
            fields += LstStructField(
                span = fieldCtx.nameToken().span(),
                name = fieldCtx.nameToken().text,
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
            span = opt.declaredNameToken().span(),
            name = opt.declaredNameToken().text,
            path = path + ctx.declaredNameToken().text,
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
        span = ctx.declaredNameToken().span(),
        name = ctx.declaredNameToken().text,
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
            span = rawParam.nameToken().span(),
            name = rawParam.nameToken().text,
            index = index++,
            typeUsage = resolveTypeUsage(rawParam.typeUsage()),
        ).apply { createVariable(body) }
    }

    val typeParameters = endTypeParams()
    val annotations = resolveAnnotations(ctx)
    var path = currentPath(ctx)

    if (ctx.modulePath() != null) {
        val subPath = ctx.modulePath()
            .nameToken()
            .joinToString(MODULE_SEPARATOR) { it.text }

        path = if (path.isNotEmpty()) path + MODULE_SEPARATOR + subPath else subPath
    }

    val func = LstFunction(
        span = ctx.declaredNameToken().span(),
        name = ctx.declaredNameToken().text,
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
            func.body.lastExpression = processExpression(ctx.functionBody().expression())
            func.hasExpressionBody = true
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

    val const = LstConst(
        span = ctx.declaredNameToken().span(),
        name = ctx.declaredNameToken().nameToken().text,
        path = currentPath(ctx),
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
    val tagName = ctx.declaredNameToken().text

    currentTagName = tagName

    ctx.tagDefinitionFunction().forEach { funHeader ->
        val header = funHeader.functionHeader()
        val name = header.declaredNameToken().nameToken().text

        if (name in headers) {
            collector.report("Duplicated function name: '$name'", header.declaredNameToken().span())
        }

        val func = processFunctionHeader(header)
        headers[name] = func
    }

    currentTagName = null

    val tag = LstTag(
        span = ctx.declaredNameToken().span(),
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
        span = ctx.declaredNameToken().span(),
        name = ctx.declaredNameToken().text,
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