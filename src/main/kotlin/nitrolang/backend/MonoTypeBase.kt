package nitrolang.backend

import nitrolang.ast.LstLambdaFunction
import nitrolang.ast.LstOption
import nitrolang.ast.LstStruct

// Monomorphized version of TTypeBase
sealed class MonoTypeBase {
    abstract val id: Int
}

// Struct or OptionItem
data class MonoStruct(
    override val id: Int,
    val instance: LstStruct,
) : MonoTypeBase() {
    val fields: MutableList<MonoStructField> = mutableListOf()
    var size: Int = 0
    var option: MonoOption? = null
    override fun toString(): String = instance.fullName
}

// Option
data class MonoOption(
    override val id: Int,
    val instance: LstOption,
) : MonoTypeBase() {
    lateinit var items: List<MonoStruct>
    // Needs to be computed after the items, otherwise it will cause an infinite recursion loop
    var size: Int = 0

    override fun toString(): String = instance.fullName
}

// Lambada function
data class MonoLambda(
    override val id: Int,
    val instance: LstLambdaFunction,
    val size: Int,
) : MonoTypeBase() {
    override fun toString(): String = "Lambda-$id"
}