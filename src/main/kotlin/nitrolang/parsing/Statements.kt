package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.gen.MainParser

fun ParserCtx.processStatementBlock(ctx: MainParser.StatementBlockContext) {
    ctx.statement().forEach { stm -> processStatement(stm) }
}

fun ParserCtx.processStatement(ctx: MainParser.StatementContext) {
    val stm = ctx.statementChoice()
    code.lastExpression = null

    when {
        stm.letStatement() != null -> processLetStatement(stm.letStatement())

        stm.ifStatement() != null -> processIfStatement(stm.ifStatement())

        stm.loopStatement() != null -> processLoopStatement(stm.loopStatement())

        stm.whileStatement() != null -> processWhileStatement(stm.whileStatement())

        stm.repeatStatement() != null -> processRepeatStatement(stm.repeatStatement())

        stm.forStatement() != null -> processForStatement(stm.forStatement())

        stm.whenExpr() != null -> processWhenStatement(stm.whenExpr(), code)

        stm.expressionStatement() != null -> {
            val subCtx = stm.expressionStatement()

            if (subCtx.assignableExpression() != null) {
                val subSubCtx = subCtx.assignableExpression()

                val operation = when {
                    subCtx.ASSIGN() != null -> null
                    subCtx.ADD_ASSIGN() != null -> ExpressionTree.Operator.ADD
                    subCtx.SUB_ASSIGN() != null -> ExpressionTree.Operator.SUB
                    subCtx.MUL_ASSIGN() != null -> ExpressionTree.Operator.MUL
                    subCtx.DIV_ASSIGN() != null -> ExpressionTree.Operator.DIV
                    subCtx.MOD_ASSIGN() != null -> ExpressionTree.Operator.MOD
                    else -> error("Grammar has been expanded and parser is outdated")
                }

                when {
                    subSubCtx.expression() != null && subSubCtx.anyName() != null -> {
                        val receiver = processExpression(subSubCtx.expression())
                        var value = processExpression(subCtx.expression())
                        val fieldName = subSubCtx.anyName().text

                        if (operation != null) {
                            val load = code.loadField(
                                span = subSubCtx.anyName().span(),
                                instance = receiver,
                                name = fieldName,
                            )
                            value = code.call(
                                span = subCtx.span(),
                                path = "",
                                name = operation.function,
                                arguments = listOf(load, value),
                            )
                        }

                        code.storeField(
                            span = subSubCtx.anyName().span(),
                            name = fieldName,
                            instance = receiver,
                            expr = value,
                        )
                    }

                    subSubCtx.collectionIndexingSuffix() != null -> {
                        val receiver = processExpression(subSubCtx.expression())
                        val index = processExpression(subSubCtx.collectionIndexingSuffix().expression())
                        var value = processExpression(subCtx.expression())

                        if (operation != null) {
                            val load = code.call(
                                span = subSubCtx.collectionIndexingSuffix().expression().span(),
                                path = "",
                                name = "get",
                                arguments = listOf(receiver, index),
                            )
                            value = code.call(
                                span = subCtx.span(),
                                path = "",
                                name = operation.function,
                                arguments = listOf(load, value),
                            )
                        }

                        code.call(
                            span = subSubCtx.collectionIndexingSuffix().expression().span(),
                            path = "",
                            name = "set",
                            arguments = listOf(receiver, index, value),
                        )
                    }

                    subSubCtx.LBRACKET() != null -> {
                        val receiver = processExpression(subSubCtx.expression())
                        val value = processExpression(subCtx.expression())

                        if (operation != null) {
                            collector.report("Operator ${operation.text} is not supported for arrays", subCtx.span())
                        }

                        code.call(
                            span = subSubCtx.span(),
                            path = "",
                            name = "add",
                            arguments = listOf(receiver, value),
                        )
                    }

                    subSubCtx.anyName() != null -> {
                        var value = processExpression(subCtx.expression())
                        val name = subSubCtx.anyName().text
                        var path = ""

                        if (subSubCtx.modulePath() != null) {
                            path = subSubCtx.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
                        }

                        if (operation != null) {
                            val load = code.loadVar(
                                span = subSubCtx.anyName().span(),
                                name = name,
                                path = path,
                            )
                            value = code.call(
                                span = subCtx.span(),
                                path = "",
                                name = operation.function,
                                arguments = listOf(load, value),
                            )
                        }

                        code.storeVar(
                            span = subSubCtx.anyName().span(),
                            name = name,
                            path = path,
                            expr = value,
                        )
                    }

                    else -> error("Grammar has been expanded and parser is outdated")
                }
            } else {
                code.lastExpression = processExpression(subCtx.expression())
            }
        }


        stm.deferStatement() != null -> {
            if (!allowDefer) {
                collector.report("Defer statements are not allowed here", stm.span())
                return
            }

            val subCtx = stm.deferStatement()

            val action: () -> Unit = when {
                subCtx.expression() != null -> ({
                    allowDefer = false
                    processExpression(subCtx.expression())
                    allowDefer = true
                })

                subCtx.statementBlock() != null -> ({
                    code.enterBlock(true)
                    allowDefer = false
                    processStatementBlock(subCtx.statementBlock())
                    allowDefer = true
                    code.exitDeferBlock()
                })

                else -> error("Grammar has been expanded and parser is outdated")
            }

            code.currentBlock.deferredActions += action
        }

        stm.foreignBlockStatement() != null -> {
            val subCtx = stm.foreignBlockStatement()

            val name = subCtx.BLOCK_START().text.substring(3)
            val lines = subCtx.foreignBlockStatementPart()
                .joinToString("") { if (it.BLOCK_BLOB() != null) it.BLOCK_BLOB().text else it.BLOCK_OTHER().text }
                .split("\n")
                .toMutableList()

            if (lines.isNotEmpty() && lines[0].isBlank()) {
                lines.removeAt(0)
            }
            if (lines.isNotEmpty() && lines.last().isBlank()) {
                lines.removeLast()
            }

            fun countIndent(line: String): Int {
                var count = 0
                for (char in line) {
                    if (!char.isWhitespace()) {
                        break
                    }
                    count++
                }
                return count
            }

            val commonIndent = lines.minOfOrNull(::countIndent) ?: 0
            val contents = lines.joinToString("\n") { it.substring(commonIndent) }

            code.nodes += LstComment(
                ref = code.nextRef(),
                span = subCtx.span(),
                block = code.currentBlock,
                comment = "```${name}\n${contents}\n```"
            )
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

private fun ParserCtx.processForStatement(subCtx: MainParser.ForStatementContext) {
    // Converts a for in while+iter.next()
    // let iter = iterable.to_iterator()
    // loop: {
    //   let next_item = iter.next()
    //   if (next_item.is_some()) {
    //     let i = next_item.get_or_crash()
    //     code...
    //     goto loop;
    //   }
    // }
    val prevBreaks = code.breaks
    val prevContinues = code.continues

    code.breaks = mutableListOf()
    code.continues = mutableListOf()

    val iterable = processExpression(subCtx.expression())

    // iterable.to_iterator()
    val toIterator = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.expression().span(),
        block = code.currentBlock,
        name = "to_iterator",
        path = "",
        arguments = listOf(iterable),
    )
    code.nodes += toIterator

    // loop {
    val prevBlock = code.currentBlock
    code.enterBlock(true)
    val breakBlock = code.currentBlock
    code.enterBlock(false)
    val continueBlock = code.currentBlock

    code.nodes += LstLoopStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = prevBlock,
        breakBlock = breakBlock,
        continueBlock = continueBlock,
    )

    // iter.next()
    val iteratorNext = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.expression().span(),
        block = code.currentBlock,
        name = "next",
        path = "",
        arguments = listOf(toIterator.ref),
    )
    code.nodes += iteratorNext

    // let next_item: Optional<#Item>
    val nextItem = code.letVar(
        span = subCtx.span(),
        name = "next_item",
        typeUsage = null,
    )

    // next_item = iter.next()
    code.nodes += LstStoreVar(
        ref = code.nextRef(),
        span = subCtx.anyName().span(),
        block = code.currentBlock,
        name = subCtx.anyName().text,
        path = "",
        expr = iteratorNext.ref,
        variable = nextItem,
    )

    // next_item.is_some()
    val isSomeCall = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.anyName().span(),
        block = code.currentBlock,
        name = "is_some",
        path = "",
        arguments = listOf(iteratorNext.ref),
    )
    code.nodes += isSomeCall

    // if
    code.nodes += LstIfStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        cond = isSomeCall.ref,
    )

    code.enterBlock(false)

    // next_item.get_or_crash()
    val getOrCrashCall = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.anyName().span(),
        block = code.currentBlock,
        name = "get_or_crash",
        path = "",
        arguments = listOf(iteratorNext.ref),
    )
    code.nodes += getOrCrashCall

    // let i
    val forVar = code.letVar(
        span = subCtx.anyName().span(),
        name = subCtx.anyName().text,
        typeUsage = null,
    )

    // i = next_item.get_or_crash()
    code.nodes += LstStoreVar(
        ref = code.nextRef(),
        span = subCtx.anyName().span(),
        block = code.currentBlock,
        name = subCtx.anyName().text,
        path = "",
        expr = getOrCrashCall.ref,
        variable = forVar,
    )

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // jump to start
    code.executeDeferredActions()
    code.jumpedOutOfBlock = true
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // Restore prev block
    code.exitBlock()

    // end-if
    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )

    // } end-loop
    code.exitBlock()
    code.exitBlock()

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breaks.forEach { it.loopBlock = breakBlock }
    code.continues.forEach { it.loopBlock = continueBlock }

    code.breaks = prevBreaks
    code.continues = prevContinues
}

