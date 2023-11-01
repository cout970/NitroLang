package nitrolang.backend

import nitrolang.ast.LstAnnotation
import nitrolang.ast.Ref

// Global static function
// Monomorphized version of LstConst
class MonoFunction(
    val id: Int,
    val signature: MonoFuncSignature,
    val finalName: String
) {
    lateinit var name: String
    lateinit var params: List<MonoVar>
    lateinit var returnType: MonoType

    var isLambda = false
    var isExternal: Boolean = false
    val annotations = mutableListOf<LstAnnotation>()
    val providers = mutableMapOf<Ref, MonoProvider>()
    val variables = mutableListOf<MonoVar>()
    val instructions = mutableListOf<MonoInstruction>()

    private var lastId = 0

    fun nextId(): MonoRef = lastId++
}