package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.HasSpan
import nitrolang.util.Span
import nitrolang.util.dump

// Linearized Syntax Tree

sealed class LstInstruction(
    open val ref: Ref,
    override val span: Span,
    open val block: LstBlock,
) : HasSpan, Dumpable

sealed class LstExpression(id: Ref, span: Span, block: LstBlock) : LstInstruction(id, span, block) {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }
}

sealed class LstConstant(id: Ref, span: Span, block: LstBlock) : LstExpression(id, span, block) {
    abstract fun toRawString(): String
}

class LstBoolean(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstInt(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstLong(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val value: Long
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

class LstFloat(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstString(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstNothing(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstIfStart(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val cond: Ref,
) : LstInstruction(ref, span, block) {
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

class LstIfElse(
    ref: Ref,
    span: Span,
    block: LstBlock,
) : LstInstruction(ref, span, block) {
    override fun toString(): String {
        return "$ref else"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IfMiddle".dump())
        it.add("block", block.dump())
    }
}

class LstIfEnd(
    ref: Ref,
    span: Span,
    block: LstBlock,
) : LstInstruction(ref, span, block) {
    override fun toString(): String {
        return "$ref end-if"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "IfEnd".dump())
        it.add("block", block.dump())
    }
}


class LstIfChoose(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstWhenStart(
    ref: Ref,
    span: Span,
    block: LstBlock,
) : LstInstruction(ref, span, block) {
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

class LstWhenJump(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val whenBlock: LstBlock,
) : LstInstruction(ref, span, block) {
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

class LstWhenStore(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val expr: Ref,
    val start: LstWhenStart,
) : LstInstruction(ref, span, block) {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    override fun toString(): String {
        return "$ref when-store $expr [$typeBox]"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "WhenStore".dump())
        it.add("block", block.dump())
    }
}

class LstWhenEnd(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val isStatement: Boolean,
    val branchStores: List<LstWhenStore> = emptyList(),
    val start: LstWhenStart,
) : LstExpression(ref, span, block) {

    override fun toString(): String {
        return "$ref end-when (${if (isStatement) "statement" else "expression"})"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "WhenEnd".dump())
        it.add("block", block.dump())
    }
}

class LstLoopStart(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val breakBlock: LstBlock,
    val continueBlock: LstBlock,
) : LstInstruction(ref, span, block) {
    override fun toString(): String = "$ref loop :$continueBlock"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoopStart".dump())
        it.add("block", block.dump())
    }
}

class LstLoopEnd(
    ref: Ref,
    span: Span,
    block: LstBlock
) : LstInstruction(ref, span, block) {
    override fun toString(): String = "$ref end-loop :$block"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoopEnd".dump())
        it.add("block", block.dump())
    }
}

class LstLoopJump(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val backwards: Boolean,
    var loopBlock: LstBlock? = null
) : LstInstruction(ref, span, block) {
    override fun toString(): String = "$ref ${if (backwards) "continue" else "break"} :$loopBlock"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "JumpTo".dump())
        it.add("block", block.dump())
        it.add("backwards", backwards.dump())
        it.add("loop", loopBlock?.dump())
    }
}

class LstIsType(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val expr: Ref,
    val typePattern: LstTypePattern
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

class LstAsType(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val expr: Ref,
    val typeUsage: LstTypeUsage
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

class LstReturn(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstSizeOf(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val typeUsage: LstTypeUsage,
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
    val block: LstBlock
    var variable: LstVar?
    var constant: LstConst?
}

class LstLetVar(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val variable: LstVar,
) : LstInstruction(ref, span, block) {
    override fun toString(): String = "$ref let ${variable.name} (${variable.ref})"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "DefVar".dump())
        it.add("block", block.dump())
        it.add("var_ref", variable.ref.dump())
    }
}

class LstLoadVar(
    ref: Ref,
    span: Span,
    block: LstBlock,
    override val name: String,
    override val path: Path,
    override var variable: LstVar? = null
) : LstExpression(ref, span, block), HasVarRef {
    override var constant: LstConst? = null
    val fullName: Path get() = createPath(path, name)
    val finalName: Path get() = "$" + variable.toString()
    val isUnbound: Boolean get() = variable == null && constant == null

    override fun toString(): String = "$ref = load_var $name (${variable?.ref}) [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "LoadVar".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("name", fullName.dump())
        it.add("var_ref", variable?.ref?.dump())
    }
}

class LstStoreVar(
    ref: Ref,
    span: Span,
    block: LstBlock,
    override val name: String,
    override val path: Path,
    val expr: Ref,
    override var variable: LstVar? = null,
    override var constant: LstConst? = null,
) : LstExpression(ref, span, block), HasVarRef {
    var varTypeBox: TypeBox? = null
    val finalName: Path get() = "$" + variable.toString()
    val isUnbound: Boolean get() = variable == null && constant == null

    val fullName: Path get() = createPath(path, name)
    override fun toString(): String = "$ref store_var $name: $varTypeBox (${variable?.ref}) = $expr"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "StoreVar".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("name", fullName.dump())
        it.add("expr", expr.dump())
        it.add("var_type", varTypeBox?.dump())
        it.add("var_ref", variable?.ref?.dump())
    }
}

interface HasFieldRef {
    val name: String
    val instance: Ref
    var fieldRef: FieldRef?
}

class LstLoadField(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstStoreField(
    ref: Ref,
    span: Span,
    block: LstBlock,
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

class LstAlloc(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val typeUsage: LstTypeUsage,
) : LstExpression(ref, span, block) {
    var typeUsageBox: TypeBox? = null
    var struct: LstStruct? = null
    var isEnumInstanceInit: Boolean = false
    val initFieldNames = mutableListOf<String>()

    override fun toString(): String = "$ref = alloc $typeUsage [$typeBox]"

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("kind", "Alloc".dump())
        it.add("block", block.dump())
        it.add("type", typeBox?.dump())
        it.add("alloc", typeUsageBox?.dump())
    }
}

class LstLambdaInit(
    ref: Ref,
    span: Span,
    block: LstBlock,
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
    ref: Ref,
    span: Span,
    block: LstBlock,
    val name: String,
    val path: Path,
    val arguments: List<Ref>,
    var funRef: FunRef? = null,
    var function: LstFunction? = null,
    val explicitTypeParams: List<LstTypeUsage> = emptyList(),
) : LstExpression(ref, span, block) {
    var usesImplicitThis: LstVar? = null
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

class LstComment(
    ref: Ref,
    span: Span,
    block: LstBlock,
    val comment: String
) : LstInstruction(ref, span, block) {

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


