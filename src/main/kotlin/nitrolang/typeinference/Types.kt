package nitrolang.typeinference

import nitrolang.ast.LstOption
import nitrolang.ast.LstStruct
import nitrolang.ast.LstTag
import nitrolang.ast.LstTypeParameterDef
import nitrolang.util.Span

sealed interface TType {
    val id: Int

    fun isNothing(): Boolean = this.isNamed("Nothing")

    fun isNever(): Boolean = this.isNamed("Never")

    private fun isNamed(name: String): Boolean =
        this is TComposite && this.base is TStruct && this.base.instance.fullName == name

    fun isInstantiable(): Boolean = this is TComposite && (this.base is TStruct || this.base is TOptionItem)
}

// Map<String, T>
data class TComposite(override val id: Int, val base: TTypeBase, val params: List<TType>) : TType {
    override fun toString(): String =
        if (params.isNotEmpty()) "$base<${params.joinToString(", ")}>" else base.toString()
}

// T
data class TGeneric(override val id: Int, val instance: LstTypeParameterDef) : TType {
    override fun toString(): String = instance.toString()
}

// ?
data class TUnresolved(override val id: Int, val span: Span) : TType {
    override fun toString(): String = "Unresolved($id)"
}

// Int|Float
data class TUnion(override val id: Int, val options: Set<TType>) : TType {
    override fun toString(): String = options.joinToString("|")
}

// <error>
data class TInvalid(override val id: Int, val span: Span, val error: String) : TType {
    override fun toString(): String = "Error($id, $error, $span)"
}

sealed interface TTypeBase {
    val id: Int
}

data class TStruct(override val id: Int, val instance: LstStruct) : TTypeBase {
    override fun toString(): String = instance.fullName
}

data class TOption(override val id: Int, val instance: LstOption) : TTypeBase {
    override fun toString(): String = instance.fullName
}

data class TOptionItem(override val id: Int, val instance: LstStruct, val option: TOption) : TTypeBase {
    override fun toString(): String = instance.fullName
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
    val msg: String
    var constraint: TConstraint

    fun replace(env: TypeEnv, key: TUnresolved, replacement: TType)
}

class TypeMismatchError(var left: TType, var right: TType) : TypeError {
    override val msg: String get() = "Type mismatch, expected '$left', found '$right'"
    override lateinit var constraint: TConstraint

    override fun replace(env: TypeEnv, key: TUnresolved, replacement: TType) {
        with(env) {
            left = left.replace(key, replacement)
            right = right.replace(key, replacement)
        }
    }
}

class TypeBoundsError(var left: TType, val tag: LstTag, override var constraint: TConstraint) : TypeError {
    override val msg: String get() = "Unsatisfied type bounds, type '$left' is missing the tag '${tag.fullName}'"

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