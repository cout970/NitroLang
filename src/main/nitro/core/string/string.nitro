
// This type is used to represent a string in the language
// Strings are immutable and are stored as UTF-8 encoded bytes
struct String {
    byte_len: Int
    ptr: RawArray<Byte>
}

// Tag for types that can be converted to a string
tag ToString {
    fun This.to_string(): String
}

// Returns the length of the string in bytes
fun String.byte_len(): Int {
    ret this.byte_len
}

// Reads a single byte from the string
// This is unrecommended as it does not account for UTF-8 encoding
fun String.get_byte(index: Int): Byte {
    ret this.ptr.get(index)
}

// tag Slice<Char>

// Returns the length of the string in chars (unicode codepoints)
fun String.len(): Int {
    ret this.codepoint_len()
}

// Gets a single char (unicode codepoint) from the string
// This operation is slow as it has to decode the UTF-8 encoding of the string
fun String.get(index: Int): Char {
    ret this.get_codepoint(index).to_char()
}

// Codepoints

// Returns the length of the string in chars (unicode codepoints)
@Extern $[lib: "core", name: "string_codepoint_len"]
fun String.codepoint_len(): Int {}

// Gets a single char (unicode codepoint) from the string
@Extern $[lib: "core", name: "string_get_codepoint"]
fun String.get_codepoint(index: Int): Int {}

// Utils

// Joins two strings together
@Extern $[lib: "core", name: "string_concat_string"]
fun String.concat(other: String): String {}

// Joins a string and a char together
@Extern $[lib: "core", name: "string_concat_char"]
fun String.concat(char: Char): String {}

// Checks if a string is equal to another string
@Extern $[lib: "core", name: "string_is_equal"]
fun String.is_equal(other: String): Boolean {}

// Checks if a string is not equal to another string
@Extern $[lib: "core", name: "string_is_not_equal"]
fun String.is_not_equal(other: String): Boolean {}

// Computes the hash value of a string, to be used in hash tables
fun String.get_hash(): Int {
    let hash: Int = 0

    repeat this.byte_len() {
        hash = (31 * hash).bitwise_xor(this.get_byte(it).to_int())
    }

    ret hash
}

// Replace all instances of a string with another string
@Extern $[lib: "core", name: "string_replace"]
fun String.replace(find: String, replacement: String): String {}

// Implementation of the ToString tag for strings
fun String.to_string(): String = this
