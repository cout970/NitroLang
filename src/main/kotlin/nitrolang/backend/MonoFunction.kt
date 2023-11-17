package nitrolang.backend

import nitrolang.ast.LstAnnotation
import nitrolang.ast.LstCode
import nitrolang.ast.LstVar
import nitrolang.ast.Ref

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
    var isLambda = false

    val annotations = mutableListOf<LstAnnotation>()
    val params= mutableListOf<MonoVar>()

    private var lastId = 0

    fun nextId(): MonoRef = lastId++
}

class MonoCode(val code: LstCode) {
    var isExternal: Boolean = false
    val providers = mutableMapOf<Ref, MonoProvider>()
    val variables = mutableListOf<MonoVar>()
    val instructions = mutableListOf<MonoInstruction>()
    val helperVars = mutableMapOf<Ref, MonoVar>()
    val varMap = mutableMapOf<LstVar, MonoVar>()

    private var lastId = 0

    fun nextId(): MonoRef = lastId++
}