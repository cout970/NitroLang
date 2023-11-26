package nitrolang.backend.wasm

import nitrolang.backend.MonoType

// WebAssembly local or param
data class WasmVar(
    val kind: WasmVarKind,
    val name: String?,
    val type: WasmPrimitive,
    var monoType: MonoType? = null,
)