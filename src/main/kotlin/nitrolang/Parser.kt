package nitrolang

import nitrolang.ast.*
import nitrolang.gen.MainLexer
import nitrolang.gen.MainParser
import nitrolang.gen.MainParser.ConstDefinitionContext
import nitrolang.gen.MainParser.DefinitionChoiceContext
import nitrolang.gen.MainParser.DefinitionContext
import nitrolang.gen.MainParser.ExpressionBaseContext
import nitrolang.gen.MainParser.ExpressionBinaryOpContext
import nitrolang.gen.MainParser.ExpressionContext
import nitrolang.gen.MainParser.ExpressionLiteralContext
import nitrolang.gen.MainParser.ExpressionOrFunctionCallContext
import nitrolang.gen.MainParser.ExpressionSimpleContext
import nitrolang.gen.MainParser.ExpressionWithSuffixContext
import nitrolang.gen.MainParser.FunctionCallEndContext
import nitrolang.gen.MainParser.FunctionCallParamsContext
import nitrolang.gen.MainParser.FunctionDefinitionContext
import nitrolang.gen.MainParser.IfExprContext
import nitrolang.gen.MainParser.IncludeDefinitionContext
import nitrolang.gen.MainParser.LambdaExprContext
import nitrolang.gen.MainParser.ListExprContext
import nitrolang.gen.MainParser.MapExprContext
import nitrolang.gen.MainParser.ModuleDefinitionContext
import nitrolang.gen.MainParser.OptionDefinitionContext
import nitrolang.gen.MainParser.SetExprContext
import nitrolang.gen.MainParser.SizeOfExprContext
import nitrolang.gen.MainParser.StatementBlockContext
import nitrolang.gen.MainParser.StatementContext
import nitrolang.gen.MainParser.StructDefinitionContext
import nitrolang.gen.MainParser.StructInstanceExprContext
import nitrolang.gen.MainParser.TypeParamDefContext
import nitrolang.gen.MainParser.TypeUsageContext
import nitrolang.gen.MainParser.UseDefinitionContext
import nitrolang.gen.MainParser.VariableExprContext
import nitrolang.gen.MainParserBaseListener
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import nitrolang.util.Span
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTreeWalker

private const val SELF_NAME = "this"

