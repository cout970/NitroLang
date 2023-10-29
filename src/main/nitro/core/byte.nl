
// This type is used to represent a byte, a sequence of 8 bits, the minimal addressable unit of memory
@Extern $[lib: "core", name: "Byte"]
@ValueType
@Intrinsic
struct Byte {}

// This function converts a byte to an integer
@Extern $[lib: "core", name: "byte_to_int"]
fun Byte.to_int(): Int {}
