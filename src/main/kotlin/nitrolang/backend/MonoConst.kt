package nitrolang.backend

import nitrolang.ast.LstConst
import nitrolang.backend.wasm.WasmDataSection

// Global inmutable value stored in the data section
// Monomorphized version of LstConst
data class MonoConst(
    val instance: LstConst,
    val type: MonoType,
) {
    var offset: Int = 0
    var size: Int = 0
    var section: WasmDataSection? = null

    lateinit var code: MonoCode

    override fun toString(): String = instance.fullName
}