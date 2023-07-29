
@Extern $[lib: "core", name: "alloc"]
@Required
fun alloc(bytes: Int): Int {}

@Extern $[lib: "core", name: "check_cast"]
@Required
fun check_cast(ptr: Int, expected_type: Int): Int {}

@Extern $[lib: "core", name: "is_variant"]
@Required
fun is_variant(ptr: Int, expected_variant: Int): Boolean {}

@Extern $[lib: "core", name: "memory_alloc"]
fun memory_alloc(bytes: Int): Int {}

@Extern $[lib: "core", name: "memory_copy"]
fun memory_copy(src: Int, dst: Int, byte_len: Int) {}

@Extern $[lib: "core", name: "memory_write_generic"]
fun memory_write(ptr: Int, value: #Value) {}

@Extern $[lib: "core", name: "memory_write_int"]
fun memory_write(ptr: Int, value: Int) {}

@Extern $[lib: "core", name: "memory_write_float"]
fun memory_write(ptr: Int): Float {}

@Extern $[lib: "core", name: "memory_write_boolean"]
fun memory_write(ptr: Int, value: Boolean) {}

@Extern $[lib: "core", name: "memory_read_generic"]
fun memory_read(ptr: Int): #Value {}

@Extern $[lib: "core", name: "memory_read_int"]
fun memory_read(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_float"]
fun memory_read(ptr: Int): Float {}

@Extern $[lib: "core", name: "memory_read_boolean"]
fun memory_read(ptr: Int): Boolean {}