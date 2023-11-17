package nitrolang.backend

import nitrolang.ast.LstTypeParameter

class MonoCtx(
    val generics: Map<LstTypeParameter, MonoType> = emptyMap(),
    val parent: MonoCtx? = null,
)