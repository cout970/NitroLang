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
    val optionItemIds = mutableMapOf<List<Int>, MonoOptionItem>()
    val optionIds = mutableMapOf<List<Int>, MonoOption>()
    val consts = mutableMapOf<Int, MonoConst>()
    val globalNames = mutableMapOf<String, Int>()

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
    val function: LstFunction,
    val paramTypes: List<MonoType>,
    val returnType: MonoType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MonoFuncSignature) return false

        if (function.ref.id != other.function.ref.id) return false
        if (paramTypes != other.paramTypes) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = function.ref.id
        result = 31 * result + paramTypes.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    override fun toString(): String {
        return "${function.fullName}: (${paramTypes.joinToString(",")})->$returnType"
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
    for (func in program.functions.values) {

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
            comment = func.fullName
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
    program.consts.values.forEach { const ->
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
        if (it.sourceFunction.isExternal) {
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

        MonoFuncSignature(func, params, returnType)
    } else {
        MonoFuncSignature(
            func,
            func.params.map { typeToMonoType(it.type, ctx) },
            typeToMonoType(func.returnType, ctx)
        )
    }

    if (key in funcCache) return funcCache[key]!!

    val nameIndex = globalNames.getOrDefault(func.name, 0)
    globalNames[func.name] = nameIndex + 1
    val finalName = if (nameIndex == 0) "$${func.name}" else "$${func.name}-${nameIndex}"

    val mono = MonoFunction(id = lastFuncId++, signature = key, finalName)
    funcCache[key] = mono
    try {
        createMonoFunction(mono, key, ctx)
    } catch (e: UnresolvedGenericError) {
        e.function = func
        throw e
    }
    return mono
}

fun WasmBuilder.createMonoFunction(mono: MonoFunction, signature: MonoFuncSignature, ctx: MonoCtx) {
    val prev = current
    current = mono
    val varMap = mutableMapOf<LstVar, MonoVar>()
    val code = signature.function.body

    for (variable in code.variables.values) {
        val type = typeToMonoType(
            if (signature.function.isExternal) removeAllGenerics(variable.type) else variable.type,
            ctx
        )

        val monoVar = MonoVar(mono.locals.size, variable.name, type)
        varMap[variable] = monoVar

        if (!variable.isParam) {
            mono.locals += monoVar
        }
    }

    mono.name = signature.function.fullName
    mono.params = signature.function.params.map { varMap[it.variable]!! }
    mono.returnType = signature.returnType
    mono.sourceFunction = signature.function

    if (signature.function.isExternal) {
        current = prev
        return
    }

    val providers = mono.providers
    val helperVars = mutableMapOf<Ref, MonoVar>()

    for (inst in code.nodes) {
        when (inst) {
            is LstFunCall -> {
                val function = inst.function ?: error("Function not bound: $inst")

                inst.arguments.forEach { ref ->
                    consumer(inst.span, ref)
                }

                val finalType = typeToMonoType(inst.type, ctx)
                val inline = function.getAnnotation(ANNOTATION_WASM_INLINE)
                val opcode = inline?.args?.get("opcode") as? ConstString

                if (opcode != null) {
                    mono.instructions += MonoOpcode(mono.nextId(), inst.span, opcode.value)
                    provider(inst.span, inst.ref, finalType)
                    continue
                }

                val external = function.getAnnotation(ANNOTATION_EXTERN)

                if (external != null) {
                    val lib = (external.args["lib"] as? ConstString)?.value
                    val name = (external.args["name"] as? ConstString)?.value

                    if (lib == null || name == null) {
                        error("Missing attributes on @External annotation")
                    }

                    mono.instructions += MonoFunCall(mono.nextId(), inst.span, getMonoFunction(function, ctx))
                    provider(inst.span, inst.ref, finalType)
                    continue
                }

                val replacements = mutableMapOf<LstTypeParameterDef, TType>()

                repeat(function.typeParameters.size) {
                    val tp = function.typeParameters[it]

                    replacements[tp] = inst.typeParamsTypes[it].type
                }

                val generics = mutableMapOf<LstTypeParameterDef, MonoType>()

                for ((key, type) in replacements) {
                    generics[key] = typeToMonoType(program.replaceGenerics(type, replacements), ctx)
                }
                val newCtx = MonoCtx(generics, ctx)
                val tag = function.tag

                // Call to tag function, must be replaced by the concrete implementation
                val monoFunction = if (tag != null) {
                    val param = function.typeParameters.first()
                    val paramType = generics[param] ?: error("No replacement for param: $param")
                    var implFunc: LstFunction? = null

                    for ((ty, map) in tag.functionImplementations) {
                        val implementorAsMonoType = try {
                            typeToMonoType(ty, newCtx)
                        } catch (e: UnresolvedGenericError) {
                            error("${e.msg}: $ty vs $paramType")
                        }

                        if (implementorAsMonoType == paramType) {
                            implFunc = map[function.fullName]!!
                            break
                        }
                    }

                    if (implFunc == null) error("Invalid state, no implementation found for $function")

                    getMonoFunction(implFunc, newCtx)
                } else {
                    getMonoFunction(function, newCtx)
                }

                mono.instructions += MonoFunCall(mono.nextId(), inst.span, monoFunction)
                provider(inst.span, inst.ref, finalType)
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

                // memory_write_int($1, type_id_of<Type>))
                dup(inst.span, type)
                int(inst.span, type.id)

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

    val wasmName = func.sourceFunction.getAnnotation(ANNOTATION_WASM_NAME)
    val wasmNameValue = (wasmName?.args?.get("name") as? ConstString)?.value

    val extern = func.sourceFunction.getAnnotation(ANNOTATION_EXTERN)
    val externName = (extern?.args?.get("name") as? ConstString)?.value

    val exportName = main ?: wasmNameValue ?: externName ?: ""

    val wasmFunc = WasmFunction(
        definition = WasmFunctionDefinition(
            name = func.finalName,
            params = params,
            results = listOf(compileTypeToPtr(func.returnType)),
            comment = func.name,
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

fun findMonomorphizationParams(type: TType, res: MutableSet<TType>) {
    when (type) {
        is TComposite -> {
            type.params.forEach { findMonomorphizationParams(it, res) }
        }

        is TGeneric -> {
            res += type
        }

        is TTag -> {
            res += type
        }

        is TInvalid -> error("Error")
        is TUnion -> error("Error")
        is TUnresolved -> error("Error")
    }
}

fun monoTypeToPrimitive(mono: MonoType): List<WasmPrimitive> {
    return when (mono.base) {
        is MonoOption -> listOf(WasmPrimitive.i32)
        is MonoOptionItem -> listOf(WasmPrimitive.i32)
        is MonoStruct -> listOf(WasmPrimitive.i32)
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
    var function: LstFunction? = null
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

    if (type is TTag) {
        TODO("Not yet!")
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
            getOptionItemType(type.base.option.instance, type.base.instance, params, ctx)
        }

        is TStruct -> {
            kind = 2
            getStructType(type.base.instance, params, ctx)
        }
    }

    val typeSign = mutableListOf(kind, base.id)
    params.forEach { typeSign += it.id }

    if (typeSign !in typeIds) {
        typeIds[typeSign] = MonoType(typeIds.size + 1, base, params)
    }

    return typeIds[typeSign]!!
}

fun WasmBuilder.getStructType(struct: LstStruct, params: List<MonoType>, ctx: MonoCtx): MonoStruct {

    val generics = mutableMapOf<LstTypeParameterDef, MonoType>()
    repeat(struct.typeParameters.size) {
        generics[struct.typeParameters[it]] = params[it]
    }

    val structSign = mutableListOf(struct.ref.id)
    generics.values.forEach { structSign += it.id }

    if (structSign !in structIds) {
        val newCtx = MonoCtx(generics, ctx)
        val fields = toMonoStructFields(struct, newCtx)

        var size = PTR_SIZE
        if (struct.parentOption != null) {
            size += PTR_SIZE
        }

        fields.forEach { size += it.size }

        structIds[structSign] = MonoStruct(structIds.size + 1, struct, fields, size)
    }

    return structIds[structSign]!!
}

fun WasmBuilder.getOptionItemType(
    option: LstOption,
    struct: LstStruct,
    params: List<MonoType>,
    ctx: MonoCtx
): MonoOptionItem {
    val generics = mutableMapOf<LstTypeParameterDef, MonoType>()
    repeat(option.typeParameters.size) {
        generics[option.typeParameters[it]] = params[it]
    }

    val optionItemSign = mutableListOf(option.ref.id, struct.ref.id)
    generics.values.forEach { optionItemSign += it.id }

    if (optionItemSign !in optionItemIds) {
        val newCtx = MonoCtx(generics, ctx)

        val fields = toMonoStructFields(struct, newCtx)
        val size = getOptionSize(option, newCtx)

        optionItemIds[optionItemSign] = MonoOptionItem(optionItemIds.size + 1, struct, fields, option, size)
    }

    return optionItemIds[optionItemSign]!!
}

fun WasmBuilder.getOptionType(option: LstOption, params: List<MonoType>, ctx: MonoCtx): MonoOption {
    val generics = mutableMapOf<LstTypeParameterDef, MonoType>()
    repeat(option.typeParameters.size) {
        generics[option.typeParameters[it]] = params[it]
    }

    val optionSign = mutableListOf(option.ref.id)
    generics.values.forEach { optionSign += it.id }

    if (optionSign !in optionIds) {
        val newCtx = MonoCtx(generics, ctx)
        val size = getOptionSize(option, newCtx)

        val items = option.items.map { struct ->
            getOptionItemType(option, struct, params, newCtx)
        }

        optionIds[optionSign] = MonoOption(optionItemIds.size + 1, option, items, size)
    }

    return optionIds[optionSign]!!
}

fun WasmBuilder.getOptionSize(option: LstOption, ctx: MonoCtx): Int {
    var size = PTR_SIZE * 2
    size += option.items.maxOf { struct ->
        val fields = toMonoStructFields(struct, ctx)
        fields.sumOf { it.size }
    }
    return size
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
        WasmPrimitive.f32
    }
    // Ints are also inlined, but they are represented the same as pointers
    if (type.isInt()) {
        WasmPrimitive.i32
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
