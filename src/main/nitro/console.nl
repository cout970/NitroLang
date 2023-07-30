
@Extern $[lib: "core", name: "println_nothing"]
fun println(i: Nothing) {}

@Extern $[lib: "core", name: "println_boolean"]
fun println(i: Boolean) {}

@Extern $[lib: "core", name: "println_int"]
fun println(i: Int) {}

@Extern $[lib: "core", name: "println_float"]
fun println(i: Float) {}

@Extern $[lib: "core", name: "println_string"]
fun println(i: String) {}

@Extern $[lib: "core", name: "eprintln_string"]
fun eprintln(i: String) {}