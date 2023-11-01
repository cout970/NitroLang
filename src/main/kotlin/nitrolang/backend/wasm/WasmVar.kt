package nitrolang.backend.wasm

// WebAssembly local or param
data class WasmVar(
    val kind: WasmVarKind,
    val name: String?,
    val type: WasmPrimitive
)