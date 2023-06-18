fun main() {
    func1()
    func2()
    func3(420)
    // [Error] Type mismatch calling function 'func3', param 0 expects 'Int', but 'String' was found instead
    // func3("Error")
    func4(420, 69)

    let a: Unit = func1()
    let b: Unit = func5(())
    let c: Int = func5(420)
    let d: String = func5("String")
    let e: List<Boolean> = func7(true)
    let f: List<List<Boolean>> = func7(func7(true))
    let g: String = func8()
    let h: Int = func8()

    let i: Int = func9(12, 34)
    // [Error] Type conflict, multiple conflicting alternatives to resolve '#Same', 'Int' vs 'String'
//    let j = func9(12, "Test")
}

// Function returning Unit with body expression
fun func1(): Unit = ()

// Function returning an Int with body statements
fun func2(): Int {
    ret 69
}

// Function returning input, must check that is an Int
fun func3(param1: Int): Int = param1

// Multiple params
fun func4(param1: Int, param2: Int): Int = param1 + param2

// Param types
fun func5(value: #T): #T = value

// Calling other functions while keeping the param types as valid local types
// and not mixing types of this function and types of the called function
fun func6(value: #X): #X {
    let a: #X = func5(value)
    ret func5(a)
}

// Using param types in subtypes
fun func7(value: #T): List<#T> {
    ret #[func6(value), func5(value)]
}

// Return type inference
fun func8(): #Any = () as #Any

// Type of a and b must be the same, but can be any type
fun func9(a: #Same, b: #Same): #Same = a