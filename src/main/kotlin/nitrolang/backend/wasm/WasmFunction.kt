package nitrolang.backend.wasm

import nitrolang.ast.LstFunction

// WebAssembly function
data class WasmFunction(
    val name: String,
    val params: List<WasmVar>,
    val result: WasmPrimitive,
    val comment: String = "",
    val exportName: String = "",
    val sourceFunction: LstFunction?,
) {
    val locals = mutableListOf<WasmVar>()
    val instructions = mutableListOf<WasmInst>()
}