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

// Allocates a number of bytes on the heap, returning a pointer to the start of the allocation
fun MemoryArena.alloc_bytes(bytes: Int): Ptr<Byte> {
    let next = this.len
    let ptr_size = size_of<Ptr<Byte>>

    let pad = (ptr_size - next) % ptr_size;
    next = next + (if pad.less_than_signed(0) { pad + ptr_size } else { pad })

    debug_alloc_bytes(bytes, next)

    this.len = next + bytes
    return this.bytes.get_ptr(next)
}

// Allocates a value on the heap, using the size of the value to determine the number of bytes to allocate
fun MemoryArena.alloc<#Value>(): Ptr<#Value> {
    ret this.alloc_bytes(size_of<#Value>).unsafe_cast()
}

// Prints the contents of the heap to the console, for debugging purposes
@Extern $[lib: "core", name: "memory_dump"]
fun MemoryArena.dump() {}

// Efficiently copies a number of bytes from one location to another
// Must not overlap
@Extern $[lib: "core", name: "memory_copy_within"]
fun MemoryArena.copy_within(src_offset: Int, dst_offset: Int, byte_len: Int) {}

// Reads a single byte from the memory
@Extern $[lib: "core", name: "memory_read_byte"]
fun MemoryArena.read_byte(offset: Int): Byte {}

// Writes a single byte to the memory
@Extern $[lib: "core", name: "memory_write_byte"]
fun MemoryArena.write_byte(offset: Int, value: Byte) {}

// Reads a single integer from the memory
@Extern $[lib: "core", name: "memory_read_int"]
fun MemoryArena.read_int(offset: Int): Int {}

// Writes a single integer to the memory
@Extern $[lib: "core", name: "memory_write_int"]
fun MemoryArena.write_int(offset: Int, value: Int) {}

// Reads a single float from the memory
@Extern $[lib: "core", name: "memory_read_float"]
fun MemoryArena.read_float(ptr: Int): Float {}

// Writes a single float to the memory
@Extern $[lib: "core", name: "memory_write_float"]
fun MemoryArena.write_float(offset: Int, value: Float) {}
