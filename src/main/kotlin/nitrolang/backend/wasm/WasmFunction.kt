package nitrolang.backend.wasm

// WebAssembly function
data class WasmFunction(
    val name: String,
    val params: List<WasmVar>,
    val results: List<WasmPrimitive>,
    val comment: String = "",
    val exportName: String = "",
) {
    val locals = mutableListOf<WasmVar>()
    val instructions = mutableListOf<WasmInst>()
}