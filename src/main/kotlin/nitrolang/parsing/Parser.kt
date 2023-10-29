package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.gen.MainLexer
import nitrolang.gen.MainParser
import nitrolang.gen.MainParser.*
import nitrolang.gen.MainParserBaseListener
import nitrolang.typeinference.TypeEnv
import nitrolang.typeinference.doAllTypeChecking
import nitrolang.util.ErrorCollector
import nitrolang.util.Prof
import nitrolang.util.SourceFile
import nitrolang.util.Span
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File

const val SELF_NAME = "this"
const val ANNOTATION_EXTERN = "Extern"
const val ANNOTATION_INTRINSIC = "Intrinsic"
const val ANNOTATION_REQUIRED = "Required"
const val ANNOTATION_WASM_NAME = "WasmName"
const val ANNOTATION_WASM_INLINE = "WasmInline"
const val ANNOTATION_VALUE_TYPE = "ValueType"
const val MAIN_FUNCTION_NAME = "main"

data class ParserCtx(
    val source: SourceFile,
    val program: LstProgram,
    val typeParamMap: MutableMap<String, LstTypeParameterDef>,
    var allowTypeParamCollection: Boolean,
    var code: LstCode,
) {
    val collector: ErrorCollector = program.collector
    val typeEnv: TypeEnv = program.typeEnv

    var currentTagName: String? = null
    var currentTag: LstTag? = null
    var allowDefer: Boolean = true

    fun ParserRuleContext.span(): Span {
        return Span(start.startIndex, stop.stopIndex, this@ParserCtx.source)
    }

    fun LstNode.asExpr(ctx: LstNode): LstExpression? {
        if (this !is LstExpression) {
            collector.report("Invalid ref: not an expression $this", ctx.span)
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

        fun includeFile(ns: String, path: String, program: LstProgram, prevSource: SourceFile?): Boolean {
            val absPath = if (ns == "core") {
                File("src/main/nitro/core/$path").absolutePath
            } else if (prevSource != null) {
                File(prevSource.path).absoluteFile.parent + "/$path"
            } else {
                error("Missing source!")
            }

            // Avoid duplicated includes
            if (absPath in program.includedFiles) {
                return true
            }

            val source = SourceFile.load(absPath)
            program.includedFiles += absPath
            return parseFile(source, program, false)
        }

        fun parseFile(
            source: SourceFile,
            program: LstProgram,
            finalize: Boolean = true
        ): Boolean {
            Prof.start("create_parser")
            val parser = createParser(source, program.collector)
            Prof.next("parse_file")
            val fileCtx = parser.parseFile()

            if (program.collector.isNotEmpty()) {
                Prof.end()
                return false
            }

            if (fileCtx.exception != null) {
                program.collector.report("Parse exception: ${fileCtx.exception}", Span.internal())
                Prof.end()
                return false
            }

            Prof.next("walk_tree")
            val ctx = ParserCtx(
                source = source,
                program = program,
                code = LstCode(),
                typeParamMap = mutableMapOf(),
                allowTypeParamCollection = false,
            )
            val astParser = AstParser(ctx)

            ParseTreeWalker().walk(astParser, fileCtx)

            if (finalize) {
                Prof.next("type_checking")
                astParser.parserCtx.doAllTypeChecking()
            }
            Prof.end()
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

            parser.removeErrorListeners()

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

                    var errorMsg =  "Syntax error: $msg"
                    var span = Span(
                        recognizer.currentToken.startIndex,
                        recognizer.currentToken.stopIndex,
                        source
                    )

                    if (offendingSymbol is CommonToken) {
                        if (msg.startsWith("no viable alternative at input")) {
                            errorMsg = "Unexpected token '${offendingSymbol.text}'"
                        }

                        span = Span(
                            offendingSymbol.startIndex,
                            offendingSymbol.stopIndex,
                            source
                        )
                    }

                    collector.report(errorMsg, span)
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

    override fun enterTagDefinition(ctx: TagDefinitionContext) {
        parserCtx.processTagDefinition(ctx)
    }

    override fun enterIncludeDefinition(ctx: IncludeDefinitionContext) {
        parserCtx.processIncludeDefinition(ctx)
    }

    override fun enterUseDefinition(ctx: UseDefinitionContext) {
        TODO("Use-declarations")
    }
}