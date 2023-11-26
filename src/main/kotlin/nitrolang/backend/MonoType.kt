package nitrolang.backend

import nitrolang.parsing.ANNOTATION_INTRINSIC
import nitrolang.parsing.ANNOTATION_VALUE_TYPE

// Monomorphized version of TType
// Only represents TComposite, since all instances of TGeneric are replaced with TComposite
// and TUnresolved, TUnion and TInvalid never reach this stage
data class MonoType(
    val id: Int,
    val base: MonoTypeBase,
    val params: List<MonoType>
) {

    fun isNothing() = isNamed("Nothing")
    fun isNever() = isNamed("Never")
    fun isInt() = isNamed("Int")
    fun isLong() = isNamed("Long")
    fun isFloat() = isNamed("Float")
    fun isBoolean() = isNamed("Boolean")
    fun isFunction() = isNamed("Function")
    fun isNumber() = isFloat() || isInt() || isLong()
    fun isLambda() = base is MonoLambda
    fun isOption() = base is MonoOption
    fun isOptionItem() = base is MonoStruct && base.instance.parentOption != null
    fun isOptionOrOptionItem() = isOption() || isOptionItem()
    fun isValueType() = base is MonoStruct && base.instance.getAnnotation(ANNOTATION_VALUE_TYPE) != null
    fun isIntrinsic() = base is MonoStruct && base.instance.getAnnotation(ANNOTATION_INTRINSIC) != null

    fun isNamed(name: String) = base is MonoStruct && base.instance.fullName == name

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