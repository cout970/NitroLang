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
    program.structs[struct.ref] = struct
}

fun ParserCtx.processOptionDefinition(ctx: MainParser.OptionDefinitionContext) {
    startTypeParams(ctx.typeParamsDef())

    val mutableTypeParametersList = mutableListOf<LstTypeParameterDef>()
    val options = mutableListOf<LstStruct>()
    val optionRef = program.nextOptionRef()

    ctx.optionDefinitionItem().forEach { opt ->

        var index = 0
        val fields = opt.structBody()?.structField()?.map { fieldCtx ->
            LstStructField(
                span = fieldCtx.nameToken().span(),
                name = fieldCtx.nameToken().text,
                index = index++,
                typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
                ref = program.nextFieldRef()
            )
        } ?: emptyList()

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

        struct.parentOption = optionRef
        program.definedNames[struct.fullName] = struct.span
        program.structs[struct.ref] = struct
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
        ref = optionRef,
    )

    if (option.fullName in program.definedNames) {
        val prev = program.definedNames[option.fullName]
        collector.report("Redeclaration of ${option.fullName}, previously defined at $prev", option.span)
        return
    }

    program.definedNames[option.fullName] = option.span
    program.options[option.ref] = option
}

fun ParserCtx.processFunctionHeader(ctx: MainParser.FunctionHeaderContext): LstFunction {
    startTypeParams(ctx.typeParamsDef())

    val params = mutableListOf<LstFunctionParam>()
    val body = LstCode()
    var hasReceiver = false
    var index = 0

    if (ctx.functionReceiver() != null) {
        val param = LstFunctionParam(
            span = ctx.functionReceiver().typeUsage().span(),
            name = SELF_NAME,
            index = index++,
            typeUsage = resolveTypeUsage(ctx.functionReceiver().typeUsage()),
        )
        params += param
        hasReceiver = true

        val variable = LstVar(
            span = param.span,
            name = param.name,
            block = body.currentBlock,
            typeUsage = param.typeUsage,
            validAfter = body.currentRef(),
            ref = body.nextVarRef()
        )

        variable.isParam = true
        param.variable = variable
        body.variables[variable.ref] = variable
    }

    val returnTypeUsage: TypeUsage = if (ctx.functionReturnType() != null) {
        resolveTypeUsage(ctx.functionReturnType().typeUsage())
    } else {
        TypeUsage.nothing()
    }

    ctx.functionParameter().forEach { rawParam ->
        val param = LstFunctionParam(
            span = rawParam.nameToken().span(),
            name = rawParam.nameToken().text,
            index = index++,
            typeUsage = resolveTypeUsage(rawParam.typeUsage()),
        )
        params += param

        val variable = LstVar(
            span = param.span,
            name = param.name,
            block = body.currentBlock,
            typeUsage = param.typeUsage,
            validAfter = body.currentRef(),
            ref = body.nextVarRef()
        )

        variable.isParam = true
        param.variable = variable
        body.variables[variable.ref] = variable
    }

    val typeParameters = endTypeParams()

    val annotations = resolveAnnotations(ctx)
    val func = LstFunction(
        span = ctx.declaredNameToken().span(),
        name = ctx.declaredNameToken().text,
        path = currentPath(ctx),
        hasReceiver = hasReceiver,
        params = params,
        returnTypeUsage = returnTypeUsage,
        typeParameters = typeParameters,
        body = body,
        annotations = annotations,
        ref = program.nextFunctionRef()
    )

    program.functions[func.ref] = func
    return func
}

fun ParserCtx.processFunctionDefinition(ctx: MainParser.FunctionDefinitionContext): LstFunction {
    val header = ctx.functionHeader()
    val func = processFunctionHeader(header)

    code = func.body
    when {
        ctx.functionBody().statementBlock() != null -> {
            processStatementBlock(ctx.functionBody().statementBlock())
        }

        ctx.functionBody().expression() != null -> {
            func.body.lastExpression = processExpression(ctx.functionBody().expression())
            func.hasExpressionBody = true
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }

    return func
}

fun ParserCtx.processConstDefinition(ctx: MainParser.ConstDefinitionContext) {
    val body = LstCode()
    code = body
    body.lastExpression = processExpression(ctx.expression())

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
    program.consts[const.ref] = const
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

    program.tags[tag.ref] = tag
}