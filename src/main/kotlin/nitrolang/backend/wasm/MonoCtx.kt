package nitrolang.backend.wasm

import nitrolang.ast.LstTypeParameter
import nitrolang.backend.MonoType

class MonoCtx(
    val generics: Map<LstTypeParameter, MonoType> = emptyMap(),
    val parent: MonoCtx? = null,
)