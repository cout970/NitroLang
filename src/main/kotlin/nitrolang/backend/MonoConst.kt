package nitrolang.backend

import nitrolang.ast.LstConst

// Global inmutable value stored in the data section
// Monomorphized version of LstConst
data class MonoConst(
    val instance: LstConst,
    val type: MonoType,
) {
    var offset: Int = 0
    var size: Int = 0

    override fun toString(): String = instance.fullName
}