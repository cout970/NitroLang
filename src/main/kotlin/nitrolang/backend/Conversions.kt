package nitrolang.backend

import nitrolang.ast.*
import nitrolang.backend.wasm.PTR_SIZE
import nitrolang.backend.wasm.heapSize
import nitrolang.typeinference.*
import kotlin.math.min

fun MonoBuilder.typeToMonoType(type: TType, ctx: MonoCtx): MonoType {
    if (type is TGeneric) {
        val replacement = ctx.generics[type.instance]
        if (replacement != null) return replacement

        if (ctx.parent != null) {
            return typeToMonoType(type, ctx.parent)
        }

        throw UnresolvedGenericTypeError("No valid replacement for generic: $type", type)
    }

    if (type !is TComposite) {
        throw UnresolvedGenericTypeError("Unable to convert type $type to MonoType", type)
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

fun MonoBuilder.monoTypeToPattern(ty: MonoType): MonoTypePattern {
    val params = ty.params.map { monoTypeToPattern(it) }

    fun monoTypeBaseToBasePattern(ty: MonoTypeBase?): MonoTypeBasePattern {
        if (ty == null) return MonoTypeBasePattern.PatternAny

        if (ty is MonoStruct) {
            return MonoTypeBasePattern.PatternStruct(ty.instance)
        }

        if (ty is MonoOption) {
            return MonoTypeBasePattern.PatternOption(ty.instance)
        }

        error("Invalid type: $ty")
    }

    return MonoTypePattern(monoTypeBaseToBasePattern(ty.base), params)
}

fun MonoBuilder.patternToMonoTypePattern(pattern: LstTypePattern, ctx: MonoCtx): MonoTypePattern {
    if (pattern.generic != null) {
        val ty = typeToMonoType(pattern.generic!!, ctx)
        return monoTypeToPattern(ty)
    }

    val params = pattern.sub.map { patternToMonoTypePattern(it, ctx) }

    val patternBase = when (val base = pattern.base) {
        null -> MonoTypeBasePattern.PatternAny
        is TStruct -> MonoTypeBasePattern.PatternStruct(base.instance)
        is TOption -> MonoTypeBasePattern.PatternOption(base.instance)
        is TOptionItem -> MonoTypeBasePattern.PatternStruct(base.instance)
        else -> error("Invalid type: $base")
    }

    return MonoTypePattern(patternBase, params)
}

fun MonoBuilder.getStructType(struct: LstStruct, params: List<MonoType>, ctx: MonoCtx): MonoStruct {

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
        val monoStruct = MonoStruct(structIds.size + 1, struct)
        structIds[sign] = monoStruct

        monoStruct.fields += toMonoStructFields(struct, newCtx)

        var size = 0
        monoStruct.fields.forEach {
            if (it.size >= 4) {
                size = pad(size)
            }
            size += it.size
        }

        if (size > 4) {
            size = pad(size)
        }

        monoStruct.size = size
    }

    return structIds[sign]!!
}

fun MonoBuilder.getOptionType(option: LstOption, params: List<MonoType>, ctx: MonoCtx): MonoOption {
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

fun MonoBuilder.getLambdaType(lambda: LstLambdaFunction, params: List<MonoType>): MonoLambda {
    val sign = mutableListOf(lambda.ref.id)
    params.forEach { sign += it.id }

    if (sign !in lambdaIds) {
        // Function index
        var size = PTR_SIZE
        // Slots
        size += PTR_SIZE * lambda.body.outerVariables.size

        lambdaIds[sign] = MonoLambda(lambdaIds.size + 1, lambda, size)
    }

    return lambdaIds[sign]!!
}

fun MonoBuilder.toMonoStructFields(struct: LstStruct, ctx: MonoCtx): List<MonoStructField> {
    val fields = mutableListOf<MonoStructField>()
    var offset = 0

    struct.fields.values.forEach { field ->
        val fieldType = typeToMonoType(field.type, ctx)

        val size: Int = if (fieldType.isValueType()) fieldType.heapSize() else PTR_SIZE

        // Pad to 4 bytes
        if (size >= PTR_SIZE) {
            offset = pad(offset)
        }

        fields += MonoStructField(field.name, fieldType, size, offset)
        offset += size
    }

    return fields
}
