package nitrolang.backend.wasm

class WasmModule {
    val imports = mutableListOf<WasmImport>()
    val sections = mutableListOf<Pair<Int, String>>()
    val functions = mutableListOf<WasmFunction>()

    var sectionOffset = 0
}

@Suppress("EnumEntryName")
enum class WasmPrimitive {
    i32,
    u32,
    i64,
    u64,
    f32,
    f64
}

data class WasmImport(
    val module: String,
    val function: String,
    val functionDefinition: WasmFunctionDefinition
)

data class WasmFunction(
    val definition: WasmFunctionDefinition,
) {
    val locals = mutableListOf<WasmVar>()
    val instructions = mutableListOf<WasmInst>()
}

data class WasmFunctionDefinition(
    val name: String,
    val params: List<WasmVar>,
    val results: List<WasmPrimitive>,
    val comment: String = "",
    val exportName: String = "",
)

data class WasmVar(val kind: WasmVarKind, val name: String?, val type: WasmPrimitive)

enum class WasmVarKind { Param, Local }

class WasmInst(val opcode: String) {
    var comment: String? = null
    var needsWrapping = true
}
