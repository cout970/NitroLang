package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump
import nitrolang.util.formatItem

class LstStructField(
    val span: Span,
    val name: String,
    val index: Int,
    val typeUsage: LstTypeUsage,
    val ref: FieldRef,
) : Dumpable {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    override fun toString(): String {
        return "LstStructureField {\n" +
                "  name=${formatItem(name)}\n" +
                "  index=${formatItem(index)}\n" +
                "  typeUsage=${formatItem(typeUsage)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", name.dump())
        it.add("index", index.dump())
        it.add("type", typeBox?.dump())
    }
}