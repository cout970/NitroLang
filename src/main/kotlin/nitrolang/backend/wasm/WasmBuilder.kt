package nitrolang.backend.wasm

import nitrolang.ast.*
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_WASM_INLINE
import nitrolang.parsing.ANNOTATION_WASM_NAME
import nitrolang.typeinference.*
import nitrolang.util.Span
import java.lang.Appendable

class WasmBuilder(
    val program: LstProgram,
    val module: WasmModule = WasmModule()
) {
    var lastFuncId = 0
    val funcCache = mutableMapOf<MonoFuncSignature, MonoFunction>()
    val typeIds = mutableMapOf<List<Int>, MonoType>()
    val structIds = mutableMapOf<List<Int>, MonoStruct>()
    val lambdaIds = mutableMapOf<List<Int>, MonoLambda>()
    val optionIds = mutableMapOf<List<Int>, MonoOption>()
    val consts = mutableMapOf<Int, MonoConst>()
    val globalNames = mutableMapOf<String, Int>()
    val funcRefTable = mutableListOf<MonoLambda>()

    var current: MonoFunction? = null

    fun int(span: Span, value: Int) {
        val mono = current!!
        mono.instructions += MonoInt(mono.nextId(), span, value)
    }

    fun call(span: Span, function: String, ctx: MonoCtx) {
        val mono = current!!
        val lstFunc = program.getFunction(function)
        val monoFunction = getMonoFunction(lstFunc, ctx)
        mono.instructions += MonoFunCall(mono.nextId(), span, monoFunction)
    }

    fun dup(span: Span, type: MonoType) {
        val mono = current!!
        val dupAux = MonoVar(mono.locals.size, "dup-${mono.instructions.size}", type)
        mono.locals += dupAux

        mono.instructions += MonoDup(mono.nextId(), span, dupAux, type)
    }

    fun swap(span: Span, top: MonoType, bottom: MonoType) {
        val mono = current!!
        val swapAux0 = MonoVar(mono.locals.size, "swap-0-${mono.instructions.size}", top)
        mono.locals += swapAux0
        val swapAux1 = MonoVar(mono.locals.size, "swap-1-${mono.instructions.size}", bottom)
        mono.locals += swapAux1

        mono.instructions += MonoSwap(mono.nextId(), span, swapAux0, swapAux1)
    }

    fun opcode(span: Span, opcode: String) {
        val mono = current!!
        mono.instructions += MonoOpcode(mono.nextId(), span, opcode)
    }

    fun comment(span: Span, msg: String) {
        val mono = current!!
        mono.instructions += MonoComment(mono.nextId(), span, msg)
    }

    fun typeOf(ref: Ref): MonoType {
        val mono = current!!
        val provider = mono.providers[ref] ?: error("Missing provider for ref $ref")
        return provider.type
    }

    fun consumer(span: Span, ref: Ref) {
        val mono = current!!
        val provider = mono.providers[ref] ?: error("Missing provider for ref $ref")
        mono.instructions += MonoConsumer(mono.nextId(), span, provider)
    }

    fun provider(span: Span, ref: Ref, type: MonoType) {
        val mono = current!!
        mono.instructions += MonoProvider(mono.nextId(), span, type).also { mono.providers[ref] = it }
    }
}

class MonoFuncSignature(
    val funRef: FunRef,
    val fullName: String,
    val paramTypes: List<MonoType>,
    val returnType: MonoType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MonoFuncSignature) return false

        if (funRef.id != other.funRef.id) return false
        if (paramTypes != other.paramTypes) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = funRef.id
        result = 31 * result + paramTypes.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    override fun toString(): String {
        return "${fullName}(${paramTypes.joinToString(", ")}): $returnType"
    }
}

class MonoCtx(
    val generics: Map<LstTypeParameterDef, MonoType> = emptyMap(),
    val parent: MonoCtx? = null,
)

fun WasmBuilder.compile(out: Appendable) {
    // Null value
    module.sections += module.sectionOffset to intToWasmHex(0)
    module.sectionOffset += 4

    // Memory instance
    val memoryAddr = module.sectionOffset
    module.sections += module.sectionOffset to ""
    module.sectionOffset += 16

    compileImports()
    compileConsts()
    compileFunctions()

    // Override section with address where the heap starts
    // @formatter:off
    padOffset()
    val memoryInstance =
        /* type_id  */ intToWasmHex(0) +
        /* capacity */ intToWasmHex(16 * 64 * 1024 - module.sectionOffset) +
        /* len      */ intToWasmHex(0) +
        /* bytes    */ intToWasmHex(module.sectionOffset)
    // @formatter:on
    module.sections[1] = memoryAddr to memoryInstance

    WasmPrint(Output(out)).module(module)
}

