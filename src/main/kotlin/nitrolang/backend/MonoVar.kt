package nitrolang.backend

import nitrolang.ast.VarRef

// Monomorphized version of LstVar
data class MonoVar(
    val id: MonoRef,
    val name: String,
    val type: MonoType,
    val varRef: VarRef?,
    val isUpValue: Boolean = false,
) {
    var upValueSlot = 0

    fun finalName(): String = if (isUpValue) "\$upvalue-$name-$id" else "$$name-$id"
}