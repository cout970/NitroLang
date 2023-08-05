package nitrolang.parsing

import nitrolang.ast.ExpressionTree
import nitrolang.ast.LstAnnotation
import nitrolang.ast.Ref
import nitrolang.backend.wasm.ConstValue
import nitrolang.gen.MainParser
import nitrolang.util.Span
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.TerminalNode

fun ParserCtx.resolveAnnotations(ctx: ParserRuleContext): List<LstAnnotation> {

    val annotations = when (ctx.parent) {
        is MainParser.ParseFunctionDefinitionContext -> {
            (ctx.parent as MainParser.ParseFunctionDefinitionContext).annotation()
        }

        is MainParser.DefinitionChoiceContext -> {
            ((ctx.parent as MainParser.DefinitionChoiceContext).parent as MainParser.DefinitionContext).annotation()
        }

        else -> {
            error("Unknown context to extract annotations: $ctx, parent: ${ctx.parent}")
        }
    }

    return annotations.map { subCtx ->
        val args = mutableMapOf<String, ConstValue>()

        if (subCtx.annotationArgs() != null) {
            subCtx.annotationArgs().annotationArgEntry().forEach { entry ->
                val name = when {
                    entry.annotationArgKey().PLAIN_STRING() != null -> {
                        processPlainString(entry.annotationArgKey().PLAIN_STRING())
                    }

                    entry.annotationArgKey().nameToken() != null -> {
                        entry.annotationArgKey().nameToken().text
                    }

                    else -> error("Grammar has been expanded and parser is outdated")
                }

                args[name] = processConstExpr(entry.constExpr())
            }
        }

        LstAnnotation(
            span = subCtx.nameToken().span(),
            name = subCtx.nameToken().text,
            args = args,
        )
    }
}

fun ParserCtx.resolvePrecedence(
    exprs: List<MainParser.ExpressionSimpleContext>,
    ops: List<Pair<MainParser.BinaryOperatorContext, Span>>,
    func: (MainParser.ExpressionSimpleContext) -> Ref
): Ref {
    if (ops.isEmpty()) {
        return func(exprs.first())
    }

    val root = resolvePrecedence(exprs.map { ExpressionTree.Leaf(it) },
        ops.map { getBinaryOperator(it.first) to it.second })
    val list = mutableListOf<Ref>()

    root.collect(list, code, func)

    return list.last()
}

private fun resolvePrecedence(
    exprs: List<ExpressionTree>,
    ops: List<Pair<ExpressionTree.Operator, Span>>
): ExpressionTree {
    val exprStack = ArrayDeque<ExpressionTree>()
    val opStack = ArrayDeque<Pair<ExpressionTree.Operator, Span>>()

    exprStack.addLast(exprs.first())

    fun fold() {
        val e0 = exprStack.removeLast()
        val e1 = exprStack.removeLast()
        val op = opStack.removeLast()

        exprStack.addLast(
            ExpressionTree.Operation(
                left = e1,
                right = e0,
                operator = op.first,
                span = op.second,
            )
        )
    }

    ops.forEachIndexed { index, operator ->
        // Left fold items with more precedence than the current operator
        while (opStack.isNotEmpty() && operator.first.precedence > opStack.last().first.precedence) {
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

fun ParserCtx.getBinaryOperator(ctx: MainParser.BinaryOperatorContext): ExpressionTree.Operator {
    return when {
        ctx.MUL() != null -> ExpressionTree.Operator.MUL
        ctx.DIV() != null -> ExpressionTree.Operator.DIV
        ctx.MOD() != null -> ExpressionTree.Operator.MOD
        ctx.ADD() != null -> {
            checkSubPreviousToken(ctx, ctx.ADD())
            ExpressionTree.Operator.ADD
        }

        ctx.SUB() != null -> {
            checkSubPreviousToken(ctx, ctx.SUB())
            ExpressionTree.Operator.SUB
        }

        ctx.RANGE_IN() != null -> ExpressionTree.Operator.RANGE_INCLUSIVE
        ctx.RANGE_EX() != null -> ExpressionTree.Operator.RANGE_EXCLUSIVE
        ctx.binopShl() != null -> ExpressionTree.Operator.SHIFT_LEFT
        ctx.binopShr() != null -> ExpressionTree.Operator.SHIFT_RIGHT
        ctx.binopUshr() != null -> ExpressionTree.Operator.SHIFT_RIGHT_UNSIGNED
        ctx.AND() != null -> ExpressionTree.Operator.BIT_AND
        ctx.XOR() != null -> ExpressionTree.Operator.BIT_XOR
        ctx.OR() != null -> ExpressionTree.Operator.BIT_OR
        ctx.LTH() != null -> ExpressionTree.Operator.LESS_THAN
        ctx.GTH() != null -> ExpressionTree.Operator.GREATER_THAN
        ctx.LEQ() != null -> ExpressionTree.Operator.LESS_EQUAL
        ctx.GEQ() != null -> ExpressionTree.Operator.GREATER_EQUAL
        ctx.COMPARE() != null -> ExpressionTree.Operator.COMPARE
        ctx.EQ() != null -> ExpressionTree.Operator.EQUAL
        ctx.NEQ() != null -> ExpressionTree.Operator.NOT_EQUAL
        ctx.ANDAND() != null -> ExpressionTree.Operator.BOOL_AND
        ctx.OROR() != null -> ExpressionTree.Operator.BOOL_OR
        ctx.XORXOR() != null -> ExpressionTree.Operator.BOOL_XOR
        else -> error("Grammar has been expanded and parser is outdated")
    }
}

// Report issues with "x-1", may mean "x - 1" but it's interpreted as "x -1"
fun ParserCtx.checkSubPreviousToken(ctx: ParserRuleContext, t: TerminalNode) {
    val s = t.symbol
    val prevChar = s.inputStream.getText(Interval.of(s.startIndex - 1, s.startIndex - 1))

    if (prevChar !in setOf("(", "{", "[", " ", ",", ";")) {
        collector.report(
            "Invalid char placement, consider adding an space before '${t.text}'",
            ctx.span()
        )
    }
}