fun WasmBuilder.compileImports() {
    for (func in program.functions) {

        val external = func.getAnnotation(ANNOTATION_EXTERN)
        val inline = func.getAnnotation(ANNOTATION_WASM_INLINE)
        if (external == null || inline != null || func.isDeadCode) continue

        val params = mutableListOf<WasmVar>()

        val mono = getMonoFunction(func, MonoCtx())

        for (param in mono.params) {
            val prims = monoTypeToPrimitive(param.type)

            prims.forEachIndexed { i, prim ->
                params += WasmVar(
                    kind = WasmVarKind.Param,
                    name = param.finalName(i),
                    type = prim
                )
            }
        }

        val results = monoTypeToPrimitive(mono.returnType)

        val def = WasmFunctionDefinition(
            name = mono.finalName,
            params = params,
            results = results,
            comment = func.toString()
        )

        val name = external.args["name"] as? ConstString ?: error("Missing external name")
        val lib = external.args["lib"] as? ConstString ?: error("Missing external lib name")

        module.imports += WasmImport(
            module = lib.value,
            function = name.value,
            functionDefinition = def
        )
    }
}

fun WasmBuilder.compileConsts() {
    program.consts.forEach { const ->
        if (const.isDeadCode) return@forEach

        val monoConst = MonoConst(const, typeToMonoType(const.type, MonoCtx()))
        monoConst.offset = module.sectionOffset
        monoConst.size = monoConst.type.stackSize()

        // TODO proper implementation
        val value = when (val last = const.body.nodes.last()) {
            is LstInt -> intToWasmHex(last.value)
            is LstFloat -> floatToWasmHex(last.value)
            is LstBoolean -> intToWasmHex(if (last.value) 1 else 0)
            is LstNothing -> intToWasmHex(0)
            else -> TODO()
        }

        module.sections += monoConst.offset to value
        module.sectionOffset += monoConst.size
        consts[const.ref.id] = monoConst
    }
}

fun WasmBuilder.compileFunctions() {
    getMonoFunction(program.getFunction("memory_alloc_internal"), MonoCtx())
    getMonoFunction(program.getFunction("main"), MonoCtx())

    for (it in funcCache.values) {
        if (it.isExternal) {
            continue
        }
        compileMonoFunction(it)
    }
}

fun WasmBuilder.removeAllGenerics(type: TType): TType {
    return when (type) {
        is TGeneric -> program.typeEnv.find("Int")
        is TComposite -> program.typeEnv.composite(type.base, type.params.map(::removeAllGenerics))
        else -> type
    }
}

fun WasmBuilder.getOrCreateMonoFunction(key: MonoFuncSignature, onInit: (MonoFunction) -> Unit): MonoFunction {
    if (key in funcCache) return funcCache[key]!!

    val nameIndex = globalNames.getOrDefault(key.fullName, 0)
    globalNames[key.fullName] = nameIndex + 1
    val finalName = if (nameIndex == 0) "$${key.fullName}" else "$${key.fullName}-${nameIndex}"

    val mono = MonoFunction(id = lastFuncId++, signature = key, finalName)
    funcCache[key] = mono
    try {
        onInit(mono)
    } catch (e: UnresolvedGenericError) {
        throw RuntimeException(e)
    }
    return mono
}

fun WasmBuilder.getMonoLambdaFunction(lambda: LstLambdaFunction, ctx: MonoCtx): MonoFunction {
    val key = MonoFuncSignature(
        lambda.ref, "lambda-${lambda.ref.id}",
        lambda.params.map { typeToMonoType(it.type, ctx) },
        typeToMonoType(lambda.returnType, ctx)
    )

    return getOrCreateMonoFunction(key) { mono ->
        createMonoLambdaFunction(mono, lambda, key, ctx)
    }
}

fun WasmBuilder.getMonoFunction(func: LstFunction, ctx: MonoCtx): MonoFunction {
    if (func.tag != null) {
        error("Calling invalid function!")
    }

    // External functions cannot be duplicated, generics become plain pointers, represented with Int
    val key = if (func.isExternal) {
        val params = func.params.map {
            typeToMonoType(removeAllGenerics(it.type), ctx)
        }
        val returnType = typeToMonoType(removeAllGenerics(func.returnType), ctx)

        MonoFuncSignature(func.ref, func.fullName, params, returnType)
    } else {
        MonoFuncSignature(
            func.ref, func.fullName,
            func.params.map { typeToMonoType(it.type, ctx) },
            typeToMonoType(func.returnType, ctx)
        )
    }

    return getOrCreateMonoFunction(key) { mono ->
        createMonoFunction(mono, func, key, ctx)
    }
}

