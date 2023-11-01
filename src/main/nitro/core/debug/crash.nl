
// Crashes the program with an error message
fun crash(msg: String): Never {
    Log::fatal(msg);
    panic()
}

// Crashes the program without an error message
fun panic(): Never = unreachable()