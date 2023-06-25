package nitrolang.ast

import nitrolang.gen.MainParser.ExpressionSimpleContext
import nitrolang.util.Span

sealed class ExpressionTree {

    data class Leaf(val ctx: ExpressionSimpleContext) : ExpressionTree()

    data class Operation(
        val left: ExpressionTree,
        val right: ExpressionTree,
        val operator: Operator,
        val span: Span,
    ) : ExpressionTree()

    fun collect(list: MutableList<Ref>, code: LstCode, func: (ExpressionSimpleContext) -> Ref) {
        when (this) {
            is Leaf -> {
                list += func(this.ctx)
            }

            is Operation -> {
                this.left.collect(list, code, func)
                val e1 = list.last()

                this.right.collect(list, code, func)
                val e2 = list.last()

                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = this.span,
                    block = code.currentBlock,
                    name = this.operator.function,
                    path = "",
                    arguments = listOf(e1, e2),
                )
                code.nodes += call
                list += call.ref

                if (this.operator.postFunction != null) {
                    val postCall = LstFunCall(
                        ref = code.nextRef(),
                        span = this.span,
                        block = code.currentBlock,
                        name = this.operator.postFunction,
                        path = "",
                        arguments = listOf(call.ref),
                    )
                    code.nodes += postCall
                    list += postCall.ref
                }
            }
        }
    }

    enum class Operator(
        val text: String,
        val precedence: Int,
        val function: String,
        val postFunction: String? = null,
    ) {
        MUL("*", 3, "mul"),
        DIV("/", 3, "div"),
        MOD("%", 3, "rem"),
        ADD("+", 4, "plus"),
        SUB("-", 4, "minus"),
        RANGE_INCLUSIVE("..=", 5, "range_in"),
        RANGE_EXCLUSIVE("..<", 5, "range_ex"),
        SHIFT_LEFT("<<", 6, "bitwise_shift_left"),
        SHIFT_RIGHT(">>", 6, "bitwise_shift_right"),
        SHIFT_RIGHT_UNSIGNED(">>>", 6, "bitwise_shift_right_unsigned"),
        BIT_AND("&", 7, "bitwise_and"),
        BIT_XOR("^", 8, "bitwise_xor"),
        BIT_OR("|", 9, "bitwise_or"),
        LESS_THAN("<", 10, "get_ordering", "is_less"),
        GREATER_THAN(">", 10, "get_ordering", "is_greater"),
        LESS_EQUAL("<=", 10, "get_ordering", "is_less_or_equals"),
        GREATER_EQUAL(">=", 10, "get_ordering", "is_greater_or_equals"),
        COMPARE("<=>", 11, "get_ordering"),
        EQUAL("==", 12, "get_ordering", "is_equals"),
        NOT_EQUAL("!=", 12, "get_ordering", "is_not_equals"),

        // TODO: short-circuit
        BOOL_AND("&&", 13, "logical_and"),
        BOOL_XOR("^^", 14, "logical_xor"),
        BOOL_OR("||", 15, "logical_or");

        override fun toString(): String = text
    }
}