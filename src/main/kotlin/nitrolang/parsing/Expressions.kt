package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.backend.*
import nitrolang.gen.MainParser
import nitrolang.util.Span
import org.antlr.v4.runtime.tree.TerminalNode

fun ParserCtx.processExpression(ctx: MainParser.ExpressionContext): Ref {
    val complex = ctx.expressionComplex()

    return when {
        complex.ifExpr() != null -> {
            processExpressionIfExpr(complex.ifExpr())
        }

        complex.returnExpr() != null -> {
            val returnExpr = complex.returnExpr()
            val expr = if (returnExpr.expression() != null) {
                processExpression(returnExpr.expression())
            } else {
                val node = LstNothing(
                    ref = code.nextRef(),
                    span = returnExpr.span(),
                    block = code.currentBlock,
                )
                code.nodes += node
                node.ref
            }

            code.executeDeferredActions()
            code.jumpedOutOfBlock = true
            val node = LstReturn(
                ref = code.nextRef(),
                span = returnExpr.span(),
                block = code.currentBlock,
                expr = expr
            )
            code.nodes += node
            node.ref
        }

        complex.expressionOr() != null -> {
            processExpressionOr(complex.expressionOr())
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

fun ParserCtx.processExpressionOr(ctx: MainParser.ExpressionOrContext): Ref {
    var left = processExpressionAnd(ctx.expressionAnd(0))

    // Transform `a || b` into `if a { true } else { b }`
    ctx.expressionAnd().drop(1).forEach { right ->
        code.nodes += LstIfStart(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = left,
        )

        code.enterBlock(false)

        val ifTrue = LstBoolean(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            value = true,
        )
        code.nodes += ifTrue

        // Restore prev block
        code.exitBlock()
        code.enterBlock(false)

        code.nodes += LstIfElse(
            ref = code.nextRef(),
            span = right.span(),
            block = code.currentBlock
        )

        val ifFalse = processExpressionAnd(right)

        // Restore prev block
        code.exitBlock()

        code.nodes += LstIfEnd(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock
        )

        val choose = LstIfChoose(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = left,
            ifTrue = ifTrue.ref,
            ifFalse = ifFalse,
        )

        code.nodes += choose
        left = choose.ref
    }

    return left
}

fun ParserCtx.processExpressionAnd(ctx: MainParser.ExpressionAndContext): Ref {
    var left = processExpressionBinOp(ctx.expressionBinaryOp(0))

    // Transform `a && b` into `if a { b } else { false }`
    ctx.expressionBinaryOp().drop(1).forEach { right ->
        code.nodes += LstIfStart(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = left,
        )


        code.enterBlock(false)

        val ifTrue = processExpressionBinOp(right)

        // Restore prev block
        code.exitBlock()
        code.enterBlock(false)

        code.nodes += LstIfElse(
            ref = code.nextRef(),
            span = right.span(),
            block = code.currentBlock
        )

        val ifFalse = LstBoolean(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            value = false,
        )
        code.nodes += ifFalse

        // Restore prev block
        code.exitBlock()

        code.nodes += LstIfEnd(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock
        )

        val choose = LstIfChoose(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = left,
            ifTrue = ifTrue,
            ifFalse = ifFalse.ref,
        )

        code.nodes += choose
        left = choose.ref
    }

    return left
}

fun ParserCtx.processExpressionBinOp(bin: MainParser.ExpressionBinaryOpContext): Ref {
    val exprs = bin.expressionSimple()
    val ops = bin.binaryOperator().map { it to it.span() }

    return resolvePrecedence(exprs, ops) { processExpressionSimple(it) }
}

fun ParserCtx.processExpressionSimple(simple: MainParser.ExpressionSimpleContext): Ref {
    return when {
        simple.AS() != null -> {
            val typeUsage = resolveTypeUsage(simple.typeUsage())
            val expr = processExpressionWithSuffix(simple.expressionWithSuffix(0))

            val node = LstAsType(
                ref = code.nextRef(),
                span = simple.span(),
                block = code.currentBlock,
                expr = expr,
                typeUsage = typeUsage,
            )
            code.nodes += node
            node.ref
        }

        simple.IS() != null -> {
            val typePattern = resolveTypePattern(simple.typePattern())
            val expr = processExpressionWithSuffix(simple.expressionWithSuffix(0))

            if (simple.NOT() != null) {
                val node = LstIsType(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    expr = expr,
                    typePattern = typePattern,
                )
                code.nodes += node

                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "logical_not",
                    path = "",
                    arguments = listOf(node.ref)
                )
                code.nodes += call
                call.ref
            } else {
                val node = LstIsType(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    expr = expr,
                    typePattern = typePattern,
                )
                code.nodes += node
                node.ref
            }
        }

        simple.IN() != null -> {
            val value = processExpressionWithSuffix(simple.expressionWithSuffix(0))
            val collection = processExpressionWithSuffix(simple.expressionWithSuffix(1))

            if (simple.NOT() != null) {
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "contains",
                    path = "",
                    arguments = listOf(collection, value),
                )
                code.nodes += call

                // not
                val call2 = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "logical_not",
                    path = "",
                    arguments = listOf(call.ref),
                )
                code.nodes += call2
                call2.ref
            } else {
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "contains",
                    path = "",
                    arguments = listOf(collection, value),
                )
                code.nodes += call
                call.ref
            }
        }

        simple.notExpr() != null -> {
            val expr = processExpressionSimple(simple.notExpr().expressionSimple())
            val call = LstFunCall(
                ref = code.nextRef(),
                span = simple.notExpr().span(),
                block = code.currentBlock,
                name = "logical_not",
                path = "",
                arguments = listOf(expr),
            )
            code.nodes += call
            call.ref
        }

        simple.minusExpr() != null -> {
            val expr = processExpressionSimple(simple.minusExpr().expressionSimple())
            val call = LstFunCall(
                ref = code.nextRef(),
                span = simple.minusExpr().span(),
                block = code.currentBlock,
                name = "unary_minus",
                path = "",
                arguments = listOf(expr),
            )
            code.nodes += call
            call.ref
        }

        simple.plusExpr() != null -> {
            val expr = processExpressionSimple(simple.plusExpr().expressionSimple())
            val call = LstFunCall(
                ref = code.nextRef(),
                span = simple.plusExpr().span(),
                block = code.currentBlock,
                name = "unary_plus",
                path = "",
                arguments = listOf(expr),
            )
            code.nodes += call
            call.ref
        }

        else -> {
            processExpressionWithSuffix(simple.expressionWithSuffix(0))
        }
    }
}

