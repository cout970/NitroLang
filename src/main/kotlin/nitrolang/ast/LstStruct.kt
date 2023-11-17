package nitrolang.ast

import com.google.gson.JsonObject
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_INTRINSIC
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump
import nitrolang.util.formatItem

class LstStruct(
    val span: Span,
    val name: String,
    val path: Path,
    val fields: Map<FieldRef, LstStructField>,
    val typeParameters: List<LstTypeParameter>,
    val annotations: List<LstAnnotation>,
    val ref: StructRef,
) : Dumpable {
    var checked: Boolean = false
    var parentOption: LstOption? = null
    var isDeadCode: Boolean = false
    val fullName: Path get() = createPath(path, name)
    val isExternal: Boolean get() = getAnnotation(ANNOTATION_EXTERN) != null
    val isIntrinsic: Boolean get() = getAnnotation(ANNOTATION_INTRINSIC) != null
    var enumConstants: Map<String, LstConst>? = null

    fun getAnnotation(name: String): LstAnnotation? = annotations.find { it.name == name }

    override fun toString(): String {
        return "LstStruct {\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  fields=${formatItem(fields.values)}\n" +
                "  typeParameters=${formatItem(typeParameters)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun dump(): JsonObject = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", fullName.dump())
        it.add("fields", fields.dump())
        it.add("type_params", typeParameters.map { p -> p.ref }.dump())
    }
}