
struct String {
    byte_len: Int
    ptr: Int
}

// Bytes

fun String.byte_len(): Int {
    ret this.byte_len
}

fun String.get_byte(index: Int): Int {
    ret memory_read_byte(this.ptr + index)
}

// Interface

fun String.get(index: Int): Char {
    ret this.get_codepoint(index).to_char()
}

fun String.len(): Int {
    ret this.codepoint_len()
}

// Codepoints
@Extern $[lib: "core", name: "string_codepoint_len"]
fun String.codepoint_len(): Int {}

@Extern $[lib: "core", name: "string_get_codepoint"]
fun String.get_codepoint(index: Int): Int {}

// Utils

@Extern $[lib: "core", name: "string_concat_string"]
fun String.concat(other: String): String {}

@Extern $[lib: "core", name: "string_concat_char"]
fun String.concat(char: Char): String {}

fun String.get_hash(): Int {
    let hash: Int = 0

    repeat this.byte_len() {
        hash = (31 * hash).bitwise_xor(this.get_byte(it))
    }

    ret hash
}

@Extern $[lib: "core", name: "string_replace"]
fun String.replace(find: String, replacement: String): String {}

@Extern $[lib: "core", name: "float_to_string"]
fun Float.to_string(): String {}

fun Boolean.to_string(): String {
    if this { ret "true" } else { ret "false" }
}

fun String.to_string(): String = this
