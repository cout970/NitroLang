// This type is used for functions that don't return anything
// There is only a single instance, and can be obtained by using the literal 'nothing'
// for example: `fun foo(): Nothing = nothing` or `return nothing`
@Extern $[lib: "core", name: "Nothing"]
@ValueType
@Intrinsic
struct Nothing {}

// Converts this instance to a string
fun Nothing.to_string(): String = "nothing"

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

// Crashes the program without an error message
fun panic(): Never = unreachable()

// Crashes the program with an error message
fun crash(msg: String): Never {
    eprintln(msg);
    panic()
}

// Given a value of type T, returns the size of T in bytes
fun size_of_value(value: #Value): Int {
    ret size_of<#Value>
}

// Internal function called by the compiler validate type casts in runtime
@Extern $[lib: "core", name: "as_type_internal"]
@Required
fun as_type_internal(ptr_to_value: Int, type_id: Int): Int {}

// Internal function called by the compiler check the type of a value in runtime
@Extern $[lib: "core", name: "is_type_internal"]
@Required
fun is_type_internal(ptr_to_value: Int, type_id: Int): Boolean {}

// Intrinsic function that allocates a block of memory of the given size in bytes
// used by the compiler to allocate memory when a struct instance is created
@WasmName $[name: "memory_alloc_internal"]
@Required
fun memory_alloc_internal(bytes: Int): Int {
    ret get_memory().alloc_bytes(bytes).get_address()
}

// Intrinsic function that copies a chunk of memory to another location
@Extern $[lib: "core", name: "memory_copy_internal"]
@Required
fun memory_copy_internal(target: Int, value: Int, len: Int) {}

// fun is_encoded_in_ref(v: #Type): Boolean
// Returns true if the given type is 4 bytes or smaller and
// instead of being stored in the heap, it is stored in the reference itself
fun is_encoded_in_ref(v: #Type): Boolean {
    if v is Int         { ret true }
    if v is Float       { ret true }
    if v is Nothing     { ret true }
    if v is Never       { ret true }
    if v is Byte        { ret true }
    if v is Ptr<*>      { ret true }
    if v is RawArray<*> { ret true }
    ret false
}