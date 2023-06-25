
fun main() {
    println(fib(40))
    println(recFib(40))
}

fun fib(count: Int): Int {
    let i = 0
    let j = 1

    while count > 1 {
        let aux = i + j
        i = j
        j = aux
        count = count - 1
    }

    ret j
}

fun recFib(count: Int): Int {
    if count <= 0 {
        ret 0
    }
    if count <= 1 {
        ret 1
    }

    ret recFib(count - 1) + recFib(count - 2)
}