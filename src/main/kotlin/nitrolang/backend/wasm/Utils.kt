package nitrolang.backend.wasm

import nitrolang.backend.MonoLambda
import nitrolang.backend.MonoOption
import nitrolang.backend.MonoStruct
import nitrolang.backend.MonoType
import kotlin.math.min

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
                "Array" -> 4
                "Function" -> 4
                "Long" -> 8
                else -> base.size
            }
        }
    }
}

fun MonoType.arraySize(): Int {
    return when (base) {
        is MonoOption -> 4
        is MonoLambda -> 4
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
                "Function" -> 4
                "Long" -> 8
                else -> 4
            }
        }
    }
}

fun intToWasm(num: Int): ByteArray {
    val hex = num.toString(16).padStart(8, '0')
    val a0 = hex.substring(0, 2)
    val a1 = hex.substring(2, 4)
    val a2 = hex.substring(4, 6)
    val a3 = hex.substring(6, 8)

    return byteArrayOf(
        a3.toUByte(16).toByte(),
        a2.toUByte(16).toByte(),
        a1.toUByte(16).toByte(),
        a0.toUByte(16).toByte()
    )
}

fun floatToWasm(num: Float): ByteArray {
    return intToWasm(num.toRawBits())
}