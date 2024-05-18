package nitrolang.backend.wasm

import nitrolang.backend.MonoConst
import nitrolang.backend.pad

// Represent a WebAssembly module
class WasmModule {
    var memoryCapacity = 1024 * 16 * (64 * 1024) // Each page is 64 KiB
    val imports = mutableListOf<WasmImport>()
    val sections = mutableListOf<WasmDataSection>()
    val functions = mutableListOf<WasmFunction>()
    val lambdaLabels = mutableListOf<String>()
    val initializers = mutableListOf<Pair<MonoConst, WasmFunction>>()

    var sectionOffset = 0

    fun addSection(section: WasmDataSection) {
        sections += section
        sectionOffset += section.data.size
        sectionOffset = pad(sectionOffset)
    }
}

class WasmDataSection(
    val offset: Int,
    var data: ByteArray,
    val comment: String
)