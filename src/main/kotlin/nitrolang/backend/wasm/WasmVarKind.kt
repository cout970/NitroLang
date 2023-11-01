package nitrolang.backend.wasm

// Defines if a WasmVar is a param or a local
enum class WasmVarKind {
    Param,
    Local,
}