private fun ParserCtx.processRepeatStatement(subCtx: MainParser.RepeatStatementContext) {
    // Converts a while in
    // {
    //   let limit = count
    //   let it = 0
    //   loop: {
    //     if (it < limit) {
    //       defer { it = it + 1 }
    //       code...
    //       goto loop;
    //     }
    //   }
    // }
    code.enterBlock(true)

    // let limit = count
    val count = processExpression(subCtx.expression())

    val varLimit = code.letVar(
        span = subCtx.expression().span(),
        name = "limit",
        typeUsage = LstTypeUsage.int()
    )

    code.nodes += LstStoreVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "limit",
        path = "",
        expr = count,
        variable = varLimit,
    )

    // let it = 0
    val varIt = code.letVar(
        span = subCtx.expression().span(),
        name = "it",
        typeUsage = LstTypeUsage.int()
    )

    val zero1 = LstInt(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        value = 0,
    )
    code.nodes += zero1

    code.nodes += LstStoreVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "it",
        path = "",
        expr = zero1.ref,
        variable = varIt,
    )

    val prevBreaks = code.breaks
    val prevContinues = code.continues

    code.breaks = mutableListOf()
    code.continues = mutableListOf()

    // loop {
    val prevBlock = code.currentBlock
    code.enterBlock(true)
    val breakBlock = code.currentBlock
    code.enterBlock(false)
    val continueBlock = code.currentBlock

    code.nodes += LstLoopStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = prevBlock,
        breakBlock = breakBlock,
        continueBlock = continueBlock,
    )

    // if it < limit
    val loadIfIt = LstLoadVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "it",
        path = "",
        variable = varIt,
    )
    code.nodes += loadIfIt

    val loadIfLimit = LstLoadVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "limit",
        path = "",
        variable = varLimit,
    )
    code.nodes += loadIfLimit

    val cond = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "less_than_signed",
        path = "",
        arguments = listOf(loadIfIt.ref, loadIfLimit.ref),
    )
    code.nodes += cond

    code.nodes += LstIfStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        cond = cond.ref,
    )

    code.enterBlock(false)

    // This must be deferred otherwise `continue` will jump to the beginning without incrementing `it` causing an infinite loop
    // defer { it = it + 1 }
    code.currentBlock.deferredActions += {
        // it = it + 1
        val loadItInc = LstLoadVar(
            ref = code.nextRef(),
            span = subCtx.span(),
            block = code.currentBlock,
            name = "it",
            path = "",
            variable = varIt,
        )
        code.nodes += loadItInc

        val one = LstInt(
            ref = code.nextRef(),
            span = subCtx.span(),
            block = code.currentBlock,
            value = 1,
        )
        code.nodes += one

        val inc = LstFunCall(
            ref = code.nextRef(),
            span = subCtx.span(),
            block = code.currentBlock,
            name = "plus",
            path = "",
            arguments = listOf(loadItInc.ref, one.ref),
        )
        code.nodes += inc

        code.nodes += LstStoreVar(
            ref = code.nextRef(),
            span = subCtx.span(),
            block = code.currentBlock,
            name = "it",
            path = "",
            variable = varIt,
            expr = inc.ref,
        )
    }

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // jump to start
    code.executeDeferredActions()
    code.jumpedOutOfBlock = true
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // Restore prev block
    code.exitBlock()

    // end-if
    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )

    // } end-loop
    code.exitBlock()
    code.exitBlock()

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breaks.forEach { it.loopBlock = breakBlock }
    code.continues.forEach { it.loopBlock = continueBlock }

    code.breaks = prevBreaks
    code.continues = prevContinues

    code.exitBlock()
}

