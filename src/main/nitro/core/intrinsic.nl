// Unit type, the literal is 'nothing', has only 1 instance
// e.g. `return nothing`
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

// Given a value, extract it's type and gets the size in bytes needed to store it in memory
fun size_of_value(value: #Value): Int {
    ret size_of<#Value>
}

@Extern $[lib: "core", name: "as_type_internal"]
@Required
fun as_type_internal(ptr: Int, ty: Int): Int {}

@Extern $[lib: "core", name: "is_type_internal"]
@Required
fun is_type_internal(ptr: Int, ty: Int): Boolean {}

// Intrinsic function used for struct automatic allocation
@WasmName $[name: "memory_alloc_internal"]
@Required
fun memory_alloc_internal(bytes: Int): Int {
    ret get_memory().alloc_bytes(bytes).get_address()
}
