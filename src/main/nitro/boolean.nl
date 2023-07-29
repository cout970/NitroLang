
@Extern $[lib: "core", name: "Boolean"]
@StackValue
struct Boolean {}

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
