fun main() {
    loop_with_break()
    loop_with_continue()
    loop_with_break_and_continue()
    infinite_loop()
}

fun infinite_loop() {
    while true {
        42
    }
}

fun loop_with_break() {
    while true {
        break
    }
}

fun loop_with_continue() {
    while true {
        continue
    }
}

fun loop_with_break_and_continue() {
    while true {
        if true {
            break
        } else {
            continue
        }
    }
}