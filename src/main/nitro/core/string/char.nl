
// This type represents a single character encoded as a unicode code point
@ValueType
struct Char {
    unicode_code_point: Int
}

// This function converts a Char to an integer, exposing the underlying unicode code point value
fun Char.to_int(): Int {
    ret this.unicode_code_point
}

// This function converts an integer to a Char instance
fun Int.to_char(): Char {
    ret Char $[unicode_code_point: this]
}

// Creates an string with only this character
fun Char.to_string(): String {
    return "".concat(this)
}
