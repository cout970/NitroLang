package nitrolang.parsing

import nitrolang.ast.MODULE_SEPARATOR
import nitrolang.ast.TypeParameter
import nitrolang.ast.TypeUsage
import nitrolang.gen.MainParser

const val THIS_TYPE = "This"

fun ParserCtx.resolveTypeUsage(ctx: MainParser.TypeUsageContext): TypeUsage {

    if (ctx.typeParameter() != null) {
        val name = ctx.typeParameter().nameToken().text
        val typeParameterRef = typeParamMap.getOrPut(name) {
            if (!allowTypeParamCollection) {
                collector.report(
                    "Found undefined type parameter '${name}'",
                    ctx.typeParameter().span()
                )
            }

            TypeParameter(
                span = ctx.typeParameter().nameToken().span(),
                name = name,
                ref = program.nextTypeParamRef()
            )
        }

        return TypeUsage(
            span = ctx.typeParameter().nameToken().span(),
            name = name,
            path = "",
            sub = emptyList(),
            modifier = TypeUsage.Modifier.NONE,
            typeParameter = typeParameterRef,
            currentPath = currentPath(ctx)
        )
    }

    if (ctx.THIS_TYPE() != null) {
        return TypeUsage(
            span = ctx.span(),
            name = THIS_TYPE,
            path = "",
            sub = emptyList(),
            modifier = TypeUsage.Modifier.NONE,
            typeParameter = null,
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

fun ParserCtx.startTypeParams(ctx: MainParser.TypeParamDefContext?) {
    typeParamMap.clear()
    allowTypeParamCollection = true

    ctx?.typeParameter()?.forEach {
        val td = TypeParameter(
            span = it.nameToken().span(),
            name = it.nameToken().text,
            ref = program.nextTypeParamRef()
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

fun ParserCtx.endTypeParams(): List<TypeParameter> {
    allowTypeParamCollection = false
    return typeParamMap.values.toList()
}