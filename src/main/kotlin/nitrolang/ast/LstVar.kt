package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump

class LstVar(
    val span: Span,
    val name: String,
    val block: LstBlock,
    val typeUsage: LstTypeUsage?,
    val definedBy: Ref,
    val ref: VarRef,
    var definedIn: LstCode
) : Dumpable {
    var isUpValue: Boolean = false
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    var isParam: Boolean = false
    val referencedBy = mutableListOf<LstExpression>()
    val finalName: String get() = "$$ref"

    override fun toString(): String {
        return "let $name: $typeUsage"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", name.dump())
        it.add("type", typeBox?.dump())
        it.add("block", block.dump())
        it.add("defined_by", definedBy.dump())
    }
}