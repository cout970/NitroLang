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

                when {
                    subSubCtx.expression() != null && subSubCtx.nameToken() != null -> {
                        val receiver = processExpression(subSubCtx.expression())
                        val value = processExpression(subCtx.expression())

                        code.nodes += LstStoreField(
                            ref = code.nextRef(),
                            span = subSubCtx.nameToken().span(),
                            block = code.currentBlock,
                            name = subSubCtx.nameToken().text,
                            instance = receiver,
                            expr = value,
                        )
                    }

                    subSubCtx.collectionIndexingSuffix() != null -> {
                        val receiver = processExpression(subSubCtx.expression())
                        val index = processExpression(subSubCtx.collectionIndexingSuffix().expression())
                        val value = processExpression(subCtx.expression())

                        code.nodes += LstFunCall(
                            ref = code.nextRef(),
                            span = subSubCtx.collectionIndexingSuffix().expression().span(),
                            block = code.currentBlock,
                            name = "set",
                            path = "",
                            arguments = listOf(receiver, index, value),
                        )
                    }

                    subSubCtx.LBRACKET() != null -> {
                        val receiver = processExpression(subSubCtx.expression())
                        val value = processExpression(subCtx.expression())

                        code.nodes += LstFunCall(
                            ref = code.nextRef(),
                            span = subSubCtx.span(),
                            block = code.currentBlock,
                            name = "add",
                            path = "",
                            arguments = listOf(receiver, value),
                        )
                    }

                    subSubCtx.nameToken() != null -> {
                        val value = processExpression(subCtx.expression())
                        val name = subSubCtx.nameToken().text
                        var path = ""

                        if (subSubCtx.modulePath() != null) {
                            path = subSubCtx.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
                        }

                        code.nodes += LstStoreVar(
                            ref = code.nextRef(),
                            span = subSubCtx.nameToken().span(),
                            block = code.currentBlock,
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
    // let aux = iterable.to_iterator()
    // loop: {
    //   let i = aux.next()
    //   if (i.is_some()) {
    //     code...
    //     goto loop;
    //   }
    // }
    val prevBreakNodes = code.breakNodes
    val prevContinueNodes = code.continueNodes

    code.breakNodes = mutableListOf()
    code.continueNodes = mutableListOf()

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
    val breakBlock = code.createBlock()
    code.currentBlock = breakBlock
    val continueBlock = code.createBlock()
    code.currentBlock = continueBlock

    code.nodes += LstLoopStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = prevBlock,
        breakBlock = breakBlock,
        continueBlock = continueBlock,
    )

    // aux.next()
    val iteratorNext = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.expression().span(),
        block = code.currentBlock,
        name = "next",
        path = "",
        arguments = listOf(toIterator.ref),
    )
    code.nodes += iteratorNext

    // let i
    val variable = LstVar(
        span = subCtx.nameToken().span(),
        name = subCtx.nameToken().text,
        block = code.currentBlock,
        typeUsage = null,
        validAfter = code.currentRef(),
        ref = code.nextVarRef(),
    )

    code.variables[variable.ref] = variable

    // i = aux.next()
    code.nodes += LstStoreVar(
        ref = code.nextRef(),
        span = subCtx.nameToken().span(),
        block = code.currentBlock,
        name = subCtx.nameToken().text,
        path = "",
        expr = iteratorNext.ref,
        varRef = variable.ref,
        variable = variable,
    )

    // i.is_some()
    val isSomeCall = LstFunCall(
        ref = code.nextRef(),
        span = subCtx.nameToken().span(),
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

    val prevBlock2 = code.currentBlock
    code.currentBlock = code.createBlock()

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // jump to start
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // Restore prev block
    code.currentBlock = prevBlock2

    // end-if
    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )

    // } end-loop
    code.currentBlock = prevBlock

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breakNodes.forEach { it.loopBlock = breakBlock }
    code.continueNodes.forEach { it.loopBlock = continueBlock }

    code.breakNodes = prevBreakNodes
    code.continueNodes = prevContinueNodes
}