private fun ParserCtx.processWhileStatement(subCtx: MainParser.WhileStatementContext) {
    // Converts a while in loop+if
    // loop: {
    //   if (cond) {
    //     code...
    //     goto loop;
    //   }
    // }
    val prevBreaks = code.breaks
    val prevContinues = code.continues

    code.breaks = mutableListOf()
    code.continues = mutableListOf()

    // loop {
    val prevBlock = code.currentBlock
    code.enterBlock(true)
    val breakBlock = code.currentBlock
    code.enterBlock(false)
    val continueBlock = code.currentBlock

    code.nodes += LstLoopStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = prevBlock,
        breakBlock = breakBlock,
        continueBlock = continueBlock,
    )

    // if
    val cond = processExpression(subCtx.expression())

    code.nodes += LstIfStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        cond = cond,
    )

    code.enterBlock(false)

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // jump to start
    code.executeDeferredActions()
    code.jumpedOutOfBlock = true
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // Restore prev block
    code.exitBlock()

    // end-if
    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )

    // } end-loop
    code.exitBlock()
    code.exitBlock()

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breaks.forEach { it.loopBlock = breakBlock }
    code.continues.forEach { it.loopBlock = continueBlock }

    code.breaks = prevBreaks
    code.continues = prevContinues
}

