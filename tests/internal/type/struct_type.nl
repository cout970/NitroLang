// Global name 'A'
struct A {}

// Uses the global name
@Extern fun a(): A {}

mod B {
    // Scoped name 'A', the full name is 'B::A'
    struct A {}

    // This reference this A::B, because A is in a closest scope
    @Extern fun b(): A {}

    // Fully quantified name
    @Extern fun c(): B::A {}
}

// Back to the global name 'A'
@Extern fun d(): A {}

// With the fully quantified name you can access the struct in the module 'B'
@Extern fun e(): B::A {}

fun main() {}