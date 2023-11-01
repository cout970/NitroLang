package nitrolang.ast

import com.google.gson.JsonObject
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_REQUIRED
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump
import nitrolang.util.formatItem

class LstConst(
    val span: Span,
    val name: String,
    val path: Path,
    val typeUsage: LstTypeUsage,
    val body: LstCode,
    val annotations: List<LstAnnotation>,
    val ref: ConstRef,
) : Dumpable {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    var checked: Boolean = false
    var codeChecked: Boolean = false
    var isDeadCode: Boolean = false
    val referencedBy = mutableListOf<LstExpression>()
    val fullName: Path get() = createPath(path, name)
    val isExternal: Boolean get() = annotations.any { it.name == ANNOTATION_EXTERN }
    val isRequired: Boolean get() = annotations.any { it.name == ANNOTATION_REQUIRED }

    override fun toString(): String {
        return "LstConst {\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  typeUsage=${formatItem(typeUsage)}\n" +
                "  body=${formatItem(body)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun dump(): JsonObject = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", fullName.dump())
        it.add("type", typeBox?.dump())
        it.add("body", body.dump())
    }
}