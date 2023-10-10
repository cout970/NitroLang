
@StackValue
struct Char {
    unicode_code_point: Int
}

fun Char.to_int(): Int {
    ret this.unicode_code_point
}

fun Int.to_char(): Char {
    ret Char $[unicode_code_point: this]
}
