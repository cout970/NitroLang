package nitrolang.ast

import nitrolang.gen.MainParser.BinaryOperatorContext
import nitrolang.gen.MainParser.ExpressionSimpleContext
import nitrolang.util.Span

sealed class ExpressionTree {

    data class Leaf(val ctx: ExpressionSimpleContext) : ExpressionTree()

    data class Operation(val left: ExpressionTree, val right: ExpressionTree, val operator: Operator) : ExpressionTree()

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

                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = Span.internal(),
                    expr = e1
                )
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = Span.internal(),
                    expr = e2
                )

                this.operator.function.split(',').forEachIndexed { index, funcName ->
                    if (index != 0) {
                        code.nodes += LstFunArg(
                            ref = code.nextRef(),
                            span = Span.internal(),
                            expr = list.last()
                        )
                    }
                    val call = LstFunCall(
                        ref = code.nextRef(),
                        span = Span.internal(),
                        name = funcName,
                        path = "",
                        argCount = if (index == 0) 2 else 1,
                    )
                    code.nodes += call
                    list += call.ref
                }
            }
        }
    }

    companion object {
        fun resolvePrecedence(
            exprs: List<ExpressionSimpleContext>,
            ops: List<BinaryOperatorContext>,
            code: LstCode,
            func: (ExpressionSimpleContext) -> Ref
        ): Ref {
            if (ops.isEmpty()) {
                return func(exprs.first())
            }

            val root = resolvePrecedence(exprs.map { Leaf(it) }, ops.map { getBinaryOperator(it) })
            val list = mutableListOf<Ref>()

            root.collect(list, code, func)

            return list.last()
        }

        private fun resolvePrecedence(exprs: List<ExpressionTree>, ops: List<Operator>): ExpressionTree {
            val exprStack = ArrayDeque<ExpressionTree>()
            val opStack = ArrayDeque<Operator>()

            exprStack.addLast(exprs.first())

            fun fold() {
                val e0 = exprStack.removeLast()
                val e1 = exprStack.removeLast()
                val op = opStack.removeLast()

                exprStack.addLast(
                    Operation(
                        left = e1,
                        right = e0,
                        operator = op
                    )
                )
            }

            ops.forEachIndexed { index, operator ->
                // Left fold items with more precedence than the current operator
                while (opStack.isNotEmpty() && operator.precedence > opStack.last().precedence) {
                    fold()
                }

                exprStack.addLast(exprs[index + 1])
                opStack.addLast(operator)
            }

            // Left fold
            while (opStack.isNotEmpty()) {
                fold()
            }

            return exprStack.removeLast()
        }

        private fun getBinaryOperator(ctx: BinaryOperatorContext): Operator {
            return when {
                ctx.MUL() != null -> Operator.MUL
                ctx.DIV() != null -> Operator.DIV
                ctx.MOD() != null -> Operator.MOD
                ctx.ADD() != null -> Operator.ADD
                ctx.SUB() != null -> Operator.SUB
                ctx.RANGE_IN() != null -> Operator.RANGE_INCLUSIVE
                ctx.RANGE_EX() != null -> Operator.RANGE_EXCLUSIVE
                ctx.binopShl() != null -> Operator.SHIFT_LEFT
                ctx.binopShr() != null -> Operator.SHIFT_RIGHT
                ctx.binopUshr() != null -> Operator.SHIFT_RIGHT_UNSIGNED
                ctx.AND() != null -> Operator.BIT_AND
                ctx.XOR() != null -> Operator.BIT_XOR
                ctx.OR() != null -> Operator.BIT_OR
                ctx.LTH() != null -> Operator.LESS_THAN
                ctx.GTH() != null -> Operator.GREATER_THAN
                ctx.LEQ() != null -> Operator.LESS_EQUAL
                ctx.GEQ() != null -> Operator.GREATER_EQUAL
                ctx.COMPARE() != null -> Operator.COMPARE
                ctx.EQ() != null -> Operator.EQUAL
                ctx.NEQ() != null -> Operator.NOT_EQUAL
                ctx.ANDAND() != null -> Operator.BOOL_AND
                ctx.OROR() != null -> Operator.BOOL_OR
                ctx.XORXOR() != null -> Operator.BOOL_XOR
                else -> error("Grammar has been expanded and parser is outdated")
            }
        }
    }

    enum class Operator(
        val text: String,
        val precedence: Int,
        val function: String
    ) {
        MUL("*", 3, "mul"),
        DIV("/", 3, "div"),
        MOD("%", 3, "rem"),
        ADD("+", 4, "plus"),
        SUB("-", 4, "minus"),
        RANGE_INCLUSIVE("..=", 5, "range_in"),
        RANGE_EXCLUSIVE("..<", 5, "range_ex"),
        SHIFT_LEFT("<<", 6, "bit_shift_left"),
        SHIFT_RIGHT(">>", 6, "bit_shift_right"),
        SHIFT_RIGHT_UNSIGNED(">>>", 6, "bit_shift_right_unsigned"),
        BIT_AND("&", 7, "bitwise_and"),
        BIT_XOR("^", 8, "bitwise_xor"),
        BIT_OR("|", 9, "bitwise_or"),
        LESS_THAN("<", 10, "get_ordering,is_less"),
        GREATER_THAN(">", 10, "get_ordering,is_greater"),
        LESS_EQUAL("<=", 10, "get_ordering,is_less_or_equals"),
        GREATER_EQUAL(">=", 10, "get_ordering,is_greater_or_equals"),
        COMPARE("<=>", 11, "get_ordering"),
        EQUAL("==", 12, "get_ordering,is_equals"),
        NOT_EQUAL("!=", 12, "get_ordering,is_not_equals"),
        // TODO: short-circuit
        BOOL_AND("&&", 13, "logic_and"),
        BOOL_XOR("^^", 14, "logic_xor"),
        BOOL_OR("||", 15, "logic_or");

        override fun toString(): String = text
    }
}