
struct String {
    byte_len: Int
    ptr: Int
}

fun String.byte_len(): Int {
    ret this.byte_len
}

fun String.get(index: Int): Char {
    ret this.get_codepoint(index).to_char()
}

@Extern $[lib: "core", name: "string_codepoint_len"]
fun String.codepoint_len(): Int {}

@Extern $[lib: "core", name: "string_get_codepoint"]
fun String.get_codepoint(index: Int): Int {}

@Extern $[lib: "core", name: "string_concat_string"]
fun String.concat(other: String): String {}

@Extern $[lib: "core", name: "string_concat_char"]
fun String.concat(char: Char): String {}

fun String.get_hash(): Int {
    let hash: Int = 0

    repeat this.len() {
        hash = (31 * hash).bitwise_xor(this[it].to_int())
    }

    ret hash
}

@Extern $[lib: "core", name: "string_replace"]
fun String.replace(find: String, replacement: String): String {}

@Extern $[lib: "core", name: "int_to_string"]
fun Int.to_string(): String {}

@Extern $[lib: "core", name: "float_to_string"]
fun Float.to_string(): String {}

fun Boolean.to_string(): String {
    if this { ret "true" } else { ret "false" }
}

fun String.to_string(): String = this

@Extern $[lib: "core", name: "any_to_string"]
fun #Any.to_string(): String {}
