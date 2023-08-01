
@Extern $[lib: "core", name: "Int"]
@StackValue
struct Int {}

@Extern $[lib: "core", name: "int_plus"]
@WasmInline $[opcode: "i32.add"]
fun Int.plus(other: Int): Int {}

@Extern $[lib: "core", name: "int_minus"]
@WasmInline $[opcode: "i32.sub"]
fun Int.minus(other: Int): Int {}

@Extern $[lib: "core", name: "int_mul"]
@WasmInline $[opcode: "i32.mul"]
fun Int.mul(other: Int): Int {}

@Extern $[lib: "core", name: "int_div"]
@WasmInline $[opcode: "i32.div_s"]
fun Int.div(other: Int): Int {}

@Extern $[lib: "core", name: "int_unsigned_div"]
@WasmInline $[opcode: "i32.div_u"]
fun Int.unsigned_div(other: Int): Int {}

@Extern $[lib: "core", name: "int_rem"]
@WasmInline $[opcode: "i32.rem_s"]
fun Int.rem(other: Int): Int {}

@Extern $[lib: "core", name: "int_unsigned_rem"]
@WasmInline $[opcode: "i32.rem_u"]
fun Int.unsigned_rem(other: Int): Int {}

fun Int.modulo(other: Int): Int {
    let rem = this.rem(other)
    ret if rem < 0 { rem + other } else { rem }
}

// Bits

@Extern $[lib: "core", name: "int_bitwise_and"]
@WasmInline $[opcode: "i32.and"]
fun Int.bitwise_and(other: Int): Int {}

@Extern $[lib: "core", name: "int_bitwise_or"]
@WasmInline $[opcode: "i32.or"]
fun Int.bitwise_or(other: Int): Int {}

@Extern $[lib: "core", name: "int_bitwise_xor"]
@WasmInline $[opcode: "i32.xor"]
fun Int.bitwise_xor(other: Int): Int {}

@Extern $[lib: "core", name: "int_bitwise_shift_left"]
@WasmInline $[opcode: "i32.shl"]
fun Int.bitwise_shift_left(other: Int): Int {}

@Extern $[lib: "core", name: "int_bitwise_shift_right"]
@WasmInline $[opcode: "i32.shr_s"]
fun Int.bitwise_shift_right(other: Int): Int {}

@Extern $[lib: "core", name: "int_bitwise_shift_right_unsigned"]
@WasmInline $[opcode: "i32.shr_u"]
fun Int.bitwise_shift_right_unsigned(other: Int): Int {}

@Extern $[lib: "core", name: "int_bitwise_count_leading_zeros"]
@WasmInline $[opcode: "i32.clz"]
fun Int.bitwise_count_leading_zeros(): Int {}

@Extern $[lib: "core", name: "int_bitwise_count_trailing_zeros"]
@WasmInline $[opcode: "i32.ctz"]
fun Int.bitwise_count_trailing_zeros(): Int {}

@Extern $[lib: "core", name: "int_bitwise_count_nonzero_bits"]
@WasmInline $[opcode: "i32.popcnt"]
fun Int.bitwise_count_nonzero_bits(): Int {}

// Comparisons

@Extern $[lib: "core", name: "int_is_equal_int"]
@WasmInline $[opcode: "i32.eq"]
fun Int.is_equal(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_is_not_equal_int"]
@WasmInline $[opcode: "i32.ne"]
fun Int.is_not_equal(other: Int): Int {}

@Extern $[lib: "core", name: "int_is_equal_zero"]
@WasmInline $[opcode: "i32.eqz"]
fun Int.is_equal_zero(): Boolean {}

@Extern $[lib: "core", name: "int_less_than_signed"]
@WasmInline $[opcode: "i32.lt_s"]
fun Int.less_than_signed(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_less_than_unsigned"]
@WasmInline $[opcode: "i32.lt_u"]
fun Int.less_than_unsigned(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_less_equal_signed"]
@WasmInline $[opcode: "i32.le_s"]
fun Int.less_equal_signed(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_less_equal_unsigned"]
@WasmInline $[opcode: "i32.le_u"]
fun Int.less_equal_unsigned(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_greater_equal_signed"]
@WasmInline $[opcode: "i32.ge_s"]
fun Int.greater_equal_signed(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_greater_equal_unsigned"]
@WasmInline $[opcode: "i32.ge_u"]
fun Int.greater_equal_unsigned(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_greater_than_signed"]
@WasmInline $[opcode: "i32.gt_s"]
fun Int.greater_than_signed(other: Int): Boolean {}

@Extern $[lib: "core", name: "int_greater_than_unsigned"]
@WasmInline $[opcode: "i32.gt_u"]
fun Int.greater_than_unsigned(other: Int): Boolean {}

fun max(a: Int, b: Int): Int {
    ret if a > b { a } else { b }
}

fun min(a: Int, b: Int): Int {
    ret if a > b { b } else { a }
}
