@Extern $[lib: "core", name: "memory_alloc"]
@Required
fun memory_alloc(bytes: Int): Int {}

@Extern $[lib: "core", name: "memory_write_internal"]
@Required
fun memory_write_internal(ptr: Int, value: Int, size: Int) {}

@Extern $[lib: "core", name: "memory_read_internal"]
@Required
fun memory_read_internal(ptr: Int, size: Int): Int {}

@Extern $[lib: "core", name: "as_type_internal"]
@Required
fun as_type_internal(ptr: Int, ty: Int): Int {}

@Extern $[lib: "core", name: "is_type_internal"]
@Required
fun is_type_internal(ptr: Int, ty: Int): Boolean {}

@Extern $[lib: "core", name: "debug"]
@Required
fun debug(ptr: Int) {}

//

@Extern $[lib: "core", name: "memory_copy"]
@Required
fun memory_copy(src: Int, dst: Int, byte_len: Int) {}

@Extern $[lib: "core", name: "memory_write_byte"]
@Required
fun memory_write_byte(ptr: Int, value: Int) {}

@Extern $[lib: "core", name: "memory_write_int"]
@Required
fun memory_write_int(ptr: Int, value: Int) {}

@Extern $[lib: "core", name: "memory_write_float"]
@Required
fun memory_write_float(ptr: Int, value: Float): Float {}

@Extern $[lib: "core", name: "memory_write_boolean"]
@Required
fun memory_write_boolean(ptr: Int, value: Boolean) {}

@Extern $[lib: "core", name: "memory_read_byte"]
@Required
fun memory_read_byte(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_int"]
@Required
fun memory_read_int(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_float"]
@Required
fun memory_read_float(ptr: Int): Float {}

@Extern $[lib: "core", name: "memory_read_boolean"]
@Required
fun memory_read_boolean(ptr: Int): Boolean {}