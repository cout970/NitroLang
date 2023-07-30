// Unit type, the literal is '()', has only 1 instance
@Extern $[lib: "core", name: "Nothing"]
@StackValue
struct Nothing {}

// Never type, a function returning Never, will never return: infinite loop, panic, etc.
@Extern $[lib: "core", name: "Never"]
@StackValue
struct Never {}

// Marks a code path that will never be executed
@Extern $[lib: "core", name: "unreachable"]
@WasmInline $[opcode: "unreachable"]
fun unreachable(): Never {}

// Crashes the program without an error message
fun panic(): Never = unreachable()

// Crashes the program with an error message
fun crash(msg: String): Never {
    eprintln(msg)
    panic()
}
