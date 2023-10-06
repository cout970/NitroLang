package nitrolang.backend.wasm

import nitrolang.ast.*
import nitrolang.parsing.ANNOTATION_STACK_VALUE
import nitrolang.util.Span

const val PTR_SIZE: Int = 4

data class MonoType(
    val id: Int,
    val base: MonoTypeBase,
    val params: List<MonoType>
) {

    fun isNothing() = isNamed("Nothing")
    fun isNever() = isNamed("Never")
    fun isInt() = isNamed("Int")
    fun isFloat() = isNamed("Float")
    fun isBoolean() = isNamed("Boolean")

    fun isStackBased() = base is MonoStruct && base.instance.getAnnotation(ANNOTATION_STACK_VALUE) != null

    fun isNamed(name: String) = base is MonoStruct && base.instance.fullName == name

    fun stackSize(): Int {
        return when (base) {
            is MonoOption -> PTR_SIZE
            is MonoStruct -> PTR_SIZE
        }
    }

    fun heapSize(): Int {
        return when (base) {
            is MonoOption -> base.size
            is MonoStruct -> {
                when (base.instance.fullName) {
                    "Never" -> 0
                    "Nothing" -> 0
                    "Byte" -> 1
                    "Boolean" -> 1
                    "Short" -> 2
                    "Char" -> 4
                    "Int" -> 4
                    "Float" -> 4
                    else -> base.size
                }
            }
        }
    }

    override fun toString(): String {
        return if (params.isNotEmpty()) "$base<${params.joinToString(", ")}>" else base.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MonoType) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

sealed class MonoTypeBase {
    abstract val id: Int
}

data class MonoConst(
    val instance: LstConst,
    val type: MonoType,
) {
    var offset: Int = 0
    var size: Int = 0
    override fun toString(): String = instance.fullName
}

data class MonoStruct(
    override val id: Int,
    val instance: LstStruct,
    val fields: List<MonoStructField>,
    val size: Int,
) : MonoTypeBase() {
    var option: MonoOption? = null
    override fun toString(): String = instance.fullName
}

data class MonoStructField(
    val name: String,
    val type: MonoType,
    val size: Int,
    val offset: Int,
)

data class MonoOption(
    override val id: Int,
    val instance: LstOption,
) : MonoTypeBase() {
    lateinit var items: List<MonoStruct>
    var size: Int = 0

    override fun toString(): String = instance.fullName
}

data class MonoVar(
    val id: MonoRef,
    val name: String,
    val type: MonoType
) {
    fun finalName(index: Int): String {
        return if (index == 0) "$$name-$id" else "$$name-$id-$index"
    }
}

sealed class ConstValue

data class ConstInt(val value: Int) : ConstValue()
data class ConstFloat(val value: Float) : ConstValue()
data class ConstBoolean(val value: Boolean) : ConstValue()
data class ConstString(val value: String) : ConstValue()
data object ConstNothing : ConstValue()

typealias MonoRef = Int

sealed class MonoNode(
    val id: MonoRef,
    val span: Span,
)

class MonoBoolean(
    id: MonoRef, span: Span,
    val value: Boolean,
) : MonoNode(id, span)

class MonoInt(
    id: MonoRef, span: Span,
    val value: Int,
) : MonoNode(id, span)

class MonoFloat(
    id: MonoRef, span: Span,
    val value: Float,
) : MonoNode(id, span)

class MonoString(
    id: MonoRef, span: Span,
    val value: String,
    val type: MonoType,
) : MonoNode(id, span)

class MonoNothing(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoIfStart(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoIfElse(
    id: MonoRef, span: Span
) : MonoNode(id, span)

// ifTrue ifFalse cond -- result
class MonoIfChoose(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoBlockStart(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoEnd(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoLoopStart(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoJump(
    id: MonoRef, span: Span,
    val depth: Int,
) : MonoNode(id, span)


class MonoIsType(
    id: MonoRef, span: Span,
    val expr: MonoRef,
    val type: MonoType,
) : MonoNode(id, span)

class MonoAsType(
    id: MonoRef, span: Span,
    val expr: MonoRef,
    val type: MonoType,
) : MonoNode(id, span)

class MonoReturn(
    id: MonoRef, span: Span,
) : MonoNode(id, span)

class MonoComment(
    id: MonoRef, span: Span,
    val msg: String,
) : MonoNode(id, span)

class MonoLoadConst(
    id: MonoRef, span: Span,
    val const: MonoConst,
) : MonoNode(id, span)

class MonoLoadVar(
    id: MonoRef, span: Span,
    val variable: MonoVar,
) : MonoNode(id, span)

class MonoStoreVar(
    id: MonoRef, span: Span,
    val variable: MonoVar,
) : MonoNode(id, span)

class MonoLoadField(
    id: MonoRef, span: Span,
    val instanceType: MonoType,
    val field: MonoStructField,
) : MonoNode(id, span)

class MonoStoreField(
    id: MonoRef, span: Span,
    val instanceType: MonoType,
    val field: MonoStructField,
) : MonoNode(id, span)

class MonoFunCall(
    id: MonoRef, span: Span,
    val function: MonoFunction,
) : MonoNode(id, span)

class MonoOpcode(
    id: MonoRef, span: Span,
    val opcode: String,
) : MonoNode(id, span)

class MonoDup(
    id: MonoRef, span: Span,
    val auxLocal: MonoVar,
    val type: MonoType,
) : MonoNode(id, span)

class MonoSwap(
    id: MonoRef, span: Span,
    val auxLocal0: MonoVar,
    val auxLocal1: MonoVar,
) : MonoNode(id, span)

class MonoFunction(val id: Int, val signature: MonoFuncSignature, val finalName: String) {
    lateinit var name: String
    lateinit var params: List<MonoVar>
    lateinit var returnType: MonoType
    lateinit var sourceFunction: LstFunction

    val providers = mutableMapOf<Ref, MonoProvider>()
    val locals = mutableListOf<MonoVar>()
    val instructions = mutableListOf<MonoNode>()
    private var lastId = 0

    fun nextId(): MonoRef = lastId++
}

class MonoProvider(
    id: MonoRef, span: Span,
    val type: MonoType
) : MonoNode(id, span) {
    val consumers = mutableListOf<MonoConsumer>()
}

class MonoConsumer(
    id: MonoRef, span: Span,
    val provider: MonoProvider
) : MonoNode(id, span) {
    var variable: MonoVar? = null

    init {
        provider.consumers += this
    }
}

class MonoDrop(id: MonoRef, span: Span, val type: MonoType) : MonoNode(id, span)
class MonoIgnore(id: MonoRef, span: Span) : MonoNode(id, span)