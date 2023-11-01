
// This type is used for functions that never return,
// like infinite loops or cases where the program always crashes
// for example: `fun reached_error_condition(): Never = crash("The program has reached an unrecoverable error condition")`
@Extern $[lib: "core", name: "Never"]
@ValueType
@Intrinsic
struct Never {}

// This function is used to mark code that is unreachable,
// meaning that the compiler will assume that it will never be executed and ignore any code after it.
// In runtime a trap instruction will be executed, which will crash the program
@Extern $[lib: "core", name: "unreachable"]
@WasmInline $[opcode: "unreachable"]
fun unreachable(): Never {}
