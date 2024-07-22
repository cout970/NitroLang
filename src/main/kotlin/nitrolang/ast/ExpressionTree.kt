package nitrolang.ast

import nitrolang.gen.MainParser.ExpressionSimpleContext
import nitrolang.util.Span

sealed class ExpressionTree {

    data class Leaf(val ctx: ExpressionSimpleContext) : ExpressionTree() {
        override fun toString(): String = ctx.text
    }

    data class Operation(
        val left: ExpressionTree,
        val right: ExpressionTree,
        val operator: Operator,
        val span: Span,
    ) : ExpressionTree() {
        override fun toString(): String = "($left ${operator.text} $right)"
    }

    fun collect(list: MutableList<Ref>, code: LstCode, func: (ExpressionSimpleContext) -> Ref) {
        when (this) {
            is Leaf -> {
                list += func(ctx)
            }

            is Operation -> {
                left.collect(list, code, func)
                val e1 = list.last()

                right.collect(list, code, func)
                val e2 = list.last()

                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = span,
                    block = code.currentBlock,
                    name = operator.function,
                    path = "",
                    arguments = listOf(e1, e2),
                )
                code.nodes += call
                list += call.ref

                if (operator.postFunction != null) {
                    val postCall = LstFunCall(
                        ref = code.nextRef(),
                        span = span,
                        block = code.currentBlock,
                        name = operator.postFunction,
                        path = "",
                        arguments = listOf(call.ref),
                    )
                    code.nodes += postCall
                    list += postCall.ref
                    // Optimization for Int and Float
                    call.posibleOptimizations = operator.posibleOptimizations
                }
            }
        }
    }

    enum class Operator(
        val text: String,
        val precedence: Int,
        val function: String,
        val postFunction: String? = null,
        val posibleOptimizations: Map<String, String> = emptyMap(),
    ) {
        MUL("*", 3, "mul"),
        DIV("/", 3, "div"),
        MOD("%", 3, "rem"),
        ADD("+", 4, "plus"),
        SUB("-", 4, "minus"),
        RANGE_INCLUSIVE("..=", 5, "range_to"),
        RANGE_EXCLUSIVE("..<", 5, "range_up_to"),
        SHIFT_LEFT("<<", 6, "bitwise_shift_left"),
        SHIFT_RIGHT(">>", 6, "bitwise_shift_right"),
        SHIFT_RIGHT_UNSIGNED(">>>", 6, "bitwise_shift_right_unsigned"),
        BIT_AND("&", 7, "bitwise_and"),
        BIT_XOR("^", 8, "bitwise_xor"),
        BIT_OR("|", 9, "bitwise_or"),
        LESS_THAN(
            "<", 10, "get_ordering", "is_less", mapOf(
                "Int" to "less_than_signed",
                "Long" to "less_than_signed",
                "Float" to "less_than",
            )
        ),
        GREATER_THAN(
            ">", 10, "get_ordering", "is_greater", mapOf(
                "Int" to "greater_than_signed",
                "Long" to "greater_than_signed",
                "Float" to "greater_than",
            )
        ),
        LESS_EQUAL(
            "<=", 10, "get_ordering", "is_less_or_equals", mapOf(
                "Int" to "less_equal_signed",
                "Long" to "less_equal_signed",
                "Float" to "less_equal",
            )
        ),
        GREATER_EQUAL(
            ">=", 10, "get_ordering", "is_greater_or_equals", mapOf(
                "Int" to "greater_equal_signed",
                "Long" to "greater_equal_signed",
                "Float" to "greater_equal",
            )
        ),
        COMPARE("<=>", 11, "get_ordering"),
        EQUAL(
            "==", 12, "get_ordering", "is_equals", mapOf(
                "Int" to "is_equal",
                "Long" to "is_equal",
                "Float" to "is_equal",
                "String" to "is_equal",
            )
        ),
        NOT_EQUAL(
            "!=", 12, "get_ordering", "is_not_equals", mapOf(
                "Int" to "is_not_equal",
                "Long" to "is_not_equal",
                "Float" to "is_not_equal",
                "String" to "is_not_equal",
            )
        ),
        BOOL_XOR("^^", 14, "logical_xor");

        override fun toString(): String = text
    }
}