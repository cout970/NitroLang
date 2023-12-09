package nitrolang.ast

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import nitrolang.CompilerOptions
import nitrolang.typeinference.TComposite
import nitrolang.typeinference.TGeneric
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeEnv
import nitrolang.util.*

class LstProgram(var compilerOptions: CompilerOptions) : Dumpable {
    val collector = ErrorCollector()
    val typeEnv = TypeEnv(collector)

    val structs = mutableListOf<LstStruct>()
    val options = mutableListOf<LstOption>()
    val typeAliases = mutableListOf<LstTypeAlias>()
    val consts = mutableListOf<LstConst>()
    val tags = mutableListOf<LstTag>()
    val functions = mutableListOf<LstFunction>()
    val lambdaFunctions = mutableListOf<LstLambdaFunction>()
    val definedNames = mutableMapOf<Path, Span>()

    val includedFiles = mutableSetOf<String>()
    var usesAlloc: Boolean = false

    private var lastStruct = 0
    private var lastOption = 0
    private var lastConst = 0
    private var lastFunction = 0
    private var lastTag = 0
    private var lastTypeParam = 0
    private var lastField = 0

    fun getFunction(fullName: String): LstFunction {
        return functions.find { it.fullName == fullName } ?: error("Function '$fullName' not found")
    }

    override fun toString(): String {
        return "LstProgram {\n" +
                "  structs=${formatItem(structs)}\n" +
                "  options=${formatItem(options)}\n" +
                "  const=${formatItem(consts)}\n" +
                "  functions=${formatItem(functions)}\n" +
                "}"
    }

    override fun dump(): JsonObject = JsonObject().apply {
        add("structs", structs.filter { !it.isExternal && !it.isDeadCode }.dump())
        add("options", options.filter { !it.isExternal && !it.isDeadCode }.dump())
        add("consts", consts.filter { !it.isExternal && !it.isDeadCode }.dump())
        add("functions", functions.filter { !it.isExternal && !it.isDeadCode }.dump())
    }

    fun nextStructRef(): StructRef = StructRef(lastStruct++)
    fun nextOptionRef(): OptionRef = OptionRef(lastOption++)
    fun nextFieldRef(): FieldRef = FieldRef(lastField++)
    fun nextConstRef(): ConstRef = ConstRef(lastConst++)
    fun nextFunctionRef(): FunRef = FunRef(lastFunction++)
    fun nextTagRef(): TagRef = TagRef(lastTag++)
    fun nextTypeParamRef(): TypeParamRef = TypeParamRef(lastTypeParam++)

    fun resetCounters(offset: Int) {
        lastStruct = offset
        lastOption = offset
        lastConst = offset
        lastFunction = offset
        lastTag = offset
        lastTypeParam = offset
        lastField = offset
    }

    fun replaceGenerics(type: TType, replacements: Map<LstTypeParameter, TType>): TType {
        if (type is TGeneric) {
            return replacements[type.instance] ?: type
        }

        if (type !is TComposite) return type

        return typeEnv.composite(
            type.base,
            type.params.map { replaceGenerics(it, replacements) }
        )
    }

    fun removeAllGenerics(type: TType): TType {
        return when (type) {
            is TGeneric -> typeEnv.find("Int")
            is TComposite -> typeEnv.composite(type.base, type.params.map(::removeAllGenerics))
            else -> type
        }
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

