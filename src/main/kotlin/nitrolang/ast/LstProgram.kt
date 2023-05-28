package nitrolang.ast

import nitrolang.util.Span
import nitrolang.util.formatItem
import nitrolang.util.formatList

typealias Path = String

class LstProgram {
    val structs = mutableMapOf<DeclRef, LstStruct>()
    val options = mutableMapOf<DeclRef, LstOption>()
    val consts = mutableMapOf<ConstRef, LstConst>()
    val functions = mutableMapOf<FunRef, LstFunction>()

    override fun toString(): String {
        return "LstProgram {\n" +
                "  structs=${formatItem(structs.values)}\n" +
                "  options=${formatItem(options.values)}\n" +
                "  const=${formatItem(consts.values)}\n" +
                "  functions=${formatItem(functions.values)}\n" +
                "}"
    }
}

class LstStruct(
    val span: Span,
    val name: String,
    val path: Path,
    val fields: Map<FieldRef, LstStructureField>,
    val typeParameters: List<TypeParameter>,
    val annotations: List<LstAnnotation>,
    val ref: DeclRef,
) {
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
}

class LstStructureField(
    val span: Span,
    val name: String,
    val index: Int,
    val typeUsage: TypeUsage,
    val ref: FieldRef,
) {
    override fun toString(): String {
        return "LstStructureField {\n" +
                "  name=${formatItem(name)}\n" +
                "  index=${formatItem(index)}\n" +
                "  typeUsage=${formatItem(typeUsage)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }
}

class LstOption(
    val span: Span,
    val name: String,
    val path: Path,
    val items: Map<DeclRef, LstStruct>,
    val typeParameters: List<TypeParameter>,
    val annotations: List<LstAnnotation>,
    val ref: DeclRef,
) {
    override fun toString(): String {
        return "LstOption {\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  items=${formatItem(items.map { it.key to it.value.name })}\n" +
                "  typeParameters=${formatItem(typeParameters)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }
}

class LstConst(
    val span: Span,
    val name: String,
    val path: Path,
    val typeUsage: TypeUsage,
    val body: LstCode,
    val annotations: List<LstAnnotation>,
    val ref: ConstRef,
) {
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
}

class LstFunction(
    val span: Span,
    val name: String,
    val path: Path,
    val hasReceiver: Boolean,
    val params: List<LstFunctionParam>,
    val returnTypeUsage: TypeUsage,
    val typeParameters: List<TypeParameter>,
    val body: LstCode,
    val annotations: List<LstAnnotation>,
    val ref: FunRef
) {
    override fun toString(): String {
        return "LstFunction {\n" +
                "  name=${formatItem(name)}\n" +
                "  path=${formatItem(path)}\n" +
                "  hasReceiver=${formatItem(hasReceiver)}\n" +
                "  params=${formatItem(params)}\n" +
                "  returnTypeUsage=${formatItem(returnTypeUsage)}\n" +
                "  typeParameters=${formatItem(typeParameters)}\n" +
                "  body=${formatItem(body)}\n" +
                "  annotations=${formatItem(annotations)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }
}

class LstLambdaFunction(
    val span: Span,
    val params: List<LstFunctionParam>,
    val returnTypeUsage: TypeUsage,
    val body: LstCode,
    val ref: FunRef
)

data class LstFunctionParam(
    val span: Span,
    val name: String,
    val index: Int,
    val typeUsage: TypeUsage,
)

class LstCode {
    val nodes: MutableList<LstNode> = mutableListOf()

    // Let declarations
    val variables: MutableMap<VarRef, LstVar> = mutableMapOf()

    // Linking loops and break/continue
    var breakNodes: MutableList<LstJumpTo> = mutableListOf()
    var continueNodes: MutableList<LstJumpTo> = mutableListOf()

    // Ref id
    private var counter = 0

    fun nextRef() = Ref(counter++)
    fun nextVarRef() = VarRef(counter++)

    override fun toString(): String {
        return "LstCode {\n" +
                "  variables=${formatItem(variables.values)}\n" +
                "  nodes=${formatItem(nodes)}\n" +
                "}"
    }
}

class LstVar(
    val span: Span,
    val name: String,
    val typeUsage: TypeUsage?,
    val ref: VarRef,
) {
    override fun toString(): String {
        return "LstVar {\n" +
                "  name=${formatItem(name)}\n" +
                "  typeUsage=${formatItem(typeUsage)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }
}

data class TypeUsage(
    val span: Span,
    val name: String,
    val path: Path,
    val sub: List<TypeUsage>,
    val modifier: Modifier,
    val currentPath: Path,
    val typeParameterRef: TypeParameter?
) {
    var resolved = false
//    var type: TypeTree = TypeTree(InvalidType)
//    val fullName: Path get() = createPath(path, name)

    override fun toString(): String {
        val prefix = if (path.isNotEmpty()) "$path::" else ""
        val mod = if (modifier != Modifier.NONE) modifier.toString().lowercase() + " " else ""
        val children = if (sub.isNotEmpty()) "<${sub.joinToString(", ")}>" else ""
        return "$mod$prefix$name$children"
    }

    companion object {
        fun unit() = TypeUsage(
            span = Span.internal(),
            name = "Unit",
            path = "core",
            sub = mutableListOf(),
            modifier = Modifier.NONE,
            typeParameterRef = null,
            currentPath = ""
        )

        fun list(other: TypeUsage) = TypeUsage(
            span = Span.internal(),
            name = "List",
            path = "core",
            sub = mutableListOf(other),
            modifier = Modifier.NONE,
            typeParameterRef = null,
            currentPath = ""
        )

        // TODO
        fun lambda(@Suppress("UNUSED_PARAMETER") lambda: LstLambdaFunction) = unit()
    }

    enum class Modifier {
        MUT, REF, NONE
    }
}

class TypeParameter(
    val span: Span,
    val name: String,
    val ref: TypeRef
) {
    override fun toString(): String = "#$name"
}

class LstAnnotation(
    val span: Span,
    val name: String
) {
    override fun toString(): String = "@$name"
}