// Global name 'A'
struct A {}

// Uses the global name
fun a(): A {}

mod B {
    // Scoped name 'A', the full name is 'B::A'
    struct A {}

    // This reference this A::B, because A is in a closest scope
    fun b(): A {}

    // Fully quantified name
    fun c(): B::A {}
}

// Back to the global name 'A'
fun d(): A {}

// With the fully quantified name you can access the struct in the module 'B'
fun e(): B::A {}

fun main() {}