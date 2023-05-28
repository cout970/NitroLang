struct Box {
    value: Int
}

fun main(): Unit {
    let a = Box$[value: 42]

    a.value = 420
    a.value
}

