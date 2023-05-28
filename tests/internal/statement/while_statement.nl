fun main(): Unit {
    while_with_break()
    while_with_continue()
    while_with_break_and_continue()
    infinite_loop()
}

fun infinite_loop(): Unit {
    while true {
        42
    }
}

fun loop_with_break(): Unit {
    while true {
        break
    }
}

fun loop_with_continue(): Unit {
    while true {
        continue
    }
}

fun loop_with_break_and_continue(): Unit {
    while true {
        if true {
            break
        } else {
            continue
        }
    }
}