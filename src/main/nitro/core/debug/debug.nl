
@Extern $[lib: "core", name: "println_string"]
fun println(i: String): String {}

@Extern $[lib: "core", name: "println_int"]
fun println(i: Int): Int {}

@Extern $[lib: "core", name: "println_float"]
fun println(i: Float): Float {}

@Extern $[lib: "core", name: "println_boolean"]
fun println(i: Boolean): Boolean {}

// Generic version of println() for any type that has a to_string() function
fun <#Value: ToString> println(value: #Value): #Value {
    println(value.to_string())
    ret value
}
