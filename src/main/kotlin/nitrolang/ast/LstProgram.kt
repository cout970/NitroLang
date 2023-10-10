package nitrolang.ast

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.backend.wasm.ConstString
import nitrolang.backend.wasm.ConstValue
import nitrolang.backend.wasm.MonoFunction
import nitrolang.backend.wasm.MonoType
import nitrolang.parsing.ANNOTATION_WASM_INLINE
import nitrolang.typeinference.TType
import nitrolang.typeinference.TTypeBase
import nitrolang.typeinference.TypeBox
import nitrolang.typeinference.TypeEnv
import nitrolang.util.*

class LstProgram : Dumpable {
    val collector = ErrorCollector()
    val typeEnv = TypeEnv(collector)

    val structs = mutableMapOf<StructRef, LstStruct>()
    val options = mutableMapOf<OptionRef, LstOption>()
    val consts = mutableMapOf<ConstRef, LstConst>()
    val tags = mutableMapOf<TagRef, LstTag>()
    val functions = mutableMapOf<FunRef, LstFunction>()
    val definedNames = mutableMapOf<Path, Span>()

    val includedFiles = mutableSetOf<String>()

    private var lastStruct = 0
    private var lastOption = 0
    private var lastConst = 0
    private var lastFunction = 0
    private var lastTag = 0
    private var lastTypeParam = 0
    private var lastUnresolvedType = 0
    private var lastField = 0

    fun getFunction(fullName: String): LstFunction {
        return functions.values.find { it.fullName == fullName } ?: error("Function '$fullName' not found")
    }

    override fun toString(): String {
        return "LstProgram {\n" +
                "  structs=${formatItem(structs.values)}\n" +
                "  options=${formatItem(options.values)}\n" +
                "  const=${formatItem(consts.values)}\n" +
                "  functions=${formatItem(functions.values)}\n" +
                "}"
    }

    override fun dump(): JsonObject = JsonObject().apply {
        add("structs", structs.values.filter { !it.isExternal && !it.isDeadCode }.dump())
        add("options", options.values.filter { !it.isExternal && !it.isDeadCode }.dump())
        add("consts", consts.values.filter { !it.isExternal && !it.isDeadCode }.dump())
        add("functions", functions.values.filter { !it.isExternal && !it.isDeadCode }.dump())
    }

    fun nextStructRef(): StructRef = StructRef(lastStruct++)
    fun nextOptionRef(): OptionRef = OptionRef(lastOption++)
    fun nextFieldRef(): FieldRef = FieldRef(lastField++)
    fun nextConstRef(): ConstRef = ConstRef(lastConst++)
    fun nextFunctionRef(): FunRef = FunRef(lastFunction++)
    fun nextTagRef(): TagRef = TagRef(lastTag++)
    fun nextTypeParamRef(): TypeParamRef = TypeParamRef(lastTypeParam++)
    fun nextUnresolvedTypeRef(): UnresolvedTypeRef = UnresolvedTypeRef(lastUnresolvedType++)

    fun resetCounters(offset: Int) {
        lastStruct = offset
        lastOption = offset
        lastConst = offset
        lastFunction = offset
        lastTag = offset
        lastTypeParam = offset
        lastUnresolvedType = offset
        lastField = offset
    }

    companion object {
        private val GSON = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create()

        fun LstProgram.toJson(): String = GSON.toJson(this.dump())
    }
}

