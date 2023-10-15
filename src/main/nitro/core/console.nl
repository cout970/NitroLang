
// Prints a string to the standard output.
@Extern $[lib: "core", name: "println_string"]
fun println(i: String) {}

// Generic version of println() for any type that has a to_string() function
fun <#Value: ToString> println(value: #Value) {
    println(value.to_string())
}

// Prints a string to the standard error.
@Extern $[lib: "core", name: "eprintln_string"]
fun eprintln(i: String) {}

