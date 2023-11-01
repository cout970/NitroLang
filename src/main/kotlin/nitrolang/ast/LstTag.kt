package nitrolang.ast

import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump
import nitrolang.util.formatItem

class LstTag(
    val span: Span,
    val name: String,
    val path: Path,
    val typeParameters: List<LstTypeParameter> = emptyList(),
    val headers: Map<String, LstFunction>,
    val annotations: List<LstAnnotation>,
    val ref: TagRef
) : Dumpable {
    var checked: Boolean = false
    val posibleImplementation: MutableMap<TType, MutableMap<String, LstFunction>> = mutableMapOf()
    val functionImplementations: MutableMap<TType, MutableMap<String, LstFunction>> = mutableMapOf()
    val implementers: MutableSet<TType> = mutableSetOf()
    val fullName: Path get() = createPath(path, name)

    fun addPosibleImpl(name: String, type: TType, func: LstFunction) {
        val map = posibleImplementation.getOrPut(type) { mutableMapOf() }
        if (name in map) error("Conflicting implementation for tag $this, prev: ${map[name]}, post: $func")
        map[name] = func
    }

    fun addImpl(name: String, type: TType, func: LstFunction) {
        val map = functionImplementations.getOrPut(type) { mutableMapOf() }
        map[name] = func
    }

    fun getAnnotation(name: String): LstAnnotation? = annotations.find { it.name == name }

    override fun toString(): String = "tag $fullName"

    fun toDebugString(): String {
        return "LstTag {\n" +
                "  span=${formatItem(span)}\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  typeParameters=${formatItem(typeParameters)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun dump(): JsonObject = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", fullName.dump())
        it.add("type_params", typeParameters.map { p -> p.ref }.dump())
    }
}