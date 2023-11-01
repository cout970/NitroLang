
@Extern $[lib: "core", name: "println_string"]
fun dbg(i: String): String {}

@Extern $[lib: "core", name: "println_int"]
fun dbg(i: Int): Int {}

@Extern $[lib: "core", name: "println_float"]
fun dbg(i: Float): Float {}

@Extern $[lib: "core", name: "println_boolean"]
fun dbg(i: Boolean): Boolean {}

// Generic version of println() for any type that has a to_string() function
fun <#Value: ToString> dbg(value: #Value): #Value {
    dbg(value.to_string())
    ret value
}
