package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump
import nitrolang.util.formatItem

class LstOption(
    val span: Span,
    val name: String,
    val path: Path,
    val itemsRef: List<StructRef>,
    val items: List<LstStruct>,
    val typeParameters: List<LstTypeParameter>,
    val annotations: List<LstAnnotation>,
    val ref: OptionRef,
) : Dumpable {
    var checked: Boolean = false
    var isDeadCode: Boolean = false
    val fullName: Path get() = createPath(path, name)
    val isExternal: Boolean get() = annotations.any { it.name == ANNOTATION_EXTERN }

    override fun toString(): String {
        return "LstOption {\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  items=${formatItem(itemsRef)}\n" +
                "  typeParameters=${formatItem(typeParameters)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", fullName.dump())
        it.add("items", itemsRef.dump())
        it.add("type_params", typeParameters.map { p -> p.ref }.dump())
    }
}