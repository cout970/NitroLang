package nitrolang.parsing

import nitrolang.ast.LstCode
import nitrolang.ast.LstProgram
import nitrolang.gen.MainLexer
import nitrolang.gen.MainParser
import nitrolang.gen.MainParserBaseListener
import nitrolang.typeinference.doAllTypeChecking
import nitrolang.util.ErrorCollector
import nitrolang.util.Prof
import nitrolang.util.SourceFile
import nitrolang.util.Span
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File

class AstParser(val parserCtx: ParserCtx) : MainParserBaseListener() {
    companion object {

        fun includeFile(ns: String, path: String, program: LstProgram, prevSource: SourceFile?): Boolean {
            val absPath = if (ns == "core") {
                File("src/main/nitro/core/$path").absolutePath
            } else if (ns in program.includeNamespaces) {
                File(program.includeNamespaces[ns], path).absolutePath
            } else if (ns == "" && prevSource != null) {
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
            Prof.next("parse_file: ${source.path.substringAfterLast('/')}")
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

                    var errorMsg = "Syntax error: $msg"
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
                    throw RuntimeException("Parser error, aborting further processing to avoid printing an excessive amount of errors.\n$collector")
                }
            })

            return parser
        }
    }

    override fun enterStructDefinition(ctx: MainParser.StructDefinitionContext) {
        parserCtx.processStructDefinition(ctx)
    }

    override fun enterOptionDefinition(ctx: MainParser.OptionDefinitionContext) {
        parserCtx.processOptionDefinition(ctx)
    }

    override fun enterFunctionDefinition(ctx: MainParser.FunctionDefinitionContext) {
        parserCtx.processFunctionDefinition(ctx)
    }

    override fun enterConstDefinition(ctx: MainParser.ConstDefinitionContext) {
        parserCtx.processConstDefinition(ctx)
    }

    override fun enterTagDefinition(ctx: MainParser.TagDefinitionContext) {
        parserCtx.processTagDefinition(ctx)
    }

    override fun enterTypeAliasDefinition(ctx: MainParser.TypeAliasDefinitionContext) {
        parserCtx.processTypeAliasDefinition(ctx)
    }

    override fun enterEnumDefinition(ctx: MainParser.EnumDefinitionContext) {
        parserCtx.processEnumDefinition(ctx)
    }

    override fun enterIncludeDefinition(ctx: MainParser.IncludeDefinitionContext) {
        parserCtx.processIncludeDefinition(ctx)
    }

    override fun enterTestDefinition(ctx: MainParser.TestDefinitionContext) {
        parserCtx.processTestDefinition(ctx)
    }

    override fun enterUseDefinition(ctx: MainParser.UseDefinitionContext) {
        TODO("Use-declarations")
    }
}