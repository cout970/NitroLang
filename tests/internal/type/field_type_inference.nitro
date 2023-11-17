
struct Struct<#A, #B, #C> {
    a: #A
    b: #B
    c: #C
}

fun main() {
    let inst = Struct $[
        a: 0
        b: 0f
        c: "Test"
    ]

    // Make sure the type inference of `inst` is correct
    let inst2: Struct<Int, Float, String> = inst

    let a_var = inst.a
    let b_var = inst.b
    let c_var = inst.c

    // Assert the types of the variables
    let a_var2: Int = a_var
    let b_var2: Float = b_var
    let c_var2: String = c_var
}