package nitrolang.backend

import nitrolang.ast.FunRef
import nitrolang.ast.LstFunction
import nitrolang.ast.LstLambdaFunction

// Signature of a monomorphized function, uniquely identifies a wasm function
class MonoFuncSignature(
    val funRef: FunRef,
    val fullName: String,
    val paramTypes: List<MonoType>,
    val returnType: MonoType
) {
    var function: LstFunction? = null
    var lambda: LstLambdaFunction? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MonoFuncSignature) return false

        if (funRef.id != other.funRef.id) return false
        if (paramTypes != other.paramTypes) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = funRef.id
        result = 31 * result + paramTypes.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    override fun toString(): String {
        return "${fullName}(${paramTypes.joinToString(", ")}): $returnType"
    }
}