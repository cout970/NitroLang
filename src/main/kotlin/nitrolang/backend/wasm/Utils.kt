package nitrolang.backend.wasm

import nitrolang.ast.LstFunction
import nitrolang.ast.LstTag
import nitrolang.ast.LstTypeParameter
import nitrolang.backend.MonoLambda
import nitrolang.backend.MonoOption
import nitrolang.backend.MonoStruct
import nitrolang.backend.MonoType
import nitrolang.typeinference.*

fun TType.isPolymorphic(): Boolean {
    if (this !is TComposite) return false
    for (param in this.params) {
        if (!param.isPolymorphic()) return false
    }
    return true
}

fun MonoType.stackSize(): Int = PTR_SIZE

fun MonoType.heapSize(): Int {
    return when (base) {
        is MonoOption -> base.size
        is MonoLambda -> base.size
        is MonoStruct -> {
            when (base.instance.fullName) {
                "Never" -> 0
                "Nothing" -> 0
                "Byte" -> 1
                "Boolean" -> 1
                "Short" -> 2
                "Char" -> 4
                "Int" -> 4
                "Float" -> 4
                "Ptr" -> 4
                "RawArray" -> 4
                else -> base.size
            }
        }
    }
}

fun intToWasmHex(num: Int): String {
    val hex = num.toString(16).padStart(8, '0')
    val a0 = hex.substring(0, 2)
    val a1 = hex.substring(2, 4)
    val a2 = hex.substring(4, 6)
    val a3 = hex.substring(6, 8)

    return "\\$a3\\$a2\\$a1\\$a0"
}

fun floatToWasmHex(num: Float): String {
    return intToWasmHex(num.toRawBits())
}

fun WasmBuilder.removeAllGenerics(type: TType): TType {
    return when (type) {
        is TGeneric -> program.typeEnv.find("Int")
        is TComposite -> program.typeEnv.composite(type.base, type.params.map(::removeAllGenerics))
        else -> type
    }
}


fun findTagImplementation(tag: LstTag, paramType: MonoType, fullName: String): Pair<TType, LstFunction>? {
    for ((ty, map) in tag.functionImplementations) {
        if (fullName in map && monoTypeMatches(ty, paramType)) {
            return ty to map[fullName]!!
        }
    }

    return null
}

fun monoTypeMatches(a: TType, b: MonoType): Boolean {
    if (a is TGeneric) return true

    if (a is TComposite) {
        if (a.params.size != b.params.size) return false

        repeat(a.params.size) {
            if (!monoTypeMatches(a.params[it], b.params[it])) return false
        }

        if (a.base is TStruct && b.base is MonoStruct && a.base.instance == b.base.instance) return true
        if (a.base is TOption && b.base is MonoOption && a.base.instance == b.base.instance) return true
        if (a.base is TOptionItem && b.base is MonoStruct && a.base.instance == b.base.instance) return true
    }
    return false
}

fun findReplacements(a: TType, b: MonoType, map: MutableMap<LstTypeParameter, MonoType>) {
    if (a is TGeneric) {
        map[a.instance] = b
        return
    }

    if (a !is TComposite) error("Invalid type: $a")
    if (a.params.size != b.params.size) return

    repeat(a.params.size) {
        findReplacements(a.params[it], b.params[it], map)
    }
}

fun pad(value: Int, alignment: Int = PTR_SIZE): Int {
    if (value % alignment != 0) {
        return value + value % alignment
    }
    return value
}