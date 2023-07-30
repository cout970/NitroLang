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
import nitrolang.gen.MainParser.JsonExprContext
import nitrolang.gen.MainParser.JsonValueContext
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
import nitrolang.gen.MainParser.StringContext
import nitrolang.gen.MainParser.StructDefinitionContext
import nitrolang.gen.MainParser.StructInstanceExprContext
import nitrolang.gen.MainParser.TypeParamDefContext
import nitrolang.gen.MainParser.TypeUsageContext
import nitrolang.gen.MainParser.UseDefinitionContext
import nitrolang.gen.MainParser.VariableExprContext
import nitrolang.gen.MainParserBaseListener
import nitrolang.sm.*
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import nitrolang.util.Span
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode

const val SELF_NAME = "this"
const val ANNOTATION_EXTERN = "Extern"
const val ANNOTATION_EXPORT = "Export"
const val ANNOTATION_REQUIRED = "Required"
const val ANNOTATION_WASM_INLINE = "WasmInline"
const val ANNOTATION_STACK_VALUE = "StackValue"
const val MAIN_FUNCTION_NAME = "main"

class AstParser(
    private val collector: ErrorCollector,
    private val source: SourceFile,
    private val program: LstProgram,
) : MainParserBaseListener() {
    private val typeParamMap = mutableMapOf<String, TypeParameter>()
    private var allowTypeParamCollection = false

    companion object {

        fun includeFile(ns: String, path: String, collector: ErrorCollector, program: LstProgram): Boolean {
            assert(ns == "core") { "Only core is supported for now" }
            val source = SourceFile.load("src/main/nitro/$path")
            return parseFile(source, collector, program, false)
        }

        fun parseFile(
            source: SourceFile,
            collector: ErrorCollector,
            program: LstProgram,
            finalize: Boolean = true
        ): Boolean {
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

            if (finalize) {
                finalize(astParser, program, collector)
            }
            return true
        }

        private fun finalize(astParser: AstParser, program: LstProgram, collector: ErrorCollector) {
            program.consts.values.forEach { const ->
                const.type = astParser.typeUsageToTypeTree(const.typeUsage)
                const.body.returnType = const.type
            }
            program.structs.values.forEach { struct ->
                struct.fields.values.forEach { field ->
                    field.type = astParser.typeUsageToTypeTree(field.typeUsage)
                }
            }
            program.functions.values.forEach { func ->
                func.params.forEach { param ->
                    param.type = astParser.typeUsageToTypeTree(param.typeUsage)
                    param.variable!!.type = param.type
                }
                func.returnType = astParser.typeUsageToTypeTree(func.returnTypeUsage)
                func.body.returnType = func.returnType
            }

            program.functions.values.forEach { func ->
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
                if (!defined.isNothing()) {
                    when (val last = func.body.lastExpression?.let { func.body.getNode(it) }) {
                        null -> {
                            collector.report(
                                "Function '${func.name}' must return '$defined' but has empty body",
                                func.span
                            )
                        }

                        // Ok, already checked in processCode()
                        is LstReturn -> Unit

                        is LstExpression -> {
                            if (!last.type!!.isNever() && !astParser.canBeAssignedTo(defined, last.type!!)) {
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
            program.consts.values.forEach { const ->
                astParser.processCode(const.body)
            }
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
                func.params.forEach { param ->
                    param.type = astParser.typeUsageToTypeTree(param.typeUsage)
                    param.variable!!.type = param.type
                }
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
        val options = mutableListOf<StructRef>()
        val optionRef = program.nextOptionRef()

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

            struct.parentOption = optionRef
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
            ref = optionRef,
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

            variable.isParam = true
            param.variable = variable
            body.variables[variable.ref] = variable
        }

        val returnTypeUsage: TypeUsage = if (ctx.functionReturnType() != null) {
            resolveTypeUsage(ctx.functionReturnType().typeUsage())
        } else {
            TypeUsage.nothing()
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

            variable.isParam = true
            param.variable = variable
            body.variables[variable.ref] = variable
        }

        val typeParameters = endTypeParams()

        when {
            ctx.functionBody().statementBlock() != null -> {
                processStatementBlock(ctx.functionBody().statementBlock(), body)
            }

            ctx.functionBody().expression() != null -> {
                body.lastExpression = processExpression(ctx.functionBody().expression(), body)
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
        body.lastExpression = processExpression(ctx.expression(), body)

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

    override fun enterIncludeDefinition(ctx: IncludeDefinitionContext) {
        val location = processPlainString(ctx.PLAIN_STRING())
        val namespace = location.substringBefore(':')
        val path = location.substringAfter(':')

//        val location = ctx.location()
//        val namespace = if (location.declaredNameToken() != null) location.declaredNameToken().text else "this"
//        var path = location.nameToken().joinToString("/") { it.text }

//        location.extension()?.let {
//            path += "."
//            path += it.nameToken().text
//        }

        includeFile(namespace, path, collector, program)
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
                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = complex.notExpr().span(),
                    block = code.currentBlock,
                    name = "logical_not",
                    path = "core",
                    arguments = listOf(expr),
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
            }

            simple.IN() != null -> {
                val value = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)
                val collection = processExpressionWithSuffix(simple.expressionWithSuffix(1), code)

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

            simple.NOT_IN() != null -> {
                val value = processExpressionWithSuffix(simple.expressionWithSuffix(0), code)
                val collection = processExpressionWithSuffix(simple.expressionWithSuffix(1), code)

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

                val call = LstFunCall(
                    ref = code.nextRef(),
                    span = ctx.collectionIndexingSuffix().span(),
                    block = code.currentBlock,
                    name = "get",
                    path = "",
                    arguments = listOf(prevRef, indexRef),
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
        val args = mutableListOf<Ref>()
        val specifiedTypeParams = mutableListOf<TypeUsage>()

        if (receiver != null) {
            args += receiver
        }

        params?.typeParamArg()?.typeUsage()?.forEach { param ->
            specifiedTypeParams += resolveTypeUsage(param)
        }

        params?.functionCallParamList()?.expression()?.forEach { param ->
            args += processExpression(param, code)
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

        val call = LstFunCall(
            ref = code.nextRef(),
            span = span,
            block = code.currentBlock,
            name = name,
            path = path,
            arguments = args,
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
                processExpressionJsonExpr(ctx.jsonExpr(), code)
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
                val node = LstLoopJump(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    backwards = false,
                )
                code.breakNodes += node
                code.nodes += node
                node.ref
            }

            ctx.CONTINUE() != null -> {
                val node = LstLoopJump(
                    ref = code.nextRef(),
                    span = ctx.span(),
                    block = code.currentBlock,
                    backwards = true,
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

    private fun processConstExpr(ctx: ConstExprContext): ConstValue {
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

                val intValue = when {
                    text.startsWith("0x") -> text.substring(2).toUInt(16).toInt()
                    text.startsWith("0o") -> text.substring(2).toUInt(8).toInt()
                    text.startsWith("0b") -> text.substring(2).toUInt(2).toInt()
                    else -> text.toInt()
                }

                if (text.startsWith("-") || text.startsWith("+")) {
                    checkSubPreviousToken(ctx.constExpressionLiteral(), ctx.constExpressionLiteral().INT_NUMBER())
                }

                ConstInt(intValue)
            }

            ctx.constExpressionLiteral().FLOAT_NUMBER() != null -> {
                val text = ctx.constExpressionLiteral().FLOAT_NUMBER().text
                val floatValue = text.toFloatOrNull()

                if (floatValue == null) {
                    collector.report("Invalid float value '${text}'", ctx.span())
                }

                if (text.startsWith("-") || text.startsWith("+")) {
                    checkSubPreviousToken(ctx.constExpressionLiteral(), ctx.constExpressionLiteral().FLOAT_NUMBER())
                }

                ConstFloat(floatValue ?: 0f)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun stringToInt(text: String): Int? {
        return when {
            text.startsWith("0x") -> text.substring(2).toUIntOrNull(16)?.toInt()
            text.startsWith("0o") -> text.substring(2).toUIntOrNull(8)?.toInt()
            text.startsWith("0b") -> text.substring(2).toUIntOrNull(2)?.toInt()
            else -> text.toIntOrNull()
        }
    }

    private fun stringToFloat(text: String): Float? = text.toFloatOrNull()

    private fun processExpressionExpressionLiteral(ctx: ExpressionLiteralContext, code: LstCode): Ref {
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

            ctx.FLOAT_NUMBER() != null -> {
                val text = ctx.FLOAT_NUMBER().text
                var floatValue = text.toFloatOrNull()

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
                processString(ctx.string(), code)
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

    private fun processPlainString(ctx: TerminalNode): String {
        val tokenText = ctx.text
        val text = tokenText.substring(1, tokenText.length - 1)
        return unescapeStringLiteral(text)
    }

    private fun processString(ctx: StringContext, code: LstCode): Ref {
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
                item.STRING_ESCAPE() != null -> buff.append("\"")
                item.STRING_VAR() != null -> {
                    endChunk()
                    val node = LstLoadVar(
                        ref = code.nextRef(),
                        span = ctx.span(),
                        block = code.currentBlock,
                        name = item.STRING_VAR().text.substring(1),
                        path = "",
                    )
                    code.nodes += node

                    val call = LstFunCall(
                        ref = code.nextRef(),
                        span = ctx.span(),
                        block = code.currentBlock,
                        name = "to_string",
                        path = "",
                        arguments = listOf(node.ref),
                    )
                    code.nodes += call

                    allParts += call.ref
                }

                item.STRING_INTERP_START() != null -> {
                    endChunk()
                    val ref = processExpression(item.expression(), code)

                    val call = LstFunCall(
                        ref = code.nextRef(),
                        span = ctx.span(),
                        block = code.currentBlock,
                        name = "to_string",
                        path = "",
                        arguments = listOf(ref),
                    )
                    code.nodes += call

                    allParts += call.ref
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

    private fun unescapeStringLiteral(tokenText: String): String {
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

    private fun createStructInstance(type: TypeUsage, fields: List<Pair<String, Ref>>, code: LstCode, span: Span): Ref {
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

    private fun processExpressionJsonExpr(ctx: JsonExprContext, code: LstCode): Ref {
        return processJsonValue(ctx.jsonValue(), code)
    }

    private fun processJsonValue(value: JsonValueContext, code: LstCode): Ref {
        return when {
            value.string() != null -> {
                val string = processString(value.string(), code)

                createStructInstance(
                    type = TypeUsage.simple("Json::String"),
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
                    type = TypeUsage.simple("Json::Number"),
                    fields = listOf("value" to const.ref),
                    code = code,
                    span = value.span(),
                )
            }

            value.FLOAT_NUMBER() != null -> {
                val text = value.FLOAT_NUMBER().text
                var floatValue = stringToFloat(text)

                if (floatValue == null) {
                    collector.report("Invalid float value '${text}'", value.span())
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
                    type = TypeUsage.simple("Json::Number"),
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
                    type = TypeUsage.simple("Json::Boolean"),
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
                    type = TypeUsage.simple("Json::Boolean"),
                    fields = listOf("value" to const.ref),
                    code = code,
                    span = value.span(),
                )
            }

            value.NULL() != null -> {
                createStructInstance(
                    type = TypeUsage.simple("Json::Null"),
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
                    name = "create_string_map",
                    path = "",
                    arguments = emptyList(),
                    specifiedTypeParams = listOf(TypeUsage.simple("Json")),
                )
                code.nodes += map

                value.jsonObject().jsonPair().forEach { pair ->
                    val keyRef = if (pair.string() != null) {
                        processString(pair.string(), code)
                    } else {
                        val entryKey = LstString(
                            ref = code.nextRef(),
                            span = value.span(),
                            block = code.currentBlock,
                            value = pair.nameToken().text
                        )
                        code.nodes += entryKey
                        entryKey.ref
                    }

                    val valueRef = processJsonValue(pair.jsonValue(), code)

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
                    type = TypeUsage.simple("Json::Object"),
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
                    name = "create_list",
                    path = "",
                    arguments = emptyList(),
                    specifiedTypeParams = listOf(TypeUsage.simple("Json")),
                )
                code.nodes += list

                value.jsonArray().jsonValue().forEach { jsonValue ->
                    val entryValue = processJsonValue(jsonValue, code)

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
                    type = TypeUsage.simple("Json::Array"),
                    fields = listOf("value" to list.ref),
                    code = code,
                    span = value.span(),
                )
            }

            value.expression() != null -> {
                processExpression(value.expression(), code)
            }

            else -> error("Grammar has been expanded and parser is outdated")
        }
    }

    private fun processExpressionListExpr(ctx: ListExprContext, code: LstCode): Ref {
        val list = LstFunCall(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            name = "create_list",
            path = "",
            arguments = emptyList(),
            specifiedTypeParams = listOf(TypeUsage.unresolved(program.nextUnresolvedTypeRef())),
        )
        code.nodes += list

        ctx.listEntry().forEach { item ->
            val span = item.expression().span()

            val itemRef = processExpression(item.expression(), code)

            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = span,
                block = code.currentBlock,
                name = "add",
                path = "",
                arguments = listOf(list.ref, itemRef)
            )
        }

        return list.ref
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

                keyCtx.string() != null -> {
                    processString(keyCtx.string(), code)
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
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                name = "set",
                path = "map",
                arguments = listOf(value.ref, keyRef, valueRef)
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
            code.nodes += LstFunCall(
                ref = code.nextRef(),
                span = spam,
                block = code.currentBlock,
                name = "add",
                path = "set",
                arguments = listOf(value.ref, itemRef),
            )
        }

        return value.ref
    }

    private fun processExpressionLambdaExpr(ctx: LambdaExprContext, code: LstCode): Ref {
        val params: MutableList<LstFunctionParam> = mutableListOf()
        var returnType: TypeUsage = TypeUsage.nothing()
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
                // TODO this variable?
            }

            if (def.lambdaParams() != null) {
                def.lambdaParams().lambdaArgument().forEach {
                    params += LstFunctionParam(
                        span = it.nameToken().span(),
                        name = it.nameToken().text,
                        index = index++,
                        typeUsage = resolveTypeUsage(it.typeUsage()),
                    )
                    // TODO link variable
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

        code.nodes += LstIfStart(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock,
            cond = cond,
        )

        val prevBlock = code.currentBlock
        code.currentBlock = code.createBlock()

        ctx.statementBlock(0).statement().map { processStatement(it, code) }
        val ifTrue = code.lastExpression ?: error("Code block has no last expression")

        // Restore prev block
        code.currentBlock = prevBlock
        code.currentBlock = code.createBlock()

        code.nodes += LstIfElse(
            ref = code.nextRef(),
            span = ctx.statementBlock(1).span(),
            block = code.currentBlock
        )

        ctx.statementBlock(1).statement().map { processStatement(it, code) }
        val ifFalse = code.lastExpression ?: error("Code block has no last expression")

        // Restore prev block
        code.currentBlock = prevBlock

        code.nodes += LstIfEnd(
            ref = code.nextRef(),
            span = ctx.span(),
            block = code.currentBlock
        )

        val choose = LstChoose(
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
        val ops = bin.binaryOperator().map { it to it.span() }

        return resolvePrecedence(exprs, ops, code) { processExpressionSimple(it, code) }
    }

    private fun resolvePrecedence(
        exprs: List<ExpressionSimpleContext>,
        ops: List<Pair<MainParser.BinaryOperatorContext, Span>>,
        code: LstCode,
        func: (ExpressionSimpleContext) -> Ref
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

    private fun getBinaryOperator(ctx: MainParser.BinaryOperatorContext): ExpressionTree.Operator {
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
    private fun checkSubPreviousToken(ctx: ParserRuleContext, t: TerminalNode) {
        val s = t.symbol
        val prevChar = s.inputStream.getText(Interval.of(s.startIndex - 1, s.startIndex - 1))

        if (prevChar !in setOf("(", "{", "[", " ", ",", ";")) {
            collector.report(
                "Invalid char placement, consider adding an space before '${t.text}'",
                ctx.span()
            )
        }
    }

    private fun processStatementBlock(ctx: StatementBlockContext, code: LstCode) {
        ctx.statement().forEach { stm -> processStatement(stm, code) }
    }

    private fun processStatement(ctx: StatementContext, code: LstCode) {
        val stm = ctx.statementChoice()
        code.lastExpression = null

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

                code.nodes += LstIfStart(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    cond = cond,
                )

                val prevBlock = code.currentBlock
                code.currentBlock = code.createBlock()

                subCtx.statementBlock(0).statement().map { processStatement(it, code) }

                // Restore prev block
                code.currentBlock = prevBlock

                if (subCtx.statementBlock(1) != null) {
                    code.currentBlock = code.createBlock()

                    code.nodes += LstIfElse(
                        ref = code.nextRef(),
                        span = subCtx.statementBlock(1).span(),
                        block = code.currentBlock
                    )

                    subCtx.statementBlock(1).statement().map { processStatement(it, code) }

                    // Restore prev block
                    code.currentBlock = prevBlock
                }

                code.nodes += LstIfEnd(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock
                )
            }

            stm.loopStatement() != null -> {
                val subCtx = stm.loopStatement()
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
                processStatementBlock(subCtx.statementBlock(), code)

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

            stm.whileStatement() != null -> {
                // Converts a while in loop+if
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
                val cond = processExpression(subCtx.expression(), code)

                code.nodes += LstIfStart(
                    ref = code.nextRef(),
                    span = subCtx.span(),
                    block = code.currentBlock,
                    cond = cond,
                )

                val prevBlock2 = code.currentBlock
                code.currentBlock = code.createBlock()

                // Code in loop...
                processStatementBlock(subCtx.statementBlock(), code)

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

            stm.repeatStatement() != null -> {
                // Converts a while in
                // {
                //   let limit = count
                //   let it = 0
                //   loop: {
                //     if (it < limit) {
                //       let it = _repeat_index_
                //       code...
                //       it = it + 1
                //       goto loop;
                //     }
                //   }
                // }

                val subCtx = stm.repeatStatement()
                val prevBlockStart = code.currentBlock
                code.currentBlock = code.createBlock()

                // let limit = count
                val count = processExpression(subCtx.expression(), code)

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
                processStatementBlock(subCtx.statementBlock(), code)

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

            stm.forStatement() != null -> {
                // Converts a for in while+iter.next()
                // let aux = iterable.to_iterator()
                // loop: {
                //   let i = aux.next()
                //   if (i.is_some()) {
                //     code...
                //     goto loop;
                //   }
                // }

                val subCtx = stm.forStatement()
                val prevBreakNodes = code.breakNodes
                val prevContinueNodes = code.continueNodes

                code.breakNodes = mutableListOf()
                code.continueNodes = mutableListOf()

                val iterable = processExpression(subCtx.expression(), code)

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
                processStatementBlock(subCtx.statementBlock(), code)

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
                            val receiver = processExpression(subSubCtx.expression(), code)
                            val value = processExpression(subCtx.expression(), code)

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
                    code.lastExpression = processExpression(subCtx.expression(), code)
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
            is LstNothing -> {
                node.type = typeOf("Nothing")
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
                node.type = typeUsageToTypeTree(node.typeUsage)
                node.hasTypeError = node.type.isInvalid()
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
                val ifTrue = code.getNode(node.ifTrue)
                val ifFalse = code.getNode(node.ifFalse)

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

                // Attempt to replace unsolved types
                attemptResolveUnresolvedTypes(
                    node.span,
                    listOf(ifTrue.type!!),
                    listOf(ifFalse.type!!),
                    code
                )
                attemptResolveUnresolvedTypes(
                    node.span,
                    listOf(ifFalse.type!!),
                    listOf(ifTrue.type!!),
                    code
                )

                node.type = commonType(ifTrue.type!!, ifFalse.type!!)

                if (node.type!!.isInvalid()) {
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
                val value = code.getNode(node.expr)

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

                node.type = typeOf("Nothing")
            }

            is LstLoadField -> {
                val instance = code.getNode(node.instance)

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
                val instance = code.getNode(node.instance)
                val value = code.getNode(node.expr)

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

                node.type = typeOf("Nothing")
            }

            is LstFunCall -> {
                val args = node.arguments.map { argRef ->
                    val argNode = code.getNode(argRef)
                    argNode as? LstExpression ?: error("Expression expected: $argNode")
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
                                "but ${args.size} arguments where provided",
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
                            "Type mismatch calling function '${function.name}', param ${index + 1} expects '${param}', " +
                                    "but '${arg}' was found instead",
                            args[index].span
                        )
                    }
                }

                node.type = replaceTypeParams(function.returnType!!, replacements)
            }

            is LstReturn -> {
                node.type = typeOf("Nothing")
                val value = code.getNode(node.expr)

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
        }
    }

    private fun commonType(a: TypeTree, b: TypeTree): TypeTree {
        val base = when {
            // Same type
            a.base == b.base -> a.base

            // Option and Option Item
            a.base is OptionType && b.base is StructType
                    && b.base.struct.ref in a.base.option.items -> a.base

            // Option Item and Option
            b.base is OptionType && a.base is StructType
                    && a.base.struct.ref in b.base.option.items -> b.base

            // Option Item and Option Item
            b.base is StructType && a.base is StructType
                    && a.base.struct.parentOption != null
                    && a.base.struct.parentOption == b.base.struct.parentOption -> {

                OptionType(program.options[b.base.struct.parentOption]!!)
            }

            else -> InvalidType
        }

        val params = a.params.zip(b.params).map { commonType(it.first, it.second) }

        return TypeTree(base, params)
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
     *  $3 = FunCall("add") // => List<#Item> -> #Item -> Nothing
     *  :
     *  List<Unresolved(1)> vs List<#Item>1
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
        args: List<LstExpression>
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

            var newReplacement = usedParam
            val ref = defParam.base.param.ref
            val prev = replacements[ref]

            if (prev != null && !prev.hasUnresolved() && !newReplacement.hasUnresolved()) {
                val common = commonType(prev, newReplacement)

                if (common.isInvalid() || !canBeAssignedTo(common, newReplacement)) {
                    collector.report(
                        "Type conflict, multiple conflicting alternatives to resolve '${defParam.base.param}', " +
                                "'$prev' vs '$newReplacement'", span
                    )
                    continue
                } else {
                    newReplacement = common
                }
            }

            replacements[ref] = newReplacement
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