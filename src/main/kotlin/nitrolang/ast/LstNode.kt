package nitrolang.ast

import nitrolang.util.HasSpan
import nitrolang.util.Span


// Linearized Syntax Tree

data class LstNodeBlock(val parent: LstNodeBlock?) {
    val depth: Int get() = if (parent == null) 0 else parent.depth + 1
}

abstract class LstNode(
    open val ref: Ref,
    override val span: Span,
    open val block: LstNodeBlock,
) : HasSpan

sealed class LstExpression(id: Ref, span: Span, block: LstNodeBlock) : LstNode(id, span, block)

data class LstMarker(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val role: String
) : LstNode(ref, span, block) {
    override fun toString(): String = "$ref marker $role"
}

data class LstIfJump(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val cond: Ref,
    var middle: Ref? = null,
    var end: Ref? = null
) : LstNode(ref, span, block) {
    override fun toString(): String {
        if (middle !== null) {
            return "$ref if cond: $cond, middle: $middle, end: $end"
        }
        return "$ref if cond: $cond, end: $end"
    }
}

data class LstChoose(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val cond: Ref,
    val ifTrue: Ref,
    val ifFalse: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = choose $cond => $ifTrue | $ifFalse"
}

sealed class LstConstant(id: Ref, span: Span, block: LstNodeBlock) : LstExpression(id, span, block)

data class LstBoolean(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Boolean
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value as Boolean"
}

data class LstInt(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Int
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value as Int"
}

data class LstFloat(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Float
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value as Float"
}

data class LstString(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: String
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = \"$value\" as String"
}

data class LstUnit(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = Unit"
}

data class LstIsType(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val typeUsage: TypeUsage,
    val ty: TypeRef? = null
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = $expr is $typeUsage ($ty)"
}

data class LstAsType(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val typeUsage: TypeUsage,
    var ty: TypeRef? = null
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = cast $expr as $typeUsage ($ty)"
}

data class LstReturn(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = return $expr"
}

data class LstJumpTo(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val backwards: Boolean,
    val role: String,
    var marker: Ref? = null
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref jump to $marker (backwards: $backwards, role: $role)"
}

interface HasVarRef {
    val name: String
    val path: Path
    val block: LstNodeBlock
    var variable: VarRef?
}

data class LstLoadVar(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    override val name: String,
    override val path: Path,
    override var variable: VarRef? = null
) : LstExpression(ref, span, block), HasVarRef {
    override fun toString(): String = "$ref = load_var $name ($variable)"
}

data class LstStoreVar(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    override val name: String,
    override val path: Path,
    val expr: Ref,
    override var variable: VarRef? = null
) : LstExpression(ref, span, block), HasVarRef {
    override fun toString(): String = "$ref store_var $name ($variable) = $expr"
}

data class LstSizeOf(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val typeUsage: TypeUsage,
) : LstExpression(ref, span, block) {
    var typeRef: TypeRef? = null

    override fun toString(): String = "$ref = size_of $typeUsage ($typeRef)"
}

data class LstLoadField(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val name: String
) : LstExpression(ref, span, block) {
    var field: VarRef? = null
    override fun toString(): String = "$ref = load_field $name ($field) $expr"
}

data class LstStoreField(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val name: String,
    val instance: Ref,
    val expr: Ref
) : LstExpression(ref, span, block) {
    var field: VarRef? = null

    override fun toString(): String = "$ref store_field $name ($field) $instance $expr"
}

data class LstAlloc(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val typeUsage: TypeUsage,
    val type: TypeRef? = null
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = alloc $typeUsage ($type)"
}

class LstFunCall(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    var name: String,
    var path: Path,
    var argCount: Int
) : LstExpression(ref, span, block) {
    var funRef: FunRef? = null

    override fun toString(): String = "$ref = call $name ($funRef, args: $argCount)"
}

data class LstFunArg(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref arg $expr"
}

data class LstComment(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val comment: String
) : LstNode(ref, span, block) {
    override fun toString(): String {
        if (comment.contains("\n")) {
            return "$ref \n/**\n * ${comment.replace("\n", "\n * ")}\n */"
        }
        return "$ref // $comment"
    }
}
