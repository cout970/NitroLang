package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.util.Dumpable
import nitrolang.util.HasSpan
import nitrolang.util.Span
import nitrolang.util.dump


// Linearized Syntax Tree

data class LstNodeBlock(
    val parent: LstNodeBlock? = null,
    val id: Int,
) : Dumpable {
    val depth: Int get() = if (parent == null) 0 else parent.depth + 1

    override fun toString(): String = "block#$id/$depth"

    override fun dump(): JsonElement = toString().dump()
}

sealed class LstNode(
    open val ref: Ref,
    override val span: Span,
    open val block: LstNodeBlock,
) : HasSpan, Dumpable

interface HasTypes {
    fun mapTypes(func: (TypeTree) -> TypeTree)
}

sealed class LstExpression(id: Ref, span: Span, block: LstNodeBlock) : LstNode(id, span, block), HasTypes {
    var type: TypeTree? = null
    var hasTypeError = false

    fun propagateTypeError() {
        hasTypeError = true
        type = InvalidType.toTypeTree()
    }

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        this.type = this.type?.let(func)
    }
}

data class LstMarker(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val role: String
) : LstNode(ref, span, block) {
    override fun toString(): String = "$ref marker $role"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "marker".dump())
        it.add("block", block.dump())
        it.add("role", role.dump())
    }
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

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IfJump".dump())
        it.add("block", block.dump())
        it.add("cond", cond.dump())
        it.add("middle", middle?.dump())
        it.add("end", end?.dump())
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
    override fun toString(): String = "$ref = choose $cond => $ifTrue | $ifFalse [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Choose".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("cond", cond.dump())
        it.add("if_true", ifTrue.dump())
        it.add("if_false", ifFalse.dump())
    }
}

sealed class LstConstant(id: Ref, span: Span, block: LstNodeBlock) : LstExpression(id, span, block) {
    abstract fun toRawString(): String
}

data class LstBoolean(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Boolean
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value as Boolean [$type]"

    override fun toRawString(): String = "$value"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Boolean".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("value", value.dump())
    }
}

data class LstInt(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Int
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value as Int [$type]"

    override fun toRawString(): String = "$value"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Int".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("value", value.dump())
    }
}

data class LstFloat(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Float
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value as Float [$type]"

    override fun toRawString(): String = "$value"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Float".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("value", value.dump())
    }
}

data class LstString(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: String
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = \"$value\" as String [$type]"

    override fun toRawString(): String = value

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "String".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("value", value.dump())
    }
}

data class LstUnit(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = Unit [$type]"

    override fun toRawString(): String = "()"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Unit".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
    }
}

data class LstIsType(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val typeUsage: TypeUsage
) : LstExpression(ref, span, block) {
    var typeTree: TypeTree? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        this.typeTree = this.typeTree?.let(func)
    }

    override fun toString(): String = "$ref = $expr is $typeUsage ($typeTree) [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IsType".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("expr", expr.dump())
        it.add("is_type", typeTree?.dump())
    }
}

data class LstAsType(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val typeUsage: TypeUsage
) : LstExpression(ref, span, block) {
    var typeTree: TypeTree? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        this.typeTree = this.typeTree?.let(func)
    }

    override fun toString(): String = "$ref = cast $expr as $typeUsage ($typeTree) [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "AsType".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("expr", expr.dump())
        it.add("as_type", typeTree?.dump())
    }
}

data class LstDup(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
) : LstExpression(ref, span, block) {

    override fun toString(): String = "$ref = dup $expr [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Dup".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("expr", expr.dump())
    }
}

data class LstReturn(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = return $expr [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Return".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("expr", expr.dump())
    }
}

data class LstJumpTo(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val backwards: Boolean,
    val role: String,
    var marker: Ref? = null
) : LstNode(ref, span, block) {
    override fun toString(): String = "$ref jump to $marker (backwards: $backwards, role: $role)"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "JumpTo".dump())
        it.add("block", block.dump())
        it.add("backwards", backwards.dump())
        it.add("role", role.dump())
        it.add("marker", marker?.dump())
    }
}

data class LstSizeOf(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val typeUsage: TypeUsage,
) : LstExpression(ref, span, block) {
    var typeTree: TypeTree? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        this.typeTree = this.typeTree?.let(func)
    }

    override fun toString(): String = "$ref = size_of $typeUsage ($typeTree) [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "SizeOf".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("size_of", typeTree?.dump())
    }
}

