package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.util.Dumpable
import nitrolang.util.dump
import java.lang.RuntimeException

data class TypeTree(
    val base: TypeBase,
    val params: List<TypeTree> = emptyList()
) : Dumpable {

    fun isUnit(): Boolean = base is StructType && base.struct.isExternal && base.struct.fullName == "Unit"

    fun isInt(): Boolean = base is StructType && base.struct.isExternal && base.struct.fullName == "Int"

    fun isFloat(): Boolean = base is StructType && base.struct.isExternal && base.struct.fullName == "Float"

    fun isBoolean(): Boolean = base is StructType && base.struct.isExternal && base.struct.fullName == "Float"

    fun isOptionItem(): Boolean = base is StructType && base.struct.parentOption != null

    fun isNever(): Boolean = base is StructType && base.struct.fullName == "Never"

    fun isGeneric(): Boolean = base is ParamType

    fun hasUnresolved(): Boolean = base is UnresolvedType || params.any { it.hasUnresolved() }

    override fun toString(): String {
        if (params.isEmpty()) return base.toString()

        return "$base<${params.joinToString(",")}>"
    }

    fun toDebugString(): String {
        if (params.isEmpty()) return base.toDebugString()

        return "${base.toDebugString()}<${params.joinToString(",") { it.toDebugString() }}>"
    }

    override fun dump(): JsonElement = toDebugString().dump()

    override fun equals(other: Any?): Boolean {
//        if (RuntimeException().stackTrace.none { it.methodName == "canBeAssignedTo" }) {
//            error("Using incorrect equals function!")
//        }

        if (this === other) return true
        if (other !is TypeTree) return false

        if (base != other.base) return false
        return params == other.params
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }
}

fun TypeTree?.isInvalid(): Boolean = this == null || base == InvalidType || params.any { it.isInvalid() }

sealed class TypeBase : Dumpable {
    abstract fun toDebugString(): String
}

data class StructType(val struct: LstStruct) : TypeBase() {
    override fun toString(): String = struct.fullName

    override fun toDebugString(): String = "struct:${struct.fullName}#${struct.ref.id}"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("kind", "struct".dump())
        it.add("ref", struct.ref.dump())
        it.add("name", struct.fullName.dump())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StructType) return false

        return struct.ref == other.struct.ref
    }

    override fun hashCode(): Int {
        return struct.ref.hashCode()
    }
}

data class OptionType(val option: LstOption) : TypeBase() {
    override fun toString(): String = option.fullName

    override fun toDebugString(): String = "option:${option.fullName}#${option.ref.id}"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("kind", "option".dump())
        it.add("ref", option.ref.dump())
        it.add("name", option.fullName.dump())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptionType) return false

        return option.ref == other.option.ref
    }

    override fun hashCode(): Int {
        return option.ref.hashCode()
    }
}

data class ParamType(val param: TypeParameter) : TypeBase() {
    override fun toString(): String = "#${param.name}"

    override fun toDebugString(): String = "param:${param.name}#${param.ref.id}"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("kind", "param".dump())
        it.add("ref", param.ref.dump())
        it.add("name", param.name.dump())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParamType) return false

        return param.ref == other.param.ref
    }

    override fun hashCode(): Int {
        return param.ref.hashCode()
    }
}

data class UnresolvedType(val ref: UnresolvedTypeRef) : TypeBase() {
    override fun toString(): String = "<$ref>"

    override fun toDebugString(): String = "unresolved#${ref.id}"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("kind", "unresolved".dump())
        it.add("ref", ref.dump())
        it.add("name", "<unresolved ${ref.id}>".dump())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnresolvedType) return false

        return ref == other.ref
    }

    override fun hashCode(): Int {
        return ref.hashCode()
    }
}

object InvalidType : TypeBase() {

    fun toTypeTree(): TypeTree = TypeTree(base = InvalidType)

    override fun toDebugString(): String = "<InvalidType>"

    override fun dump(): JsonElement = toDebugString().dump()

    override fun hashCode(): Int {
        return -1
    }
}