fun WasmBuilder.createMonoFunction(
    mono: MonoFunction,
    function: LstFunction,
    signature: MonoFuncSignature,
    ctx: MonoCtx
) {
    val prev = current
    current = mono
    val varMap = mutableMapOf<LstVar, MonoVar>()
    val code = function.body

    for (variable in code.variables.values) {
        val type = typeToMonoType(
            if (function.isExternal) removeAllGenerics(variable.type) else variable.type,
            ctx
        )

        val monoVar = MonoVar(mono.locals.size, variable.name, type)
        varMap[variable] = monoVar

        if (!variable.isParam) {
            mono.locals += monoVar
        }
    }

    mono.name = signature.fullName
    mono.params = function.params.map { varMap[it.variable]!! }
    mono.returnType = signature.returnType
    mono.isExternal = function.isExternal
    mono.annotations += function.annotations

    if (function.isExternal) {
        current = prev
        return
    }

    val providers = mono.providers
    val helperVars = mutableMapOf<Ref, MonoVar>()

    for (inst in code.nodes) {
        processNode(inst, mono, code, ctx, providers, helperVars, varMap)
    }

    for ((index, inst) in mono.instructions.toList().withIndex()) {
        if (inst is MonoProvider) {
            if (inst.consumers.isEmpty()) {
                if (index != mono.instructions.lastIndex || signature.returnType.isNothing()) {
                    mono.instructions[index] = MonoDrop(mono.nextId(), inst.span, inst.type)
                } else {
                    mono.instructions[index] = MonoIgnore(mono.nextId(), inst.span)
                }
                continue
            }

            if (inst.consumers.size == 1 && inst.consumers.first().id == inst.id + 1) {
                mono.instructions[index] = MonoIgnore(mono.nextId(), inst.span)
                continue
            }

            val variable = MonoVar(mono.locals.size, "tmp${inst.id}", inst.type)
            mono.locals += variable
            mono.instructions[index] = MonoStoreVar(mono.nextId(), inst.span, variable)
            inst.consumers.forEach { it.variable = variable }
        }

        if (inst is MonoConsumer) {
            if (inst.variable == null) {
                mono.instructions[index] = MonoIgnore(mono.nextId(), inst.span)
                continue
            }

            mono.instructions[index] = MonoLoadVar(mono.nextId(), inst.span, inst.variable!!)
        }
    }

    if (signature.returnType.isNothing()) {
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    } else if (mono.instructions.lastOrNull() is MonoEnd) {
        val msg = "Added to suppress error, 'end' returns nothing"
        mono.instructions += MonoComment(mono.nextId(), Span.internal(), msg)
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    }

    current = prev
}

fun WasmBuilder.createMonoLambdaFunction(
    mono: MonoFunction,
    function: LstLambdaFunction,
    signature: MonoFuncSignature,
    ctx: MonoCtx
) {
    val prev = current
    current = mono
    val varMap = mutableMapOf<LstVar, MonoVar>()
    val code = function.body

    for (variable in code.variables.values) {
        val type = typeToMonoType(variable.type, ctx)

        val monoVar = MonoVar(mono.locals.size, variable.name, type)
        varMap[variable] = monoVar

        if (!variable.isParam) {
            mono.locals += monoVar
        }
    }

    mono.name = signature.fullName
    mono.params = function.params.map { varMap[it.variable]!! }
    mono.returnType = signature.returnType
    mono.isLambda = true
    module.lambdaLabels += mono.name

    val providers = mono.providers
    val helperVars = mutableMapOf<Ref, MonoVar>()

    for (inst in code.nodes) {
        processNode(inst, mono, code, ctx, providers, helperVars, varMap)
    }

    for ((index, inst) in mono.instructions.toList().withIndex()) {
        if (inst is MonoProvider) {
            if (inst.consumers.isEmpty()) {
                if (index != mono.instructions.lastIndex || signature.returnType.isNothing()) {
                    mono.instructions[index] = MonoDrop(mono.nextId(), inst.span, inst.type)
                } else {
                    mono.instructions[index] = MonoIgnore(mono.nextId(), inst.span)
                }
                continue
            }

            if (inst.consumers.size == 1 && inst.consumers.first().id == inst.id + 1) {
                mono.instructions[index] = MonoIgnore(mono.nextId(), inst.span)
                continue
            }

            val variable = MonoVar(mono.locals.size, "tmp${inst.id}", inst.type)
            mono.locals += variable
            mono.instructions[index] = MonoStoreVar(mono.nextId(), inst.span, variable)
            inst.consumers.forEach { it.variable = variable }
        }

        if (inst is MonoConsumer) {
            if (inst.variable == null) {
                mono.instructions[index] = MonoIgnore(mono.nextId(), inst.span)
                continue
            }

            mono.instructions[index] = MonoLoadVar(mono.nextId(), inst.span, inst.variable!!)
        }
    }

    if (signature.returnType.isNothing()) {
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    } else if (mono.instructions.lastOrNull() is MonoEnd) {
        val msg = "Added to suppress error, 'end' returns nothing"
        mono.instructions += MonoComment(mono.nextId(), Span.internal(), msg)
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    }

    current = prev
}

