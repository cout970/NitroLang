// Pointer to an array of values stored sequentially in heap
// The size of the array must be keep separately and proper check must be used to avoid crashes
// Note: the array *can* be a null pointer
//
// It's recommended to use List<#Item> instead of this low level representation
//
@Extern $[lib: "core", name: "RawArray"]
@StackValue
struct RawArray<#Item> {}

fun create_raw_array<#Item>(len: Int): RawArray<#Item> {
    let ptr: Ptr<Byte> = get_memory().alloc_bytes(size_of<#Item> * len)
    ret ptr.unsafe_cast().to_raw_array<#Item>()
}

// Gets the pointer to the first element of the array
@Extern $[lib: "core", name: "raw_array_to_ptr"]
fun RawArray<#Item>.to_ptr(): Ptr<#Item> {}

// Gets the pointer to an element of the array
// The array bounds are not checked
fun RawArray<#Item>.get_ptr(index: Int): Ptr<#Item> {
    ret this.to_ptr().offset_in_bytes(size_of<#Item> * index)
}

// Gets a reference to a value in the array
// The array bounds are not checked
fun RawArray<#Item>.get(index: Int): #Item {
    ret this.get_ptr(index).get_value()
}

// Returns the address of the array
@Extern $[lib: "core", name: "raw_array_copy_into"]
fun RawArray<#Item>.copy_into(other: RawArray<#Item>, len: Int) {}

// Returns the address of the array
fun RawArray<#Item>.get_address(): Int {
    ret this.to_ptr().get_address()
}

// Debug representation of the pointer to the array
fun RawArray<#Item>.to_debug_string(): String {
    ret this.to_ptr().to_debug_string()
}
