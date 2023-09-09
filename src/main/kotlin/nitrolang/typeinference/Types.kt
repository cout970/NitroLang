package nitrolang.typeinference

import nitrolang.ast.LstOption
import nitrolang.ast.LstStruct
import nitrolang.ast.LstTag
import nitrolang.ast.TypeParameter
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
data class TGeneric(override val id: Int, val instance: TypeParameter) : TType {
    override fun toString(): String = "[generic $id $instance]"
}

// ?
data class TUnresolved(override val id: Int, val span: Span) : TType {
    override fun toString(): String = "[unresolved $id]"
}

data class TTag(override val id: Int, val instance: LstTag) : TType {
    override fun toString(): String = instance.fullName
}

// Int|Float
data class TUnion(override val id: Int, val options: Set<TType>) : TType {
    override fun toString(): String = "[union $options]"
}

// <error>
data class TInvalid(override val id: Int, val error: String) : TType {
    override fun toString(): String = "[invalid $id $error]"
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

data class TypeError(val msg: String, var left: TType, var right: TType) {
    lateinit var constraint: TUnify
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