package nitrolang.parsing

import nitrolang.ast.*
import nitrolang.gen.MainParser.ModuleDefinitionContext
import nitrolang.typeinference.TypeEnv
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import nitrolang.util.Span
import org.antlr.v4.runtime.ParserRuleContext

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
    val typeParamMap: MutableMap<String, LstTypeParameter>,
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

    fun LstInstruction.asExpr(ctx: LstInstruction): LstExpression? {
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
