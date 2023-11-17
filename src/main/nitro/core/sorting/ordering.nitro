
// Represents the result of a comparison between two values
//
// The ordering is defined as follows:
// - Less: The first value is less than the second value
// - Equals: The first value is equal to the second value
// - Greater: The first value is greater than the second value
//
// Since the default sorting order is ascending, when comparing two values:
// - If the first value is less than the second value, the first value should be placed before the second value
// - If the first value is equal to the second value, the order of the two values should not be changed
// - If the first value is greater than the second value, the first value should be placed after the second value
//
option Ordering {
    Less
    Equals
    Greater
}

// A tag for types that can be compared
tag GetOrdering {
    fun This.get_ordering(other: This): Ordering
}

// Checks if the ordering is 'less than'
fun Ordering.is_less(): Boolean = this is Ordering::Less

// Checks if the ordering is 'less than' or 'equal to'
fun Ordering.is_less_or_equals(): Boolean = this is Ordering::Less || this is Ordering::Equals

// Checks if the ordering is 'equal to'
fun Ordering.is_equals(): Boolean = this is Ordering::Equals

// Checks if the ordering is 'not equal to'
fun Ordering.is_not_equals(): Boolean = not (this is Ordering::Equals)

// Checks if the ordering is 'greater than' or 'equal to'
fun Ordering.is_greater_or_equals(): Boolean = this is Ordering::Greater || this is Ordering::Equals

// Checks if the ordering is 'greater than'
fun Ordering.is_greater(): Boolean = this is Ordering::Greater

// Using the get_ordering function, finds which of the two values is the maximum
fun <#Value2: GetOrdering> max(a: #Value2, b: #Value2): #Value2 {
    ret if a > b { a } else { b }
}

// Using the get_ordering function, finds which of the two values is the minimum
fun <#Value: GetOrdering> min(a: #Value, b: #Value): #Value {
    ret if a > b { b } else { a }
}

// Compares two strings
fun String.get_ordering(other: String): Ordering = this.get_ordering_internal(other).to_ordering()

// Internal function for comparing two strings using the current locale and user preferences
@Extern $[lib: "core", name: "string_get_ordering_internal"]
fun String.get_ordering_internal(other: String): Int {}

// Utility function for converting an integer (-1, 0, 1) to an ordering
fun Int.to_ordering(): Ordering {
    if this == 0 {
        ret Ordering::Equals $[]
    }

    if this < 0 {
        ret Ordering::Less $[]
    }

    ret Ordering::Greater $[]
}
