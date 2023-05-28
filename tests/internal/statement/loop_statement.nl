fun main(): Unit {
    loop_with_break()
    loop_with_continue()
    loop_with_break_and_continue()
    infinite_loop()
}

fun infinite_loop(): Unit {
    loop {
        42
    }
}

fun loop_with_break(): Unit {
    loop {
        break
    }
}

fun loop_with_continue(): Unit {
    loop {
        continue
    }
}

fun loop_with_break_and_continue(): Unit {
    loop {
        if true {
            break
        } else {
            continue
        }
    }
}