private fun ParserCtx.processRepeatStatement(subCtx: MainParser.RepeatStatementContext) {
    // Converts a while in
    // {
    //   let limit = count
    //   let it = 0
    //   loop: {
    //     if (it < limit) {
    //       code...
    //       it = it + 1
    //       goto loop;
    //     }
    //   }
    // }
    val prevBlockStart = code.currentBlock
    code.currentBlock = code.createBlock()

    // let limit = count
    val count = processExpression(subCtx.expression())

    val varLimit = LstVar(
        span = subCtx.expression().span(),
        block = code.currentBlock,
        name = "limit",
        typeUsage = TypeUsage.int(),
        validAfter = code.currentRef(),
        ref = code.nextVarRef(),
    )

    code.variables[varLimit.ref] = varLimit

    code.nodes += LstStoreVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "limit",
        path = "",
        expr = count,
        varRef = varLimit.ref,
        variable = varLimit,
    )

    // let it = 0
    val varIt = LstVar(
        span = subCtx.expression().span(),
        block = code.currentBlock,
        name = "it",
        typeUsage = TypeUsage.int(),
        validAfter = code.currentRef(),
        ref = code.nextVarRef(),
    )

    code.variables[varIt.ref] = varIt

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
        varRef = varIt.ref,
        variable = varIt,
    )

    val prevBreakNodes = code.breakNodes
    val prevContinueNodes = code.continueNodes

    code.breakNodes = mutableListOf()
    code.continueNodes = mutableListOf()

    // loop {
    val prevBlock = code.currentBlock
    val breakBlock = code.createBlock()
    code.currentBlock = breakBlock
    val continueBlock = code.createBlock()
    code.currentBlock = continueBlock

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
        varRef = varIt.ref,
        variable = varIt,
    )
    code.nodes += loadIfIt

    val loadIfLimit = LstLoadVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "limit",
        path = "",
        varRef = varLimit.ref,
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

    val prevBlock2 = code.currentBlock
    code.currentBlock = code.createBlock()

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // it = it + 1
    val loadItInc = LstLoadVar(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        name = "it",
        path = "",
        varRef = varIt.ref,
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
        name = "repeat_aux",
        path = "",
        varRef = varIt.ref,
        variable = varIt,
        expr = inc.ref,
    )

    // jump to start
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // Restore prev block
    code.currentBlock = prevBlock2

    // end-if
    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )

    // } end-loop
    code.currentBlock = prevBlock

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breakNodes.forEach { it.loopBlock = breakBlock }
    code.continueNodes.forEach { it.loopBlock = continueBlock }

    code.breakNodes = prevBreakNodes
    code.continueNodes = prevContinueNodes

    code.currentBlock = prevBlockStart
}

private fun ParserCtx.processWhileStatement(subCtx: MainParser.WhileStatementContext) {
    // Converts a while in loop+if
    // loop: {
    //   if (cond) {
    //     code...
    //     goto loop;
    //   }
    // }
    val prevBreakNodes = code.breakNodes
    val prevContinueNodes = code.continueNodes

    code.breakNodes = mutableListOf()
    code.continueNodes = mutableListOf()

    // loop {
    val prevBlock = code.currentBlock
    val breakBlock = code.createBlock()
    code.currentBlock = breakBlock
    val continueBlock = code.createBlock()
    code.currentBlock = continueBlock

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

    val prevBlock2 = code.currentBlock
    code.currentBlock = code.createBlock()

    // Code in loop...
    processStatementBlock(subCtx.statementBlock())

    // jump to start
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // Restore prev block
    code.currentBlock = prevBlock2

    // end-if
    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )

    // } end-loop
    code.currentBlock = prevBlock

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breakNodes.forEach { it.loopBlock = breakBlock }
    code.continueNodes.forEach { it.loopBlock = continueBlock }

    code.breakNodes = prevBreakNodes
    code.continueNodes = prevContinueNodes
}

private fun ParserCtx.processLoopStatement(subCtx: MainParser.LoopStatementContext) {
    val prevBreakNodes = code.breakNodes
    val prevContinueNodes = code.continueNodes

    code.breakNodes = mutableListOf()
    code.continueNodes = mutableListOf()

    // loop {
    val prevBlock = code.currentBlock
    val breakBlock = code.createBlock()
    code.currentBlock = breakBlock
    val continueBlock = code.createBlock()
    code.currentBlock = continueBlock

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
    code.nodes += LstLoopJump(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        backwards = true,
        loopBlock = continueBlock,
    )

    // } end-loop
    code.currentBlock = prevBlock

    code.nodes += LstLoopEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
    )

    // Link jumps inside this block
    code.breakNodes.forEach { it.loopBlock = breakBlock }
    code.continueNodes.forEach { it.loopBlock = continueBlock }

    code.breakNodes = prevBreakNodes
    code.continueNodes = prevContinueNodes
}

private fun ParserCtx.processIfStatement(subCtx: MainParser.IfStatementContext) {
    val cond = processExpression(subCtx.expression())

    code.nodes += LstIfStart(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock,
        cond = cond,
    )

    val prevBlock = code.currentBlock
    code.currentBlock = code.createBlock()

    subCtx.statementBlock(0).statement().map { processStatement(it) }

    // Restore prev block
    code.currentBlock = prevBlock

    if (subCtx.statementBlock(1) != null) {
        code.currentBlock = code.createBlock()

        code.nodes += LstIfElse(
            ref = code.nextRef(),
            span = subCtx.statementBlock(1).span(),
            block = code.currentBlock
        )

        subCtx.statementBlock(1).statement().map { processStatement(it) }

        // Restore prev block
        code.currentBlock = prevBlock
    }

    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = subCtx.span(),
        block = code.currentBlock
    )
}

private fun ParserCtx.processLetStatement(subCtx: MainParser.LetStatementContext) {
    val variable = LstVar(
        span = subCtx.nameToken().span(),
        block = code.currentBlock,
        name = subCtx.nameToken().text,
        typeUsage = subCtx.typeUsage()?.let { resolveTypeUsage(it) },
        validAfter = code.currentRef(),
        ref = code.nextVarRef(),
    )

    code.variables[variable.ref] = variable

    if (subCtx.expression() != null) {
        val expr = processExpression(subCtx.expression())
        code.nodes += LstStoreVar(
            ref = code.nextRef(),
            span = subCtx.expression().span(),
            block = code.currentBlock,
            name = variable.name,
            path = "",
            expr = expr,
            varRef = variable.ref,
            variable = variable
        )
    }
}