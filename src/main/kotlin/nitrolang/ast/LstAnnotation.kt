package nitrolang.ast

import com.google.gson.JsonElement
import nitrolang.backend.ConstString
import nitrolang.backend.ConstValue
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump

class LstAnnotation(
    val span: Span,
    val name: String,
    val args: Map<String, ConstValue> = emptyMap(),
) : Dumpable {
    override fun toString(): String {
        if (args.isNotEmpty()) return "@$name $[${args.entries.joinToString(", ") { "${it.key}: ${it.value}" }}]"
        return "@$name"
    }

    override fun dump(): JsonElement = toString().dump()

    companion object {
        fun extern(lib: String, name: String): LstAnnotation {
            return of(ANNOTATION_EXTERN, "lib" to ConstString(lib), "name" to ConstString(name))
        }

        fun of(name: String, vararg args: Pair<String, ConstValue>): LstAnnotation {
            return LstAnnotation(
                span = Span.internal(),
                name = name,
                args = mutableMapOf(*args)
            )
        }
    }
}