fun WasmBuilder.processNode(
    inst: LstNode,
    mono: MonoFunction,
    code: LstCode,
    ctx: MonoCtx,
    providers: MutableMap<Ref, MonoProvider>,
    helperVars: MutableMap<Ref, MonoVar>,
    varMap: MutableMap<LstVar, MonoVar>
) {
    when (inst) {
        is LstFunCall -> {
            val function = inst.function ?: error("Function not bound: $inst")

            processFunctionCall(mono, function, inst, ctx)
        }

        is LstComment -> {
            mono.instructions += MonoComment(mono.nextId(), inst.span, inst.comment)
        }

        is LstBoolean -> {
            val type = typeToMonoType(inst.type, ctx)
            mono.instructions += MonoBoolean(mono.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstFloat -> {
            val type = typeToMonoType(inst.type, ctx)
            mono.instructions += MonoFloat(mono.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstInt -> {
            val type = typeToMonoType(inst.type, ctx)
            mono.instructions += MonoInt(mono.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstNothing -> {
            val type = typeToMonoType(inst.type, ctx)
            mono.instructions += MonoNothing(mono.nextId(), inst.span)
            provider(inst.span, inst.ref, type)
        }

        is LstString -> {
            val type = typeToMonoType(inst.type, ctx)
            mono.instructions += MonoString(mono.nextId(), inst.span, inst.value, type)
            provider(inst.span, inst.ref, type)
        }

        is LstSizeOf -> {
            val type = typeToMonoType(inst.type, ctx)
            val sizeType = typeToMonoType(inst.typeUsageBox!!.type, ctx)
            mono.instructions += MonoInt(mono.nextId(), inst.span, sizeType.heapSize())
            provider(inst.span, inst.ref, type)
        }

        is LstAlloc -> {
            val type = typeToMonoType(inst.type, ctx)

            // $1 = memory_alloc_internal(size_of<Type>)
            int(inst.span, type.heapSize())
            call(inst.span, "memory_alloc_internal", ctx)

            // *$1 = type_id_of<Type>
            dup(inst.span, type)
            int(inst.span, type.id)
            opcode(inst.span, "i32.store")

            provider(inst.span, inst.ref, type)
        }

        is LstLambdaInit -> {
            val type = typeToMonoType(inst.type, ctx)
            consumer(inst.span, inst.alloc)
            dup(inst.span, type)
            int(inst.span, PTR_SIZE)
            opcode(inst.span, "i32.add")

            val monoLambda = getMonoLambdaFunction(inst.lambda, ctx)
            val index = module.lambdaLabels.indexOf(monoLambda.name)
            comment(inst.span, "Lambda function \$${monoLambda.name} at index $index in \$lambdas")
            int(inst.span, index)
            opcode(inst.span, "i32.store")

            provider(inst.span, inst.ref, type)
        }

        is LstIsType -> {
            val type = typeToMonoType(inst.type, ctx)
            val typeParam = typeToMonoType(inst.typeUsageBox!!.type, ctx)
            val expr = code.getNode(inst.expr) as LstExpression
            val exprType = typeToMonoType(expr.type, ctx)

            if (typeParam.isStackBased() || exprType.isStackBased()) {
                int(inst.span, if (exprType == typeParam) 1 else 0)
            } else {
                consumer(inst.span, inst.expr)
                int(inst.span, typeParam.id)
                call(inst.span, "is_type_internal", ctx)
            }
            provider(inst.span, inst.ref, type)
        }

        is LstAsType -> {
            val type = typeToMonoType(inst.type, ctx)
            val typeParam = typeToMonoType(inst.typeUsageBox!!.type, ctx)
            val expr = code.getNode(inst.expr) as LstExpression
            val exprType = typeToMonoType(expr.type, ctx)

            if (typeParam.isStackBased() || exprType.isStackBased()) {
                if (exprType == typeParam) {
                    consumer(inst.span, inst.expr)
                } else {
                    call(inst.span, "panic", ctx)
                }
            } else {
                consumer(inst.span, inst.expr)
                int(inst.span, type.id)
                call(inst.span, "as_type_internal", ctx)
            }
            provider(inst.span, inst.ref, type)
        }

        is LstIfChoose -> {
            val type = typeToMonoType(inst.type, ctx)
            consumer(inst.span, inst.ifTrue)
            consumer(inst.span, inst.ifFalse)
            consumer(inst.span, inst.cond)
            mono.instructions += MonoIfChoose(mono.nextId(), inst.span)
            provider(inst.span, inst.ref, type)
        }

        is LstReturn -> {
            consumer(inst.span, inst.expr)
            mono.instructions += MonoReturn(mono.nextId(), inst.span)
        }

        is LstLoadVar -> {
            val type = if (inst.constant != null) {
                val const = inst.constant!!
                val constType = typeToMonoType(const.type, ctx)


                mono.instructions += MonoLoadConst(mono.nextId(), inst.span, consts[const.ref.id]!!)
                constType
            } else {
                val variable = varMap[inst.variable] ?: error("Missing variable: ${inst.variable}")

                mono.instructions += MonoLoadVar(mono.nextId(), inst.span, variable)
                variable.type
            }

            mono.instructions += MonoProvider(mono.nextId(), inst.span, type).also {
                providers[inst.ref] = it
            }
        }

        is LstStoreVar -> {
            val variable = varMap[inst.variable] ?: error("Missing variable: ${inst.variable}")
            consumer(inst.span, inst.expr)
            mono.instructions += MonoStoreVar(mono.nextId(), inst.span, variable)
        }

        is LstLoadField -> {
            val instance = code.getNode(inst.instance) as LstExpression
            val instanceType = typeToMonoType(instance.type, ctx)
            val type = typeToMonoType(inst.type, ctx)

            val struct = instanceType.base as MonoStruct
            val field = struct.fields[inst.field!!.index]

            consumer(inst.span, inst.instance)
            // Field offset
            int(inst.span, field.offset)
            opcode(inst.span, "i32.add")

            mono.instructions += MonoLoadField(mono.nextId(), inst.span, instanceType, field)
            provider(inst.span, inst.ref, type)
        }

        is LstStoreField -> {
            val instance = code.getNode(inst.instance) as LstExpression
            val instanceType = typeToMonoType(instance.type, ctx)

            val struct = instanceType.base as MonoStruct
            val field = struct.fields[inst.field!!.index]

            consumer(inst.span, inst.instance)
            // Field offset
            int(inst.span, field.offset)
            opcode(inst.span, "i32.add")

            consumer(inst.span, inst.expr)
            mono.instructions += MonoStoreField(mono.nextId(), inst.span, instanceType, field)
        }

        is LstIfStart -> {
            consumer(inst.span, inst.cond)
            mono.instructions += MonoIfStart(mono.nextId(), inst.span)
        }

        is LstIfElse -> {
            mono.instructions += MonoIfElse(mono.nextId(), inst.span)
        }

        is LstIfEnd -> {
            mono.instructions += MonoEnd(mono.nextId(), inst.span)
        }

        is LstWhenStart -> {
            val type = typeToMonoType(inst.type, ctx)
            val variable = MonoVar(mono.locals.size, "when-${inst.ref.id}", type)
            helperVars[inst.ref] = variable
            mono.locals += variable
            mono.instructions += MonoBlockStart(mono.nextId(), inst.span)
        }

        is LstWhenJump -> {
            mono.instructions += MonoJump(mono.nextId(), inst.span, inst.block.depth - inst.whenBlock.depth)
        }

        is LstWhenStore -> {
            val variable = helperVars[inst.start.ref]!!
            consumer(inst.span, inst.expr)
            mono.instructions += MonoStoreVar(mono.nextId(), inst.span, variable)
        }

        is LstWhenEnd -> {
            mono.instructions += MonoEnd(mono.nextId(), inst.span)
            mono.instructions += MonoLoadVar(mono.nextId(), inst.span, helperVars[inst.start.ref]!!)
        }

        is LstLoopStart -> {
            mono.instructions += MonoBlockStart(mono.nextId(), inst.span)
            mono.instructions += MonoLoopStart(mono.nextId(), inst.span)
        }

        is LstLoopJump -> {
            mono.instructions += MonoJump(mono.nextId(), inst.span, inst.block.depth - inst.loopBlock!!.depth)
        }

        is LstLoopEnd -> {
            mono.instructions += MonoEnd(mono.nextId(), inst.span)
            mono.instructions += MonoEnd(mono.nextId(), inst.span)
        }
    }
}

fun WasmBuilder.processFunctionCall(mono: MonoFunction, function: LstFunction, inst: LstFunCall, ctx: MonoCtx) {
    val finalType = typeToMonoType(inst.type, ctx)
    val inline = function.getAnnotation(ANNOTATION_WASM_INLINE)
    val opcode = inline?.args?.get("opcode") as? ConstString

    // Just a wasm opcode
    if (opcode != null) {
        inst.arguments.forEach { ref -> consumer(inst.span, ref) }
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, opcode.value)
        provider(inst.span, inst.ref, finalType)
        return
    }

    val external = function.getAnnotation(ANNOTATION_EXTERN)

    // External function in JS
    if (external != null) {
        val lib = (external.args["lib"] as? ConstString)?.value
        val name = (external.args["name"] as? ConstString)?.value

        if (lib == null || name == null) {
            error("Missing attributes on @External annotation")
        }

        inst.arguments.forEach { ref -> consumer(inst.span, ref) }
        mono.instructions += MonoFunCall(mono.nextId(), inst.span, getMonoFunction(function, ctx))
        provider(inst.span, inst.ref, finalType)
        return
    }

    // Lambda dynamic dispatch
    if (function.isIntrinsic && function.fullName == "invoke") {
        val lambdaParam = inst.arguments.first()
        val lambdaType = typeOf(lambdaParam)
        val wasmFuncType = funcTypeToWasm(lambdaType)

        inst.arguments.drop(1).forEach { ref -> consumer(inst.span, ref) }
        consumer(inst.span, lambdaParam)
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "i32.const 4")
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "i32.add")
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "i32.load")
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "call_indirect $wasmFuncType")
        provider(inst.span, inst.ref, finalType)
        return
    }

    // Regular function call
    val replacements = mutableMapOf<LstTypeParameterDef, TType>()
    val generics = mutableMapOf<LstTypeParameterDef, MonoType>()

    repeat(function.typeParameters.size) {
        val tp = function.typeParameters[it]

        replacements[tp] = inst.typeParamsTypes[it].type
    }

    for ((key, type) in replacements) {
        generics[key] = typeToMonoType(program.replaceGenerics(type, replacements), ctx)
    }

    val tag = function.tag

    // Call to tag function, must be replaced by the concrete implementation
    val monoFunction = if (tag != null) {
        val param = function.typeParameters.first()
        val paramType = generics[param] ?: error("No replacement for param: $param")

        val (ty, finalFunction) = findTagImplementation(tag, paramType, function.fullName)
            ?: error("Invalid state, no implementation found for $paramType: $function")

        findReplacements(ty, paramType, generics)

        val newCtx = MonoCtx(generics, ctx)

        getMonoFunction(finalFunction, newCtx)
    } else {
        val newCtx = MonoCtx(generics, ctx)
        getMonoFunction(function, newCtx)
    }

    inst.arguments.forEach { ref -> consumer(inst.span, ref) }
    mono.instructions += MonoFunCall(mono.nextId(), inst.span, monoFunction)
    provider(inst.span, inst.ref, finalType)
}

fun findTagImplementation(tag: LstTag, paramType: MonoType, fullName: String): Pair<TType, LstFunction>? {
    for ((ty, map) in tag.functionImplementations) {
        if (fullName in map && monoTypeMatches(ty, paramType)) {
            return ty to map[fullName]!!
        }
    }

    return null
}

fun monoTypeMatches(a: TType, b: MonoType): Boolean {
    if (a is TGeneric) return true

    if (a is TComposite) {
        if (a.params.size != b.params.size) return false

        repeat(a.params.size) {
            if (!monoTypeMatches(a.params[it], b.params[it])) return false
        }

        if (a.base is TStruct && b.base is MonoStruct && a.base.instance == b.base.instance) return true
        if (a.base is TOption && b.base is MonoOption && a.base.instance == b.base.instance) return true
        if (a.base is TOptionItem && b.base is MonoStruct && a.base.instance == b.base.instance) return true
    }
    return false
}

fun findReplacements(a: TType, b: MonoType, map: MutableMap<LstTypeParameterDef, MonoType>) {
    if (a is TGeneric) {
        map[a.instance] = b
        return
    }

    if (a !is TComposite) error("Invalid type: $a")
    if (a.params.size != b.params.size) return

    repeat(a.params.size) {
        findReplacements(a.params[it], b.params[it], map)
    }
}

fun WasmBuilder.compileMonoFunction(func: MonoFunction) {
    val params = mutableListOf<WasmVar>()

    func.params.map { variable ->
        val prim = monoTypeToPrimitive(variable.type)

        prim.forEachIndexed { index, primitive ->
            params += WasmVar(
                kind = WasmVarKind.Param,
                name = variable.finalName(index),
                type = primitive
            )
        }
    }

    val main = if (func.name == "main") "main" else null

    val wasmName = func.annotations.find { it.name == ANNOTATION_WASM_NAME }
    val wasmNameValue = (wasmName?.args?.get("name") as? ConstString)?.value

    val extern = func.annotations.find { it.name == ANNOTATION_EXTERN }
    val externName = (extern?.args?.get("name") as? ConstString)?.value

    val exportName = main ?: wasmNameValue ?: externName ?: ""

    val wasmFunc = WasmFunction(
        definition = WasmFunctionDefinition(
            name = func.finalName,
            params = params,
            results = listOf(compileTypeToPtr(func.returnType)),
            comment = func.signature.toString(),
            exportName = exportName
        )
    )

    func.locals.forEach { variable ->
        val prim = monoTypeToPrimitive(variable.type)

        prim.forEachIndexed { index, primitive ->
            wasmFunc.locals += WasmVar(
                kind = WasmVarKind.Local,
                name = variable.finalName(index),
                type = primitive
            )
        }
    }

    for (inst in func.instructions) {
        when (inst) {
            is MonoConsumer -> error("MonoConsumer")
            is MonoProvider -> error("MonoProvider")
            is MonoIgnore -> Unit
            is MonoDrop -> {
                val prim = monoTypeToPrimitive(inst.type)
                repeat(prim.size) {
                    wasmFunc.instructions += WasmInst("drop")
                }
            }

            is MonoDup -> {
                val prim = monoTypeToPrimitive(inst.type)
                if (prim.size != 1) error("Dup only works with 1 value")

                wasmFunc.instructions += WasmInst("local.tee ${inst.auxLocal.finalName(0)}")
                wasmFunc.instructions += WasmInst("local.get ${inst.auxLocal.finalName(0)}")
            }

            is MonoSwap -> {
                wasmFunc.instructions += WasmInst("local.set ${inst.auxLocal0.finalName(0)}")
                wasmFunc.instructions += WasmInst("local.set ${inst.auxLocal1.finalName(0)}")
                wasmFunc.instructions += WasmInst("local.get ${inst.auxLocal0.finalName(0)}")
                wasmFunc.instructions += WasmInst("local.get ${inst.auxLocal1.finalName(0)}")
            }

            is MonoComment -> {
                wasmFunc.instructions += WasmInst("; ${inst.msg} ;")
            }

            is MonoBoolean -> {
                wasmFunc.instructions += WasmInst(if (inst.value) "i32.const 1" else "i32.const 0")
            }

            is MonoFloat -> {
                wasmFunc.instructions += WasmInst("f32.const ${inst.value}")
            }

            is MonoInt -> {
                wasmFunc.instructions += WasmInst("i32.const ${inst.value}")
            }

            is MonoNothing -> {
                wasmFunc.instructions += WasmInst("i32.const 0")
            }

            is MonoString -> {
                // Align to 4 bytes for the 32bit length field
                padOffset()

                val bytes = inst.value.encodeToByteArray()
                val start = module.sectionOffset
                // type
                module.sectionOffset += PTR_SIZE
                // size
                module.sectionOffset += PTR_SIZE
                // pointer to string characters in heap or data section
                module.sectionOffset += PTR_SIZE

                // content
                val contentStart = module.sectionOffset
                module.sectionOffset += bytes.size

                val type = intToWasmHex(inst.type.id)
                val size = intToWasmHex(bytes.size)
                val contentsPtr = intToWasmHex(contentStart)
                val contents = bytes.joinToString("") {
                    "\\" + it.toUByte().toString(16).padStart(2, '0')
                }

                module.sections += start to "$type$size$contentsPtr$contents"

                wasmFunc.instructions += WasmInst("i32.const $start")
            }

            is MonoAsType -> TODO()
            is MonoIsType -> TODO()

            is MonoFunCall -> {
                wasmFunc.instructions += WasmInst("call ${inst.function.finalName}")
            }

            is MonoOpcode -> {
                wasmFunc.instructions += WasmInst(inst.opcode)
            }

            is MonoIfChoose -> {
                wasmFunc.instructions += WasmInst("select").also { it.needsWrapping = false }
            }

            is MonoIfStart -> {
                wasmFunc.instructions += WasmInst("if").also { it.needsWrapping = false }
            }

            is MonoIfElse -> {
                wasmFunc.instructions += WasmInst("else").also { it.needsWrapping = false }
            }

            is MonoLoadConst -> {
                val prims = monoTypeToPrimitive(inst.const.type)
                prims.forEachIndexed { index, prim ->
                    wasmFunc.instructions += WasmInst("i32.const ${inst.const.offset + index}")
                    wasmFunc.instructions += WasmInst("$prim.load")
                }
            }

            is MonoLoadVar -> {
                val prim = monoTypeToPrimitive(inst.variable.type)
                repeat(prim.size) {
                    wasmFunc.instructions += WasmInst("local.get ${inst.variable.finalName(it)}")
                }
            }

            is MonoStoreVar -> {
                val prim = monoTypeToPrimitive(inst.variable.type)
                repeat(prim.size) {
                    wasmFunc.instructions += WasmInst("local.set ${inst.variable.finalName(it)}")
                }
            }

            is MonoLoadField -> {
                val instancePrim = monoTypeToPrimitive(inst.instanceType)
                if (instancePrim.size != 1 || instancePrim.first() !== WasmPrimitive.i32) {
                    error("Instance is not a pointer!")
                }
                val fieldPrim = monoTypeToPrimitive(inst.field.type)
                fieldPrim.forEach {
                    wasmFunc.instructions += WasmInst("$it.load")
                }
            }

            is MonoStoreField -> {
                val instancePrim = monoTypeToPrimitive(inst.instanceType)
                if (instancePrim.size != 1 || instancePrim.first() !== WasmPrimitive.i32) {
                    error("Instance is not a pointer!")
                }

                val fieldPrim = monoTypeToPrimitive(inst.field.type)
                if (fieldPrim.size != 1) TODO("Handle multi value fields")

                fieldPrim.forEach { prim ->
                    wasmFunc.instructions += WasmInst("$prim.store").also { it.needsWrapping = false }
                }
            }

            is MonoReturn -> {
                wasmFunc.instructions += WasmInst("return")
            }

            is MonoLoopStart -> {
                wasmFunc.instructions += WasmInst("loop").also { it.needsWrapping = false }
            }

            is MonoBlockStart -> {
                wasmFunc.instructions += WasmInst("block").also { it.needsWrapping = false }
            }

            is MonoJump -> {
                wasmFunc.instructions += WasmInst("br ${inst.depth}")
            }

            is MonoEnd -> {
                wasmFunc.instructions += WasmInst("end").also { it.needsWrapping = false }
            }
        }
    }

    module.functions += wasmFunc
}

private fun intToWasmHex(num: Int): String {
    val hex = num.toString(16).padStart(8, '0')
    val a0 = hex.substring(0, 2)
    val a1 = hex.substring(2, 4)
    val a2 = hex.substring(4, 6)
    val a3 = hex.substring(6, 8)

    return "\\$a3\\$a2\\$a1\\$a0"
}

private fun floatToWasmHex(num: Float): String {
    return intToWasmHex(num.toRawBits())
}

private fun WasmBuilder.padOffset() {
    // Align to 4 bytes for the 32bit length field
    val pad = (PTR_SIZE - module.sectionOffset) % PTR_SIZE
    module.sectionOffset += if (pad < 0) pad + PTR_SIZE else pad
}

fun monoTypeToPrimitive(mono: MonoType): List<WasmPrimitive> {
    return when (mono.base) {
        is MonoOption -> listOf(WasmPrimitive.i32)
        is MonoLambda -> listOf(WasmPrimitive.i32)
        is MonoStruct -> {
            val prim = if (mono.base.instance.fullName == "Float") WasmPrimitive.f32 else WasmPrimitive.i32
            listOf(prim)
        }
    }
}

fun funcTypeToWasm(mono: MonoType): String {
    if (!mono.isFunction() && !mono.isLambda()) error("Invalid type $mono")

    return buildString {
        mono.params.dropLast(1).forEach { p ->
            val prim = monoTypeToPrimitive(p)
            append("(param ")
            prim.forEachIndexed { index, it ->
                append(it)
                if (index != prim.size - 1) append(" ")
            }
            append(")")
            append(" ")
        }

        val prim = monoTypeToPrimitive(mono.params.last())
        append("(result ")
        prim.forEachIndexed { index, it ->
            append(it)
            if (index != prim.size - 1) append(" ")
        }
        append(")")
    }
}

fun LstProgram.replaceGenerics(type: TType, replacements: Map<LstTypeParameterDef, TType>): TType {
    if (type is TGeneric) {
        return replacements[type.instance] ?: type
    }

    if (type !is TComposite) return type

    return typeEnv.composite(
        type.base,
        type.params.map { replaceGenerics(it, replacements) }
    )
}

data class UnresolvedGenericError(
    val msg: String,
    val type: TType,
) : RuntimeException()

fun WasmBuilder.typeToMonoType(type: TType, ctx: MonoCtx): MonoType {
    if (type is TGeneric) {
        val replacement = ctx.generics[type.instance]
        if (replacement != null) return replacement

        if (ctx.parent != null) {
            return typeToMonoType(type, ctx.parent)
        }

        throw UnresolvedGenericError("No valid replacement for generic: $type", type)
    }

    if (type !is TComposite) {
        throw UnresolvedGenericError("Unable to convert type $type to MonoType", type)
    }

    val params = mutableListOf<MonoType>()

    for (param in type.params) {
        params += typeToMonoType(param, ctx)
    }

    val kind: Int
    val base = when (type.base) {
        is TOption -> {
            kind = 0
            getOptionType(type.base.instance, params, ctx)
        }

        is TOptionItem -> {
            kind = 1
            getStructType(type.base.instance, params, ctx)
        }

        is TStruct -> {
            kind = 2
            getStructType(type.base.instance, params, ctx)
        }

        is TLambda -> {
            kind = 3
            getLambdaType(type.base.instance, params)
        }
    }

    val typeSign = mutableListOf(kind, base.id)
    params.forEach { typeSign += it.id }

    if (typeSign !in typeIds) {
        val newType = MonoType(typeIds.size + 1, base, params)
        typeIds[typeSign] = newType
    }

    return typeIds[typeSign]!!
}

fun WasmBuilder.getStructType(struct: LstStruct, params: List<MonoType>, ctx: MonoCtx): MonoStruct {

    val generics = mutableMapOf<LstTypeParameterDef, MonoType>()
    repeat(struct.typeParameters.size) {
        generics[struct.typeParameters[it]] = params[it]
    }

    val sign = mutableListOf(struct.ref.id)
    generics.values.forEach { sign += it.id }

    if (sign !in structIds) {
        val newCtx = MonoCtx(generics, ctx)
        val fields = toMonoStructFields(struct, newCtx)

        var size = PTR_SIZE
        if (struct.parentOption != null) {
            size += PTR_SIZE
        }

        fields.forEach { size += it.size }

        structIds[sign] = MonoStruct(structIds.size + 1, struct, fields, size)
    }

    return structIds[sign]!!
}

fun WasmBuilder.getOptionType(option: LstOption, params: List<MonoType>, ctx: MonoCtx): MonoOption {
    val generics = mutableMapOf<LstTypeParameterDef, MonoType>()
    repeat(option.typeParameters.size) {
        generics[option.typeParameters[it]] = params[it]
    }

    val optionSign = mutableListOf(option.ref.id)
    generics.values.forEach { optionSign += it.id }

    if (optionSign !in optionIds) {
        val monoOption = MonoOption(optionIds.size + 1, option)
        optionIds[optionSign] = monoOption

        val newCtx = MonoCtx(generics, ctx)

        val items = option.items.map { struct ->
            getStructType(struct, params, newCtx)
        }

        monoOption.items = items
        monoOption.size = PTR_SIZE + items.maxOf { it.size }
        items.forEach { it.option = monoOption }
    }

    return optionIds[optionSign]!!
}

fun WasmBuilder.getLambdaType(lambda: LstLambdaFunction, params: List<MonoType>): MonoLambda {
    val sign = mutableListOf(lambda.ref.id)
    params.forEach { sign += it.id }

    if (sign !in lambdaIds) {
        val size = PTR_SIZE * 2
        lambdaIds[sign] = MonoLambda(lambdaIds.size + 1, lambda, size)
    }

    return lambdaIds[sign]!!
}

fun WasmBuilder.toMonoStructFields(struct: LstStruct, ctx: MonoCtx): List<MonoStructField> {
    val fields = mutableListOf<MonoStructField>()
    var offset = PTR_SIZE

    struct.fields.values.forEach { field ->
        val fieldType = typeToMonoType(field.type, ctx)
        val stackSize = fieldType.stackSize()
        fields += MonoStructField(field.name, fieldType, stackSize, offset)
        offset += stackSize
    }

    return fields
}

fun compileTypeToPtr(type: MonoType): WasmPrimitive {
    // Floats are inlined instead of passed by pointer
    if (type.isFloat()) {
        return WasmPrimitive.f32
    }
    // Ints are also inlined, but they are represented the same as pointers
    if (type.isInt()) {
        return WasmPrimitive.i32
    }

    return WasmPrimitive.i32
}

fun TType.isPolymorphic(): Boolean {
    if (this !is TComposite) return false
    for (param in this.params) {
        if (!param.isPolymorphic()) return false
    }
    return true
}
