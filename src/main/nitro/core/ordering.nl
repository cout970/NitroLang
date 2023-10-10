// Represents the result of comparing 2 values, determines the order of elements
option Ordering {
    Less
    Equals
    Greater
}

tag GetOrdering {
    fun This.get_ordering(other: This): Ordering
}

fun Ordering.is_less(): Boolean = this is Ordering::Less

fun Ordering.is_less_or_equals(): Boolean = this is Ordering::Less || this is Ordering::Equals

fun Ordering.is_equals(): Boolean = this is Ordering::Equals

fun Ordering.is_not_equals(): Boolean = not (this is Ordering::Equals)

fun Ordering.is_greater_or_equals(): Boolean = this is Ordering::Greater || this is Ordering::Equals

fun Ordering.is_greater(): Boolean = this is Ordering::Greater

fun <#Value2: GetOrdering> max(a: #Value2, b: #Value2): #Value2 {
    ret if a > b { a } else { b }
}

fun <#Value: GetOrdering> min(a: #Value, b: #Value): #Value {
    ret if a > b { b } else { a }
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
