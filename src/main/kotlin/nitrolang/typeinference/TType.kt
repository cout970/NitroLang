package nitrolang.typeinference

import nitrolang.ast.LstTypeParameter
import nitrolang.util.Span

sealed interface TType {
    val id: Int
    val indexKey: String

    fun isNothing(): Boolean = this.isNamed("Nothing")

    fun isNever(): Boolean = this.isNamed("Never")

    private fun isNamed(name: String): Boolean =
        this is TComposite && this.base is TStruct && this.base.instance.fullName == name

    fun isInstantiable(): Boolean = this is TComposite && (this.base is TStruct || this.base is TOptionItem)
}

// Map<String, T>
data class TComposite(override val id: Int, val base: TTypeBase, val params: List<TType>) : TType {
    override val indexKey: String = "C${base.id}<${params.joinToString(",") { it.indexKey }}>"

    override fun toString(): String {
        if (base is TStruct && base.isFunction() && params.isNotEmpty()) {
            return "(${params.dropLast(1).joinToString(", ")}) -> ${params.last()}"
        }
        if (base is TLambda && params.isNotEmpty()) {
            return "lambda: (${params.dropLast(1).joinToString(", ")}) -> ${params.last()}"
        }
        return if (params.isNotEmpty()) "$base<${params.joinToString(", ")}>" else base.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TComposite) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

// T
data class TGeneric(override val id: Int, val instance: LstTypeParameter) : TType {
    override val indexKey: String = "G${instance.ref.id}"
    override fun toString(): String = instance.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TGeneric) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

// <error>
data class TInvalid(override val id: Int, val span: Span, val error: String) : TType {
    override val indexKey: String = "E$id"
    override fun toString(): String = "Error($id, $error, $span)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TInvalid) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

// Int|Float
data class TUnion(override val id: Int, val options: Set<TType>) : TType {
    override val indexKey: String = "U<${options.sortedBy { it.indexKey }.joinToString(",") { it.indexKey }}>"
    override fun toString(): String = options.joinToString("|")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TUnion) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

// ?
data class TUnresolved(override val id: Int, val span: Span) : TType {
    override val indexKey: String = "?$id"
    override fun toString(): String = "Unresolved($id)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TUnresolved) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}