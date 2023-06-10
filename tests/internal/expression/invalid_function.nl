fun main(): Unit {
    func1()
    func2()
    func3()
}

// Type mismatch, attempt to return 'String' on a function that must return 'Int'
fun func1(): Int = "Test"

// Function 'func2' must return 'Int' but has empty body
fun func2(): Int {}

// Type mismatch, attempt to return 'String' on a function that must return 'Int'
fun func3(): Int {
    if true {
        ret "Test"
    }
    ret 0
}
