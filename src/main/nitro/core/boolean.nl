
// This type is used to represent boolean values in the language
// The posible values are 'true' and 'false'
@Extern $[lib: "core", name: "Boolean"]
@StackValue
struct Boolean {}

// This function returns the logical negation of a boolean value
// Implementation note: unfortunately, i32.not converts 0 to -1 instead of 1,
// so we need to use a conditional expression
fun Boolean.logical_not(): Boolean = if this { false } else { true }

// This function returns the logical 'or' of two boolean values
@Extern $[lib: "core", name: "logical_or"]
@WasmInline $[opcode: "i32.or"]
fun Boolean.logical_or(other: Boolean): Boolean {}

// This function returns the logical 'and' of two boolean values
@Extern $[lib: "core", name: "logical_and"]
@WasmInline $[opcode: "i32.and"]
fun Boolean.logical_and(other: Boolean): Boolean {}

// This function returns the logical 'xor' of two boolean values
@Extern $[lib: "core", name: "logical_xor"]
@WasmInline $[opcode: "i32.xor"]
fun Boolean.logical_xor(other: Boolean): Boolean {}

// This function compares two boolean values for equality
@Extern $[lib: "core", name: "boolean_is_equal"]
@WasmInline $[opcode: "i32.eq"]
fun Boolean.is_equal(other: Boolean): Boolean {}

// This function compares two boolean values for inequality
@Extern $[lib: "core", name: "boolean_is_not_equal"]
@WasmInline $[opcode: "i32.ne"]
fun Boolean.is_not_equal(other: Boolean): Boolean {}

// This function compares two boolean values and returns an ordering value
// that represents the sorting order of the values
fun Boolean.get_ordering(other: Boolean): Ordering {
    ret when {
        this && not other -> Ordering::Greater $[]
        not this && other -> Ordering::Less $[]
        else -> Ordering::Equals $[]
    }
}

// This function converts a boolean value to a string
fun Boolean.to_string(): String {
    ret if this { "true" } else { "false" }
}
