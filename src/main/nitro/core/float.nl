
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

@Extern $[lib: "core", name: "float_to_int"]
@WasmInline $[opcode: "i32.trunc_f32_s"]
fun Float.to_int(): Int {}

@Extern $[lib: "core", name: "float_to_int"]
@WasmInline $[opcode: "f32.convert_i32_s"]
fun Int.to_float(): Float {}

// Comparisons

@Extern $[lib: "core", name: "float_is_equal_float"]
@WasmInline $[opcode: "f32.eq"]
fun Float.is_equal(other: Float): Boolean {}

@Extern $[lib: "core", name: "float_is_not_equal_float"]
@WasmInline $[opcode: "f32.ne"]
fun Float.is_not_equal(other: Float): Boolean {}

@Extern $[lib: "core", name: "float_less_than"]
@WasmInline $[opcode: "f32.lt"]
fun Float.less_than(other: Float): Boolean {}

@Extern $[lib: "core", name: "float_less_equal"]
@WasmInline $[opcode: "f32.le"]
fun Float.less_equal(other: Float): Boolean {}

@Extern $[lib: "core", name: "float_greater_equal"]
@WasmInline $[opcode: "f32.ge"]
fun Float.greater_equal(other: Float): Boolean {}

@Extern $[lib: "core", name: "float_greater_than"]
@WasmInline $[opcode: "f32.gt"]
fun Float.greater_than(other: Float): Boolean {}

@Extern $[lib: "core", name: "float_to_string"]
fun Float.to_string(): String {}

