package nitrolang.parsing

import nitrolang.ast.MODULE_SEPARATOR
import nitrolang.ast.LstTypeParameter
import nitrolang.ast.LstTypePattern
import nitrolang.ast.LstTypeUsage
import nitrolang.gen.MainParser

fun ParserCtx.resolveTypeUsage(ctx: MainParser.TypeUsageContext): LstTypeUsage {

    if (ctx.THIS_TYPE() != null) {
        val name = "This"
        val typeParameterDef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '$name'",
                    ctx.typeParameter().span()
                )
            }

            val typeBound = LstTypeUsage(
                span = ctx.span(),
                name = currentTagName ?: error("Using `This` outside a tag definition"),
                path = "",
                sub = emptyList(),
                typeParameter = null,
                currentPath = currentPath(ctx)
            )

            LstTypeParameter(
                span = ctx.span(),
                name = name,
                ref = program.nextTypeParamRef(),
                bounds = listOf(typeBound)
            )
        }

        return LstTypeUsage(
            span = ctx.span(),
            name = name,
            path = "",
            sub = emptyList(),
            typeParameter = typeParameterDef,
            currentPath = currentPath(ctx)
        )
    }

    if (ctx.typeParameter() != null) {
        val name = ctx.typeParameter().upperName().text
        val typeParameterDef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '${name}'",
                    ctx.typeParameter().span()
                )
            }

            LstTypeParameter(
                span = ctx.typeParameter().upperName().span(),
                name = name,
                ref = program.nextTypeParamRef(),
                bounds = emptyList()
            )
        }

        return LstTypeUsage(
            span = ctx.typeParameter().upperName().span(),
            name = name,
            path = "",
            sub = emptyList(),
            typeParameter = typeParameterDef,
            currentPath = currentPath(ctx)
        )
    }

    if (ctx.functionTypeUsage() != null) {
        val func = ctx.functionTypeUsage()
        val sub = mutableListOf<LstTypeUsage>()

        // Receiver
        if (ctx.baseTypeUsage() != null) {
            sub += resolveBaseTypeUsage(ctx.baseTypeUsage())
        }

        // Arguments
        func.functionTypeUsageParam().forEach {
            sub += resolveTypeUsage(it.typeUsage())
        }

        // Return
        sub += resolveTypeUsage(func.functionTypeUsageReturn().typeUsage())

        return LstTypeUsage(
            span = func.span(),
            name = "Function",
            path = "",
            sub = sub,
            typeParameter = null,
            currentPath = currentPath(ctx)
        ).also {
            it.hasReceiver = ctx.baseTypeUsage() != null
        }
    }

    return resolveBaseTypeUsage(ctx.baseTypeUsage())
}

fun ParserCtx.resolveBaseTypeUsage(base: MainParser.BaseTypeUsageContext): LstTypeUsage {
    val name = base.upperName().text
    var path = ""

    if (base.modulePath() != null) {
        path = base.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
    }

    val sub = if (base.typeParamArg() != null) {
        base.typeParamArg().typeUsage().map { resolveTypeUsage(it) }
    } else {
        listOf()
    }

    return LstTypeUsage(
        span = base.upperName().span(),
        name = name,
        path = path,
        sub = sub,
        typeParameter = null,
        currentPath = currentPath(base)
    )
}

fun ParserCtx.startTypeParams(ctx: MainParser.TypeParamsDefContext?) {
    typeParamMap.clear()
    allowTypeParamCollection = true

    ctx?.typeParamDef()?.forEach { def ->
        val typeParam = def.typeParameter()
        val bounds = def.typeUsage().map { resolveTypeUsage(it) }

        val td = LstTypeParameter(
            span = typeParam.upperName().span(),
            name = typeParam.upperName().text,
            ref = program.nextTypeParamRef(),
            bounds = bounds,
        )

        if (td.name in typeParamMap) {
            val prev = typeParamMap[td.name]
            collector.report(
                "Name conflict, type parameter name ${td.name} is already in use at ${prev?.span}",
                td.span
            )
            return
        }
        typeParamMap[td.name] = td
    }
}

fun ParserCtx.endTypeParams(): List<LstTypeParameter> {
    allowTypeParamCollection = false
    return typeParamMap.values.toList()
}

fun ParserCtx.resolveTypePattern(ctx: MainParser.TypePatternContext): LstTypePattern {
    if (ctx.THIS_TYPE() != null) {
        val name = "This"
        val typeParameterDef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '$name'",
                    ctx.typeParameter().span()
                )
            }

            val typeBound = LstTypeUsage(
                span = ctx.span(),
                name = currentTagName ?: error("Using `This` outside a tag definition"),
                path = "",
                sub = emptyList(),
                typeParameter = null,
                currentPath = currentPath(ctx)
            )

            LstTypeParameter(
                span = ctx.span(),
                name = name,
                ref = program.nextTypeParamRef(),
                bounds = listOf(typeBound)
            )
        }

        return LstTypePattern(
            span = ctx.span(),
            name = name,
            path = "",
            sub = emptyList(),
            typeParameter = typeParameterDef,
            currentPath = currentPath(ctx)
        )
    }

    if (ctx.typeParameter() != null) {
        val name = ctx.typeParameter().upperName().text
        val typeParameterDef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '${name}'",
                    ctx.typeParameter().span()
                )
            }

            LstTypeParameter(
                span = ctx.typeParameter().upperName().span(),
                name = name,
                ref = program.nextTypeParamRef(),
                bounds = emptyList()
            )
        }

        return LstTypePattern(
            span = ctx.typeParameter().upperName().span(),
            name = name,
            path = "",
            sub = emptyList(),
            typeParameter = typeParameterDef,
            currentPath = currentPath(ctx)
        )
    }

    return resolveBaseTypePattern(ctx.baseTypePattern())
}

fun ParserCtx.resolveBaseTypePattern(base: MainParser.BaseTypePatternContext): LstTypePattern {
    val name = base.upperName().text
    var path = ""

    if (base.modulePath() != null) {
        path = base.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
    }

    val sub = if (base.typePatternArgs() != null) {
        base.typePatternArgs().typePatternArg().map {
            when {
                it.typePattern() != null -> resolveTypePattern(it.typePattern())
                it.MUL() != null -> {
                    LstTypePattern(
                        span = it.span(),
                        name = "*",
                        path = "",
                        sub = emptyList(),
                        typeParameter = null,
                        currentPath = currentPath(base),
                        isAny = true,
                    )
                }

                else -> error("Grammar has been expanded and parser is outdated")
            }
        }
    } else {
        listOf()
    }

    return LstTypePattern(
        span = base.upperName().span(),
        name = name,
        path = path,
        sub = sub,
        typeParameter = null,
        currentPath = currentPath(base)
    )
}
