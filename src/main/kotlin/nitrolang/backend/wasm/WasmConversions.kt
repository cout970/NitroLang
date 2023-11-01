package nitrolang.backend.wasm

import nitrolang.ast.*
import nitrolang.backend.*
import nitrolang.typeinference.*
import kotlin.math.min

fun WasmBuilder.typeToMonoType(type: TType, ctx: MonoCtx): MonoType {
    if (type is TGeneric) {
        val replacement = ctx.generics[type.instance]
        if (replacement != null) return replacement

        if (ctx.parent != null) {
            return typeToMonoType(type, ctx.parent)
        }

        throw WasmUnresolvedGenericTypeError("No valid replacement for generic: $type", type)
    }

    if (type !is TComposite) {
        throw WasmUnresolvedGenericTypeError("Unable to convert type $type to MonoType", type)
    }

    val params = mutableListOf<MonoType>()

    for (param in type.params) {
        params += typeToMonoType(param, ctx)
    }

    val kind: Int = when (type.base) {
        is TOption -> 0
        is TOptionItem -> 1
        is TStruct -> 2
        is TLambda -> 3
    }

    val base = when (type.base) {
        is TOption -> getOptionType(type.base.instance, params, ctx)
        is TOptionItem -> getStructType(type.base.instance, params, ctx)
        is TStruct -> getStructType(type.base.instance, params, ctx)
        is TLambda -> getLambdaType(type.base.instance, params)
    }

    val typeSign = mutableListOf(kind, base.id)
    params.forEach { typeSign += it.id }

    if (typeSign !in typeIds) {
        val newType = MonoType(typeIds.size + 1, base, params)
        typeIds[typeSign] = newType
    }

    return typeIds[typeSign]!!
}

fun WasmBuilder.monoTypeToPattern(ty: MonoType): MonoTypePattern {
    val params = ty.params.map { monoTypeToPattern(it) }
    return MonoTypePattern(monoTypeBaseToBasePattern(ty.base), params)
}

fun WasmBuilder.monoTypeBaseToBasePattern(ty: MonoTypeBase?): MonoTypeBasePattern {
    if (ty == null) return MonoTypeBasePattern.PatternAny

    if (ty is MonoStruct) {
        return MonoTypeBasePattern.PatternStruct(ty.instance)
    }

    if (ty is MonoOption) {
        return MonoTypeBasePattern.PatternOption(ty.instance)
    }

    error("Invalid type: $ty")
}

fun WasmBuilder.patternToMonoTypePattern(pattern: LstTypePattern, ctx: MonoCtx): MonoTypePattern {
    if (pattern.generic != null) {
        val ty = typeToMonoType(pattern.generic!!, ctx)
        return monoTypeToPattern(ty)
    }

    val params = pattern.sub.map { patternToMonoTypePattern(it, ctx) }

    val patternBase = when (val base = pattern.base) {
        null -> MonoTypeBasePattern.PatternAny
        is TOption -> MonoTypeBasePattern.PatternOption(base.instance)
        is TStruct -> MonoTypeBasePattern.PatternStruct(base.instance)
        else -> error("Invalid type: $base")
    }

    return MonoTypePattern(patternBase, params)
}

fun monoTypeToPrimitive(mono: MonoType): List<WasmPrimitive> {
    return when (mono.base) {
        is MonoOption -> listOf(WasmPrimitive.i32)
        is MonoLambda -> listOf(WasmPrimitive.i32)
        is MonoStruct -> {
            val prim = if (mono.base.instance.fullName == "Float") WasmPrimitive.f32 else WasmPrimitive.i32
            listOf(prim)
        }
    }
}

fun WasmBuilder.funcTypeToWasm(mono: MonoType): String {
    if (!mono.isFunction() && !mono.isLambda()) error("Invalid type $mono")

    return buildString {
        mono.params.dropLast(1).forEach { p ->
            val prim = monoTypeToPrimitive(p)
            append("(param ")
            prim.forEachIndexed { index, it ->
                append(it)
                if (index != prim.size - 1) append(" ")
            }
            append(")")
            append(" ")
        }

        val prim = monoTypeToPrimitive(mono.params.last())
        append("(result ")
        prim.forEachIndexed { index, it ->
            append(it)
            if (index != prim.size - 1) append(" ")
        }
        append(")")
    }
}

fun WasmBuilder.getStructType(struct: LstStruct, params: List<MonoType>, ctx: MonoCtx): MonoStruct {

    val generics = mutableMapOf<LstTypeParameter, MonoType>()

    if (struct.typeParameters.size != params.size && !struct.isIntrinsic) {
        error("Invalid number of type parameters for type ${struct.fullName}")
    }

    repeat(min(struct.typeParameters.size, params.size)) {
        generics[struct.typeParameters[it]] = params[it]
    }

    val sign = mutableListOf(struct.ref.id)
    generics.values.forEach { sign += it.id }

    if (sign !in structIds) {
        val newCtx = MonoCtx(generics, ctx)
        val fields = toMonoStructFields(struct, newCtx)

        var size = 0
        fields.forEach {
            if (it.size >= 4) {
                size = pad(size)
            }
            size += it.size
        }

        if (size > 4) {
            size = pad(size)
        }

        structIds[sign] = MonoStruct(structIds.size + 1, struct, fields, size)
    }

    return structIds[sign]!!
}

fun WasmBuilder.getOptionType(option: LstOption, params: List<MonoType>, ctx: MonoCtx): MonoOption {
    val generics = mutableMapOf<LstTypeParameter, MonoType>()
    repeat(option.typeParameters.size) {
        generics[option.typeParameters[it]] = params[it]
    }

    val optionSign = mutableListOf(option.ref.id)
    generics.values.forEach { optionSign += it.id }

    if (optionSign !in optionIds) {
        val monoOption = MonoOption(optionIds.size + 1, option)
        optionIds[optionSign] = monoOption

        val newCtx = MonoCtx(generics, ctx)

        val items = option.items.map { struct ->
            getStructType(struct, params, newCtx)
        }

        monoOption.items = items
        monoOption.size = pad(items.maxOf { it.size })
        items.forEach { it.option = monoOption }
    }

    return optionIds[optionSign]!!
}

fun WasmBuilder.getLambdaType(lambda: LstLambdaFunction, params: List<MonoType>): MonoLambda {
    val sign = mutableListOf(lambda.ref.id)
    params.forEach { sign += it.id }

    if (sign !in lambdaIds) {
        val size = PTR_SIZE
        lambdaIds[sign] = MonoLambda(lambdaIds.size + 1, lambda, size)
    }

    return lambdaIds[sign]!!
}

fun WasmBuilder.toMonoStructFields(struct: LstStruct, ctx: MonoCtx): List<MonoStructField> {
    val fields = mutableListOf<MonoStructField>()
    var offset = 0

    struct.fields.values.forEach { field ->
        val fieldType = typeToMonoType(field.type, ctx)

        val size: Int = if (fieldType.isValueType()) fieldType.heapSize() else fieldType.stackSize()

        // Pad to 4 bytes
        if (size >= PTR_SIZE) {
            offset = pad(offset)
        }

        fields += MonoStructField(field.name, fieldType, size, offset)
        offset += size
    }

    return fields
}
