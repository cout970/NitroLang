package nitrolang.ast

import com.google.gson.JsonElement
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump

class LstTypeParameter(
    val span: Span,
    val name: String,
    val ref: TypeParamRef,
    val bounds: List<LstTypeUsage>,
) : Dumpable {
    val requiredTags = mutableListOf<LstTag>()

    override fun toString(): String = "#$name"

    override fun dump(): JsonElement = "$name#${ref.id}".dump()
}