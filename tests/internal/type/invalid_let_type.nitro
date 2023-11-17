struct A<#Param1, #Param2> {}

fun main() {
    // Type 'B' not found
    let a: B

    // Variable 'a1' has invalid type: missing type parameters (Param2)
    let a1: A<Int>

    // Variable 'a2' has invalid type: too many type parameters, expecting 2
    let a2: A<Int, Int, String>

    // Found undefined type parameter 'P'
    let a3: A<Int, #P>

    // Variable 'int' has no type
    let int
}