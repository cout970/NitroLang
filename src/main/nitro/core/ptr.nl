
// Pointer to a single value stored in heap,
// if the value is a @ValueType, the pointer itself will be the value
@Extern $[lib: "core", name: "Ptr"]
@ValueType
@Intrinsic
struct Ptr<#Value> {}

// Converts a reference to a value into a Pointer to the value
// References are guaranteed to be valid, while pointers are just addresses to memory that can be manipulated
@Extern $[lib: "core", name: "ptr_ptr_of"]
fun ptr_of(value: #Value): Ptr<#Value> {}

// Obtains a null pointer
fun null_ptr<#Value>(): Ptr<#Value> {
    ret ptr_from_address<#Value>(0)
}

// Create a pointer given a numerical address of memory
@Extern $[lib: "core", name: "ptr_ptr_from_address"]
fun ptr_from_address<#Value>(address: Int): Ptr<#Value> {}

// Checks if the pointer is null, i.e. created by null_ptr()
fun Ptr<#Value>.is_null(): Boolean {
    ret this.get_address() == 0
}

// Converts the pointer into a reference
// if the pointer is null, the program will crash
@Extern $[lib: "core", name: "ptr_as_ref"]
fun Ptr<#Value>.unsafe_as_ref(): #Value {}

// Convert the pointer to a numerical address of memory
@Extern $[lib: "core", name: "ptr_get_address"]
fun Ptr<#Value>.get_address(): Int {}

// Cast a pointer of one type to the pointer of another type
// This operation is inherently unsafe and must be avoided as much as possible
@Extern $[lib: "core", name: "ptr_unsafe_cast"]
fun <#Value, #Target> Ptr<#Value>.unsafe_cast(): Ptr<#Target> {}

// Override a pointer, given a pointer to the pointer
// Same in C:
// `void write(int **this, int *ptr) { *this = ptr; }`
@Extern $[lib: "core", name: "ptr_write"]
fun Ptr<Ptr<#Value>>.write(value: Ptr<#Value>) {}

// Writes a copy of the value provided into the memory pointed to by this pointer
fun Ptr<#Value>.write_copy(value: #Value) {
    if is_encoded_in_ref(value) {
        this.unsafe_cast().write(ptr_of(value))
    } else {
        ptr_of(value).copy_into_internal(this, size_of<#Value>)
    }
}

@Extern $[lib: "core", name: "ptr_copy_into"]
fun Ptr<#Item>.copy_into_internal(other: Ptr<#Item>, byte_len: Int) {}

// Read a pointer, given a pointer to the pointer
// Same in C:
// `int * read(int **this) { return *this; }`
@Extern $[lib: "core", name: "ptr_read"]
fun Ptr<Ptr<#Value>>.read(): Ptr<#Value> {}

// Creates a new pointer that is [num_bytes] ahead of this
fun Ptr<#Value>.offset_in_bytes(num_bytes: Int): Ptr<#Value> {
    ret ptr_from_address(this.get_address() + num_bytes)
}

// Cast the pointer into a pointer to an array of values in memory
@Extern $[lib: "core", name: "ptr_to_raw_array"]
fun Ptr<#Value>.to_raw_array(): RawArray<#Value> {}

// Debug representation of the pointer
fun Ptr<#Value>.to_string(): String {
    ret "0x".concat(this.get_address().to_string_in_base(16))
}