fun ParserCtx.processExpressionWithSuffix(ctx: MainParser.ExpressionWithSuffixContext): Ref {
    return when {
        ctx.earlyReturnSuffix() != null -> {
            val subCtx = ctx.earlyReturnSuffix()
            val prevRef = processExpressionWithSuffix(ctx.expressionWithSuffix())

            val call = code.call(subCtx.span(), "", "is_returnable_error", listOf(prevRef))
            code.ifStart(subCtx.span(), call)
            code.returnExpr(subCtx.span(), prevRef)
            code.ifEnd(subCtx.span())
            code.call(subCtx.span(), "", "get_or_crash", listOf(prevRef))
        }

        ctx.assertSuffix() != null -> {
            val prevRef = processExpressionWithSuffix(ctx.expressionWithSuffix())

            code.call(ctx.assertSuffix().span(), "", "get_or_crash", listOf(prevRef))
        }

        ctx.collectionIndexingSuffix() != null -> {
            val prevRef = processExpressionWithSuffix(ctx.expressionWithSuffix())
            val indexRef = processExpression(ctx.collectionIndexingSuffix().expression())

            code.call(ctx.collectionIndexingSuffix().span(), "", "get", listOf(prevRef, indexRef))
        }

        ctx.structFieldAccessSuffix() != null -> {
            val prevRef = processExpressionWithSuffix(ctx.expressionWithSuffix())
            val name = ctx.structFieldAccessSuffix().anyName().text

            code.loadField(ctx.structFieldAccessSuffix().span(), prevRef, name)
        }

        ctx.anyName() != null -> {
            val prev = processExpressionWithSuffix(ctx.expressionWithSuffix())
            val name = ctx.anyName().text
            var path = ""
            if (ctx.modulePath() != null) {
                path = ctx.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
            }

            processFunctionCall(
                span = ctx.anyName().span(),
                receiver = prev,
                path = path,
                name = name,
                params = ctx.functionCallParams(),
                end = ctx.functionCallEnd(),
                code = code
            )
        }

        ctx.expressionOrFunctionCall() != null -> {
            processExpressionOrFunctionCall(ctx.expressionOrFunctionCall())
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

fun ParserCtx.processExpressionOrFunctionCall(ctx: MainParser.ExpressionOrFunctionCallContext): Ref {
    return when {
        ctx.anyName() != null -> {
            val name = ctx.anyName().text
            var path = ""
            if (ctx.modulePath() != null) {
                path = ctx.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
            }

            processFunctionCall(
                span = ctx.anyName().span(),
                receiver = null,
                path = path,
                name = name,
                params = ctx.functionCallParams(),
                end = ctx.functionCallEnd(),
                code = code
            )
        }

        ctx.parenthesizedExpression() != null -> {
            val prev = processExpression(ctx.parenthesizedExpression().expression())

            processFunctionCall(
                span = ctx.parenthesizedExpression().expression().span(),
                receiver = prev,
                path = "",
                name = LAMBDA_CALL_FUNCTION,
                params = ctx.functionCallParams(),
                end = ctx.functionCallEnd(),
                code = code
            )
        }

        ctx.expressionLiteral() != null -> {
            val prev = processExpressionExpressionLiteral(ctx.expressionLiteral())

            processFunctionCall(
                span = ctx.expressionLiteral().span(),
                receiver = prev,
                path = "",
                name = LAMBDA_CALL_FUNCTION,
                params = ctx.functionCallParams(),
                end = ctx.functionCallEnd(),
                code = code
            )
        }

        ctx.structInstanceExpr() != null -> {
            val prev = processExpressionStructInstanceExpr(ctx.structInstanceExpr())

            processFunctionCall(
                span = ctx.structInstanceExpr().upperName().span(),
                receiver = prev,
                path = "",
                name = LAMBDA_CALL_FUNCTION,
                params = ctx.functionCallParams(),
                end = ctx.functionCallEnd(),
                code = code
            )
        }

        ctx.THIS() != null -> {
            val load = LstLoadVar(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                name = SELF_NAME,
                path = "",
            )
            code.nodes += load
            processFunctionCall(
                span = ctx.span(),
                receiver = load.ref,
                path = "",
                name = LAMBDA_CALL_FUNCTION,
                params = ctx.functionCallParams(),
                end = ctx.functionCallEnd(),
                code = code
            )
        }

        ctx.expressionBase() != null -> {
            processExpressionBase(ctx.expressionBase())
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

fun ParserCtx.processFunctionCall(
    span: Span,
    receiver: Ref?,
    path: String,
    name: String,
    params: MainParser.FunctionCallParamsContext?,
    end: MainParser.FunctionCallEndContext?,
    code: LstCode
): Ref {
    val args = mutableListOf<Ref>()
    val specifiedTypeParams = mutableListOf<LstTypeUsage>()

    if (receiver != null) {
        args += receiver
    }

    params?.typeParamArg()?.typeUsage()?.forEach { param ->
        specifiedTypeParams += resolveTypeUsage(param)
    }

    params?.functionCallParamList()?.expression()?.forEach { param ->
        args += processExpression(param)
    }

    if (end != null) {
        when {
            end.lambdaExpr() != null -> {
                args += processExpressionLambdaExpr(end.lambdaExpr())
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    val call = LstFunCall(
        ref = code.nextRef(),
        span = span,
        block = code.currentBlock,
        name = name,
        path = path,
        arguments = args,
        explicitTypeParams = specifiedTypeParams,
    )
    code.nodes += call
    return call.ref
}


fun ParserCtx.processExpressionBase(ctx: MainParser.ExpressionBaseContext): Ref {
    return when {
        ctx.parenthesizedExpression() != null -> {
            processExpression(ctx.parenthesizedExpression().expression())
        }

        ctx.nothingExpression() != null -> {
            val node = LstNothing(
                ref = code.nextRef(),
                span = ctx.nothingExpression().span(),
                block = code.currentBlock,
            )
            code.nodes += node
            node.ref
        }

        ctx.expressionLiteral() != null -> {
            processExpressionExpressionLiteral(ctx.expressionLiteral())
        }

        ctx.whenExpr() != null -> {
            processExpressionWhenExpr(ctx.whenExpr())
        }

        ctx.listExpr() != null -> {
            processExpressionListExpr(ctx.listExpr())
        }

        ctx.mapExpr() != null -> {
            processExpressionMapExpr(ctx.mapExpr())
        }

        ctx.setExpr() != null -> {
            processExpressionSetExpr(ctx.setExpr())
        }

        ctx.lambdaExpr() != null -> {
            processExpressionLambdaExpr(ctx.lambdaExpr())
        }

        ctx.jsonExpr() != null -> {
            processExpressionJsonExpr(ctx.jsonExpr())
        }

        ctx.THIS() != null -> {
            val node = LstLoadVar(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                name = SELF_NAME,
                path = "",
            )
            code.nodes += node
            node.ref
        }

        ctx.BREAK() != null -> {
            code.executeDeferredActions()
            code.jumpedOutOfBlock = true
            val node = LstLoopJump(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                backwards = false,
            )
            code.breaks += node
            code.nodes += node
            node.ref
        }

        ctx.CONTINUE() != null -> {
            code.executeDeferredActions()
            code.jumpedOutOfBlock = true
            val node = LstLoopJump(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                backwards = true,
            )
            code.continues += node
            code.nodes += node
            node.ref
        }

        ctx.structInstanceExpr() != null -> {
            processExpressionStructInstanceExpr(ctx.structInstanceExpr())
        }

        ctx.variableExpr() != null -> {
            processExpressionVariableExpr(ctx.variableExpr())
        }

        ctx.sizeOfExpr() != null -> {
            processExpressionSizeOf(ctx.sizeOfExpr())
        }

        ctx.templateLiteral() != null -> {
            processExpressionTemplateLiteral(ctx.templateLiteral())
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

fun ParserCtx.processConstExpr(ctx: MainParser.ConstExprContext): ConstValue {
    if (ctx.nothingExpression() != null) {
        return ConstNothing
    }

    if (ctx.constExpressionLiteral() == null) {
        error("Grammar has been expanded and parser is outdated")
    }

    return when {
        ctx.constExpressionLiteral().PLAIN_STRING() != null -> {
            ConstString(processPlainString(ctx.constExpressionLiteral().PLAIN_STRING()))
        }

        ctx.constExpressionLiteral().FALSE() != null -> ConstBoolean(false)
        ctx.constExpressionLiteral().TRUE() != null -> ConstBoolean(true)
        ctx.constExpressionLiteral().NULL() != null -> {
            collector.report(
                "Null values are not available in this language, use Optional::None instead",
                ctx.span()
            )
            ConstNothing
        }

        ctx.constExpressionLiteral().INT_NUMBER() != null -> {
            val text = ctx.constExpressionLiteral().INT_NUMBER().text

            val intValue = stringToInt(text) ?: run {
                collector.report("Invalid int value '${text}'", ctx.span())
                0
            }

            if (text.startsWith("-") || text.startsWith("+")) {
                checkSubPreviousToken(ctx.constExpressionLiteral(), ctx.constExpressionLiteral().INT_NUMBER())
            }

            ConstInt(intValue)
        }

        ctx.constExpressionLiteral().FLOAT_NUMBER() != null -> {
            val text = ctx.constExpressionLiteral().FLOAT_NUMBER().text
            var floatValue = stringToFloat(text)

            if (floatValue == null) {
                collector.report("Invalid float value '${text}'", ctx.span())
                floatValue = 0f
            }

            if (text.startsWith("-") || text.startsWith("+")) {
                checkSubPreviousToken(ctx.constExpressionLiteral(), ctx.constExpressionLiteral().FLOAT_NUMBER())
            }

            ConstFloat(floatValue)
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}


fun ParserCtx.processExpressionExpressionLiteral(ctx: MainParser.ExpressionLiteralContext): Ref {
    return when {
        ctx.INT_NUMBER() != null -> {
            val text = ctx.INT_NUMBER().text
            var intValue = stringToInt(text)

            if (intValue == null) {
                collector.report("Invalid int value '${text}'", ctx.span())
                intValue = 0
            }

            if (text.startsWith("-") || text.startsWith("+")) {
                checkSubPreviousToken(ctx, ctx.INT_NUMBER())
            }

            val node = LstInt(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                value = intValue
            )
            code.nodes += node
            node.ref
        }

        ctx.LONG_NUMBER() != null -> {
            val text = ctx.LONG_NUMBER().text
            var longValue = stringToLong(text)

            if (longValue == null) {
                collector.report("Invalid long value '${text}'", ctx.span())
                longValue = 0
            }

            if (text.startsWith("-") || text.startsWith("+")) {
                checkSubPreviousToken(ctx, ctx.LONG_NUMBER())
            }

            val node = LstLong(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                value = longValue
            )
            code.nodes += node
            node.ref
        }

        ctx.FLOAT_NUMBER() != null -> {
            val text = ctx.FLOAT_NUMBER().text
            var floatValue = stringToFloat(text)

            if (floatValue == null) {
                collector.report("Invalid float value '${text}'", ctx.span())
                floatValue = 0f
            }

            if (text.startsWith("-") || text.startsWith("+")) {
                checkSubPreviousToken(ctx, ctx.FLOAT_NUMBER())
            }

            val node = LstFloat(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                value = floatValue
            )
            code.nodes += node
            node.ref
        }

        ctx.string() != null -> {
            processString(ctx.string())
        }

        ctx.TRUE() != null -> {
            val node = LstBoolean(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                value = true,
            )
            code.nodes += node
            node.ref
        }

        ctx.FALSE() != null -> {
            val node = LstBoolean(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                value = false,
            )
            code.nodes += node
            node.ref
        }

        ctx.NULL() != null -> {
            collector.report(
                "Null values are not available in this language, use Optional::None instead",
                ctx.span()
            )

            val node = LstNothing(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
            )
            code.nodes += node
            node.ref
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

fun ParserCtx.processPlainString(ctx: TerminalNode): String {
    val tokenText = ctx.text
    val text = tokenText.substring(1, tokenText.length - 1)
    return unescapeStringLiteral(text)
}

fun ParserCtx.processString(ctx: MainParser.StringContext): Ref {
    if (ctx.PLAIN_STRING() != null) {
        val node = LstString(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            value = processPlainString(ctx.PLAIN_STRING())
        )
        code.nodes += node
        return node.ref
    }

    if (ctx.STRING2_START() != null) {
        val text = ctx.string2Contents().joinToString("") { it.text }
        val node = LstString(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            value = text
        )
        code.nodes += node
        return node.ref
    }

    val buff = StringBuilder()
    val allParts = mutableListOf<Ref>()

    fun endChunk() {
        val part = LstString(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            value = unescapeStringLiteral(buff.toString())
        )
        code.nodes += part
        buff.clear()
        allParts += part.ref
    }

    ctx.stringContents().forEach { item ->
        when {
            item.STRING_BLOB() != null -> buff.append(item.STRING_BLOB().text)
            item.STRING_ESCAPE() != null -> buff.append(item.STRING_ESCAPE().text)
            item.STRING_VAR() != null -> {
                endChunk()
                val variable = code.loadVar(ctx.span(), "", item.STRING_VAR().text.substring(1))

                allParts += code.call(
                    span = ctx.span(),
                    path = "",
                    name = "to_string",
                    arguments = listOf(variable)
                )
            }

            item.STRING_INTERP_START() != null -> {
                endChunk()
                val ref = processExpression(item.expression())

                allParts += code.call(
                    span = ctx.span(),
                    path = "",
                    name = "to_string",
                    arguments = listOf(ref)
                )
            }
        }
    }

    if (buff.isNotEmpty() || allParts.isEmpty()) {
        val node = LstString(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            value = unescapeStringLiteral(buff.toString())
        )
        code.nodes += node
        buff.clear()
        allParts += node.ref
    }

    if (allParts.size == 1) {
        return allParts.first()
    }

    return allParts.reduce { acc, ref ->
        val node = LstFunCall(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            name = "concat",
            path = "",
            arguments = listOf(acc, ref),
        )
        code.nodes += node
        node.ref
    }
}

fun unescapeStringLiteral(tokenText: String): String {
    var text = tokenText

    // Hex \xFF
    text = text.replace(Regex("""\\x([0-9a-fA-F]{2})""")) { res ->
        val hex = res.groupValues[1].toInt(16)
        hex.toChar().toString()
    }

    // Unicode \uFFFF
    text = text.replace(Regex("""\\u([0-9a-fA-F]{4})""")) { res ->
        val hex = res.groupValues[1].toInt(16)
        hex.toChar().toString()
    }

    // C escape codes \n
    text = text.replace(Regex("""\\(.)""")) { res ->
        when (val char = res.groupValues[1][0]) {
            'n' -> "\n"
            'r' -> "\r"
            '"' -> "\""
            '$' -> "\$"
            't' -> "\t"
            '\'' -> "\'"
            '\\' -> "\\"
            'a' -> "\u0007"
            'b' -> "\u0008"
            'e' -> "\u001b"
            'f' -> "\u000c"
            'v' -> "\u000b"
            else -> char.toString()
        }
    }

    return text
}

fun ParserCtx.createStructInstance(
    type: LstTypeUsage,
    fields: List<Pair<String, Ref>>,
    code: LstCode,
    span: Span
): Ref {
    val alloc = LstAlloc(
        ref = code.nextRef(),
        span = span,
        block = code.currentBlock,
        typeUsage = type
    )
    code.nodes += alloc

    fields.forEach { (name, expr) ->
        val store = LstStoreField(
            ref = code.nextRef(),
            span = span,
            block = code.currentBlock,
            name = name,
            instance = alloc.ref,
            expr = expr
        )
        code.nodes += store
    }

    return alloc.ref
}

fun ParserCtx.processExpressionJsonExpr(ctx: MainParser.JsonExprContext): Ref {
    return processJsonValue(ctx.jsonValue())
}

fun ParserCtx.processJsonValue(value: MainParser.JsonValueContext): Ref {
    return when {
        value.string() != null -> {
            val string = processString(value.string())

            createStructInstance(
                type = LstTypeUsage.simple("Json::String"),
                fields = listOf("value" to string),
                code = code,
                span = value.span(),
            )
        }

        value.INT_NUMBER() != null -> {
            val text = value.INT_NUMBER().text
            var intValue = stringToInt(text)

            if (intValue == null) {
                collector.report("Invalid int value '${text}'", value.span())
                intValue = 0
            }

            val const = LstFloat(
                ref = code.nextRef(),
                span = value.span(),
                block = code.currentBlock,
                value = intValue.toFloat(),
            )
            code.nodes += const

            createStructInstance(
                type = LstTypeUsage.simple("Json::Number"),
                fields = listOf("value" to const.ref),
                code = code,
                span = value.span(),
            )
        }

        value.FLOAT_NUMBER() != null -> {
            var floatValue = stringToFloat(value.FLOAT_NUMBER().text)

            if (floatValue == null) {
                collector.report("Invalid float value '${value.FLOAT_NUMBER().text}'", value.span())
                floatValue = 0f
            }

            val const = LstFloat(
                ref = code.nextRef(),
                span = value.span(),
                block = code.currentBlock,
                value = floatValue,
            )
            code.nodes += const

            createStructInstance(
                type = LstTypeUsage.simple("Json::Number"),
                fields = listOf("value" to const.ref),
                code = code,
                span = value.span(),
            )
        }

        value.TRUE() != null -> {
            val const = LstBoolean(
                ref = code.nextRef(),
                span = value.span(),
                block = code.currentBlock,
                value = true,
            )
            code.nodes += const

            createStructInstance(
                type = LstTypeUsage.simple("Json::Boolean"),
                fields = listOf("value" to const.ref),
                code = code,
                span = value.span(),
            )
        }

        value.FALSE() != null -> {
            val const = LstBoolean(
                ref = code.nextRef(),
                span = value.span(),
                block = code.currentBlock,
                value = false,
            )
            code.nodes += const

            createStructInstance(
                type = LstTypeUsage.simple("Json::Boolean"),
                fields = listOf("value" to const.ref),
                code = code,
                span = value.span(),
            )
        }

        value.NULL() != null -> {
            createStructInstance(
                type = LstTypeUsage.simple("Json::Null"),
                fields = listOf(),
                code = code,
                span = value.span(),
            )
        }

        value.jsonObject() != null -> {
            val map = LstFunCall(
                ref = code.nextRef(),
                span = value.span(),
                block = code.currentBlock,
                name = "StringMap::new",
                path = "",
                arguments = emptyList(),
                explicitTypeParams = listOf(LstTypeUsage.simple("Json")),
            )
            code.nodes += map

            value.jsonObject().jsonPair().forEach { pair ->
                val keyRef = if (pair.string() != null) {
                    processString(pair.string())
                } else {
                    val entryKey = LstString(
                        ref = code.nextRef(),
                        span = value.span(),
                        block = code.currentBlock,
                        value = pair.anyName().text
                    )
                    code.nodes += entryKey
                    entryKey.ref
                }

                val valueRef = processJsonValue(pair.jsonValue())

                val set = LstFunCall(
                    ref = code.nextRef(),
                    span = value.span(),
                    block = code.currentBlock,
                    name = "set",
                    path = "",
                    arguments = listOf(map.ref, keyRef, valueRef),
                )
                code.nodes += set
            }

            createStructInstance(
                type = LstTypeUsage.simple("Json::Object"),
                fields = listOf("value" to map.ref),
                code = code,
                span = value.span(),
            )
        }

        value.jsonArray() != null -> {
            val list = LstFunCall(
                ref = code.nextRef(),
                span = value.span(),
                block = code.currentBlock,
                name = "new",
                path = "List",
                arguments = emptyList(),
                explicitTypeParams = listOf(LstTypeUsage.simple("Json")),
            )
            code.nodes += list

            value.jsonArray().jsonValue().forEach { jsonValue ->
                val entryValue = processJsonValue(jsonValue)

                val add = LstFunCall(
                    ref = code.nextRef(),
                    span = value.span(),
                    block = code.currentBlock,
                    name = "add",
                    path = "",
                    arguments = listOf(list.ref, entryValue),
                )
                code.nodes += add
            }

            createStructInstance(
                type = LstTypeUsage.simple("Json::Array"),
                fields = listOf("value" to list.ref),
                code = code,
                span = value.span(),
            )
        }

        value.expression() != null -> {
            processExpression(value.expression())
        }

        else -> error("Grammar has been expanded and parser is outdated")
    }
}

fun ParserCtx.processExpressionWhenExpr(ctx: MainParser.WhenExprContext): Ref {
    val entries = ctx.whenEntry()

    val whenArg = ctx.expression()?.let { processExpression(it) }

    val start = LstWhenStart(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
    )
    code.nodes += start

    code.enterBlock(false)
    val whenBlock = code.currentBlock

    val branchStores = mutableListOf<LstWhenStore>()

    entries.forEachIndexed { index, entry ->
        val key = entry.whenKey()

        if (key.expression() != null) {
            val keyRef = processExpression(key.expression())

            val condRef = if (whenArg != null) {
                val cond = LstFunCall(
                    ref = code.nextRef(),
                    span = key.expression().span(),
                    block = code.currentBlock,
                    name = "is_equal",
                    path = "",
                    arguments = listOf(keyRef, whenArg),
                )
                code.nodes += cond
                cond.ref
            } else {
                keyRef
            }

            code.nodes += LstIfStart(
                ref = code.nextRef(),
                span = key.expression().span(),
                block = code.currentBlock,
                cond = condRef,
            )

            code.enterBlock(false)

            val (branchValue, span) = if (entry.expression() != null) {
                processExpression(entry.expression()) to entry.expression().span()

            } else if (entry.statementBlock() != null) {
                processStatementBlock(entry.statementBlock())

                (code.lastExpression ?: error("Code block has no last expression")) to entry.statementBlock().span()
            } else {
                error("Grammar has been expanded and parser is outdated")
            }

            val store = LstWhenStore(
                ref = code.nextRef(),
                span = span,
                block = code.currentBlock,
                expr = branchValue,
                start = start,
            )
            code.nodes += store
            branchStores += store

            code.nodes += LstWhenJump(
                ref = code.nextRef(),
                span = entry.span(),
                block = code.currentBlock,
                whenBlock = whenBlock,
            )

            // Restore prev block
            code.exitBlock()

            code.nodes += LstIfEnd(
                ref = code.nextRef(),
                span = entry.span(),
                block = code.currentBlock,
            )

        } else if (key.ELSE() != null) {
            if (index != entries.lastIndex) {
                collector.report("'else' entry must be the last one in a when-expression", ctx.span())
            }

            val (branchValue, span) = if (entry.expression() != null) {
                processExpression(entry.expression()) to entry.expression().span()

            } else if (entry.statementBlock() != null) {
                processStatementBlock(entry.statementBlock())

                (code.lastExpression ?: error("Code block has no last expression")) to entry.statementBlock().span()
            } else {
                error("Grammar has been expanded and parser is outdated")
            }

            val store = LstWhenStore(
                ref = code.nextRef(),
                span = span,
                block = code.currentBlock,
                expr = branchValue,
                start = start,
            )
            code.nodes += store
            branchStores += store

        } else {
            error("Grammar has been expanded and parser is outdated")
        }
    }

    // Restore prev block
    code.exitBlock()

    val end = LstWhenEnd(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        isStatement = false,
        branchStores = branchStores,
        start = start,
    )
    code.nodes += end

    if (entries.isEmpty()) {
        collector.report("when-expression must have at least one entry", ctx.span())
        return end.ref
    }

    val elseCount = entries.count { it.whenKey().ELSE() != null }

    if (elseCount != 1) {
        collector.report("when-expression must have one 'else' entry", ctx.span())
        return end.ref
    }

    return end.ref
}

fun ParserCtx.processWhenStatement(ctx: MainParser.WhenExprContext, code: LstCode) {
    val entries = ctx.whenEntry()

    val whenArg = ctx.expression()?.let { processExpression(it) }

    if (entries.isEmpty()) {
        collector.report("when-statement must have at least one entry", ctx.span())
        return
    }

    val elseCount = entries.count { it.whenKey().ELSE() != null }

    if (elseCount > 1) {
        collector.report("when-statement must have at most 1 'else' entry", ctx.span())
        return
    }

    val start = LstWhenStart(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
    )
    code.nodes += start

    code.enterBlock(false)
    val whenBlock = code.currentBlock

    entries.forEachIndexed { index, entry ->
        val key = entry.whenKey()

        if (key.expression() != null) {
            val keyRef = processExpression(key.expression())

            val condRef = if (whenArg != null) {
                val cond = LstFunCall(
                    ref = code.nextRef(),
                    span = key.expression().span(),
                    block = code.currentBlock,
                    name = "is_equal",
                    path = "",
                    arguments = listOf(keyRef, whenArg),
                )
                code.nodes += cond
                cond.ref
            } else {
                keyRef
            }

            code.nodes += LstIfStart(
                ref = code.nextRef(),
                span = key.expression().span(),
                block = code.currentBlock,
                cond = condRef,
            )

            code.enterBlock(false)

            if (entry.expression() != null) {
                processExpression(entry.expression()) to entry.expression().span()

            } else if (entry.statementBlock() != null) {
                processStatementBlock(entry.statementBlock())

            } else {
                error("Grammar has been expanded and parser is outdated")
            }

            code.nodes += LstWhenJump(
                ref = code.nextRef(),
                span = entry.span(),
                block = code.currentBlock,
                whenBlock = whenBlock,
            )

            // Restore prev block
            code.exitBlock()

            code.nodes += LstIfEnd(
                ref = code.nextRef(),
                span = entry.span(),
                block = code.currentBlock,
            )

        } else if (key.ELSE() != null) {
            if (index != entries.lastIndex) {
                collector.report("'else' entry must be the last one in a when-expression", ctx.span())
            }

            if (entry.expression() != null) {
                processExpression(entry.expression()) to entry.expression().span()

            } else if (entry.statementBlock() != null) {
                processStatementBlock(entry.statementBlock())

            } else {
                error("Grammar has been expanded and parser is outdated")
            }
        } else {
            error("Grammar has been expanded and parser is outdated")
        }
    }

    // Restore prev block
    code.exitBlock()

    code.nodes += LstWhenEnd(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        isStatement = true,
        start = start,
    )
}

fun ParserCtx.processExpressionListExpr(ctx: MainParser.ListExprContext): Ref {
    val value = LstFunCall(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        name = "new",
        path = "List",
        arguments = emptyList(),
        explicitTypeParams = listOf(LstTypeUsage.unresolved(ctx.span(), "List item type")),
    )
    code.nodes += value

    val expressions = mutableListOf<Ref>()

    ctx.listEntry().forEach { item ->
        val itemRef = processExpression(item.expression())

        expressions += itemRef

        val add = LstFunCall(
            ref = code.nextRef(),
            span = item.expression().span(),
            block = code.currentBlock,
            name = "add",
            path = "",
            arguments = listOf(value.ref, itemRef),
        )
        code.nodes += add
    }

    return value.ref
}

fun ParserCtx.processExpressionMapExpr(ctx: MainParser.MapExprContext): Ref {
    val value = LstFunCall(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        name = "new",
        path = "Map",
        arguments = emptyList(),
        explicitTypeParams = listOf(
            LstTypeUsage.unresolved(ctx.span(), "Map key type"),
            LstTypeUsage.unresolved(ctx.span(), "Map value type")
        ),
    )
    code.nodes += value

    ctx.mapEntry().forEach { entry ->
        val keyCtx = entry.mapKey()
        val keyRef = when {
            keyCtx.anyName() != null -> {
                val node = LstString(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    value = keyCtx.anyName().text
                )
                code.nodes += node
                node.ref
            }

            keyCtx.string() != null -> {
                processString(keyCtx.string())
            }

            keyCtx.expression() != null -> {
                processExpression(keyCtx.expression())
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }

        val valueRef = processExpression(entry.expression())
        val span = keyCtx.span()

        code.nodes += LstFunCall(
            ref = code.nextRef(),
            span = span,
            block = code.currentBlock,
            name = "set",
            path = "",
            arguments = listOf(value.ref, keyRef, valueRef)
        )
    }

    return value.ref
}

fun ParserCtx.processExpressionSetExpr(ctx: MainParser.SetExprContext): Ref {
    val value = LstFunCall(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        name = "new",
        path = "Set",
        arguments = emptyList(),
        explicitTypeParams = listOf(LstTypeUsage.unresolved(ctx.span(), "Set item type")),
    )
    code.nodes += value

    ctx.listEntry().map { item ->
        val itemRef = processExpression(item.expression())
        val span = item.expression().span()

        code.nodes += LstFunCall(
            ref = code.nextRef(),
            span = span,
            block = code.currentBlock,
            name = "add",
            path = "",
            arguments = listOf(value.ref, itemRef),
        )

        span to itemRef
    }

    return value.ref
}

fun ParserCtx.processExpressionLambdaExpr(ctx: MainParser.LambdaExprContext): Ref {
    val params: MutableList<LstFunctionParam> = mutableListOf()
    var returnType: LstTypeUsage? = null
    var index = 0

    val prevCode = this.code
    val body = LstCode()
    this.code = body

    if (ctx.lambdaDef() != null) {
        val def = ctx.lambdaDef()

        if (def.lambdaReceiver() != null) {
            val receiverType = resolveTypeUsage(def.lambdaReceiver().typeUsage())

            params += LstFunctionParam(
                span = def.lambdaReceiver().span(),
                name = SELF_NAME,
                index = index++,
                typeUsage = receiverType,
            ).apply { createVariable(body) }
        }

        if (def.lambdaParams() != null) {
            def.lambdaParams().lambdaArgument().forEach {
                val typeUsage = if (it.typeUsage() != null) {
                    resolveTypeUsage(it.typeUsage())
                } else {
                    LstTypeUsage.unresolved(it.span(), "Lambda parameter type")
                }

                val name = if (it.anyName() != null) {
                    it.anyName().text
                } else if (it.UNDERSCORE() != null) {
                    "_"
                } else {
                    error("Grammar has been expanded and parser is outdated")
                }

                params += LstFunctionParam(
                    span = it.span(),
                    name = name,
                    index = index++,
                    typeUsage = typeUsage,
                ).apply { createVariable(body) }
            }
        }

        if (def.lambdaReturn() != null) {
            returnType = resolveTypeUsage(def.lambdaReturn().typeUsage())
        }
    }

    if (returnType == null) {
        returnType = LstTypeUsage.unresolved(ctx.span(), "Lambda return type")
    }

    ctx.statement().forEach { processStatement(it) }
    this.code = prevCode
    body.currentPath = currentPath(ctx)

    val lambda = LstLambdaFunction(
        ref = program.nextFunctionRef(),
        span = ctx.span(),
        params = params,
        returnTypeUsage = returnType,
        body = body,
    )

    program.lambdaFunctions += lambda

    val init = LstLambdaInit(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        lambda = lambda,
    )
    code.nodes += init

    body.parent = prevCode
    body.parentRef = init.ref
    body.parentBlock = init.block

    return init.ref
}

fun ParserCtx.processExpressionIfExpr(ctx: MainParser.IfExprContext): Ref {
    val cond = processExpression(ctx.expression())

    code.nodes += LstIfStart(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        cond = cond,
    )

    code.enterBlock(false)

    ctx.statementBlock(0).statement().map { processStatement(it) }
    val ifTrue = code.lastExpression ?: error("Code block has no last expression")

    // Restore prev block
    code.exitBlock()
    code.enterBlock(false)

    code.nodes += LstIfElse(
        ref = code.nextRef(),
        span = ctx.statementBlock(1).span(),
        block = code.currentBlock
    )

    ctx.statementBlock(1).statement().map { processStatement(it) }
    val ifFalse = code.lastExpression ?: error("Code block has no last expression")

    // Restore prev block
    code.exitBlock()

    code.nodes += LstIfEnd(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock
    )

    val choose = LstIfChoose(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        cond = cond,
        ifTrue = ifTrue,
        ifFalse = ifFalse,
    )

    code.nodes += choose
    return choose.ref
}

fun ParserCtx.processExpressionStructInstanceExpr(ctx: MainParser.StructInstanceExprContext): Ref {
    val name = ctx.upperName().text
    var path = ""
    if (ctx.modulePath() != null) {
        path = ctx.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
    }

    var typeParamArgs = listOf<LstTypeUsage>()

    if (ctx.typeParamArg() != null) {
        typeParamArgs = ctx.typeParamArg().typeUsage().map { resolveTypeUsage(it) }
    }

    val alloc = LstAlloc(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        typeUsage = LstTypeUsage(
            span = ctx.upperName().span(),
            name = name,
            path = path,
            sub = typeParamArgs,
            typeParameter = null,
            currentPath = currentPath(ctx)
        )
    )
    code.nodes += alloc

    ctx.structInstanceEntry().forEach { entry ->

        if (entry.expression() != null) {
            val expr = processExpression(entry.expression())

            code.nodes += LstStoreField(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                name = entry.anyName().text,
                instance = alloc.ref,
                expr = expr,
            )
        } else {
            val expr = processExpressionVariableExpr(entry.variableExpr())

            code.nodes += LstStoreField(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                name = entry.variableExpr().anyName().text,
                instance = alloc.ref,
                expr = expr,
            )
        }
    }

    return alloc.ref
}

fun ParserCtx.processExpressionVariableExpr(ctx: MainParser.VariableExprContext): Ref {
    val name = ctx.anyName().text
    var path = ""
    if (ctx.modulePath() != null) {
        path = ctx.modulePath().anyName().joinToString(MODULE_SEPARATOR) { it.text }
    }

    val node = LstLoadVar(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        name = name,
        path = path,
    )
    code.nodes += node
    return node.ref
}

fun ParserCtx.processExpressionSizeOf(ctx: MainParser.SizeOfExprContext): Ref {
    val node = LstSizeOf(
        ref = code.nextRef(),
        span = ctx.span(),
        block = code.currentBlock,
        typeUsage = resolveTypeUsage(ctx.typeUsage()),
    )
    code.nodes += node
    return node.ref
}

fun ParserCtx.processExpressionTemplateLiteral(ctx: MainParser.TemplateLiteralContext): Ref {
    val span = ctx.anyName().span()
    val arguments = mutableListOf<Ref>()

    val newList = code.call(
        span = span,
        path = "List",
        name = "new",
    )

    arguments += newList

    ctx.stringContents().forEach { item ->
        when {
            item.STRING_BLOB() != null -> {
                val str = code.string(span, unescapeStringLiteral(item.STRING_BLOB().text))
                code.call(
                    span = span,
                    path = "",
                    name = "add",
                    arguments = listOf(newList, str),
                )
            }

            item.STRING_ESCAPE() != null -> {
                val str = code.string(span, unescapeStringLiteral(item.STRING_ESCAPE().text))
                code.call(
                    span = span,
                    path = "",
                    name = "add",
                    arguments = listOf(newList, str),
                )
            }

            item.STRING_VAR() != null -> {
                val variable = code.loadVar(ctx.span(), "", item.STRING_VAR().text.substring(1))

                arguments += code.call(
                    span = ctx.span(),
                    path = "",
                    name = "to_string",
                    arguments = listOf(variable)
                )
            }

            item.STRING_INTERP_START() != null -> {
                val ref = processExpression(item.expression())

                arguments += code.call(
                    span = ctx.span(),
                    path = "",
                    name = "to_string",
                    arguments = listOf(ref)
                )
            }
        }
    }

    return code.call(
        span = span,
        path = currentPath(ctx),
        name = ctx.anyName().text,
        arguments = arguments,
        explicitTypeParams = listOf(),
    )
}

private fun stringToInt(text: String): Int? {
    val num = text.replace("_", "")
    return when {
        num.startsWith("0x") -> num.substring(2).toUIntOrNull(16)?.toInt()
        num.startsWith("0o") -> num.substring(2).toUIntOrNull(8)?.toInt()
        num.startsWith("0b") -> num.substring(2).toUIntOrNull(2)?.toInt()
        else -> num.toIntOrNull()
    }
}

private fun stringToLong(text: String): Long? {
    val num = text.trimEnd('l', 'L').replace("_", "")
    return when {
        num.startsWith("0x") -> num.substring(2).toULongOrNull(16)?.toLong()
        num.startsWith("0o") -> num.substring(2).toULongOrNull(8)?.toLong()
        num.startsWith("0b") -> num.substring(2).toULongOrNull(2)?.toLong()
        else -> num.toLongOrNull()
    }
}

private fun stringToFloat(text: String): Float? {
    return text.trimEnd('f', 'F', 'd', 'D').replace("_", "").toFloatOrNull()
}