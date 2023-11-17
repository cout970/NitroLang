fun main() {
    func1(0)
    func2<Int>(42)
    func2(42)
    let a1: Int = func3<Int, Int>(69)
    let a2: Float = func3<Int, Float>(69)
}

fun func1(param1: Int): Int = param1

fun func2<#T>(param1: #T): #T = param1

fun func3<#A, #B>(param1: #A): #B = param1 as #B

@Extern $[lib: "core", name: "func4"]
fun func4() {}