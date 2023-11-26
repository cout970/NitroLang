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
    var dupCount = 0
    var swapCount = 0

    fun dup(type: WasmPrimitive) {
        val index = dupCount++
        locals += WasmVar(WasmVarKind.Local, "\$dup$index", type)
        instructions += WasmInst("local.tee \$dup$index").also { it.comment = "Dup" }
        instructions += WasmInst("local.get \$dup$index")
    }

    fun swap(firstType: WasmPrimitive, secondType: WasmPrimitive = firstType) {
        val index = swapCount++
        locals += WasmVar(WasmVarKind.Local, "\$swap$index", firstType)
        locals += WasmVar(WasmVarKind.Local, "\$swap_rev$index", secondType)
        // a b -- b a
        instructions += WasmInst("local.set \$swap$index").also { it.comment = "Swap" }
        instructions += WasmInst("local.set \$swap_rev$index")
        instructions += WasmInst("local.get \$swap$index")
        instructions += WasmInst("local.get \$swap_rev$index")
    }
}