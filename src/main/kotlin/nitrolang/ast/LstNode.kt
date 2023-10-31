package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.parsing.ParserCtx
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.HasSpan
import nitrolang.util.Span
import nitrolang.util.dump

// Linearized Syntax Tree

data class LstNodeBlock(
    val parent: LstNodeBlock? = null,
    val id: Int,
    val isJumpTarget: Boolean,
) : Dumpable {
    val depth: Int get() = if (parent == null) 0 else parent.depth + 1
    val deferredActions = mutableListOf<() -> Unit>()

    override fun toString(): String = "block#$id/$depth"

    override fun dump(): JsonElement = toString().dump()
}

sealed class LstNode(
    open val ref: Ref,
    override val span: Span,
    open val block: LstNodeBlock,
) : HasSpan, Dumpable

sealed class LstExpression(id: Ref, span: Span, block: LstNodeBlock) : LstNode(id, span, block) {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
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
    override fun toString(): String = "$ref = $value [$typeBox]"

    override fun toRawString(): String = "$value"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Boolean".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("value", value.dump())
    }
}

data class LstInt(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Int
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value [$typeBox]"

    override fun toRawString(): String = "$value"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Int".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("value", value.dump())
    }
}

data class LstFloat(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: Float
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = $value [$typeBox]"

    override fun toRawString(): String = "$value"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Float".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("value", value.dump())
    }
}

data class LstString(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val value: String
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = \"${value.replace("\n", "\\n")}\" [$typeBox]"

    override fun toRawString(): String = value

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "String".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("value", value.dump())
    }
}

data class LstNothing(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
) : LstConstant(ref, span, block) {
    override fun toString(): String = "$ref = nothing [$typeBox]"

    override fun toRawString(): String = "()"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Nothing".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
    }
}

data class LstIfStart(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val cond: Ref,
) : LstNode(ref, span, block) {
    override fun toString(): String {
        return "$ref if cond: $cond"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IfStart".dump())
        it.add("block", block.dump())
        it.add("cond", cond.dump())
    }
}

data class LstIfElse(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
) : LstNode(ref, span, block) {
    override fun toString(): String {
        return "$ref else"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IfMiddle".dump())
        it.add("block", block.dump())
    }
}

data class LstIfEnd(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
) : LstNode(ref, span, block) {
    override fun toString(): String {
        return "$ref end-if"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IfEnd".dump())
        it.add("block", block.dump())
    }
}


data class LstIfChoose(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val cond: Ref,
    val ifTrue: Ref,
    val ifFalse: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = if-choose $cond => $ifTrue | $ifFalse [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Choose".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("cond", cond.dump())
        it.add("if_true", ifTrue.dump())
        it.add("if_false", ifFalse.dump())
    }
}

data class LstWhenStart(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
) : LstNode(ref, span, block) {
    val name: String get() = "\$when-${ref.id}"

    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    override fun toString(): String {
        return "$ref when"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "WhenStart".dump())
        it.add("block", block.dump())
    }
}

data class LstWhenJump(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val whenBlock: LstNodeBlock,
) : LstNode(ref, span, block) {
    override fun toString(): String {
        return "$ref break"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "WhenJump".dump())
        it.add("block", block.dump())
        it.add("when", whenBlock.dump())
    }
}

data class LstWhenStore(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val start: LstWhenStart,
) : LstNode(ref, span, block) {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    override fun toString(): String {
        return "$ref when-store"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "WhenStore".dump())
        it.add("block", block.dump())
    }
}

data class LstWhenEnd(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val isStatement: Boolean,
    val branchStores: List<LstWhenStore> = emptyList(),
    val start: LstWhenStart,
) : LstExpression(ref, span, block) {

    override fun toString(): String {
        return "$ref end-when"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "WhenEnd".dump())
        it.add("block", block.dump())
    }
}

data class LstLoopStart(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val breakBlock: LstNodeBlock,
    val continueBlock: LstNodeBlock,
) : LstNode(ref, span, block) {
    override fun toString(): String = "$ref loop"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoopStart".dump())
        it.add("block", block.dump())
    }
}

