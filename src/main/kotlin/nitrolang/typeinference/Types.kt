package nitrolang.typeinference

import nitrolang.ast.*
import nitrolang.util.ErrorInfo
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

    override fun toString(): String =
        if (params.isNotEmpty()) "$base<${params.joinToString(", ")}>" else base.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TComposite) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

// T
data class TGeneric(override val id: Int, val instance: LstTypeParameterDef) : TType {
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

sealed interface TTypeBase {
    val id: Int
    val indexKey: String
}

data class TStruct(override val id: Int, val instance: LstStruct) : TTypeBase {
    override val indexKey: String = "S${instance.ref.id}"

    fun isFunction(): Boolean = instance.fullName == "Function" && instance.isIntrinsic

    override fun toString(): String = instance.fullName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TStruct) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

data class TOption(override val id: Int, val instance: LstOption) : TTypeBase {
    override val indexKey: String = "O${instance.ref.id}"

    override fun toString(): String = instance.fullName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TOption) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

data class TOptionItem(override val id: Int, val instance: LstStruct, val option: TOption) : TTypeBase {
    override val indexKey: String = "I${instance.ref.id}"

    override fun toString(): String = instance.fullName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TOptionItem) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

// Lambda function
data class TLambda(override val id: Int, val instance: LstLambdaFunction) : TTypeBase {
    override val indexKey: String = "L${instance.ref.id}"

    override fun toString(): String = instance.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TLambda) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

sealed interface TConstraint {
    val span: Span
}

data class TUnify(val id: Int, val left: TypeBox, val right: TypeBox, override val span: Span) : TConstraint

data class TBounds(val id: Int, val target: TypeBox, val requiredTags: List<LstTag>, override val span: Span) :
    TConstraint

data class TFindField(val id: Int, val dependency: TypeBox, val callback: (TType) -> Unit, override val span: Span) :
    TConstraint

data class TFindFunction(
    val id: Int,
    val dependencies: List<TypeBox>,
    val callback: (List<TType>) -> Unit,
    override val span: Span
) : TConstraint

data class TypeBox(val env: TypeEnv, var type: TType, val span: Span) {
    fun replace(find: TUnresolved, replacement: TType): TypeBox {
        env.apply {
            this@TypeBox.type = this@TypeBox.type.replace(find, replacement)
        }
        return this
    }

    override fun toString(): String = type.toString()
}

interface TypeError {
    val message: String
    val constraint: TConstraint

    fun replace(env: TypeEnv, key: TUnresolved, replacement: TType)
}

class TypeMismatchError(
    var left: TType,
    var right: TType,
    override val constraint: TConstraint,
) : ErrorInfo(
    message = "Type mismatch, expected '$left', found '$right'",
    span = constraint.span,
), TypeError {

    override fun replace(env: TypeEnv, key: TUnresolved, replacement: TType) {
        with(env) {
            left = left.replace(key, replacement)
            right = right.replace(key, replacement)
        }
    }
}

class TypeBoundsError(
    var left: TType,
    val tag: LstTag,
    override val constraint: TConstraint
) : ErrorInfo(
    message = "Unsatisfied type bounds, type '$left' is missing the tag '${tag.fullName}'",
    span = constraint.span
), TypeError {

    override fun replace(env: TypeEnv, key: TUnresolved, replacement: TType) {
        with(env) {
            left = left.replace(key, replacement)
        }
    }
}

// listOf(Some(Ordering::Less), Some(Ordering::Equal), Some(Ordering::Greater), None()) => List<Option<Ordering>>
// listOf(Some(Ordering.Less()), Some(Ordering.Equal()), Some(Ordering.Greater()), None()) => List<Option<Ordering>>
//val a = TComposite(
//    id = 0, base = TStruct(id = 1, "List"), params = listOf(
//        TComposite(
//            id = 2, base = TOption(id = 3, "Option"), params = listOf(
//                TComposite(id = 4, base = TOption(id = 5, "Ordering"), params = emptyList())
//            )
//        )
//    )
//)