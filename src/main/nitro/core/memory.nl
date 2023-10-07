// Represents and area on the heap where the user can allocate instances
struct MemoryArena {
    capacity: Int
    len: Int
    bytes: RawArray<Byte>
}

// Gets a reference to the global memory used as a heap, located after the data section on the wasm memory
@Extern $[lib: "core", name: "memory_get_memory"]
fun get_memory(): MemoryArena {}

@Extern $[lib: "core", name: "debug_alloc_bytes"]
fun debug_alloc_bytes(amount: Int, ptr: Int) {}

fun MemoryArena.alloc_bytes(bytes: Int): Ptr<Byte> {
    let next = this.len
    let ptr_size = size_of<Ptr<Byte>>

    let pad = (ptr_size - next) % ptr_size;
    next = next + (if pad.less_than_signed(0) { pad + ptr_size } else { pad })

    debug_alloc_bytes(bytes, next)

    this.len = next + bytes
    return this.bytes.get_ptr(next)
}

fun MemoryArena.alloc<#Value>(): Ptr<#Value> {
    ret this.alloc_bytes(size_of<#Value>).unsafe_cast()
}

@Extern $[lib: "core", name: "memory_dump"]
fun MemoryArena.dump() {}

//

@Extern $[lib: "core", name: "memory_copy"]
fun memory_copy(src: Int, dst: Int, byte_len: Int) {}

@Extern $[lib: "core", name: "memory_write_byte"]
fun memory_write_byte(ptr: Int, value: Int) {}

@Extern $[lib: "core", name: "memory_write_int"]
fun memory_write_int(ptr: Int, value: Int) {}

@Extern $[lib: "core", name: "memory_write_float"]
fun memory_write_float(ptr: Int, value: Float): Float {}

@Extern $[lib: "core", name: "memory_write_boolean"]
fun memory_write_boolean(ptr: Int, value: Boolean) {}

@Extern $[lib: "core", name: "memory_read_byte"]
fun memory_read_byte(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_int"]
fun memory_read_int(ptr: Int): Int {}

@Extern $[lib: "core", name: "memory_read_float"]
fun memory_read_float(ptr: Int): Float {}

@Extern $[lib: "core", name: "memory_read_boolean"]
fun memory_read_boolean(ptr: Int): Boolean {}
