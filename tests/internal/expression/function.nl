fun main(): Unit {
    func1()
    func2()
    func3(420)
    func4(420, 69)
}

fun func1(): Unit = ()

fun func2(): Int = 42

fun func3(param1: Int): Int = param1

fun func4(param1: Int, param2: Int): Int = param1 + param2