data class LstLoopEnd(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock
) : LstNode(ref, span, block) {
    override fun toString(): String = "$ref end-loop"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoopEnd".dump())
        it.add("block", block.dump())
    }
}

data class LstLoopJump(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val backwards: Boolean,
    var loopBlock: LstNodeBlock? = null
) : LstNode(ref, span, block) {
    override fun toString(): String = "$ref ${if (backwards) "continue" else "break"} :$loopBlock"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "JumpTo".dump())
        it.add("block", block.dump())
        it.add("backwards", backwards.dump())
        it.add("loop", loopBlock?.dump())
    }
}

data class LstIsType(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val typePattern: TypePattern
) : LstExpression(ref, span, block) {

    override fun toString(): String = "$ref = $expr is $typePattern [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IsType".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("expr", expr.dump())
    }
}

data class LstAsType(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref,
    val typeUsage: TypeUsage
) : LstExpression(ref, span, block) {
    var typeUsageBox: TypeBox? = null

    override fun toString(): String = "$ref = cast $expr as $typeUsage [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "AsType".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("expr", expr.dump())
    }
}

data class LstReturn(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val expr: Ref
) : LstExpression(ref, span, block) {
    override fun toString(): String = "$ref = return $expr [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Return".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("expr", expr.dump())
    }
}

data class LstSizeOf(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val typeUsage: TypeUsage,
) : LstExpression(ref, span, block) {
    var typeUsageBox: TypeBox? = null

    override fun toString(): String = "$ref = size_of<$typeUsage> ($typeUsageBox) [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "SizeOf".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("type_param", typeUsageBox?.dump())
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
    override var varRef: VarRef? = null,
    override var variable: LstVar? = null
) : LstExpression(ref, span, block), HasVarRef {
    override var constant: LstConst? = null
    val fullName: Path get() = createPath(path, name)
    val finalName: Path get() = "$" + varRef.toString()

    override fun toString(): String = "$ref = load_var $name ($varRef) [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoadVar".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
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
    var varTypeBox: TypeBox? = null
    val finalName: Path get() = "$" + varRef.toString()

    val fullName: Path get() = createPath(path, name)
    override fun toString(): String = "$ref store_var $name: $varTypeBox ($varRef) = $expr"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "StoreVar".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("name", fullName.dump())
        it.add("expr", expr.dump())
        it.add("var_type", varTypeBox?.dump())
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
    var struct: LstStruct? = null
    var field: LstStructField? = null

    override fun toString(): String = "$ref = load_field $name ($fieldRef) $instance [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoadField".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
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
    var struct: LstStruct? = null
    var field: LstStructField? = null

    override fun toString(): String = "$ref store_field $name ($fieldRef) $instance $expr [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "StoreField".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
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
    var typeUsageBox: TypeBox? = null
    var struct: LstStruct? = null

    override fun toString(): String = "$ref = alloc $typeUsage [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Alloc".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("alloc", typeUsageBox?.dump())
    }
}

data class LstLambdaInit(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val alloc: Ref,
    val lambda: LstLambdaFunction,
) : LstExpression(ref, span, block) {

    override fun toString(): String = "$ref = init $lambda [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LambdaInit".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
    }
}

class LstFunCall(
    override val ref: Ref,
    override val span: Span,
    override val block: LstNodeBlock,
    val name: String,
    val path: Path,
    val arguments: List<Ref>,
    var funRef: FunRef? = null,
    var function: LstFunction? = null,
    val explicitTypeParams: List<TypeUsage> = emptyList(),
) : LstExpression(ref, span, block) {
    val concreteArgTypes = mutableListOf<TypeBox>()
    val typeParamsTypes = mutableListOf<TypeBox>()
    var posibleOptimizations = emptyMap<String, String>()
    val fullName: Path get() = createPath(path, name)

    override fun toString(): String {
        val args = if (arguments.isNotEmpty()) arguments.joinToString(", ") else "<none>"
        return "$ref = call $fullName: (${concreteArgTypes.joinToString(", ")}) -> $typeBox [args: $args]"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "FunCall".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("name", fullName.dump())
        it.add("func_ref", funRef?.dump())
        it.add("args", arguments.dump())
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


