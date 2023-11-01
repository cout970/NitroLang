package nitrolang.backend.wasm

// WebAssembly import
data class WasmImport(
    val module: String,
    val functionName: String,
    val function: WasmFunction,
)