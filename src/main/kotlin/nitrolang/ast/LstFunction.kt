package nitrolang.ast

import com.google.gson.JsonObject
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_INTRINSIC
import nitrolang.parsing.ANNOTATION_REQUIRED
import nitrolang.parsing.ANNOTATION_WASM_INLINE
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump
import nitrolang.util.formatItem

class LstFunction(
    val span: Span,
    val name: String,
    val path: Path,
    val hasReceiver: Boolean,
    val params: List<LstFunctionParam>,
    val returnTypeUsage: LstTypeUsage,
    val typeParameters: List<LstTypeParameter>,
    val body: LstCode,
    val annotations: List<LstAnnotation>,
    val ref: FunRef
) : Dumpable {
    var tag: LstTag? = null
    var hasExpressionBody: Boolean = false

    var returnTypeBox: TypeBox? = null
    var returnType: TType
        get() = returnTypeBox!!.type
        set(v) {
            returnTypeBox!!.type = v
        }

    var checked: Boolean = false
    var codeChecked: Boolean = false
    var isDeadCode: Boolean = false
    val fullName: Path get() = createPath(path, name)
    val isExternal: Boolean get() = getAnnotation(ANNOTATION_EXTERN) != null
    val isRequired: Boolean get() = getAnnotation(ANNOTATION_REQUIRED) != null
    val isIntrinsic: Boolean get() = getAnnotation(ANNOTATION_INTRINSIC) != null
    val isInline: Boolean get() = getAnnotation(ANNOTATION_WASM_INLINE) != null
    val omitBody: Boolean get() = isExternal || isIntrinsic

    val finalName: String = ref.toString()

    fun getAnnotation(name: String): LstAnnotation? = annotations.find { it.name == name }

    fun toDebugString(): String {
        return "LstFunction {\n" +
                "  span=${formatItem(span)}\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  hasReceiver=${formatItem(hasReceiver)}\n" +
                "  params=${formatItem(params)}\n" +
                "  returnType=${formatItem(returnTypeBox)} (${formatItem(returnTypeUsage)})\n" +
                "  typeParameters=${formatItem(typeParameters)}\n" +
                "  body=${formatItem(body)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun toString(): String {
        if (returnTypeBox == null) return "function($fullName)"
        return "$fullName(${params.joinToString(", ") { it.type.toString() }}): $returnType"
    }

    override fun dump(): JsonObject = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", fullName.dump())
        it.add("has_receiver", hasReceiver.dump())
        it.add("type_params", typeParameters.map { p -> p.ref }.dump())
        it.add("params", params.dump())
        it.add("return_type", returnTypeBox?.dump())
        it.add("body", body.dump())
    }
}