
fun debug() {
    println("1")
    defer { println("defer 1") }
    println("2")
    repeat 3 {
        println("3")
        defer { println("defer 2") }
        continue
        println("4")
    }
    println("5")
    println("6")
}
