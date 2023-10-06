
@Extern $[lib: "core", name: "println_string"]
fun println(i: String) {}

fun <#Value: ToString> println(value: #Value) {
    println(value.to_string())
}

@Extern $[lib: "core", name: "eprintln_string"]
fun eprintln(i: String) {}

