package nitrolang.backend

import nitrolang.typeinference.TType

// Error thrown when a TGeneric type cannot be resolved to a TComposite
data class UnresolvedGenericTypeError(
    val msg: String,
    val type: TType,
) : RuntimeException()