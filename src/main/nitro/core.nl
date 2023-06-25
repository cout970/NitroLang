
@Extern $[lib: "core", name: "Unit"]
@StackValue
struct Unit {}

// Intrinsic functions

@Extern $[lib: "core", name: "alloc"]
@Required
fun alloc(bytes: Int): Int {}

@Extern $[lib: "core", name: "check_cast"]
@Required
fun check_cast(ptr: Int, expected_type: Int): Int {}

//region type Boolean

@Extern $[lib: "core", name: "Boolean"]
@StackValue
struct Boolean {}

// @Extern $[lib: "core", name: "logical_not"]
// Unfortunately, i32.not converts 0 to -1 instead of 1
fun Boolean.logical_not(): Boolean = if this { false } else { true }

@Extern $[lib: "core", name: "logical_or"]
@WasmInline $[opcode: "i32.or"]
fun Boolean.logical_or(other: Boolean): Boolean {}

@Extern $[lib: "core", name: "logical_and"]
@WasmInline $[opcode: "i32.and"]
fun Boolean.logical_and(other: Boolean): Boolean {}

@Extern $[lib: "core", name: "logical_xor"]
@WasmInline $[opcode: "i32.xor"]
fun Boolean.logical_xor(other: Boolean): Boolean {}

//endregion

//region type Int

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

//endregion

//region type Float

@Extern $[lib: "core", name: "Float"]
@StackValue
struct Float {}

@Extern $[lib: "core", name: "float_plus"]
@WasmInline $[opcode: "f32.add"]
fun Float.plus(other: Float): Float {}

@Extern $[lib: "core", name: "float_minus"]
@WasmInline $[opcode: "f32.sub"]
fun Float.minus(other: Float): Float {}

@Extern $[lib: "core", name: "float_mul"]
@WasmInline $[opcode: "f32.mul"]
fun Float.mul(other: Float): Float {}

@Extern $[lib: "core", name: "float_div"]
@WasmInline $[opcode: "f32.div"]
fun Float.div(other: Float): Float {}

@Extern $[lib: "core", name: "float_rem"]
// WASM does not provide f32.rem
fun Float.rem(other: Float): Float {}

@Extern $[lib: "core", name: "float_min"]
@WasmInline $[opcode: "f32.min"]
fun Float.min(other: Float): Float {}

@Extern $[lib: "core", name: "float_min"]
@WasmInline $[opcode: "f32.max"]
fun Float.max(other: Float): Float {}

@Extern $[lib: "core", name: "float_copy_sign"]
@WasmInline $[opcode: "f32.copysign"]
fun Float.copy_sign(other: Float): Float {}

@Extern $[lib: "core", name: "float_abs"]
@WasmInline $[opcode: "f32.abs"]
fun Float.abs(): Float {}

@Extern $[lib: "core", name: "float_neg"]
@WasmInline $[opcode: "f32.neg"]
fun Float.neg(): Float {}

@Extern $[lib: "core", name: "float_sqrt"]
@WasmInline $[opcode: "f32.sqrt"]
fun Float.sqrt(): Float {}

@Extern $[lib: "core", name: "float_ceil"]
@WasmInline $[opcode: "f32.ceil"]
fun Float.ceil(): Float {}

@Extern $[lib: "core", name: "float_floor"]
@WasmInline $[opcode: "f32.floor"]
fun Float.floor(): Float {}

@Extern $[lib: "core", name: "float_truncate"]
@WasmInline $[opcode: "f32.trunc"]
fun Float.truncate(): Float {}

@Extern $[lib: "core", name: "float_nearest"]
@WasmInline $[opcode: "f32.nearest"]
fun Float.nearest(): Float {}

//endregion

//region type Ptr

@Extern $[lib: "core", name: "Ptr"]
@StackValue
struct Ptr {}

//endregion

//region type Ordering

option Ordering {
    Less    {}
    Equals  {}
    Greater {}
}

@Extern $[lib: "core", name: "int_get_ordering"]
fun Int.get_ordering(other: Int): Ordering {}

@Extern $[lib: "core", name: "float_get_ordering"]
fun Float.get_ordering(other: Float): Ordering {}

@Extern $[lib: "core", name: "is_less"]
fun Ordering.is_less(): Boolean {}

@Extern $[lib: "core", name: "is_less_or_equals"]
fun Ordering.is_less_or_equals(): Boolean {}

@Extern $[lib: "core", name: "is_equals"]
fun Ordering.is_equals(): Boolean {}

@Extern $[lib: "core", name: "is_not_equals"]
fun Ordering.is_not_equals(): Boolean {}

@Extern $[lib: "core", name: "is_greater_or_equals"]
fun Ordering.is_greater_or_equals(): Boolean {}

@Extern $[lib: "core", name: "is_greater"]
fun Ordering.is_greater(): Boolean {}

//endregion

//region type Optional

option Optional<#Value> {
    Some { value: #Value }
    None {}
}

//fun Some(some: #Value): Optional<#Value> = Optional::Some<#Value> $[value: some]
//fun None<#Value>(): Optional<#Value> = Optional::None<#Value> $[]

//fun Optional<#Value>.is_some(): Boolean = this is Optional::Some<#Value>
//fun Optional<#Value>.is_none(): Boolean = this is Optional::None<#Value>

//endregion

//region type Result

option Result<#Ok, #Err> {
    Ok { value: #Ok }
    Err { value: #Err }
}

//endregion

//region type String

@Extern $[lib: "core", name: "String"]
struct String {
    data: Int
    len: Int
}

//endregion

@Extern $[lib: "core", name: "List"]
struct List<#Item> {
    data: Int
    len: Int
    capacity: Int
}

@Extern $[lib: "core", name: "list_add"]
fun List<#Item>.add(item: #Item) {}

@Extern $[lib: "core", name: "list_get"]
fun List<#Item>.get(index: Int): #Item {}

@Extern $[lib: "core", name: "list_set"]
fun List<#Item>.set(index: Int, value: #Item) {}

@Extern $[lib: "core", name: "list_len"]
fun List<#Item>.len(): Int {}

@Extern $[lib: "core", name: "Set"]
struct Set<#Item> {}

@Extern $[lib: "core", name: "set_add"]
fun Set<#Item>.add(item: #Item) {}

@Extern $[lib: "core", name: "set_has"]
fun Set<#Item>.has(item: #Item) {}

@Extern $[lib: "core", name: "set_len"]
fun Set<#Item>.len(): Int {}

@Extern $[lib: "core", name: "Map"]
struct Map<#Key, #Value> {}

@Extern $[lib: "core", name: "map_set"]
fun Map<#Key, #Value>.set(key: #Key, value: #Value) {}

@Extern $[lib: "core", name: "map_get"]
fun Map<#Key, #Value>.get(key: #Key): Optional<#Value> {}

@Extern $[lib: "core", name: "map_len"]
fun Map<#Key, #Value>.len(): Int {}

@Extern $[lib: "core", name: "println_int"]
fun println(i: Int) {}

@Extern $[lib: "core", name: "println_float"]
fun println(i: Float) {}

@Extern $[lib: "core", name: "println_string"]
fun println(i: String) {}

@Extern $[lib: "core", name: "println_string_list"]
fun println(i: List<String>) {}
