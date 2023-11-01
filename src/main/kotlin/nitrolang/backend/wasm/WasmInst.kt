package nitrolang.backend.wasm

// WebAssembly instruction
class WasmInst(
    val opcode: String,
    var comment: String? = null,
    var needsWrapping: Boolean = true,
)