private fun ParserCtx.processLoopStatement(subCtx: MainParser.LoopStatementContext) {
    val prevBreaks = code.breaks
    val prevContinues = code.continues

    code.breaks = mutableListOf()
    code.continues = mutableListOf()

    // loop {
    val prevBlock = code.currentBlock
    code.enterBlock(true)
    val breakBlock = code.currentBlock
    code.enterBlock(false)
    val continueBlock = code.currentBlock

    code.nodes += LstLoopStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = prevBlock,
        breakBlock = breakBlock,
        continueBlock = continueBlock,
    )

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // jump to start
    code.executeDeferredActions()
    code.jumpedOutOfBlock = true
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // } end-loop
    code.exitBlock()
    code.exitBlock()

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breaks.forEach { it.loopBlock = breakBlock }
    code.continues.forEach { it.loopBlock = continueBlock }

    code.breaks = prevBreaks
    code.continues = prevContinues
}

private fun ParserCtx.processIfStatement(subCtx: MainParser.IfStatementContext) {
    val cond = processExpression(subCtx.expression())

    code.nodes += LstIfStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        cond = cond,
    )

    code.enterBlock(false)

    subCtx.statementBlock(0).statement().map { processStatement(it) }

    // Restore prev block
    code.exitBlock()

    if (subCtx.statementBlock(1) != null) {
        code.enterBlock(false)

        code.nodes += LstIfElse(
            ref = code.nextRef(),
            span = subCtx.statementBlock(1).span(),
            block = code.currentBlock
        )

        subCtx.statementBlock(1).statement().map { processStatement(it) }

        // Restore prev block
        code.exitBlock()
    }

    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )
}

private fun ParserCtx.processLetStatement(subCtx: MainParser.LetStatementContext) {
    val span = subCtx.anyName().span()
    val name = subCtx.anyName().text
    val typeUsage = subCtx.typeUsage()?.let { resolveTypeUsage(it) }
    val variable = code.letVar(span, name, typeUsage)

    if (subCtx.expression() != null) {
        val expr = processExpression(subCtx.expression())
        code.nodes += LstStoreVar(
            ref = code.nextRef(),
            span = subCtx.expression().span(),
            block = code.currentBlock,
            name = variable.name,
            path = "",
            expr = expr,
            variable = variable
        )
    }
}