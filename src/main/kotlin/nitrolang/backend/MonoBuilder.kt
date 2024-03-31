package nitrolang.backend

import nitrolang.ast.*
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_WASM_INLINE

interface IBuilder {
    fun init(root: MonoBuilder)
    fun finish()
    fun compileImport(func: LstFunction, mono: MonoFunction, name: ConstString, lib: ConstString)
    fun preCompileConst(const: LstConst, mono: MonoConst)
    fun compileConst(const: LstConst, mono: MonoConst)
    fun compileFunction(lst: LstFunction?, func: MonoFunction)
    fun onCompileLambda(mono: MonoFunction)
    fun onCompileFunctionCall(
        mono: MonoCode,
        function: LstFunction,
        inst: LstFunCall,
        finalType: MonoType
    ): Boolean
}

class MonoBuilder(val program: LstProgram, val builder: IBuilder) {
    var lastFuncId = 0
    val funcCache = mutableMapOf<MonoFuncSignature, MonoFunction>()
    val typeIds = mutableMapOf<List<Int>, MonoType>()
    val structIds = mutableMapOf<List<Int>, MonoStruct>()
    val lambdaIds = mutableMapOf<List<Int>, MonoLambda>()
    val optionIds = mutableMapOf<List<Int>, MonoOption>()
    val consts = mutableMapOf<Int, MonoConst>()
    val globalNames = mutableMapOf<String, Int>()
    val funcRefTable = mutableListOf<MonoLambda>()

    var initMemoryCopyInternal = false
    var initAsTypeInternal = false
    var initIsTypeInternal = false
    var current: MonoCode? = null

    fun compileAll() {
        builder.init(this)
        compileConsts()
        compileFunctions()
        compileImports()
        builder.finish()
    }

    fun compileImports() {
        for (func in program.functions) {
            val external = func.getAnnotation(ANNOTATION_EXTERN)
            val inline = func.getAnnotation(ANNOTATION_WASM_INLINE)
            if (external == null || inline != null || func.isDeadCode) continue

            val name = external.args["name"] as? ConstString ?: error("Missing external name")
            val lib = external.args["lib"] as? ConstString ?: error("Missing external lib name")

            if (func.autogenerate) continue

            val mono = getMonoFunction(func, MonoCtx())

            builder.compileImport(func, mono, name, lib)
        }
    }

    fun compileConsts() {
        val aux = mutableListOf<Pair<LstConst, MonoConst>>()
        program.consts.forEach { const ->
            if (const.isDeadCode) return@forEach
            val ctx = MonoCtx()

            val type = typeToMonoType(const.type, ctx)

            val monoConst = MonoConst(const, type)
            monoConst.code = MonoCode(const.body)

            consts[const.ref.id] = monoConst
            aux += const to monoConst
            builder.preCompileConst(const, monoConst)

            if (!type.isIntrinsic() && !type.isEncodedInRef()) {
                if (!initMemoryCopyInternal) {
                    initMemoryCopyInternal = true
                    getMonoFunction(program.getFunction("memory_copy_internal"), MonoCtx())
                }
            }
        }

        aux.forEach { (const, monoConst) ->
            this.current = monoConst.code
            processCode(monoConst.code, MonoCtx())
            builder.compileConst(const, monoConst)
        }
    }

    fun compileFunctions() {
        val root = MonoCtx()
        program.functions.filter { it.isRequired }.forEach {
            getMonoFunction(it, root)
        }

        if (program.compilerOptions.runTests) {
            getMonoFunction(program.getFunction("run_test"), root)
            program.functions.filter { it.isTest }.forEach {
                getMonoFunction(it, root)
            }
        } else {
            getMonoFunction(program.getFunction("main"), root)
        }

        if (program.usesAlloc) {
            getMonoFunction(program.getFunction("memory_alloc_internal"), root)
        }

        for ((key, func) in funcCache) {
            if (func.code.isExternal) {
                continue
            }
            builder.compileFunction(key.function, func)
        }
    }

}