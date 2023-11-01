package nitrolang.backend.wasm

// Represent a WebAssembly module
class WasmModule {
    val imports = mutableListOf<WasmImport>()
    val sections = mutableListOf<Pair<Int, String>>()
    val functions = mutableListOf<WasmFunction>()
    val lambdaLabels = mutableListOf<String>()

    var sectionOffset = 0
}