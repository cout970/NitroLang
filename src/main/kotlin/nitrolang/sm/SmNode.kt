package nitrolang.sm

import nitrolang.ast.Ref
import nitrolang.ast.TypeTree
import nitrolang.util.HasSpan
import nitrolang.util.Span

sealed class ConstValue

data class ConstInt(val value: Int) : ConstValue()
data class ConstFloat(val value: Float) : ConstValue()
data class ConstBoolean(val value: Boolean) : ConstValue()
data class ConstString(val value: String) : ConstValue()
object ConstUnit : ConstValue()

sealed class SmNode(
    override val span: Span,
) : HasSpan {
    var comment: String = ""
    abstract fun updateStack(stack: ArrayDeque<TypeTree>)
}

data class SmConst(
    override val span: Span,
    val value: ConstValue,
    val type: TypeTree,
) : SmNode(span) {

    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.addLast(type)
    }
}

data class SmIf(
    override val span: Span,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.removeLast()
    }
}

data class SmElse(
    override val span: Span,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) = Unit
}

data class SmBlock(
    override val span: Span,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) = Unit
}

data class SmLoop(
    override val span: Span,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) = Unit
}

data class SmEnd(
    override val span: Span,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) = Unit
}

data class SmBranch(
    override val span: Span,
    val level: Int,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) = Unit
}

data class SmLoadVar(
    override val span: Span,
    val name: String,
    val type: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.addLast(type)
    }
}

data class SmSaveVar(
    override val span: Span,
    val name: String,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.removeLast()
        stack.removeLast()
    }
}

data class SmLoadGlobal(
    override val span: Span,
    val name: String,
    val type: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.addLast(type)
    }
}

data class SmSaveGlobal(
    override val span: Span,
    val name: String
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.removeLast()
        stack.removeLast()
    }
}

data class SmSaveAux(
    override val span: Span,
    val ref: Ref,
    var type: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.addLast(type)
    }
}

data class SmLoadAux(
    override val span: Span,
    val ref: Ref,
    var type: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.removeLast()
    }
}

data class SmWrite(
    override val span: Span,
    var type: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.removeLast()
        stack.removeLast()
    }
}

data class SmRead(
    override val span: Span,
    var type: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        stack.addLast(type)
    }
}

data class SmCall(
    override val span: Span,
    val name: String,
    val args: Int,
    val result: TypeTree,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        repeat(args) { stack.removeLast() }
        stack.addLast(result)
    }
}

data class SmOpcode(
    override val span: Span,
    val name: String,
    val args: Int,
    val result: TypeTree?,
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) {
        repeat(args) { stack.removeLast() }
        result?.let { stack.addLast(it) }
    }
}

data class SmNop(
    override val span: Span
) : SmNode(span) {
    override fun updateStack(stack: ArrayDeque<TypeTree>) = Unit
}

class SmCode {
    val inst: ArrayDeque<SmNode> = ArrayDeque()
    val variables = mutableSetOf<SmLocalVar>()
}

class SmLocalVar(
    val name: String,
    val type: TypeTree,
)