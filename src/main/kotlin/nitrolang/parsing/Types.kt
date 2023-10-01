package nitrolang.parsing

import nitrolang.ast.MODULE_SEPARATOR
import nitrolang.ast.LstTypeParameterDef
import nitrolang.ast.TypeUsage
import nitrolang.gen.MainParser

fun ParserCtx.resolveTypeUsage(ctx: MainParser.TypeUsageContext): TypeUsage {

    if (ctx.THIS_TYPE() != null) {
        val name = "This"
        val typeParameterDef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '${name}'",
                    ctx.typeParameter().span()
                )
            }

            val typeBound = TypeUsage(
                span = ctx.span(),
                name = currentTagName ?: error("Using `This` outside a tag definition"),
                path = "",
                sub = emptyList(),
                modifier = TypeUsage.Modifier.NONE,
                typeParameter = null,
                currentPath = currentPath(ctx)
            )

            LstTypeParameterDef(
                span = ctx.span(),
                name = name,
                ref = program.nextTypeParamRef(),
                bounds = listOf(typeBound)
            )
        }

        return TypeUsage(
            span = ctx.span(),
            name = name,
            path = "",
            sub = emptyList(),
            modifier = TypeUsage.Modifier.NONE,
            typeParameter = typeParameterDef,
            currentPath = currentPath(ctx)
        )
    }

    if (ctx.typeParameter() != null) {
        val name = ctx.typeParameter().nameToken().text
        val typeParameterDef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '${name}'",
                    ctx.typeParameter().span()
                )
            }

            LstTypeParameterDef(
                span = ctx.typeParameter().nameToken().span(),
                name = name,
                ref = program.nextTypeParamRef(),
                bounds = emptyList()
            )
        }

        return TypeUsage(
            span = ctx.typeParameter().nameToken().span(),
            name = name,
            path = "",
            sub = emptyList(),
            modifier = TypeUsage.Modifier.NONE,
            typeParameter = typeParameterDef,
            currentPath = currentPath(ctx)
        )
    }

    val base = ctx.baseTypeUsage()

    val modifier = if (base.refModifier() != null && base.refModifier().MUT() != null) {
        TypeUsage.Modifier.MUT
    } else if (base.refModifier() != null && base.refModifier().REF() != null) {
        TypeUsage.Modifier.REF
    } else {
        TypeUsage.Modifier.NONE
    }

    val name = base.nameToken().text
    var path = ""

    if (base.modulePath() != null) {
        path = base.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
    }

    val sub = if (base.typeParamArg() != null) {
        base.typeParamArg().typeUsage().map { resolveTypeUsage(it) }
    } else {
        listOf()
    }

    return TypeUsage(
        span = base.nameToken().span(),
        name = name,
        path = path,
        sub = sub,
        modifier = modifier,
        typeParameter = null,
        currentPath = currentPath(ctx)
    )
}

fun ParserCtx.startTypeParams(ctx: MainParser.TypeParamsDefContext?) {
    typeParamMap.clear()
    allowTypeParamCollection = true

    ctx?.typeParamDef()?.forEach { def ->
        val typeParam = def.typeParameter()
        val bounds = def.typeUsage().map { resolveTypeUsage(it) }

        val td = LstTypeParameterDef(
            span = typeParam.nameToken().span(),
            name = typeParam.nameToken().text,
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

fun ParserCtx.endTypeParams(): List<LstTypeParameterDef> {
    allowTypeParamCollection = false
    return typeParamMap.values.toList()
}