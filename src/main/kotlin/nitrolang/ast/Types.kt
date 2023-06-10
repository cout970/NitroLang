package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.util.Dumpable
import nitrolang.util.dump

data class TypeTree(
    val base: TypeBase,
    val params: List<TypeTree> = emptyList()
) : Dumpable {

    fun isUnit(): Boolean = base is StructType && base.struct.isBuiltin && base.struct.name === "Unit"

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
}

data class OptionType(val option: LstOption) : TypeBase() {
    override fun toString(): String = option.fullName

    override fun toDebugString(): String = "option:${option.fullName}#${option.ref.id}"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("kind", "option".dump())
        it.add("ref", option.ref.dump())
        it.add("name", option.fullName.dump())
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
}

data class UnresolvedType(val ref: UnresolvedTypeRef) : TypeBase() {
    override fun toString(): String = "<$ref>"

    override fun toDebugString(): String = "unresolved#${ref.id}"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("kind", "unresolved".dump())
        it.add("ref", ref.dump())
        it.add("name", "<unresolved ${ref.id}>".dump())
    }
}

object InvalidType : TypeBase() {

    fun toTypeTree(): TypeTree = TypeTree(base = InvalidType)

    override fun toDebugString(): String = "<InvalidType>"

    override fun dump(): JsonElement = toDebugString().dump()
}