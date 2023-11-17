
// Gets a reference to the global memory used as a heap, located after the data section on the wasm memory
@Extern $[lib: "core", name: "memory_get_memory"]
fun get_memory(): MemoryArena {}

// Intrinsic function that allocates a block of memory of the given size in bytes
// used by the compiler to allocate memory when a struct instance is created
@WasmName $[name: "memory_alloc_internal"]
@Required
fun memory_alloc_internal(bytes: Int): Int {
    ret get_memory().alloc_bytes(bytes).get_address()
}

// Intrinsic function that copies a chunk of memory to another location
@Extern $[lib: "core", name: "memory_copy_internal"]
@Required
fun memory_copy_internal(target: Int, source: Int, len: Int) {}

// Intrinsic function that tracks the memory allocated
@Extern $[lib: "core", name: "memory_alloc_trace"]
fun memory_alloc_trace(amount: Int, ptr: Int) {}