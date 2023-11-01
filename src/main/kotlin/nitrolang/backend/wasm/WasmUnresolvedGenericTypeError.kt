package nitrolang.backend.wasm

import nitrolang.typeinference.TType

// Error thrown when a TGeneric type cannot be resolved to a TComposite
data class WasmUnresolvedGenericTypeError(
    val msg: String,
    val type: TType,
) : RuntimeException()