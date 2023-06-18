package nitrolang

import nitrolang.ast.*
import nitrolang.gen.MainLexer
import nitrolang.gen.MainParser
import nitrolang.gen.MainParser.ConstDefinitionContext
import nitrolang.gen.MainParser.ConstExprContext
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
import nitrolang.gen.MainParser.ParseFunctionDefinitionContext
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

const val SELF_NAME = "this"
const val EXTERN_NAME = "Extern"
const val WASM_INLINE_NAME = "WasmInline"
const val MAIN_FUNCTION_NAME = "main"

class AstParser(
    private val collector: ErrorCollector,
    private val source: SourceFile,
    private val program: LstProgram,
) : MainParserBaseListener() {
    private val typeParamMap = mutableMapOf<String, TypeParameter>()
    private var allowTypeParamCollection = false

    companion object {

        fun parseFile(source: SourceFile, collector: ErrorCollector, program: LstProgram): Boolean {
            val parser = createParser(source, collector)
            val fileCtx = parser.parseFile()

            if (collector.isNotEmpty()) {
                return false
            }

            if (fileCtx.exception != null) {
                collector.report("Parse exception: ${fileCtx.exception}", Span.internal())
                return false
            }

            val astParser = AstParser(collector, source, program)

            ParseTreeWalker().walk(astParser, fileCtx)

            astParser.program.consts.values.forEach { const ->
                const.type = astParser.typeUsageToTypeTree(const.typeUsage)
                const.body.returnType = const.type
            }
            astParser.program.structs.values.forEach { struct ->
                struct.fields.values.forEach { field ->
                    field.type = astParser.typeUsageToTypeTree(field.typeUsage)
                }
            }
            astParser.program.functions.values.forEach { func ->
                func.params.forEach { param -> param.type = astParser.typeUsageToTypeTree(param.typeUsage) }
                func.returnType = astParser.typeUsageToTypeTree(func.returnTypeUsage)
                func.body.returnType = func.returnType
            }

            astParser.program.functions.values.forEach { func ->
                // Extern functions have empty body
                if (func.isExternal) {
                    if (func.body.nodes.isNotEmpty()) {
                        collector.report("Extern function must have empty body", func.span)
                    }
                    return@forEach
                }

                astParser.processCode(func.body)

                // Check that the function actually returns something
                val defined = func.body.returnType!!
                if (!defined.isUnit()) {
                    when (val last = func.body.nodes.lastOrNull()) {
                        null -> {
                            collector.report(
                                "Function '${func.name}' must return '$defined' but has empty body",
                                func.span
                            )
                        }

                        // Ok, already checked in processCode()
                        is LstReturn -> Unit

                        is LstExpression -> {
                            if (!astParser.canBeAssignedTo(defined, last.type!!)) {
                                collector.report(
                                    "Type mismatch, attempt to return '${last.type}' on a function that must return '$defined'",
                                    last.span
                                )
                            }
                        }

                        else -> {
                            collector.report(
                                "Function '${func.name}' must return '$defined'",
                                func.span
                            )
                        }
                    }
                }
            }
            astParser.program.consts.values.forEach { const ->
                astParser.processCode(const.body)
            }

            return true
        }

        fun parseFunctionDefinition(source: SourceFile, collector: ErrorCollector, program: LstProgram): LstFunction? {
            val parser = createParser(source, collector)
            val funcCtx = parser.parseFunctionDefinition()

            if (funcCtx.exception != null || collector.isNotEmpty()) {
                return null
            }

            val astParser = AstParser(collector, source, program)

            ParseTreeWalker().walk(astParser, funcCtx)

            astParser.program.functions.values.forEach { func ->
                func.params.forEach { param -> param.type = astParser.typeUsageToTypeTree(param.typeUsage) }
                func.returnType = astParser.typeUsageToTypeTree(func.returnTypeUsage)
                func.body.returnType = func.returnType
            }

            return astParser.program.functions.values.first()
        }

        private fun createParser(source: SourceFile, collector: ErrorCollector): MainParser {
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

            return parser
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
                ref = program.nextFieldRef()
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
            ref = program.nextStructRef(),
        )

        if (struct.fullName in program.definedNames) {
            val prev = program.definedNames[struct.fullName]
            collector.report("Redeclaration of ${struct.fullName}, previously defined at $prev", struct.span)
            return
        }

        program.definedNames[struct.fullName] = struct.span
        program.structs[struct.ref] = struct
    }

    override fun enterOptionDefinition(ctx: OptionDefinitionContext) {
        startTypeParams(ctx.typeParamDef())

        val mutableTypeParametersList = mutableListOf<TypeParameter>()
        val options = mutableSetOf<StructRef>()

        ctx.optionDefinitionItem().forEach { opt ->

            var index = 0
            val fields = opt.structBody()?.structField()?.map { fieldCtx ->
                LstStructureField(
                    span = fieldCtx.nameToken().span(),
                    name = fieldCtx.nameToken().text,
                    index = index++,
                    typeUsage = resolveTypeUsage(fieldCtx.typeUsage()),
                    ref = program.nextFieldRef()
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
                ref = program.nextStructRef(),
            )

            if (struct.fullName in program.definedNames) {
                val prev = program.definedNames[struct.fullName]
                collector.report("Redeclaration of ${struct.fullName}, previously defined at $prev", struct.span)
                return
            }

            program.definedNames[struct.fullName] = struct.span
            program.structs[struct.ref] = struct
            options += struct.ref
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
            ref = program.nextOptionRef()
        )

        if (option.fullName in program.definedNames) {
            val prev = program.definedNames[option.fullName]
            collector.report("Redeclaration of ${option.fullName}, previously defined at $prev", option.span)
            return
        }

        program.definedNames[option.fullName] = option.span
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

        val annotations = resolveAnnotations(ctx)
        val func = LstFunction(
            span = ctx.declaredNameToken().span(),
            name = ctx.declaredNameToken().text,
            path = currentPath(ctx),
            hasReceiver = hasReceiver,
            params = params,
            returnTypeUsage = returnTypeUsage,
            typeParameters = typeParameters,
            body = body,
            annotations = annotations,
            ref = program.nextFunctionRef()
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
            ref = program.nextConstRef(),
        )

        if (const.fullName in program.definedNames) {
            val prev = program.definedNames[const.fullName]
            collector.report("Redeclaration of ${const.fullName}, previously defined at $prev", const.span)
            return
        }

        program.definedNames[const.fullName] = const.span
        program.consts[const.ref] = const
    }

    private fun resolveAnnotations(ctx: ParserRuleContext): List<LstAnnotation> {

        val annotations = when (ctx.parent) {
            is ParseFunctionDefinitionContext -> {
                (ctx.parent as ParseFunctionDefinitionContext).annotation()
            }

            is DefinitionChoiceContext -> {
                ((ctx.parent as DefinitionChoiceContext).parent as DefinitionContext).annotation()
            }

            else -> {
                error("Unknown context to extract annotations: $ctx, parent: ${ctx.parent}")
            }
        }

        return annotations.map { subCtx ->
            val args = mutableMapOf<String, LstConstValue>()

            if (subCtx.annotationArgs() != null) {
                subCtx.annotationArgs().annotationArgEntry().forEach { entry ->
                    val name = when {
                        entry.annotationArgKey().STRING() != null -> {
                            unescapeStringLiteral(entry.annotationArgKey().STRING().text)
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
        allowTypeParamCollection = true

        ctx?.typeParameter()?.forEach {
            val td = TypeParameter(
                span = it.nameToken().span(),
                name = it.nameToken().text,
                ref = program.nextTypeParamRef()
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
        allowTypeParamCollection = false
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
                    name = "logical_not",
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
                    name = "logical_not",
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
                    name = "logical_not",
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
                    instance = prevRef,
                    name = name,
                )
                code.nodes += node
                node.ref
            }

            ctx.nameToken() != null -> {
                val prev = processExpressionWithSuffix(ctx.expressionWithSuffix(), code)
                val name = ctx.nameToken().text

                processFunctionCall(
                    span = ctx.nameToken().span(),
                    receiver = prev,
                    path = "",
                    name = name,
                    params = ctx.functionCallParams(),
                    end = ctx.functionCallEnd(),
                    code = code
                )
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

                processFunctionCall(
                    span = ctx.nameToken().span(),
                    receiver = null,
                    path = path,
                    name = name,
                    params = ctx.functionCallParams(),
                    end = ctx.functionCallEnd(),
                    code = code
                )
            }

            ctx.parenthesizedExpression() != null -> {
                val prev = processExpression(ctx.parenthesizedExpression().expression(), code)

                processFunctionCall(
                    span = ctx.parenthesizedExpression().expression().span(),
                    receiver = prev,
                    path = "",
                    name = "invoke",
                    params = ctx.functionCallParams(),
                    end = ctx.functionCallEnd(),
                    code = code
                )
            }

            ctx.expressionLiteral() != null -> {
                val prev = processExpressionExpressionLiteral(ctx.expressionLiteral(), code)

                processFunctionCall(
                    span = ctx.expressionLiteral().span(),
                    receiver = prev,
                    path = "",
                    name = "invoke",
                    params = ctx.functionCallParams(),
                    end = ctx.functionCallEnd(),
                    code = code
                )
            }

            ctx.structInstanceExpr() != null -> {
                val prev = processExpressionStructInstanceExpr(ctx.structInstanceExpr(), code)

                processFunctionCall(
                    span = ctx.structInstanceExpr().nameToken().span(),
                    receiver = prev,
                    path = "",
                    name = "invoke",
                    params = ctx.functionCallParams(),
                    end = ctx.functionCallEnd(),
                    code = code
                )
            }

            ctx.sizeOfExpr() != null -> {
                val prev = processExpressionSizeOf(ctx.sizeOfExpr(), code)

                processFunctionCall(
                    span = ctx.sizeOfExpr().span(),
                    receiver = prev,
                    path = "",
                    name = "invoke",
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
                    name = "invoke",
                    params = ctx.functionCallParams(),
                    end = ctx.functionCallEnd(),
                    code = code
                )
            }

            ctx.expressionBase() != null -> {
                processExpressionBase(ctx.expressionBase(), code)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun processFunctionCall(
        span: Span,
        receiver: Ref?,
        path: String,
        name: String,
        params: FunctionCallParamsContext?,
        end: FunctionCallEndContext?,
        code: LstCode
    ): Ref {
        val args = mutableListOf<Pair<Ref, Span>>()
        val specifiedTypeParams = mutableListOf<TypeUsage>()

        if (receiver != null) {
            args += receiver to span
        }

        params?.typeParamArg()?.typeUsage()?.forEach { param ->
            specifiedTypeParams += resolveTypeUsage(param)
        }

        params?.functionCallParam()?.forEach { param ->
            args += processExpression(param.expression(), code) to param.span()
        }

        if (end != null) {
            when {
                end.lambdaExpr() != null -> {
                    args += processExpressionLambdaExpr(end.lambdaExpr(), code) to end.span()
                }

                end.listExpr() != null -> {
                    args += processExpressionListExpr(end.listExpr(), code) to end.span()
                }

                end.mapExpr() != null -> {
                    args += processExpressionMapExpr(end.mapExpr(), code) to end.span()
                }

                end.setExpr() != null -> {
                    args += processExpressionSetExpr(end.setExpr(), code) to end.span()
                }

                else -> error("Grammar has been expanded and parser is outdated")
            }
        }

        args.forEach { (argExpr, argSpan) ->
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = argSpan,
                block = code.currentBlock,
                expr = argExpr,
            )
        }

        val call = LstFunCall(
            ref = code.nextRef(),
            span = span,
            block = code.currentBlock,
            name = name,
            path = path,
            argCount = args.size,
            specifiedTypeParams = specifiedTypeParams,
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

            ctx.jsonExpr() != null -> {
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

    private fun processConstExpr(ctx: ConstExprContext): LstConstValue {
        if (ctx.unitExpression() != null) {
            return LstConstUnit
        }

        if (ctx.expressionLiteral() == null) {
            error("Grammar has been expanded and parser is outdated")
        }

        return when {
            ctx.expressionLiteral().STRING() != null -> {
                LstConstString(unescapeStringLiteral(ctx.expressionLiteral().STRING().text))
            }

            ctx.expressionLiteral().FALSE() != null -> LstConstBoolean(false)
            ctx.expressionLiteral().TRUE() != null -> LstConstBoolean(true)
            ctx.expressionLiteral().NULL() != null -> {
                collector.report(
                    "Null values are not available in this language, use Optional::None instead",
                    ctx.span()
                )
                LstConstUnit
            }

            ctx.expressionLiteral().INT_NUMBER() != null -> {
                val text = ctx.expressionLiteral().INT_NUMBER().text

                val intValue = when {
                    text.startsWith("0x") -> text.substring(2).toUInt(16).toInt()
                    text.startsWith("0o") -> text.substring(2).toUInt(8).toInt()
                    text.startsWith("0b") -> text.substring(2).toUInt(2).toInt()
                    else -> text.toInt()
                }

                LstConstInt(intValue)
            }

            ctx.expressionLiteral().FLOAT_NUMBER() != null -> {
                val text = ctx.expressionLiteral().FLOAT_NUMBER().text
                val floatValue = text.toFloatOrNull()

                if (floatValue == null) {
                    collector.report("Invalid float value '${text}'", ctx.span())
                }

                LstConstFloat(floatValue ?: 0f)
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
            typeUsage = TypeUsage.list(TypeUsage.unresolved(program.nextUnresolvedTypeRef()))
        )
        code.nodes += value

        ctx.listEntry().map { item ->
            val itemRef = processExpression(item.expression(), code)
            val span = item.expression().span()

            span to itemRef
        }.forEach { (spam, itemRef) ->

            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = value.ref
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = itemRef
            )
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                name = "add",
                path = "list",
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
            typeUsage = TypeUsage.map(
                TypeUsage.unresolved(program.nextUnresolvedTypeRef()),
                TypeUsage.unresolved(program.nextUnresolvedTypeRef())
            )
        )

        ctx.mapEntry().map { entry ->
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
            val span = keyCtx.span()

            Triple(span, keyRef, valueRef)
        }.forEach { (spam, keyRef, valueRef) ->
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = value.ref
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = keyRef
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = valueRef
            )
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                name = "set",
                path = "map",
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
            typeUsage = TypeUsage.set(TypeUsage.unresolved(program.nextUnresolvedTypeRef()))
        )

        ctx.listEntry().map { item ->
            val itemRef = processExpression(item.expression(), code)
            val span = item.expression().span()

            span to itemRef
        }.forEach { (spam, itemRef) ->
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = value.ref
            )
            code.nodes += LstFunArg(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                expr = itemRef
            )
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                name = "add",
                path = "set",
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
            ref = program.nextFunctionRef(),
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
                typeParameter = null,
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

        val dup = LstDup(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            expr = alloc.ref
        )
        code.nodes += dup

        return dup.ref
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
        val ops = bin.binaryOperator().map { it to it.span() }

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
                        varRef = variable.ref,
                        variable = variable
                    )
                }
            }

            stm.ifStatement() != null -> {
                val subCtx = stm.ifStatement()

                val cond = processExpression(subCtx.expression(), code)

                val prevBlock = code.currentBlock
                code.currentBlock = code.createBlock()

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
                    code.currentBlock = code.createBlock()

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
                code.currentBlock = code.createBlock()

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
                code.currentBlock = code.createBlock()

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
                code.currentBlock = code.createBlock()

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
                    varRef = variable.ref,
                    variable = variable,
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
                if (!allowTypeParamCollection) {
                    collector.report(
                        "Found undefined type parameter '${name}'",
                        ctx.typeParameter().span()
                    )
                }

                TypeParameter(
                    span = ctx.typeParameter().nameToken().span(),
                    name = name,
                    ref = program.nextTypeParamRef()
                )
            }

            return TypeUsage(
                span = ctx.typeParameter().nameToken().span(),
                name = name,
                path = "",
                sub = emptyList(),
                modifier = TypeUsage.Modifier.NONE,
                typeParameter = typeParameterRef,
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
            typeParameter = null,
            currentPath = currentPath(ctx)
        )
    }

    private fun <T> List<T?>.allNonNullOrNull(): List<T>? {
        if (any { it == null }) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return this as List<T>
    }

    private fun typeUsageToTypeTree(tu: TypeUsage): TypeTree {
        val params = tu.sub.map { typeUsageToTypeTree(it) }

        if (tu.unresolvedTypeRef != null) {
            return TypeTree(
                base = UnresolvedType(tu.unresolvedTypeRef),
                params = params,
            )
        }

        if (tu.typeParameter != null) {
            return TypeTree(
                base = ParamType(tu.typeParameter),
                params = params,
            )
        }

        val segments = createPathSegments(tu.currentPath, tu.fullName)

        for (segment in segments) {
            val option = program.options.values.find { it.fullName == segment }

            if (option != null) {
                return TypeTree(
                    base = OptionType(option),
                    params = params,
                )
            }

            val struct = program.structs.values.find { it.fullName == segment }

            if (struct != null) {
                return TypeTree(
                    base = StructType(struct),
                    params = params,
                )
            }
        }

        collector.report("Type '${tu.fullName}' not found", tu.span)
        return TypeTree(InvalidType, params)
    }

    private fun typeOf(name: String): TypeTree {
        val struct = program.structs.values.find { it.name == name } ?: error("Invalid type: $name")
        return TypeTree(base = StructType(struct))
    }

    private fun <T> linkVariable(node: T, code: LstCode) where T : LstExpression, T : HasVarRef {
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
            node.varRef = found.ref
            node.variable = found
            found.referencedBy.add(node)
            return
        }

        val found2 = program.consts.values.find { it.name == node.name }

        if (found2 != null) {
            node.varRef = found2.ref
            node.constant = found2
            found2.referencedBy.add(node)
            return
        }

        collector.report("Variable not found: ${node.name}", node.span)
    }

    private fun processCode(code: LstCode) {
        // Resolve specified types in let expressions
        code.variables.values.forEach { variable ->
            variable.type = variable.typeUsage?.let { typeUsageToTypeTree(it) }
        }

        // Link variable usage with the corresponding variable in scope
        code.nodes.forEach { node ->
            when (node) {
                is LstLoadVar -> {
                    if (node.varRef == null) {
                        linkVariable(node, code)
                    }
                }

                is LstStoreVar -> {
                    if (node.varRef == null) {
                        linkVariable(node, code)
                    }
                }

                else -> Unit
            }
        }

        // Resolve the type of every expression
        code.nodes.filterIsInstance<LstExpression>()
            .filter { it.type == null && !it.hasTypeError }
            .forEach { node -> processNodeExpression(node, code) }

        // Flag errors
        code.variables.values.forEach { variable ->

            fun reportNumMismatch(typeParameters: List<TypeParameter>, type: TypeTree) {
                val actual = type.params.size
                val expected = typeParameters.size

                if (expected > actual) {
                    val missing = typeParameters
                        .takeLast(expected - actual)
                        .joinToString(", ") { it.name }

                    collector.report(
                        "Variable '${variable.name}' has invalid type: missing type parameters ($missing)",
                        variable.span
                    )
                }

                if (expected < actual) {
                    collector.report(
                        "Variable '${variable.name}' has invalid type: too many type parameters, " +
                                "expecting $expected",
                        variable.span
                    )
                }
            }

            when (val base = variable.type?.base) {
                InvalidType -> {
                    // Already reported in typeUsageToTypeTree()
                }

                is OptionType -> {
                    reportNumMismatch(base.option.typeParameters, variable.type!!)
                }

                is StructType -> {
                    reportNumMismatch(base.struct.typeParameters, variable.type!!)
                }

                is ParamType -> Unit

                is UnresolvedType -> {
                    collector.report("Variable '${variable.name}' has a type that cannot be inferred", variable.span)
                }

                null -> {
                    collector.report("Variable '${variable.name}' has no type", variable.span)
                }
            }
        }
    }

    private fun processNodeExpression(node: LstExpression, code: LstCode) {
        when (node) {
            is LstUnit -> {
                node.type = typeOf("Unit")
            }

            is LstBoolean -> {
                node.type = typeOf("Boolean")
            }

            is LstInt -> {
                node.type = typeOf("Int")
            }

            is LstFloat -> {
                node.type = typeOf("Float")
            }

            is LstString -> {
                node.type = typeOf("String")
            }

            is LstIsType -> {
                node.typeTree = typeUsageToTypeTree(node.typeUsage)
                node.hasTypeError = node.typeTree.isInvalid()
                node.type = typeOf("Boolean")
            }

            is LstAsType -> {
                node.typeTree = typeUsageToTypeTree(node.typeUsage)
                node.hasTypeError = node.typeTree.isInvalid()
                node.type = node.typeTree
            }

            is LstSizeOf -> {
                node.typeTree = typeUsageToTypeTree(node.typeUsage)
                node.hasTypeError = node.typeTree.isInvalid()
                node.type = typeOf("Int")
            }

            is LstAlloc -> {
                // Declared type, ej. List<*>, Map<*, *>, Struct<Int>
                val type = typeUsageToTypeTree(node.typeUsage)

                // Only structs can be allocated, options and traits are only for the type system
                if (type.base !is StructType) {
                    collector.report(
                        "Type '${type}' is not an struct",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                val struct: LstStruct = type.base.struct

                // The real type is unknown until we resolve al unresolved types
                // and can replace the struct type template with concrete types
                val realParams = List(struct.typeParameters.size) {
                    type.params.getOrNull(it) ?: TypeTree(base = UnresolvedType(program.nextUnresolvedTypeRef()))
                }

                val finalType = TypeTree(base = type.base, params = realParams)

                node.typeTree = finalType
                node.struct = struct
                node.hasTypeError = finalType.isInvalid()
                node.type = finalType
            }

            is LstChoose -> {
                val ifTrue = code.nodes.find { it.ref == node.ifTrue } ?: error("Ref not found!")
                val ifFalse = code.nodes.find { it.ref == node.ifFalse } ?: error("Ref not found!")

                if (ifTrue !is LstExpression || ifFalse !is LstExpression) {
                    collector.report(
                        "Choice has invalid ref: not an expression ($ifTrue | $ifFalse)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (ifTrue.hasTypeError || ifFalse.hasTypeError) {
                    node.propagateTypeError()
                    return
                }

                if (ifTrue.type == null || ifFalse.type == null) {
                    node.propagateTypeError()
                    return
                }

                node.type = ifTrue.type

                // TODO allow common type
                if (ifTrue.type != ifFalse.type) {
                    collector.report(
                        "Choice has different types for each choice: " +
                                "${ifTrue.type} vs ${ifFalse.type}",
                        node.span
                    )
                    node.propagateTypeError()
                }
            }

            is LstLoadVar -> {
                if (node.varRef == null) {
                    node.propagateTypeError()
                    return
                }

                val type = if (node.varRef in program.consts) {
                    val const = program.consts[node.varRef] ?: error("ConstRef not found!")
                    const.type
                } else {
                    val variable = code.variables[node.varRef] ?: error("VarRef not found!")
                    variable.type
                }

                node.type = type

                if (type == null) {
                    node.propagateTypeError()
                }
            }

            is LstStoreVar -> {
                val value = code.nodes.find { it.ref == node.expr } ?: error("Ref not found!")

                if (value !is LstExpression) {
                    collector.report(
                        "StoreVar has invalid ref: not an expression ($value)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (value.hasTypeError || value.type == null) {
                    node.propagateTypeError()
                    return
                }

                if (node.varRef == null) {
                    node.propagateTypeError()
                    return
                }

                when (node.varRef) {
                    in program.consts -> {
                        val const = program.consts[node.varRef] ?: error("ConstRef not found!")
                        node.varType = const.type
                    }

                    else -> {
                        val variable = code.variables[node.varRef] ?: error("VarRef not found!")

                        // Variable without defined type, first assigned here
                        if (variable.type == null) {
                            variable.type = value.type
                        }

                        node.varType = variable.type
                    }
                }

                if (node.varType == null) {
                    node.propagateTypeError()
                    return
                }

                // Attempt to replace unsolved types
                // For example:
                // `let a: Optional<Int> = Optional::None $[]`
                // the value `Optional::None $[]` has type `Optional::None<unresolved#1>`
                // but, with `a` we can infer that `unresolved#1` is `Int`
                attemptResolveUnresolvedTypes(
                    node.span,
                    listOf(node.varType!!),
                    listOf(value.type!!),
                    code
                )

                if (!canBeAssignedTo(node.varType!!, value.type!!)) {
                    collector.report(
                        "Type mismatch, variable '${node.name}' " +
                                "expects a value of type '${node.varType}', " +
                                "but found '${value.type}' ($value)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                node.type = typeOf("Unit")
            }

            is LstLoadField -> {
                val instance = code.nodes.find { it.ref == node.instance } ?: error("Ref not found!")

                if (instance !is LstExpression) {
                    collector.report(
                        "LstLoadField has invalid ref: not an expression ($instance)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (instance.hasTypeError) {
                    node.propagateTypeError()
                    return
                }

                val instanceType = instance.type

                if (instanceType == null) {
                    node.propagateTypeError()
                    return
                }

                // Find the field
                val struct: LstStruct
                val field: LstStructureField

                if (instanceType.base is StructType) {
                    struct = instanceType.base.struct
                    val found = struct.fields.values.find { it.name == node.name }

                    if (found == null) {
                        collector.report(
                            "Type '${instanceType}' ha no field named '${node.name}'",
                            node.span
                        )
                        node.propagateTypeError()
                        return
                    }

                    node.fieldRef = found.ref
                    node.field = found
                    field = found
                } else {
                    collector.report(
                        "Type '${instanceType}' has no fields",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                val fieldType = field.type
                if (fieldType == null) {
                    node.propagateTypeError()
                    return
                }

                // Replace parametric types with concrete types
                val replacements = findStructReplacements(node.span, struct, instanceType)

                val concreteStructType = replaceTypeParams(struct.templateType, replacements)
                val concreteFieldType = replaceTypeParams(fieldType, replacements)

                node.concreteStructType = concreteStructType
                node.concreteFieldType = concreteFieldType

                node.type = node.concreteFieldType
            }

            is LstStoreField -> {
                val instance = code.nodes.find { it.ref == node.instance } ?: error("Ref not found!")
                val value = code.nodes.find { it.ref == node.expr } ?: error("Ref not found!")

                if (instance !is LstExpression || value !is LstExpression) {
                    collector.report(
                        "LstLoadField has invalid ref: not an expression ($instance | $value)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (instance.hasTypeError || value.hasTypeError) {
                    node.propagateTypeError()
                    return
                }

                val instanceType = instance.type
                val valueType = value.type

                // Process later
                if (instanceType == null || valueType == null) {
                    node.propagateTypeError()
                    return
                }

                // Find the field
                val struct: LstStruct
                val field: LstStructureField

                if (instanceType.base is StructType) {
                    struct = instanceType.base.struct
                    val found = struct.fields.values.find { it.name == node.name }

                    if (found == null) {
                        collector.report(
                            "Type '${instanceType}' ha no field named '${node.name}'",
                            node.span
                        )
                        node.propagateTypeError()
                        return
                    }

                    node.fieldRef = found.ref
                    node.field = found
                    field = found
                } else {
                    collector.report(
                        "Type '${instanceType}' has no fields",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                val fieldType = field.type
                if (fieldType == null) {
                    node.propagateTypeError()
                    return
                }

                // Replace parametric types with concrete types
                val replacements = findStructReplacements(node.span, struct, instanceType)

                val concreteStructType = replaceTypeParams(struct.templateType, replacements)
                val concreteFieldType = replaceTypeParams(fieldType, replacements)

                node.concreteStructType = concreteStructType
                node.concreteFieldType = concreteFieldType

                attemptResolveUnresolvedTypes(
                    node.span,
                    listOf(value.type!!),
                    listOf(node.concreteFieldType!!),
                    code
                )

                // By directional type resolution
                attemptResolveUnresolvedTypes(
                    node.span,
                    listOf(node.concreteFieldType!!),
                    listOf(value.type!!),
                    code
                )

                if (!canBeAssignedTo(node.concreteFieldType!!, value.type!!)) {
                    collector.report(
                        "Type mismatch, field '${field.name}' " +
                                "expects a value of type '${node.concreteFieldType}', " +
                                "but found '${value.type!!}' ($value)",
                        node.span
                    )
                }

                node.type = typeOf("Unit")
            }

            is LstFunArg -> {
                val value = code.nodes.find { it.ref == node.expr } ?: error("Ref not found!")

                if (value !is LstExpression) {
                    collector.report(
                        "LstFunArg has invalid ref: not an expression ($value)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (value.hasTypeError) {
                    node.propagateTypeError()
                    return
                }

                if (value.type == null) {
                    node.propagateTypeError()
                    return
                }

                node.type = value.type
            }

            is LstFunCall -> {
                val funCallIndex = code.nodes.indexOf(node)
                val args = mutableListOf<LstFunArg>()

                repeat(node.argCount) {
                    val prevNode = code.nodes[funCallIndex - node.argCount + it]
                    args += prevNode as? LstFunArg ?: error("Invalid node sequence: $prevNode")
                }

                // Propagate errors
                if (args.any { it.hasTypeError || it.type == null }) {
                    node.propagateTypeError()
                    return
                }

                val choices = program.functions.values.filter { it.name == node.name }

                if (choices.isEmpty()) {
                    if (args.isNotEmpty() && args.first().type != null) {
                        collector.report(
                            "Function '${node.name}' not found " +
                                    "for type '${args.first().type}'", node.span
                        )
                    } else {
                        collector.report("Function '${node.name}' not found", node.span)
                    }

                    node.propagateTypeError()
                    return
                }

                val tmpArgsTypes = args.map { it.type!! }

                val sortedChoices = choices.sortedByDescending {
                    val paramsTypes = it.params.map { param -> param.type!! }
                    functionParamsSimilarity(paramsTypes, tmpArgsTypes)
                }

                // Best match
                val function = sortedChoices.first()

                node.funRef = function.ref
                node.function = function

                if (function.returnType == null || function.params.any { it.type == null }) {
                    node.propagateTypeError()
                    return
                }

                if (function.params.size != args.size) {
                    collector.report(
                        "Function '${function.name}' expects ${function.params.size}, " +
                                "but only ${args.size} arguments where provided",
                        node.span
                    )
                }

                // Replace parametric types with concrete types
                val replacements = findFuncReplacements(node, function, args)
                val allTypeParams = mutableSetOf<TypeParamRef>()

                collectAllTypeParams(function.returnType!!, allTypeParams)

                function.params.forEach {
                    collectAllTypeParams(it.type!!, allTypeParams)
                }

                // All unbound type parameters are replaced by unresolved types, so they can be inferred by usage
                // For example: `let a: Int = func()` where `fun func(): #T = ...`, #T is inferred to be Int
                allTypeParams.forEach {
                    if (it !in replacements) {
                        replacements[it] = TypeTree(base = UnresolvedType(program.nextUnresolvedTypeRef()))
                    }
                }

                val concreteTypeParams = function.params.map { replaceTypeParams(it.type!!, replacements) }
                val concreteArgTypes = args.map { replaceTypeParams(it.type!!, replacements) }

                node.concreteTypeParams = concreteTypeParams

                // Attempt to infer the types of unresolved type-values, for example:
                // `let a = #[]` and `a.add(0)` => `a` is List<Int>
                attemptResolveUnresolvedTypes(node.span, concreteTypeParams, concreteArgTypes, code)

                // Replace type params with concrete types

                val paramsTypes = function.params.map { param ->
                    replaceTypeParams(param.type!!, replacements)
                }

                val argsTypes = args.map {
                    replaceTypeParams(it.type!!, replacements)
                }

                paramsTypes.zip(argsTypes).forEachIndexed { index, (param, arg) ->
                    if (!canBeAssignedTo(param, arg)) {
                        collector.report(
                            "Type mismatch calling function '${function.name}', param $index expects '${param}', " +
                                    "but '${arg}' was found instead",
                            args[index].span
                        )
                    }
                }

                node.type = replaceTypeParams(function.returnType!!, replacements)
            }

            is LstReturn -> {
                node.type = typeOf("Unit")
                val value = code.nodes.find { it.ref == node.expr } ?: error("Ref not found!")

                if (value !is LstExpression) {
                    collector.report(
                        "LstReturn has invalid ref: not an expression ($value)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (value.hasTypeError) {
                    node.propagateTypeError()
                    return
                }

                if (value.type == null) {
                    node.propagateTypeError()
                    return
                }

                // If the return type is not defined, we infer from the first return
                if (code.returnType == null) {
                    code.returnType = value.type
                }

                attemptResolveUnresolvedTypes(
                    node.span,
                    listOf(code.returnType!!),
                    listOf(value.type!!),
                    code
                )

                if (!canBeAssignedTo(code.returnType!!, value.type!!)) {
                    collector.report(
                        "Type mismatch, attempt to return '${value.type}' on a function that must return '${code.returnType}'",
                        node.span
                    )
                }
            }

            is LstDup -> {
                val value = code.nodes.find { it.ref == node.expr } ?: error("Ref not found!")

                if (value !is LstExpression) {
                    collector.report(
                        "LstReturn has invalid ref: not an expression ($value)",
                        node.span
                    )
                    node.propagateTypeError()
                    return
                }

                // Propagate errors
                if (value.hasTypeError) {
                    node.propagateTypeError()
                    return
                }

                if (value.type == null) {
                    node.propagateTypeError()
                    return
                }

                node.type = value.type
            }
        }
    }

    private fun canBeAssignedTo(defined: TypeTree, used: TypeTree): Boolean {
        if (defined == used) {
            return true
        }

        // Check base
        if (!baseCanBeAssignedTo(defined.base, used.base)) {
            return false
        }

        // Check params
        if (defined.params.size != used.params.size) {
            return false
        }

        repeat(defined.params.size) {
            if (!canBeAssignedTo(defined.params[it], used.params[it])) {
                return false
            }
        }

        // Everything ok
        return true
    }

    private fun baseCanBeAssignedTo(defined: TypeBase, used: TypeBase): Boolean = when {
        // Common case, same type
        defined == used -> true

        // Expected Option, found Struct
        // let a: Optional<Int> = Optional::None $[]
        defined is OptionType && used is StructType && used.struct.ref in defined.option.items -> true

        // No match
        else -> false
    }

    private fun findStructReplacements(
        span: Span,
        struct: LstStruct,
        instanceType: TypeTree
    ): Map<TypeParamRef, TypeTree> {
        val replacements = mutableMapOf<TypeParamRef, TypeTree>()

        extractTypeReplacements(
            span,
            struct.typeParameters.map { it.toTypeTree() },
            instanceType.params,
            replacements
        )

        return replacements
    }

    /**
     * Attempt to solve unresolved types
     *
     * First argument is the one being modified, the second is used to determine what value should be used
     *
     * Example:
     *  $1 = alloc List<Unresolved(1)>
     *  $2 = 1 as Int
     *  $3   FunArg($1)     // => List<Unresolved(1)>
     *  $3   FunArg($2)     // => Int
     *  $3 = FunCall("add") // => List<#Item> -> #Item -> Unit
     *  :
     *  List<Unresolved(1)> vs List<#Item>
     *  Int vs #Item
     *  =>
     *  Unresolved(1) => Int
     */
    private fun attemptResolveUnresolvedTypes(
        span: Span,
        concreteTypeParams: List<TypeTree>,
        concreteArgTypes: List<TypeTree>,
        code: LstCode
    ) {
        val resolvedTypes = mutableMapOf<UnresolvedTypeRef, TypeTree>()

        extractUnresolvedReplacements(span, concreteArgTypes, concreteTypeParams, resolvedTypes)

        if (resolvedTypes.isNotEmpty()) {
            replaceAllUnresolved(code, resolvedTypes)
        }
    }

    private fun extractUnresolvedReplacements(
        span: Span,
        defined: List<TypeTree>,
        used: List<TypeTree>,
        replacements: MutableMap<UnresolvedTypeRef, TypeTree>
    ) {
        for ((defParam, usedParam) in defined.zip(used)) {
            if (defParam.base !is UnresolvedType) {
                extractUnresolvedReplacements(span, defParam.params, usedParam.params, replacements)
                continue
            }

            val ref = defParam.base.ref
            val prev = replacements[ref]

            if (prev != null && prev != usedParam) {
                collector.report("Type conflict, '$ref': '$prev' vs '$usedParam'", span)
            }

            replacements[ref] = usedParam
        }
    }

    private fun findFuncReplacements(
        node: LstFunCall,
        function: LstFunction,
        args: List<LstFunArg>
    ): MutableMap<TypeParamRef, TypeTree> {
        val replacements = mutableMapOf<TypeParamRef, TypeTree>()

        if (node.specifiedTypeParams.size > function.typeParameters.size) {
            collector.report(
                "Function '${function.name}' expects ${function.typeParameters.size} type parameters, " +
                        "but ${node.specifiedTypeParams.size} where provided",
                node.span
            )
        }

        node.specifiedTypeParams.zip(function.typeParameters).forEach { (usage, funTyParam) ->
            val type = typeUsageToTypeTree(usage)

            replacements[funTyParam.ref] = type
        }

        val argsTypes = args.map { it.type!! }

        extractTypeReplacements(
            node.span,
            function.params.map { it.type!! },
            argsTypes,
            replacements
        )

        return replacements
    }

    private fun extractTypeReplacements(
        span: Span,
        defined: List<TypeTree>,
        used: List<TypeTree>,
        replacements: MutableMap<TypeParamRef, TypeTree>
    ) {

        for ((defParam, usedParam) in defined.zip(used)) {
            if (defParam.base !is ParamType) {
                extractTypeReplacements(span, defParam.params, usedParam.params, replacements)
                continue
            }

            val ref = defParam.base.param.ref
            val prev = replacements[ref]

            if (prev != null && !prev.hasUnresolved() && !usedParam.hasUnresolved() && prev != usedParam) {
                collector.report(
                    "Type conflict, multiple conflicting alternatives to resolve '${defParam.base.param}', " +
                            "'$prev' vs '$usedParam'", span
                )
                continue
            }

            replacements[ref] = usedParam
        }
    }

    private fun collectAllTypeParams(type: TypeTree, result: MutableSet<TypeParamRef>) {
        if (type.base is ParamType) {
            result += type.base.param.ref
        }

        type.params.forEach { collectAllTypeParams(it, result) }
    }

    private fun replaceTypeParams(type: TypeTree, replacements: Map<TypeParamRef, TypeTree>): TypeTree {
        if (type.base is ParamType) {
            return replacements[type.base.param.ref] ?: type
        }

        return TypeTree(
            base = type.base,
            params = type.params.map { replaceTypeParams(it, replacements) }
        )
    }

    private fun replaceAllUnresolved(code: LstCode, replacements: Map<UnresolvedTypeRef, TypeTree>) {
        code.variables.values.forEach { variable ->
            variable.type = variable.type?.let { replaceUnresolved(it, replacements) }
        }

        code.nodes.filterIsInstance<LstExpression>().forEach { expr ->
            expr.mapTypes { replaceUnresolved(it, replacements) }
        }
    }

    private fun replaceUnresolved(type: TypeTree, replacements: Map<UnresolvedTypeRef, TypeTree>): TypeTree {
        if (type.base is UnresolvedType) {
            return replacements[type.base.ref] ?: type
        }

        return TypeTree(
            base = type.base,
            params = type.params.map { replaceUnresolved(it, replacements) }
        )
    }

    private fun functionParamsSimilarity(params: List<TypeTree>, args: List<TypeTree>): Float {
        var total = 1000000f

        if (params.size != args.size) {
            total -= 100000f
        }

        for ((param, arg) in params.zip(args)) {
            total += typeSimilarity(param, arg) - 10000f
        }

        return total
    }

    private fun typeSimilarity(param: TypeTree, arg: TypeTree): Float {
        var total = 10000f

        if (param.base != arg.base) {
            total -= 10000f
        }

        if (param.params.size != arg.params.size) {
            total -= 1000f
        }

        param.params.zip(arg.params).forEach { (p, a) ->
            total += (typeSimilarity(p, a) - 10000f) / 100f
        }

        return total
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