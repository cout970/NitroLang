package nitrolang.ast

import nitrolang.util.HasSpan
import nitrolang.util.Span


// Linearized Syntax Tree

abstract class LstNode(open val ref: Ref) : HasSpan

sealed class LstExpression(id: Ref) : LstNode(id)

data class LstMarker(
    override val ref: Ref,
    override val span: Span,
    val role: String
) : LstNode(ref) {
    override fun toString(): String = "$ref marker $role"
}

data class LstIfJump(
    override val ref: Ref,
    override val span: Span,
    val cond: Ref,
    var middle: Ref? = null,
    var end: Ref? = null
) : LstNode(ref) {
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
    val cond: Ref,
    val ifTrue: Ref,
    val ifFalse: Ref
) : LstExpression(ref) {
    override fun toString(): String = "$ref = choose $cond => $ifTrue | $ifFalse"
}

sealed class LstConstant(id: Ref) : LstExpression(id)

data class LstBoolean(
    override val ref: Ref,
    override val span: Span,
    val value: Boolean
) : LstConstant(ref) {
    override fun toString(): String = "$ref = $value as Boolean"
}

data class LstInt(
    override val ref: Ref,
    override val span: Span,
    val value: Int
) : LstConstant(ref) {
    override fun toString(): String = "$ref = $value as Int"
}

data class LstFloat(
    override val ref: Ref,
    override val span: Span,
    val value: Float
) : LstConstant(ref) {
    override fun toString(): String = "$ref = $value as Float"
}

data class LstString(
    override val ref: Ref,
    override val span: Span,
    val value: String
) : LstConstant(ref) {
    override fun toString(): String = "$ref = \"$value\" as String"
}

data class LstUnit(
    override val ref: Ref,
    override val span: Span
) : LstConstant(ref) {
    override fun toString(): String = "$ref = Unit"
}

data class LstIsType(
    override val ref: Ref,
    override val span: Span,
    val expr: Ref,
    val typeUsage: TypeUsage,
    val ty: TypeRef? = null
) : LstExpression(ref) {
    override fun toString(): String = "$ref = $expr is $typeUsage ($ty)"
}

data class LstAsType(
    override val ref: Ref,
    override val span: Span,
    val expr: Ref,
    val typeUsage: TypeUsage,
    var ty: TypeRef? = null
) : LstExpression(ref) {
    override fun toString(): String = "$ref = cast $expr as $typeUsage ($ty)"
}

data class LstReturn(
    override val ref: Ref,
    override val span: Span,
    val expr: Ref
) : LstExpression(ref) {
    override fun toString(): String = "$ref = return $expr"
}

data class LstJumpTo(
    override val ref: Ref,
    override val span: Span,
    val backwards: Boolean,
    val role: String,
    var marker: Ref? = null
) : LstExpression(ref) {
    override fun toString(): String = "$ref jump to $marker (backwards: $backwards, role: $role)"
}

data class LstLoadVar(
    override val ref: Ref,
    override val span: Span,
    val name: String,
    val path: Path,
    var variable: VarRef? = null
) : LstExpression(ref) {
    override fun toString(): String = "$ref = load_var $name ($variable)"
}

data class LstStoreVar(
    override val ref: Ref,
    override val span: Span,
    val name: String,
    val path: Path,
    val expr: Ref
) : LstExpression(ref) {
    var variable: VarRef? = null
    override fun toString(): String = "$ref store_var $name ($variable) = $expr"
}

data class LstSizeOf(
    override val ref: Ref,
    override val span: Span,
    val typeUsage: TypeUsage,
) : LstExpression(ref) {
    var ty: TypeRef? = null

    override fun toString(): String = "$ref = size_of $typeUsage ($ty)"
}

data class LstLoadField(
    override val ref: Ref,
    override val span: Span,
    val expr: Ref,
    val name: String
) : LstExpression(ref) {
    var field: VarRef? = null
    override fun toString(): String = "$ref = load_field $name ($field) $expr"
}

data class LstStoreField(
    override val ref: Ref,
    override val span: Span,
    val name: String,
    val instance: Ref,
    val expr: Ref
) : LstExpression(ref) {
    var field: VarRef? = null

    override fun toString(): String = "$ref store_field $name ($field) $instance $expr"
}

data class LstAlloc(
    override val ref: Ref,
    override val span: Span,
    val typeUsage: TypeUsage,
    val type: TypeRef? = null
) : LstExpression(ref) {
    override fun toString(): String = "$ref = alloc $typeUsage ($type)"
}

class LstFunCall(
    override val ref: Ref,
    override val span: Span,
    var name: String,
    var path: Path,
    var argCount: Int
) : LstExpression(ref) {
    var funRef: FunRef? = null

    override fun toString(): String = "$ref = call $name ($funRef, args: $argCount)"
}

data class LstFunArg(
    override val ref: Ref,
    override val span: Span,
    val expr: Ref
) : LstExpression(ref) {
    override fun toString(): String = "$ref arg $expr"
}

data class LstComment(
    override val ref: Ref,
    override val span: Span,
    val comment: String
) : LstNode(ref) {
    override fun toString(): String {
        if (comment.contains("\n")) {
            return "$ref \n/**\n * ${comment.replace("\n", "\n * ")}\n */"
        }
        return "$ref // $comment"
    }
}
