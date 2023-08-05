package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.gen.MainLexer
import nitrolang.gen.MainParser
import nitrolang.gen.MainParser.*
import nitrolang.gen.MainParserBaseListener
import nitrolang.types.doTypeChecking
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import nitrolang.util.Span
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File

const val SELF_NAME = "this"
const val ANNOTATION_EXTERN = "Extern"
const val ANNOTATION_EXPORT = "Export"
const val ANNOTATION_REQUIRED = "Required"
const val ANNOTATION_WASM_INLINE = "WasmInline"
const val ANNOTATION_STACK_VALUE = "StackValue"
const val MAIN_FUNCTION_NAME = "main"

data class ParserCtx(
    val collector: ErrorCollector,
    val source: SourceFile,
    val program: LstProgram,
    val typeParamMap: MutableMap<String, TypeParameter>,
    var allowTypeParamCollection: Boolean,
    var code: LstCode,
) {
    fun ParserRuleContext.span(): Span {
        return Span(start.startIndex, stop.stopIndex, this@ParserCtx.source)
    }

    fun LstNode.asExpr(ctx: LstNode): LstExpression? {
        if (this !is LstExpression) {
            collector.report("Invalid ref: not an expression $this", ctx.span)
            (ctx as? LstExpression)?.propagateTypeError()
            return null
        }
        return this
    }

    fun currentPath(ctx: ParserRuleContext): String {
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

    override fun toString(): String {
        // This function is slowing down debugging with the huge generated strings
        return "ParserCtx()"
    }
}

class AstParser(val parserCtx: ParserCtx) : MainParserBaseListener() {
    companion object {

        fun includeFile(ns: String, path: String, collector: ErrorCollector, program: LstProgram): Boolean {
            assert(ns == "core") { "Only core is supported for now" }

            val realPath = "src/main/nitro/$path"
            val absPath = File(realPath).absolutePath

            // Avoid duplicated includes
            if (absPath in program.includedFiles) {
                return true
            }

            val source = SourceFile.load(realPath)
            program.includedFiles += absPath
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

            val ctx = ParserCtx(
                collector = collector,
                source = source,
                program = program,
                code = LstCode(),
                typeParamMap = mutableMapOf(),
                allowTypeParamCollection = false,
            )
            val astParser = AstParser(ctx)

            ParseTreeWalker().walk(astParser, fileCtx)

            if (finalize) {
                astParser.parserCtx.doTypeChecking()
            }
            return true
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
        parserCtx.processStructDefinition(ctx)
    }

    override fun enterOptionDefinition(ctx: OptionDefinitionContext) {
        parserCtx.processOptionDefinition(ctx)
    }

    override fun enterFunctionDefinition(ctx: FunctionDefinitionContext) {
        parserCtx.processFunctionDefinition(ctx)
    }

    override fun enterConstDefinition(ctx: ConstDefinitionContext) {
        parserCtx.processConstDefinition(ctx)
    }

    override fun enterIncludeDefinition(ctx: IncludeDefinitionContext) {
        parserCtx.processIncludeDefinition(ctx)
    }

    override fun enterUseDefinition(ctx: UseDefinitionContext) {
        TODO("Use-declarations")
    }
}