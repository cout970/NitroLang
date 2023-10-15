
fun debug() {
    let list: List<Int> = #[]

    if list.is_not_empty() && list[0]!! == 1 {
        println("list is not empty and first element is 1")
    } else {
        println("list is empty or first element is not 1")
    }
}

fun cond1(): Boolean {
    println("cond1")
    return true
}

fun cond2(): Boolean {
    println("cond2")
    return true
}

fun cond3(): Boolean {
    println("cond3")
    return false
}

fun cond4(): Boolean {
    println("cond4")
    return false
}