// Represents the result of comparing 2 values, determines the order of elements
option Ordering {
    Less    {}
    Equals  {}
    Greater {}
}

fun Ordering.is_less(): Boolean = this is Ordering::Less

fun Ordering.is_less_or_equals(): Boolean = this is Ordering::Less || this is Ordering::Equals

fun Ordering.is_equals(): Boolean = this is Ordering::Equals

fun Ordering.is_not_equals(): Boolean = not (this is Ordering::Equals)

fun Ordering.is_greater_or_equals(): Boolean = this is Ordering::Greater || this is Ordering::Equals

fun Ordering.is_greater(): Boolean = this is Ordering::Greater

fun Int.get_ordering(other: Int): Ordering {
    if this.is_equal(other) {
        ret Ordering::Equals $[]
    } else {
        if this.greater_than_signed(other) {
            ret Ordering::Greater $[]
        } else {
            ret Ordering::Less $[]
        }
    }
}

fun Float.get_ordering(other: Float): Ordering {
    if this.is_equal(other) {
        ret Ordering::Equals $[]
    } else {
        if this.greater_than(other) {
            ret Ordering::Greater $[]
        } else {
            ret Ordering::Less $[]
        }
    }
}

@Extern $[lib: "core", name: "string_get_ordering_internal"]
fun String.get_ordering_internal(other: String): Int {}

fun String.get_ordering(other: String): Ordering = ordering_from_int(this.get_ordering_internal(other))

fun ordering_from_int(value: Int): Ordering {
    if value == 0 {
        ret Ordering::Equals $[]
    }

    if value < 0 {
        ret Ordering::Less $[]
    }

    ret Ordering::Greater $[]
}