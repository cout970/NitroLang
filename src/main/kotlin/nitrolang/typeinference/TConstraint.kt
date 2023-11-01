package nitrolang.typeinference

import nitrolang.ast.LstTag
import nitrolang.util.Span

sealed interface TConstraint {
    val span: Span
}

data class TBounds(
    val id: Int,
    val target: TypeBox,
    val requiredTags: List<LstTag>,
    override val span: Span
) : TConstraint

data class TFindField(
    val id: Int,
    val dependency: TypeBox,
    val callback: (TType) -> Unit,
    override val span: Span
) : TConstraint

data class TFindFunction(
    val id: Int,
    val dependencies: List<TypeBox>,
    val callback: (List<TType>) -> Unit,
    override val span: Span
) : TConstraint

data class TUnify(
    val id: Int,
    val left: TypeBox,
    val right: TypeBox,
    override val span: Span
) : TConstraint
