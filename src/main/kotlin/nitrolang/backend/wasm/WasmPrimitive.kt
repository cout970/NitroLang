package nitrolang.backend.wasm

@Suppress("EnumEntryName")
enum class WasmPrimitive {
    // 32 bit integers
    i32,
    u32,

    // 64 bit integers
    i64,
    u64,

    // floating point numbers
    f32,
    f64
}