class LstStruct(
    val span: Span,
    val name: String,
    val path: Path,
    val fields: Map<FieldRef, LstStructField>,
    val typeParameters: List<LstTypeParameterDef>,
    val annotations: List<LstAnnotation>,
    val ref: StructRef,
) : Dumpable {
    var checked: Boolean = false
    var parentOption: OptionRef? = null
    var isDeadCode: Boolean = false
    val fullName: Path get() = createPath(path, name)
    val isExternal: Boolean get() = getAnnotation(ANNOTATION_EXTERN) != null

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

class LstStructField(
    val span: Span,
    val name: String,
    val index: Int,
    val typeUsage: TypeUsage,
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

class LstOption(
    val span: Span,
    val name: String,
    val path: Path,
    val itemsRef: List<StructRef>,
    val items: List<LstStruct>,
    val typeParameters: List<LstTypeParameterDef>,
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

class LstConst(
    val span: Span,
    val name: String,
    val path: Path,
    val typeUsage: TypeUsage,
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

class LstTag(
    val span: Span,
    val name: String,
    val path: Path,
    val typeParameters: List<LstTypeParameterDef> = emptyList(),
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

class LstFunction(
    val span: Span,
    val name: String,
    val path: Path,
    val hasReceiver: Boolean,
    val params: List<LstFunctionParam>,
    val returnTypeUsage: TypeUsage,
    val typeParameters: List<LstTypeParameterDef>,
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
    val isInline: Boolean get() = getAnnotation(ANNOTATION_WASM_INLINE) != null

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
) : Dumpable {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    var variable: LstVar? = null

    override fun dump(): JsonElement = JsonObject().also {
        it.add("name", name.dump())
        it.add("index", index.dump())
        it.add("type", typeBox?.dump())
    }
}

class LstCode : Dumpable {
    // Ref id
    private var counter = 0
    private var lastBlock = 0

    val nodes: MutableList<LstNode> = ArrayDeque()

    var returnTypeBox: TypeBox? = null
    var returnType: TType
        get() = returnTypeBox!!.type
        set(v) {
            returnTypeBox!!.type = v
        }

    var lastExpression: Ref? = null

    // Block nesting
    val rootBlock: LstNodeBlock = LstNodeBlock(null, lastBlock++)
    var currentBlock: LstNodeBlock = rootBlock

    // Let declarations
    val variables: MutableMap<VarRef, LstVar> = mutableMapOf()

    // Linking loops and break/continue
    var breakNodes: MutableList<LstLoopJump> = mutableListOf()
    var continueNodes: MutableList<LstLoopJump> = mutableListOf()

    fun createBlock(): LstNodeBlock = LstNodeBlock(currentBlock, lastBlock++)

    fun currentRef() = Ref(counter)

    fun nextRef() = Ref(counter++)

    fun nextVarRef() = LocalVarRef(counter++)

    fun getNode(ref: Ref): LstNode = nodes.find { it.ref == ref } ?: error("Ref not found!")

    override fun dump(): JsonElement = JsonObject().also {
        it.add("variables", variables.dump())
        it.add("nodes", nodes.dump())
    }

    fun toPrettyString(): String {
        return "LstCode {\n" +
                "  variables=${formatItem(variables.values)}\n" +
                "  nodes=${formatItem(nodes)}\n" +
                "}"
    }

    override fun toString(): String {
        // Printing all the variables and nodes slows down the debugger
        return "LstCode ()"
    }
}

class LstVar(
    val span: Span,
    val name: String,
    val block: LstNodeBlock,
    val typeUsage: TypeUsage?,
    val validAfter: Ref,
    val ref: VarRef,
) : Dumpable {
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
        return "LstVar {\n" +
                "  name=${formatItem(name)}\n" +
                "  typeUsage=${formatItem(typeUsage)}\n" +
                "  validAfter=${formatItem(validAfter)}\n" +
                "  ref=${formatItem(ref)}\n" +
                "}"
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("name", name.dump())
        it.add("type", typeBox?.dump())
        it.add("block", block.dump())
        it.add("valid_after", validAfter.dump())
    }
}

data class TypeUsage(
    val span: Span,
    val name: String,
    val path: Path,
    val sub: List<TypeUsage>,
    val modifier: Modifier,
    val currentPath: Path,
    val typeParameter: LstTypeParameterDef?,
    val unresolvedTypeRef: UnresolvedTypeRef? = null,
) : Dumpable {
    val fullName: Path get() = createPath(path, name)

    override fun toString(): String {
        val prefix = if (path.isNotEmpty()) "$path::" else ""
        val mod = if (modifier != Modifier.NONE) modifier.toString().lowercase() + " " else ""
        val children = if (sub.isNotEmpty()) "<${sub.joinToString(", ")}>" else ""
        return "$mod$prefix$name$children"
    }

    override fun dump(): JsonElement = this.toString().dump()

    companion object {
        fun simple(name: String) = TypeUsage(
            span = Span.internal(),
            name = name,
            path = "",
            sub = mutableListOf(),
            modifier = Modifier.NONE,
            typeParameter = null,
            currentPath = ""
        )

        fun nothing() = simple("Nothing")

        fun int() = simple("Int")

        fun float() = simple("Float")

        fun string() = simple("String")

        fun unresolved(unresolvedTypeRef: UnresolvedTypeRef) = TypeUsage(
            span = Span.internal(),
            name = "<unresolved>",
            path = "",
            sub = mutableListOf(),
            modifier = Modifier.NONE,
            typeParameter = null,
            currentPath = "",
            unresolvedTypeRef = unresolvedTypeRef,
        )

        fun typeParam(param: LstTypeParameterDef) = TypeUsage(
            span = Span.internal(),
            name = param.name,
            path = "",
            sub = mutableListOf(),
            modifier = Modifier.NONE,
            typeParameter = param,
            currentPath = ""
        )

        fun list(other: TypeUsage) = TypeUsage(
            span = Span.internal(),
            name = "List",
            path = "",
            sub = mutableListOf(other),
            modifier = Modifier.NONE,
            typeParameter = null,
            currentPath = ""
        )

        fun map(key: TypeUsage, value: TypeUsage) = TypeUsage(
            span = Span.internal(),
            name = "Map",
            path = "",
            sub = mutableListOf(key, value),
            modifier = Modifier.NONE,
            typeParameter = null,
            currentPath = ""
        )

        fun set(other: TypeUsage) = TypeUsage(
            span = Span.internal(),
            name = "Set",
            path = "",
            sub = mutableListOf(other),
            modifier = Modifier.NONE,
            typeParameter = null,
            currentPath = ""
        )

        // TODO
        fun lambda(@Suppress("UNUSED_PARAMETER") lambda: LstLambdaFunction) = nothing()
    }

    enum class Modifier {
        MUT, REF, NONE
    }
}

class LstTypeParameterDef(
    val span: Span,
    val name: String,
    val ref: TypeParamRef,
    val bounds: List<TypeUsage>,
) : Dumpable {
    val requiredTags = mutableListOf<LstTag>()

    override fun toString(): String = "#$name"

    override fun dump(): JsonElement = "$name#${ref.id}".dump()
}

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