class AstParser(
    private val collector: ErrorCollector,
    private val source: SourceFile,
) : MainParserBaseListener() {
    private val program = LstProgram()
    private val typeParamMap = mutableMapOf<String, TypeParameter>()
    private val definedNames = mutableMapOf<Path, Span>()

    companion object {
        fun parseFile(source: SourceFile, collector: ErrorCollector): LstProgram? {
            // create a CharStream that reads from standard input
            val input = CharStreams.fromString(source.contents, source.path)
            // create a lexer that feeds off of input CharStream
            val lexer2 = MainLexer(input)
            // create a buffer of tokens pulled from the lexer
            val tokens2 = CommonTokenStream(lexer2)
            // create a parser that feeds off the tokens buffer
            val parser = MainParser(tokens2)

            parser.addErrorListener(object : BaseErrorListener() {
                override fun syntaxError(
                    recognizer: Recognizer<*, *>,
                    offendingSymbol: Any,
                    line: Int,
                    charPositionInLine: Int,
                    msg: String,
                    e: RecognitionException?
                ) {
                    recognizer as MainParser
                    collector.report(
                        "Syntax error: $msg", Span(
                            recognizer.currentToken.startIndex,
                            recognizer.currentToken.stopIndex,
                            source
                        )
                    )
                }
            })

            val fileCtx = parser.file()

            if (fileCtx.exception != null || collector.isNotEmpty()) {
                return null
            }

            val astParser = AstParser(collector, source)

            ParseTreeWalker().walk(astParser, fileCtx)

            astParser.program.functions.values.forEach {
                astParser.processCode(it.body)
            }
            astParser.program.consts.values.forEach {
                astParser.processCode(it.body)
            }

            return astParser.program
        }
    }

    override fun enterStructDefinition(ctx: StructDefinitionContext) {
        startTypeParams(ctx.typeParamDef())

        var index = 0
        val fields = ctx.structBody().structField().map { fieldCtx ->
            LstStructureField(
                span = fieldCtx.nameToken().span(),
                name = fieldCtx.nameToken().text,
                index = index++,
                typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
                ref = FieldRef.next()
            )
        }

        val typeParameters = endTypeParams()

        val struct = LstStruct(
            span = ctx.declaredNameToken().span(),
            name = ctx.declaredNameToken().text,
            path = currentPath(ctx),
            fields = fields.associateBy { it.ref },
            typeParameters = typeParameters,
            annotations = resolveAnnotations(ctx),
            ref = DeclRef.next(),
        )

        if (struct.fullName in definedNames) {
            val prev = definedNames[struct.fullName]
            collector.report("Redeclaration of ${struct.fullName}, previously defined at $prev", struct.span)
            return
        }

        definedNames[struct.fullName] = struct.span
        program.structs[struct.ref] = struct
    }

    override fun enterOptionDefinition(ctx: OptionDefinitionContext) {
        startTypeParams(ctx.typeParamDef())

        val mutableTypeParametersList = mutableListOf<TypeParameter>()
        val options = mutableMapOf<DeclRef, LstStruct>()

        ctx.optionDefinitionItem().forEach { opt ->

            var index = 0
            val fields = opt.structBody()?.structField()?.map { fieldCtx ->
                LstStructureField(
                    span = fieldCtx.nameToken().span(),
                    name = fieldCtx.nameToken().text,
                    index = index++,
                    typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
                    ref = FieldRef.next()
                )
            } ?: emptyList()

            var path = currentPath(ctx)

            if (path != "") {
                path += MODULE_SEPARATOR
            }

            val struct = LstStruct(
                span = opt.declaredNameToken().span(),
                name = opt.declaredNameToken().text,
                path = path + ctx.declaredNameToken().text,
                fields = fields.associateBy { it.ref },
                typeParameters = mutableTypeParametersList,
                annotations = emptyList(),
                ref = DeclRef.next(),
            )

            if (struct.fullName in definedNames) {
                val prev = definedNames[struct.fullName]
                collector.report("Redeclaration of ${struct.fullName}, previously defined at $prev", struct.span)
                return
            }

            definedNames[struct.fullName] = struct.span
            program.structs[struct.ref] = struct
            options[struct.ref] = struct
        }

        // Type parameters are shared between the option's items and the option type
        mutableTypeParametersList.addAll(endTypeParams())

        val option = LstOption(
            span = ctx.declaredNameToken().span(),
            name = ctx.declaredNameToken().text,
            path = currentPath(ctx),
            items = options,
            typeParameters = mutableTypeParametersList,
            annotations = resolveAnnotations(ctx),
            ref = DeclRef.next()
        )

        if (option.fullName in definedNames) {
            val prev = definedNames[option.fullName]
            collector.report("Redeclaration of ${option.fullName}, previously defined at $prev", option.span)
            return
        }

        definedNames[option.fullName] = option.span
        program.options[option.ref] = option
    }

    override fun enterFunctionDefinition(ctx: FunctionDefinitionContext) {
        startTypeParams(ctx.typeParamDef())

        val params = mutableListOf<LstFunctionParam>()
        val body = LstCode()
        var hasReceiver = false
        var index = 0

        if (ctx.functionReceiver() != null) {
            val param = LstFunctionParam(
                span = ctx.functionReceiver().typeUsage().span(),
                name = SELF_NAME,
                index = index++,
                typeUsage = resolveTypeUsage(ctx.functionReceiver().typeUsage()),
            )
            params += param
            hasReceiver = true

            val variable = LstVar(
                span = param.span,
                name = param.name,
                block = body.currentBlock,
                typeUsage = param.typeUsage,
                validAfter = body.currentRef(),
                ref = body.nextVarRef()
            )

            body.variables[variable.ref] = variable
        }

        val returnTypeUsage: TypeUsage = if (ctx.functionReturnType() != null) {
            resolveTypeUsage(ctx.functionReturnType().typeUsage())
        } else {
            TypeUsage.unit()
        }

        ctx.functionParameter().forEach { rawParam ->
            val param = LstFunctionParam(
                span = rawParam.nameToken().span(),
                name = rawParam.nameToken().text,
                index = index++,
                typeUsage = resolveTypeUsage(rawParam.typeUsage()),
            )
            params += param

            val variable = LstVar(
                span = param.span,
                name = param.name,
                block = body.currentBlock,
                typeUsage = param.typeUsage,
                validAfter = body.currentRef(),
                ref = body.nextVarRef()
            )

            body.variables[variable.ref] = variable
        }

        val typeParameters = endTypeParams()

        when {
            ctx.functionBody().statementBlock() != null -> {
                processStatementBlock(ctx.functionBody().statementBlock(), body)
            }

            ctx.functionBody().expression() != null -> {
                processExpression(ctx.functionBody().expression(), body)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }

        typeParamMap.clear()

        val func = LstFunction(
            span = ctx.declaredNameToken().span(),
            name = ctx.declaredNameToken().text,
            path = currentPath(ctx),
            hasReceiver = hasReceiver,
            params = params,
            returnTypeUsage = returnTypeUsage,
            typeParameters = typeParameters,
            body = body,
            annotations = resolveAnnotations(ctx),
            ref = FunRef.next()
        )

        program.functions[func.ref] = func
    }

    override fun enterConstDefinition(ctx: ConstDefinitionContext) {
        val body = LstCode()
        processExpression(ctx.expression(), body)

        val const = LstConst(
            span = ctx.declaredNameToken().span(),
            name = ctx.declaredNameToken().nameToken().text,
            path = currentPath(ctx),
            typeUsage = resolveTypeUsage(ctx.typeUsage()),
            body = body,
            annotations = resolveAnnotations(ctx),
            ref = ConstRef.next(),
        )

        if (const.fullName in definedNames) {
            val prev = definedNames[const.fullName]
            collector.report("Redeclaration of ${const.fullName}, previously defined at $prev", const.span)
            return
        }

        definedNames[const.fullName] = const.span
        program.consts[const.ref] = const
    }

    private fun resolveAnnotations(ctx: ParserRuleContext): List<LstAnnotation> {
        val definitionContext = (ctx.parent as DefinitionChoiceContext).parent as DefinitionContext

        return definitionContext.annotation().map {
            LstAnnotation(
                span = it.nameToken().span(),
                name = it.nameToken().text
            )
        }
    }

    override fun enterIncludeDefinition(ctx: IncludeDefinitionContext) {
//        val location = ctx.location()
//        val namespace = if (location.declaredNameToken() != null) location.declaredNameToken().text else ""
//        val path = location.nameToken().joinToString("/") { it.text }

        TODO("Includes")
    }

    override fun enterUseDefinition(ctx: UseDefinitionContext) {
        TODO("Use-declarations")
//        val path = ctx.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.IDENTIFIER().text }
//
//        when {
//            ctx.useDefinitionConst() != null -> {
//                val def = ctx.useDefinitionConst()
//
//                root.constImports += CstConstImport(
//                    name = def.declaredNameToken().text,
//                    path = path,
//                )
//            }
//
//            ctx.useDefinitionType() != null -> {
//                val def = ctx.useDefinitionType()
//
//                val name = def.declaredNameToken().text
//                val subPath = if (def.modulePath() != null) {
//                    def.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
//                } else {
//                    ""
//                }
//
//                root.typeImports += CstTypeImport(
//                    name = name,
//                    path = createPath(path, subPath),
//                )
//            }
//
//            ctx.useDefinitionFunction() != null -> {
//                val def = ctx.useDefinitionFunction()
//
//                root.functionImports += CstFunctionImport(
//                    name = def.declaredNameToken().text,
//                    path = path,
//                    receiver = CstTypeUsage.unit().also { root.typeUsages += it },
//                )
//            }
//
//            ctx.useDefinitionExtension() != null -> {
//                val def = ctx.useDefinitionExtension()
//
//                root.functionImports += CstFunctionImport(
//                    name = def.declaredNameToken().text,
//                    path = path,
//                    receiver = resolveTypeUsage(def.typeUsage()),
//                )
//            }
//        }
    }

    private fun startTypeParams(ctx: TypeParamDefContext?) {
        typeParamMap.clear()

        ctx?.typeParameter()?.forEach {
            val td = TypeParameter(
                span = it.nameToken().span(),
                name = it.nameToken().text,
                ref = TypeRef.next()
            )

            if (td.name in typeParamMap) {
                val prev = typeParamMap[td.name]
                collector.report(
                    "Name conflict, type parameter name ${td.name} is already in use at ${prev?.span}",
                    td.span
                )
                return
            }
            typeParamMap[td.name] = td
        }
    }

    private fun endTypeParams(): List<TypeParameter> {
        return typeParamMap.values.toList()
    }

    private fun processExpression(ctx: ExpressionContext, code: LstCode): Ref {
        val complex = ctx.expressionComplex()

        return when {
            complex.ifExpr() != null -> {
                processExpressionIfExpr(complex.ifExpr(), code)
            }

            complex.notExpr() != null -> {
                val expr = processExpressionBase(complex.notExpr().expressionBase(), code)

                val arg1 = LstFunArg(
                    ref = code.nextRef(),
                    span = complex.span(),
                    block = code.currentBlock,
                    expr = expr
                )
                code.nodes += arg1

                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = complex.notExpr().span(),
                    block = code.currentBlock,
                    name = "logic_not",
                    path = "core",
                    argCount = 1,
                )
                code.nodes += call
                call.ref
            }

            complex.returnExpr() != null -> {
                val expr = processExpression(complex.returnExpr().expression(), code)
                val node = LstReturn(
                    ref = code.nextRef(),
                    span = complex.returnExpr().span(),
                    block = code.currentBlock,
                    expr = expr
                )
                code.nodes += node

                node.ref
            }

            complex.expressionBinaryOp() != null -> {
                processExpressionBinOp(complex.expressionBinaryOp(), code)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun processExpressionSimple(simple: ExpressionSimpleContext, code: LstCode): Ref {
        return when {
            simple.AS() != null -> {
                val typeUsage = resolveTypeUsage(simple.typeUsage())
                val expr = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)

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
                val typeUsage = resolveTypeUsage(simple.typeUsage())
                val expr = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)

                val node = LstIsType(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    expr = expr,
                    typeUsage = typeUsage,
                )
                code.nodes += node
                node.ref
            }

            simple.NOT_IS() != null -> {
                val typeUsage = resolveTypeUsage(simple.typeUsage())
                val expr = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)

                val node = LstIsType(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    expr = expr,
                    typeUsage = typeUsage,
                )
                code.nodes += node

                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    expr = node.ref
                )
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "logic_not",
                    path = "",
                    argCount = 1
                )
                code.nodes += call
                call.ref
            }

            simple.IN() != null -> {
                val value = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)
                val collection = processExpressionWithSuffix(simple.expressionWithSuffix(1), code)

                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = simple.expressionWithSuffix(0).span(),
                    block = code.currentBlock,
                    expr = value
                )
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = simple.expressionWithSuffix(1).span(),
                    block = code.currentBlock,
                    expr = collection
                )
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "contains",
                    path = "",
                    argCount = 1
                )
                code.nodes += call
                call.ref
            }

            simple.NOT_IN() != null -> {
                val value = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)
                val collection = processExpressionWithSuffix(simple.expressionWithSuffix(1), code)

                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = simple.expressionWithSuffix(0).span(),
                    block = code.currentBlock,
                    expr = value
                )
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = simple.expressionWithSuffix(1).span(),
                    block = code.currentBlock,
                    expr = collection
                )
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "contains",
                    path = "",
                    argCount = 1
                )
                code.nodes += call

                // not
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    expr = call.ref
                )
                val call2 = LstFunCall(
                    ref = code.nextRef(),
                    span = simple.span(),
                    block = code.currentBlock,
                    name = "logic_not",
                    path = "",
                    argCount = 1
                )
                code.nodes += call2
                call2.ref
            }

            else -> {
                processExpressionWithSuffix(simple.expressionWithSuffix(0), code)
            }
        }
    }

    private fun processExpressionWithSuffix(ctx: ExpressionWithSuffixContext, code: LstCode): Ref {
        return when {
            ctx.collectionIndexingSuffix() != null -> {
                val prevRef = processExpressionWithSuffix(ctx.expressionWithSuffix(), code)
                val indexRef = processExpression(ctx.collectionIndexingSuffix().expression(), code)

                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = ctx.expressionWithSuffix().span(),
                    block = code.currentBlock,
                    expr = prevRef
                )
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = ctx.collectionIndexingSuffix().span(),
                    block = code.currentBlock,
                    expr = indexRef
                )
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = ctx.collectionIndexingSuffix().span(),
                    block = code.currentBlock,
                    name = "get",
                    path = "",
                    argCount = 2
                )
                code.nodes += call
                call.ref
            }

            ctx.structFieldAccessSuffix() != null -> {
                val prevRef = processExpressionWithSuffix(ctx.expressionWithSuffix(), code)
                val name = ctx.structFieldAccessSuffix().nameToken().text

                val node = LstLoadField(
                    ref = code.nextRef(),
                    span = ctx.structFieldAccessSuffix().nameToken().span(),
                    block = code.currentBlock,
                    expr = prevRef,
                    name = name,
                )
                code.nodes += node
                node.ref
            }

            ctx.nameToken() != null -> {
                val prev = processExpressionWithSuffix(ctx.expressionWithSuffix(), code)
                val name = ctx.nameToken().text

                processFunctionCall(prev, "", name, ctx.functionCallParams(), ctx.functionCallEnd(), code)
            }

            ctx.expressionOrFunctionCall() != null -> {
                processExpressionOrFunctionCall(ctx.expressionOrFunctionCall(), code)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun processExpressionOrFunctionCall(ctx: ExpressionOrFunctionCallContext, code: LstCode): Ref {
        return when {
            ctx.nameToken() != null -> {
                val name = ctx.nameToken().text
                var path = ""
                if (ctx.modulePath() != null) {
                    path = ctx.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
                }

                processFunctionCall(null, path, name, ctx.functionCallParams(), ctx.functionCallEnd(), code)
            }

            ctx.parenthesizedExpression() != null -> {
                val prev = processExpression(ctx.parenthesizedExpression().expression(), code)

                processFunctionCall(prev, "", "invoke", ctx.functionCallParams(), ctx.functionCallEnd(), code)
            }

            ctx.expressionLiteral() != null -> {
                val prev = processExpressionExpressionLiteral(ctx.expressionLiteral(), code)

                processFunctionCall(prev, "", "invoke", ctx.functionCallParams(), ctx.functionCallEnd(), code)
            }

            ctx.structInstanceExpr() != null -> {
                val prev = processExpressionStructInstanceExpr(ctx.structInstanceExpr(), code)

                processFunctionCall(prev, "", "invoke", ctx.functionCallParams(), ctx.functionCallEnd(), code)
            }

            ctx.sizeOfExpr() != null -> {
                val prev = processExpressionSizeOf(ctx.sizeOfExpr(), code)

                processFunctionCall(prev, "", "invoke", ctx.functionCallParams(), ctx.functionCallEnd(), code)
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
                processFunctionCall(load.ref, "", "invoke", ctx.functionCallParams(), ctx.functionCallEnd(), code)
            }

            ctx.expressionBase() != null -> {
                processExpressionBase(ctx.expressionBase(), code)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun processFunctionCall(
        receiver: Ref?,
        path: String,
        name: String,
        params: FunctionCallParamsContext?,
        end: FunctionCallEndContext?,
        code: LstCode
    ): Ref {
        val span = params?.span() ?: end?.span() ?: error("Either CallParams or CallEnd is required")
        val args = mutableListOf<Ref>()

        if (receiver != null) {
            args += receiver
        }

        params?.functionCallParam()?.forEach {
            args += processExpression(it.expression(), code)
        }

        if (end != null) {
            when {
                end.lambdaExpr() != null -> {
                    args += processExpressionLambdaExpr(end.lambdaExpr(), code)
                }

                end.listExpr() != null -> {
                    args += processExpressionListExpr(end.listExpr(), code)
                }

                end.mapExpr() != null -> {
                    args += processExpressionMapExpr(end.mapExpr(), code)
                }

                end.setExpr() != null -> {
                    args += processExpressionSetExpr(end.setExpr(), code)
                }

                else -> error("Grammar has been expanded and parser is outdated")
            }
        }


        args.forEach {
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = span,
                block = code.currentBlock,
                expr = it,
            )
        }

        val call = LstFunCall(
            ref = code.nextRef(),
            span = span,
            block = code.currentBlock,
            name = name,
            path = path,
            argCount = args.size,
        )
        code.nodes += call
        return call.ref
    }

    private fun processExpressionBase(ctx: ExpressionBaseContext, code: LstCode): Ref {
        return when {
            ctx.parenthesizedExpression() != null -> {
                processExpression(ctx.parenthesizedExpression().expression(), code)
            }

            ctx.unitExpression() != null -> {
                val node = LstUnit(
                    ref = code.nextRef(),
                    span = ctx.unitExpression().span(),
                    block = code.currentBlock,
                )
                code.nodes += node
                node.ref
            }

            ctx.expressionLiteral() != null -> {
                processExpressionExpressionLiteral(ctx.expressionLiteral(), code)
            }

            ctx.listExpr() != null -> {
                processExpressionListExpr(ctx.listExpr(), code)
            }

            ctx.mapExpr() != null -> {
                processExpressionMapExpr(ctx.mapExpr(), code)
            }

            ctx.setExpr() != null -> {
                processExpressionSetExpr(ctx.setExpr(), code)
            }

            ctx.lambdaExpr() != null -> {
                processExpressionLambdaExpr(ctx.lambdaExpr(), code)
            }

            ctx.json_value() != null -> {
                error("Json value not currently implemented")
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
                val node = LstJumpTo(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    backwards = false,
                    role = "break",
                )
                code.breakNodes += node
                code.nodes += node
                node.ref
            }

            ctx.CONTINUE() != null -> {
                val node = LstJumpTo(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    backwards = true,
                    role = "continue",
                )
                code.continueNodes += node
                code.nodes += node
                node.ref
            }

            ctx.structInstanceExpr() != null -> {
                processExpressionStructInstanceExpr(ctx.structInstanceExpr(), code)
            }

            ctx.variableExpr() != null -> {
                processExpressionVariableExpr(ctx.variableExpr(), code)
            }

            ctx.sizeOfExpr() != null -> {
                processExpressionSizeOf(ctx.sizeOfExpr(), code)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun processExpressionExpressionLiteral(ctx: ExpressionLiteralContext, code: LstCode): Ref {
        return when {
            ctx.INT_NUMBER() != null -> {
                val text = ctx.INT_NUMBER().text

                val intValue = when {
                    text.startsWith("0x") -> text.substring(2).toUInt(16).toInt()
                    text.startsWith("0o") -> text.substring(2).toUInt(8).toInt()
                    text.startsWith("0b") -> text.substring(2).toUInt(2).toInt()
                    else -> text.toInt()
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

            ctx.FLOAT_NUMBER() != null -> {
                val text = ctx.FLOAT_NUMBER().text
                val floatValue = text.toFloatOrNull()

                if (floatValue == null) {
                    collector.report("Invalid float value '${text}'", ctx.span())
                }

                val node = LstFloat(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    value = floatValue ?: 0f
                )
                code.nodes += node
                node.ref
            }

            ctx.STRING() != null -> {
                val value = unescapeStringLiteral(ctx.STRING().text)

                val node = LstString(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    value = value
                )
                code.nodes += node
                node.ref
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

                val node = LstUnit(
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

    private fun unescapeStringLiteral(tokenText: String): String {
        // "a" -> a
        var text = tokenText.substring(1, tokenText.length - 1)

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

    private fun processExpressionListExpr(ctx: ListExprContext, code: LstCode): Ref {
        val value = LstAlloc(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            typeUsage = TypeUsage.list(TypeUsage.unit())
        )
        code.nodes += value

        ctx.listEntry().forEach { item ->
            val itemRef = processExpression(item.expression(), code)

            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = item.expression().span(),
                block = code.currentBlock,
                expr = value.ref
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = item.expression().span(),
                block = code.currentBlock,
                expr = itemRef
            )
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = item.expression().span(),
                block = code.currentBlock,
                name = "add",
                path = "core::list",
                argCount = 2
            )
        }

        return value.ref
    }

    private fun processExpressionMapExpr(ctx: MapExprContext, code: LstCode): Ref {
        val value = LstAlloc(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            typeUsage = TypeUsage.list(TypeUsage.unit())
        )

        ctx.mapEntry().forEach { entry ->
            val keyCtx = entry.mapKey()
            val keyRef = when {
                keyCtx.nameToken() != null -> {
                    val node = LstString(
                        ref = code.nextRef(),
                        span = ctx.span(),
                        block = code.currentBlock,
                        value = keyCtx.nameToken().text
                    )
                    code.nodes += node
                    node.ref
                }

                keyCtx.STRING() != null -> {
                    val node = LstString(
                        ref = code.nextRef(),
                        span = ctx.span(),
                        block = code.currentBlock,
                        value = unescapeStringLiteral(keyCtx.STRING().text)
                    )
                    code.nodes += node
                    node.ref
                }

                keyCtx.expression() != null -> {
                    processExpression(keyCtx.expression(), code)
                }

                else -> error("Grammar has been expanded and parser is outdated")
            }

            val valueRef = processExpression(entry.expression(), code)

            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = keyCtx.span(),
                block = code.currentBlock,
                expr = value.ref
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = keyCtx.span(),
                block = code.currentBlock,
                expr = keyRef
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = keyCtx.span(),
                block = code.currentBlock,
                expr = valueRef
            )
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = keyCtx.span(),
                block = code.currentBlock,
                name = "set",
                path = "core::map",
                argCount = 3
            )
        }

        return value.ref
    }

    private fun processExpressionSetExpr(ctx: SetExprContext, code: LstCode): Ref {
        val value = LstAlloc(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            typeUsage = TypeUsage.list(TypeUsage.unit())
        )

        ctx.listEntry().forEach { item ->
            val itemRef = processExpression(item.expression(), code)

            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = item.expression().span(),
                block = code.currentBlock,
                expr = value.ref
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = item.expression().span(),
                block = code.currentBlock,
                expr = itemRef
            )
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = item.expression().span(),
                block = code.currentBlock,
                name = "add",
                path = "core::set",
                argCount = 2
            )
        }

        return value.ref
    }

    private fun processExpressionLambdaExpr(ctx: LambdaExprContext, code: LstCode): Ref {
        val params: MutableList<LstFunctionParam> = mutableListOf()
        var returnType: TypeUsage = TypeUsage.unit()
        var index = 0

        if (ctx.lambdaDef() != null) {
            val def = ctx.lambdaDef()

            if (def.lambdaReceiver() != null) {
                val receiver = resolveTypeUsage(def.lambdaReceiver().typeUsage())

                params += LstFunctionParam(
                    span = def.lambdaReceiver().span(),
                    name = SELF_NAME,
                    index = index++,
                    typeUsage = receiver,
                )
            }

            if (def.lambdaParams() != null) {
                def.lambdaParams().lambdaArgument().forEach {
                    params += LstFunctionParam(
                        span = it.nameToken().span(),
                        name = it.nameToken().text,
                        index = index++,
                        typeUsage = resolveTypeUsage(it.typeUsage()),
                    )
                }
            }

            if (def.lambdaReturn() != null) {
                returnType = resolveTypeUsage(def.lambdaReturn().typeUsage())
            }
        }

        val body = LstCode()

        ctx.statement().forEach { processStatement(it, body) }

        val lambda = LstLambdaFunction(
            ref = FunRef.next(),
            span = ctx.span(),
            params = params,
            returnTypeUsage = returnType,
            body = body
        )

        val node = LstAlloc(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            typeUsage = TypeUsage.lambda(lambda)
        )
        code.nodes += node
        return node.ref
    }

    private fun processExpressionIfExpr(ctx: IfExprContext, code: LstCode): Ref {
        val cond = processExpression(ctx.expression(), code)

        val jump = LstIfJump(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = cond,
            middle = null,
            end = null,
        )
        code.nodes += jump

        processStatementBlock(ctx.statementBlock(0), code)
        val ifTrueRef = code.nodes.last().ref

        val jump2 = LstJumpTo(
            ref = code.nextRef(),
            span = ctx.statementBlock(1).span(),
            block = code.currentBlock,
            backwards = false,
            role = "if_jump_to_end",
        )
        code.nodes += jump2

        val middle = LstMarker(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            role = "if_middle"
        )
        code.nodes += middle
        jump.middle = middle.ref

        processStatementBlock(ctx.statementBlock(1), code)
        val ifFalseRef = code.nodes.last().ref

        val end = LstMarker(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            role = "if_end"
        )

        code.nodes += end
        jump.end = end.ref
        jump2.marker = end.ref

        val choose = LstChoose(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = cond,
            ifTrue = ifTrueRef,
            ifFalse = ifFalseRef,
        )

        code.nodes += choose
        return choose.ref
    }

    private fun processExpressionStructInstanceExpr(ctx: StructInstanceExprContext, code: LstCode): Ref {
        val name = ctx.nameToken().text
        var path = ""
        if (ctx.modulePath() != null) {
            path = ctx.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
        }

        var typeParamArgs = listOf<TypeUsage>()

        if (ctx.typeParamArg() != null) {
            typeParamArgs = ctx.typeParamArg().typeUsage().map { resolveTypeUsage(it) }
        }

        val alloc = LstAlloc(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            typeUsage = TypeUsage(
                span = ctx.nameToken().span(),
                name = name,
                path = path,
                sub = typeParamArgs,
                modifier = TypeUsage.Modifier.NONE,
                typeParameterRef = null,
                currentPath = currentPath(ctx)
            )
        )
        code.nodes += alloc

        ctx.structInstanceEntry().forEach { entry ->

            val expr = if (entry.expression() != null) {
                processExpression(entry.expression(), code)
            } else {
                val node = LstString(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    value = entry.nameToken().text
                )
                code.nodes += node
                node.ref
            }

            code.nodes += LstStoreField(
                ref = code.nextRef(),
                span = ctx.span(),
                block = code.currentBlock,
                name = entry.nameToken().text,
                instance = alloc.ref,
                expr = expr,
            )
        }

        return alloc.ref
    }

    private fun processExpressionVariableExpr(ctx: VariableExprContext, code: LstCode): Ref {
        val name = ctx.nameToken().text
        var path = ""
        if (ctx.modulePath() != null) {
            path = ctx.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
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

    private fun processExpressionSizeOf(ctx: SizeOfExprContext, code: LstCode): Ref {
        val node = LstSizeOf(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            typeUsage = resolveTypeUsage(ctx.typeUsage()),
        )
        code.nodes += node
        return node.ref
    }

    private fun processExpressionBinOp(bin: ExpressionBinaryOpContext, code: LstCode): Ref {
        val exprs = bin.expressionSimple()
        val ops = bin.binaryOperator()

        return ExpressionTree.resolvePrecedence(exprs, ops, code) { processExpressionSimple(it, code) }
    }

    private fun processStatementBlock(ctx: StatementBlockContext, code: LstCode) {
        ctx.statement().forEach { stm -> processStatement(stm, code) }
    }

    private fun processStatement(ctx: StatementContext, code: LstCode) {
        val stm = ctx.statementChoice()

        when {
            stm.letStatement() != null -> {
                val subCtx = stm.letStatement()

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
                    val expr = processExpression(subCtx.expression(), code)
                    code.nodes += LstStoreVar(
                        ref = code.nextRef(),
                        span = subCtx.expression().span(),
                        block = code.currentBlock,
                        name = variable.name,
                        path = "",
                        expr = expr,
                        variable = variable.ref
                    )
                }
            }

            stm.ifStatement() != null -> {
                val subCtx = stm.ifStatement()

                val cond = processExpression(subCtx.expression(), code)

                val prevBlock = code.currentBlock
                code.currentBlock = LstNodeBlock(prevBlock)

                val jump = LstIfJump(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    cond = cond,
                    middle = null,
                    end = null,
                )
                code.nodes += jump

                subCtx.statementBlock(0).statement().map { processStatement(it, code) }

                var jump2: LstJumpTo? = null

                if (subCtx.statementBlock(1) != null) {
                    code.currentBlock = LstNodeBlock(prevBlock)

                    jump2 = LstJumpTo(
                        ref = code.nextRef(),
                        span = subCtx.statementBlock(1).span(),
                        block = code.currentBlock,
                        backwards = false,
                        role = "if_jump_to_end",
                    )
                    code.nodes += jump2
                }

                val middle = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "if_middle"
                )
                code.nodes += middle
                jump.middle = middle.ref

                if (subCtx.statementBlock(1) != null) {
                    subCtx.statementBlock(1).statement().map { processStatement(it, code) }
                    val end = LstMarker(
                        ref = code.nextRef(),
                        span = subCtx.span(),
                        block = code.currentBlock,
                        role = "if_end"
                    )

                    code.nodes += end
                    jump.end = end.ref
                    jump2?.marker = end.ref
                }

                // Restore prev block
                code.currentBlock = prevBlock
            }

            stm.loopStatement() != null -> {
                val subCtx = stm.loopStatement()
                val prevBreakNodes = code.breakNodes
                val prevContinueNodes = code.continueNodes

                code.breakNodes = mutableListOf()
                code.continueNodes = mutableListOf()

                val prevBlock = code.currentBlock
                code.currentBlock = LstNodeBlock(prevBlock)

                val start = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "loop_start"
                )
                code.nodes += start

                processStatementBlock(subCtx.statementBlock(), code)

                code.nodes += LstJumpTo(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    backwards = true,
                    marker = start.ref,
                    role = "loop_jump_to_beginning",
                )

                val end = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "loop_end"
                )
                code.nodes += end
                code.currentBlock = prevBlock

                // Link jumps
                code.breakNodes.forEach { it.marker = end.ref }
                code.continueNodes.forEach { it.marker = start.ref }

                code.breakNodes = prevBreakNodes
                code.continueNodes = prevContinueNodes
            }

            stm.whileStatement() != null -> {
                // Convierte un  while en loop+if
                // loop: {
                //   if (cond) {
                //     code...
                //     goto loop;
                //   }
                // }

                val subCtx = stm.whileStatement()

                val prevBreakNodes = code.breakNodes
                val prevContinueNodes = code.continueNodes

                code.breakNodes = mutableListOf()
                code.continueNodes = mutableListOf()

                val prevBlock = code.currentBlock
                code.currentBlock = LstNodeBlock(prevBlock)

                val start = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "while"
                )
                code.nodes += start

                val cond = processExpression(subCtx.expression(), code)

                val jump = LstIfJump(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    cond = cond
                )
                code.nodes += jump

                processStatementBlock(subCtx.statementBlock(), code)

                code.nodes += LstJumpTo(
                    code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    backwards = true,
                    marker = start.ref,
                    role = "while_jump_to_beginning",
                )

                val end = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "while"
                )
                code.nodes += end
                jump.middle = end.ref
                code.currentBlock = prevBlock

                // Link jumps
                code.breakNodes.forEach { it.marker = end.ref }
                code.continueNodes.forEach { it.marker = start.ref }

                code.breakNodes = prevBreakNodes
                code.continueNodes = prevContinueNodes
            }

            stm.forStatement() != null -> {
                val subCtx = stm.forStatement()

                val iterable = processExpression(subCtx.expression(), code)

                // iterable.to_iterator()
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = subCtx.expression().span(),
                    block = code.currentBlock,
                    expr = iterable,
                )
                val toIterator = LstFunCall(
                    ref = code.nextRef(),
                    span = subCtx.expression().span(),
                    block = code.currentBlock,
                    name = "to_iterator",
                    path = "",
                    argCount = 1,
                )
                code.nodes += toIterator

                val prevBreakNodes = code.breakNodes
                val prevContinueNodes = code.continueNodes

                code.breakNodes = mutableListOf()
                code.continueNodes = mutableListOf()

                val prevBlock = code.currentBlock
                code.currentBlock = LstNodeBlock(prevBlock)

                // loop {}
                val start = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "for_start"
                )
                code.nodes += start

                // aux.next()
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = subCtx.expression().span(),
                    block = code.currentBlock,
                    expr = toIterator.ref,
                )
                val iteratorNext = LstFunCall(
                    ref = code.nextRef(),
                    span = subCtx.expression().span(),
                    block = code.currentBlock,
                    name = "next",
                    path = "",
                    argCount = 1,
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
                    variable = variable.ref,
                )

                // i.is_none()
                code.nodes += LstFunArg(
                    ref = code.nextRef(),
                    span = subCtx.nameToken().span(),
                    block = code.currentBlock,
                    expr = iteratorNext.ref,
                )

                val isNoneCall = LstFunCall(
                    ref = code.nextRef(),
                    span = subCtx.nameToken().span(),
                    block = code.currentBlock,
                    name = "is_some",
                    path = "",
                    argCount = 1,
                )
                code.nodes += isNoneCall

                // if (i.is_some()) {

                val jump = LstIfJump(
                    ref = code.nextRef(),
                    span = subCtx.nameToken().span(),
                    block = code.currentBlock,
                    cond = isNoneCall.ref,
                )
                code.nodes += jump

                // Code...
                processStatementBlock(subCtx.statementBlock(), code)

                code.nodes += LstJumpTo(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    backwards = true,
                    marker = start.ref,
                    role = "for_jump_to_beginning",
                )

                // }
                val end = LstMarker(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    role = "for_end"
                )
                code.nodes += end
                jump.middle = end.ref
                code.currentBlock = prevBlock

                // Link jumps
                code.breakNodes.forEach { it.marker = end.ref }
                code.continueNodes.forEach { it.marker = start.ref }

                code.breakNodes = prevBreakNodes
                code.continueNodes = prevContinueNodes
            }

            stm.expressionStatement() != null -> {
                val subCtx = stm.expressionStatement()

                if (subCtx.assignableExpression() != null) {
                    val subSubCtx = subCtx.assignableExpression()

                    when {
                        subSubCtx.expression() != null && subSubCtx.nameToken() != null -> {
                            val receiver = processExpression(subSubCtx.expression(), code)
                            val value = processExpression(subCtx.expression(), code)

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
                            val receiver = processExpression(subSubCtx.expression(), code)
                            val index = processExpression(subSubCtx.collectionIndexingSuffix().expression(), code)
                            val value = processExpression(subCtx.expression(), code)

                            code.nodes += LstFunArg(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                expr = receiver,
                            )
                            code.nodes += LstFunArg(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                expr = index,
                            )
                            code.nodes += LstFunArg(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                expr = value,
                            )
                            code.nodes += LstFunCall(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                name = "set",
                                path = "",
                                argCount = 3,
                            )
                        }

                        subSubCtx.LBRACKET() != null -> {
                            val receiver = processExpression(subSubCtx.expression(), code)
                            val value = processExpression(subCtx.expression(), code)

                            code.nodes += LstFunArg(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                expr = receiver,
                            )
                            code.nodes += LstFunArg(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                expr = value,
                            )
                            code.nodes += LstFunCall(
                                ref = code.nextRef(),
                                span = subSubCtx.nameToken().span(),
                                block = code.currentBlock,
                                name = "add",
                                path = "",
                                argCount = 2,
                            )
                        }

                        subSubCtx.nameToken() != null -> {
                            val value = processExpression(subCtx.expression(), code)
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
                    processExpression(subCtx.expression(), code)
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

    private fun resolveTypeUsage(ctx: TypeUsageContext): TypeUsage {

        if (ctx.typeParameter() != null) {
            val name = ctx.typeParameter().nameToken().text
            val typeParameterRef = typeParamMap.getOrPut(name) {
                TypeParameter(
                    span = ctx.typeParameter().nameToken().span(),
                    name = name,
                    ref = TypeRef.next()
                )
            }

            return TypeUsage(
                span = ctx.typeParameter().nameToken().span(),
                name = name,
                path = "",
                sub = emptyList(),
                modifier = TypeUsage.Modifier.NONE,
                typeParameterRef = typeParameterRef,
                currentPath = currentPath(ctx)
            )
        }

        val base = ctx.baseTypeUsage()

        val modifier = if (base.refModifier() != null && base.refModifier().MUT() != null) {
            TypeUsage.Modifier.MUT
        } else if (base.refModifier() != null && base.refModifier().REF() != null) {
            TypeUsage.Modifier.REF
        } else {
            TypeUsage.Modifier.NONE
        }

        val name = base.nameToken().text
        var path = ""

        if (base.modulePath() != null) {
            path = base.modulePath().nameToken().joinToString(MODULE_SEPARATOR) { it.text }
        }

        val sub = if (base.typeParamArg() != null) {
            base.typeParamArg().typeUsage().map { resolveTypeUsage(it) }
        } else {
            listOf()
        }

        return TypeUsage(
            span = base.nameToken().span(),
            name = name,
            path = path,
            sub = sub,
            modifier = modifier,
            typeParameterRef = null,
            currentPath = currentPath(ctx)
        )
    }

    private fun processCode(code: LstCode) {
        fun <T> linkVariable(node: T) where T : LstNode, T : HasVarRef {
            var found: LstVar? = null
            var block: LstNodeBlock? = node.block

            while (block != null && found == null) {
                found = code.variables.values
                    .filter { it.block == block }
                    .sortedByDescending { it.validAfter.id }
                    .filter { it.validAfter.id <= node.ref.id }
                    .find { it.name == node.name }

                block = block.parent
            }

            if (found != null) {
                node.variable = found.ref
                return
            }

            val found2 = program.consts.values.find { it.name == node.name }

            if (found2 != null) {
                node.variable = found2.ref
                return
            }

            collector.report("Variable not found: ${node.name}", node.span)
        }

        code.nodes.forEach { node ->
            when (node) {
                is HasVarRef -> {
                    if (node.variable == null) {
                        linkVariable(node)
                    }
                }
            }
        }
    }

    private fun currentPath(ctx: ParserRuleContext): String {
        val pathComponents = mutableListOf<String>()
        var parent = ctx.getParent()

        while (parent != null) {
            if (parent is ModuleDefinitionContext) {
                pathComponents += parent.declaredNameToken().text

                parent.modulePath()?.nameToken()?.forEach { name ->
                    pathComponents += name.text
                }
            }
            parent = parent.getParent()
        }

        pathComponents.reverse()
        return pathComponents.joinToString(MODULE_SEPARATOR)
    }

    private fun ParserRuleContext.span(): Span {
        return Span(start.startIndex, stop.stopIndex, source)
    }
}