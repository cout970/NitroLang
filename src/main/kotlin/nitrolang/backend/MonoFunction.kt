package nitrolang.backend

import nitrolang.ast.*

// Global static function
// Monomorphized version of LstConst
class MonoFunction(
    val id: Int,
    val signature: MonoFuncSignature,
    val finalName: String
) {
    lateinit var name: String
    lateinit var returnType: MonoType
    lateinit var code: MonoCode

    val annotations = mutableListOf<LstAnnotation>()
}

class MonoCode(val code: LstCode) {
    var isExternal: Boolean = false
    val params = mutableListOf<MonoParam>()
    val providers = mutableMapOf<Ref, MonoProvider>()
    val variables = mutableListOf<MonoVar>()
    val instructions = mutableListOf<MonoInstruction>()
    val helperVars = mutableMapOf<Ref, MonoVar>()
    val variableMap = mutableMapOf<VarRef, MonoVar>()
    val upValues = mutableListOf<MonoVar>()
    var isLambda = false

    private var lastId = 0

    fun nextId(): MonoRef = lastId++
}