fun main(): Unit {
    single_branch()
    multi_branch()
}

fun single_branch(): Unit {
    if true {
        42
    }
}

fun multi_branch(): Unit {
    if true {
        1
    } else {
        2
    }
}
