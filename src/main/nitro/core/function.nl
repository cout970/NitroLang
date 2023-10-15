
// Represents a function type, has a variable number of type parameters representing:
// - 0 or more parameter types (including the receiver type)
// - the last type parameter is the function return type
@Extern $[lib: "core", name: "Function"]
@Intrinsic
struct Function {}

// Invokes a function with no parameters
@Intrinsic
fun invoke(f: () -> #R): #R {}

// Invokes a function with 1 parameter
@Intrinsic
fun invoke(f: (#A) -> #R, a: #A): #R {}

// Invokes a function with 2 parameters
@Intrinsic
fun invoke(f: (#A, #B) -> #R, a: #A, b: #B): #R {}

// Invokes a function with 3 parameters
@Intrinsic
fun invoke(f: (#A, #B, #C) -> #R, a: #A, b: #B, c: #C): #R {}

// Invokes a function with 4 parameters
@Intrinsic
fun invoke(f: (#A, #B, #C, #D) -> #R, a: #A, b: #B, c: #C, d: #D): #R {}

// Invokes a function with 5 parameters
@Intrinsic
fun invoke(f: (#A, #B, #C, #D, #E) -> #R, a: #A, b: #B, c: #C, d: #D, e: #E): #R {}

// Invokes a function with 6 parameters
@Intrinsic
fun invoke(f: (#A, #B, #C, #D, #E, #F) -> #R, a: #A, b: #B, c: #C, d: #D, e: #E, f: #F): #R {}

// Invokes a function with 7 parameters
@Intrinsic
fun invoke(f: (#A, #B, #C, #D, #E, #F, #G) -> #R, a: #A, b: #B, c: #C, d: #D, e: #E, f: #F, g: #G): #R {}

// Invokes a function with 8 parameters
@Intrinsic
fun invoke(f: (#A, #B, #C, #D, #E, #F, #G, #H) -> #R, a: #A, b: #B, c: #C, d: #D, e: #E, f: #F, g: #G, h: #H): #R {}

// If you need more than 8 parameters, consider using a single struct/list/map parameter instead
