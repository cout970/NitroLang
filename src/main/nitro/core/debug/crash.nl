
// Crashes the program with an error message
fun crash(msg: String): Never {
    eprintln(msg);
    panic()
}

// Crashes the program without an error message
fun panic(): Never = unreachable()