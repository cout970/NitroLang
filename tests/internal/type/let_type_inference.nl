struct Struct {}

fun main() {
    let a = 1
    let a_check: Int = a

    let b = 1.5f
    let b_check: Float = b

    let c = true
    let c_check: Boolean = c

    let d = "Test"
    let d_check: String = d

    let e = Struct $[]
    let e_check: Struct = e

    let f
    f = 1f

    // [Error] Type mismatch, variable 'f' expects a value of type 'Float', but found 'Boolean'
    // f = false

    // [Error] Variable not found: g
    //  g = 10
}