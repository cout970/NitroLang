
@Extern $[lib: "core", name: "internal_is_variant"]
@Required
fun internal_is_variant(ptr: Int, expected_variant: Int): Boolean {}

@Extern $[lib: "core", name: "memory_alloc"]
@Required
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

@Extern $[lib: "core", name: "memory_read_byte"]
fun memory_read_byte(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_int"]
fun memory_read_int(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_float"]
fun memory_read_float(ptr: Int): Float {}

@Extern $[lib: "core", name: "memory_read_boolean"]
fun memory_read_boolean(ptr: Int): Boolean {}