interface HasVarRef {
    val name: String
    val path: Path
    val block: LstNodeBlock
    var varRef: VarRef?
    var variable: LstVar?
    var constant: LstConst?
}

data class LstLoadVar(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    override val name: String,
    override val path: Path,
    override var varRef: VarRef? = null
) : LstExpression(ref, span, block), HasVarRef {
    override var variable: LstVar? = null
    override var constant: LstConst? = null
    val fullName: Path get() = createPath(path, name)

    override fun toString(): String = "$ref = load_var $name ($varRef) [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoadVar".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("name", fullName.dump())
        it.add("var_ref", varRef?.dump())
    }
}

data class LstStoreVar(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    override val name: String,
    override val path: Path,
    val expr: Ref,
    override var varRef: VarRef? = null,
    override var variable: LstVar? = null,
    override var constant: LstConst? = null,
) : LstExpression(ref, span, block), HasVarRef {
    var varType: TypeTree? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        varType = varType?.let(func)
    }

    val fullName: Path get() = createPath(path, name)
    override fun toString(): String = "$ref store_var $name ($varRef) = $expr [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "StoreVar".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("name", fullName.dump())
        it.add("expr", expr.dump())
        it.add("var_ref", varRef?.dump())
    }
}

interface HasFieldRef {
    val name: String
    val instance: Ref
    var fieldRef: FieldRef?
}

data class LstLoadField(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    override val instance: Ref,
    override val name: String
) : LstExpression(ref, span, block), HasFieldRef {
    override var fieldRef: FieldRef? = null
    var field: LstStructureField? = null
    var concreteStructType: TypeTree? = null
    var concreteFieldType: TypeTree? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        concreteStructType = concreteStructType?.let(func)
        concreteFieldType = concreteFieldType?.let(func)
    }

    override fun toString(): String = "$ref = load_field $name ($fieldRef) $instance [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoadField".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("name", name.dump())
        it.add("instance", instance.dump())
        it.add("field_ref", fieldRef?.dump())
    }
}

data class LstStoreField(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    override val name: String,
    override val instance: Ref,
    val expr: Ref
) : LstExpression(ref, span, block), HasFieldRef {
    override var fieldRef: FieldRef? = null
    var field: LstStructureField? = null
    var concreteStructType: TypeTree? = null
    var concreteFieldType: TypeTree? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        concreteStructType = concreteStructType?.let(func)
        concreteFieldType = concreteFieldType?.let(func)
    }

    override fun toString(): String = "$ref store_field $name ($fieldRef) $instance $expr [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "StoreField".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("name", name.dump())
        it.add("instance", instance.dump())
        it.add("field_ref", fieldRef?.dump())
        it.add("expr", expr.dump())
    }
}

data class LstAlloc(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val typeUsage: TypeUsage,
) : LstExpression(ref, span, block) {
    var typeTree: TypeTree? = null
    var struct: LstStruct? = null

    override fun mapTypes(func: (TypeTree) -> TypeTree) {
        super.mapTypes(func)
        this.typeTree = this.typeTree?.let(func)
    }

    override fun toString(): String = "$ref = alloc $typeUsage [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Alloc".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("alloc", typeTree?.dump())
    }
}

data class LstFunArg(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref arg $expr [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "FunArg".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("expr", expr.dump())
    }
}

class LstFunCall(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val name: String,
    val path: Path,
    val argCount: Int,
    val specifiedTypeParams: List<TypeUsage> = emptyList(),
) : LstExpression(ref, span, block) {
    var funRef: FunRef? = null
    var function: LstFunction? = null
    var concreteTypeParams: List<TypeTree>? = null
    val fullName: Path get() = createPath(path, name)

    override fun toString(): String = "$ref = call $name ($funRef, args: $argCount) [$type]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "FunCall".dump())
        it.add("block", block.dump())
        it.add("type", type?.dump())
        it.add("name", fullName.dump())
        it.add("func_ref", funRef?.dump())
        it.add("arg_count", argCount.dump())
        it.add("concrete_type_params", concreteTypeParams?.dump())
    }
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

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "FunCall".dump())
        it.add("block", block.dump())
        it.add("comment", comment.dump())
    }
}

sealed class LstConstValue

data class LstConstInt(val value: Int) : LstConstValue()
data class LstConstFloat(val value: Float) : LstConstValue()
data class LstConstBoolean(val value: Boolean) : LstConstValue()
data class LstConstString(val value: String) : LstConstValue()
object LstConstUnit : LstConstValue()

