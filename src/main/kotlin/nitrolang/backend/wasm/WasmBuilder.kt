package nitrolang.backend.wasm

import nitrolang.ast.*
import nitrolang.backend.*
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_WASM_INLINE
import nitrolang.parsing.ANNOTATION_WASM_NAME

const val PTR_SIZE: Int = 4

// Utility to convert an already type-checked LstProgram into a WasmModule
open class WasmBuilder(
    val program: LstProgram,
    val module: WasmModule,
) : IBuilder {
    var initialMemoryAddr: Int = 0
    var root: MonoBuilder? = null

    companion object {
        fun compile(program: LstProgram, out: Appendable) {
            val module = WasmModule()
            MonoBuilder(program, WasmBuilder(program, module)).compileAll()
            CodePrinter(out).wasmModule(module)
        }
    }

    override fun init(root: MonoBuilder) {
        this.root = root
        // Null value
        module.addSection(WasmDataSection(module.sectionOffset, intToWasm(0), "Null value"))

        // Memory instance
        initialMemoryAddr = module.sectionOffset
        module.addSection(WasmDataSection(module.sectionOffset, ByteArray(12), "Memory instance"))
    }

    override fun finish() {
        val start = WasmFunction(
            name = "\$_start_main",
            params = emptyList(),
            result = WasmPrimitive.i32,
            comment = "Init constants and call main",
            exportName = "_start_main",
            sourceFunction = null,
        )

        module.initializers.forEach { (mono, wasmFunc) ->
            // memory_copy_internal(target: Int, value: Int, len: Int)
            start.instructions += WasmInst("i32.const ${mono.offset}")
            start.instructions += WasmInst("call ${wasmFunc.name}")
            if (mono.type.isEncodedInRef()) {
                val prim = mono.type.getReferencePrimitive()
                start.instructions += WasmInst("$prim.store")
            } else if (mono.type.isIntrinsic()) {
                start.instructions += WasmInst("i32.store")
            } else {
                start.instructions += WasmInst("i32.const ${mono.size}")
                start.instructions += WasmInst("call \$memory_copy_internal")
                start.instructions += WasmInst("drop")
            }
        }

        if (program.compilerOptions.runTests) {
            module.functions
            module.functions.forEach { func ->
                val sourceFunction: LstFunction = func.sourceFunction ?: return@forEach

                if (sourceFunction.isTest) {
                    pushString(func.exportName, start)
                    pushString(sourceFunction.getTestName() ?: sourceFunction.fullName, start)
                    start.instructions += WasmInst("call \$run_test")
                    start.instructions += WasmInst("drop")
                }
            }
            start.instructions += WasmInst("i32.const 0")
        } else {
            start.instructions += WasmInst("call \$main")
        }
        module.functions += start

        // Override section with address where the heap starts
        // @formatter:off
        module.sectionOffset = pad(module.sectionOffset)
        val memoryInstance = byteArrayOf(
            /* capacity */ *intToWasm(16 * 64 * 1024 - module.sectionOffset),
            /* len      */ *intToWasm(0),
            /* bytes    */ *intToWasm(module.sectionOffset)
        )
        // @formatter:on
        module.sections[1].data = memoryInstance
    }

    override fun compileImport(func: LstFunction, mono: MonoFunction, name: ConstString, lib: ConstString) {
        val params = mutableListOf<WasmVar>()

        for (param in mono.code.params) {
            params += WasmVar(
                kind = WasmVarKind.Param,
                name = param.finalName(),
                type = monoTypeToPrimitive(param.type),
                monoType = param.type,
            )
        }

        val def = WasmFunction(
            name = mono.finalName,
            params = params,
            result = monoTypeToPrimitive(mono.returnType),
            comment = func.toString(),
            sourceFunction = func,
        )

        module.imports += WasmImport(
            module = lib.value,
            functionName = name.value,
            function = def
        )
    }

    override fun compileConst(const: LstConst, mono: MonoConst) {
        mono.offset = module.sectionOffset
        mono.size = mono.type.heapSize()
        val section = WasmDataSection(mono.offset, ByteArray(mono.size), "${const.fullName} at ${mono.offset}")
        module.addSection(section)

        var value: ByteArray? = null

        if (const.body.nodes.size == 1) {
            value = when (val last = const.body.nodes.last()) {
                is LstInt -> intToWasm(last.value)
                is LstFloat -> floatToWasm(last.value)
                is LstBoolean -> intToWasm(if (last.value) 1 else 0)
                is LstNothing -> intToWasm(0)
                else -> null
            }
        }

        if (value != null) {
            section.data = value
            return
        }

        val wasmFunction = WasmFunction(
            name = "\$init_const_${mono.instance.ref.id}",
            params = emptyList(),
            result = monoTypeToPrimitive(mono.type),
            comment = "${const.fullName} at ${mono.offset}",
            sourceFunction = null,
        )

        compileConstInit(mono, wasmFunction)
        module.initializers += mono to wasmFunction
    }

    fun compileConstInit(const: MonoConst, wasmFunc: WasmFunction) {
        const.code.variables.forEach { variable ->
            wasmFunc.locals += WasmVar(
                kind = WasmVarKind.Local,
                name = variable.finalName(),
                type = monoTypeToPrimitive(variable.type),
                monoType = variable.type,
            )
        }

        for (inst in const.code.instructions) {
            compileInstruction(inst, wasmFunc)
        }

        module.functions += wasmFunc
    }

    override fun compileFunction(lst: LstFunction?, func: MonoFunction) {
        val params = mutableListOf<WasmVar>()

        func.code.params.map { variable ->
            params += WasmVar(
                kind = WasmVarKind.Param,
                name = if (variable.name == "_") null else variable.finalName(),
                type = monoTypeToPrimitive(variable.type),
                monoType = variable.type,
            )
        }

        val main = if (func.name == "main") "main" else null

        val wasmName = func.annotations.find { it.name == ANNOTATION_WASM_NAME }
        val wasmNameValue = (wasmName?.args?.get("name") as? ConstString)?.value

        val extern = func.annotations.find { it.name == ANNOTATION_EXTERN }
        val externName = (extern?.args?.get("name") as? ConstString)?.value

        var exportName = main ?: wasmNameValue ?: externName ?: ""

        if (lst != null && lst.isTest) {
            exportName = "test_${lst.ref.id}"
        }

        val wasmFunc = WasmFunction(
            name = func.finalName,
            params = params,
            result = monoTypeToPrimitive(func.returnType),
            comment = func.signature.toString(),
            exportName = exportName,
            sourceFunction = lst,
        )

        func.code.variables.forEach { variable ->
            wasmFunc.locals += WasmVar(
                kind = WasmVarKind.Local,
                name = variable.finalName(),
                type = monoTypeToPrimitive(variable.type),
                monoType = variable.type,
            )
        }

        for (inst in func.code.instructions) {
            compileInstruction(inst, wasmFunc)
        }

        module.functions += wasmFunc
    }

    override fun onCompileLambda(mono: MonoFunction) {
        module.lambdaLabels += mono.name
    }

    override fun onCompileFunctionCall(
        mono: MonoCode,
        function: LstFunction,
        inst: LstFunCall,
        finalType: MonoType
    ): Boolean {
        val inline = function.getAnnotation(ANNOTATION_WASM_INLINE)
        val opcode = inline?.args?.get("opcode") as? ConstString

        // Just a wasm opcode
        if (opcode != null) {
            inst.arguments.forEach { ref -> root!!.consumer(inst.span, ref) }
            mono.instructions += MonoInline(mono.nextId(), inst.span, opcode.value)
            root!!.provider(inst.span, inst.ref, finalType)
            return true
        }

        return false
    }

    fun compileInstruction(inst: MonoInstruction, wasmFunc: WasmFunction) {
        when (inst) {
            is MonoConsumer -> error("MonoConsumer")
            is MonoProvider -> error("MonoProvider")
            is MonoNoop -> Unit
            is MonoDrop -> {
                wasmFunc.instructions += WasmInst("drop")
            }

            is MonoDup -> {
                wasmFunc.instructions += WasmInst("local.tee ${inst.auxLocal.finalName()}")
                wasmFunc.instructions += WasmInst("local.get ${inst.auxLocal.finalName()}")
            }

            is MonoSwap -> {
                wasmFunc.instructions += WasmInst("local.set ${inst.auxLocal0.finalName()}")
                wasmFunc.instructions += WasmInst("local.set ${inst.auxLocal1.finalName()}")
                wasmFunc.instructions += WasmInst("local.get ${inst.auxLocal0.finalName()}")
                wasmFunc.instructions += WasmInst("local.get ${inst.auxLocal1.finalName()}")
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

            is MonoLong -> {
                wasmFunc.instructions += WasmInst("i64.const ${inst.value}")
            }

            is MonoNothing -> {
                wasmFunc.instructions += WasmInst("i32.const 0")
            }

            is MonoString -> {
                pushString(inst.value, wasmFunc)
            }

            is MonoMemoryLoad -> {
                val prim = monoTypeToPrimitive(inst.type)
                if (inst.offset != 0) {
                    wasmFunc.instructions += WasmInst("i32.const ${inst.offset}")
                    wasmFunc.instructions += WasmInst("i32.add")
                }
                wasmFunc.instructions += WasmInst("$prim.load")
            }

            is MonoLambdaCall -> {
                val wasmFuncType = funcTypeToWasm(inst.functionType, true)
                wasmFunc.instructions += WasmInst("call_indirect $wasmFuncType")
            }

            is MonoFunCall -> {
                wasmFunc.instructions += WasmInst("call ${inst.function.finalName}")
            }

            is MonoInline -> {
                wasmFunc.instructions += WasmInst(inst.inline)
            }

            is MonoGetFieldAddress -> {
                wasmFunc.instructions += WasmInst("i32.const ${inst.field.offset}")
                wasmFunc.instructions += WasmInst("i32.add")
            }

            is MonoUnreachable -> {
                wasmFunc.instructions += WasmInst("unreachable")
            }

            is MonoLambdaInit -> {
                // $1 = memory_alloc_internal(size_of<Type>)
                wasmFunc.instructions += WasmInst("i32.const ${inst.type.heapSize()}")
                wasmFunc.instructions += WasmInst("call \$memory_alloc_internal")
                wasmFunc.dup(WasmPrimitive.i32)

                val index = module.lambdaLabels.indexOf(inst.lambda.name)
                val msg = "Lambda function \$${inst.lambda.name} at index $index in \$lambdas"
                wasmFunc.instructions += WasmInst("; $msg ;")
                wasmFunc.instructions += WasmInst("i32.const $index")
                wasmFunc.instructions += WasmInst("i32.store")

                inst.localUpValues.forEach { (lambdaVar, functionVar) ->
                    val slot = lambdaVar.upValueSlot
                    val prim = monoTypeToPrimitive(lambdaVar.type)
                    wasmFunc.instructions += WasmInst("; Copy upValue ref to slot $slot ;")
                    wasmFunc.dup(WasmPrimitive.i32)
                    wasmFunc.instructions += WasmInst("i32.const ${PTR_SIZE + slot * PTR_SIZE}")
                    wasmFunc.instructions += WasmInst("i32.add")
                    wasmFunc.instructions += WasmInst("local.get ${functionVar.finalName()}")
                    wasmFunc.instructions += WasmInst("${prim}.store")
                }
            }

            is MonoIfChoose -> {
                wasmFunc.instructions += WasmInst("select", needsWrapping = false)
            }

            is MonoIfStart -> {
                wasmFunc.instructions += WasmInst("if", needsWrapping = false)
            }

            is MonoIfElse -> {
                wasmFunc.instructions += WasmInst("else", needsWrapping = false)
            }

            is MonoLoadConst -> {
                if (inst.const.type.isIntrinsic()) {
                    wasmFunc.instructions += WasmInst("i32.const ${inst.const.offset}")
                    val prim = monoTypeToPrimitive(inst.const.type)
                    wasmFunc.instructions += WasmInst("$prim.load")
                } else {
                    wasmFunc.instructions += WasmInst("i32.const ${inst.const.offset}")
                }
            }

            is MonoCreateUpValue -> {
                val ptrType = inst.upValue.type
                val type = ptrType.params.first()
                wasmFunc.instructions += WasmInst("i32.const ${type.heapSize()}")
                wasmFunc.instructions += WasmInst("call \$memory_alloc_internal")
                wasmFunc.instructions += WasmInst("local.set ${inst.upValue.finalName()}")
            }

            is MonoInitUpValue -> {
                val selfParam = wasmFunc.params.first().name
                wasmFunc.instructions += WasmInst("local.get $selfParam")
                wasmFunc.instructions += WasmInst("i32.const ${PTR_SIZE + inst.upValue.upValueSlot * PTR_SIZE}")
                wasmFunc.instructions += WasmInst("i32.add")
                wasmFunc.instructions += WasmInst("i32.load")
                wasmFunc.instructions += WasmInst("local.set ${inst.upValue.finalName()}")
            }

            is MonoLoadUpValue -> {
                val ptrType = inst.upValue.type.params.first()
                val prim = ptrType.getReferencePrimitive()
                wasmFunc.instructions += WasmInst("local.get ${inst.upValue.finalName()}")
                wasmFunc.instructions += WasmInst("$prim.load")
            }

            is MonoStoreUpValue -> {
                val ptrType = inst.upValue.type.params.first()
                val prim = ptrType.getReferencePrimitive()
                wasmFunc.instructions += WasmInst("local.get ${inst.upValue.finalName()}")
                wasmFunc.swap(WasmPrimitive.i32, prim)
                wasmFunc.instructions += WasmInst("$prim.store")
            }

            is MonoLoadParam -> {
                wasmFunc.instructions += WasmInst("local.get ${inst.param.finalName()}")
            }

            is MonoLoadVar -> {
                wasmFunc.instructions += WasmInst("local.get ${inst.variable.finalName()}")
            }

            is MonoStoreVar -> {
                wasmFunc.instructions += WasmInst("local.set ${inst.variable.finalName()}")
            }

            is MonoLoadField -> {
                val instancePrim = monoTypeToPrimitive(inst.instanceType)
                if (instancePrim !== WasmPrimitive.i32) {
                    error("Instance is not a pointer!")
                }

                val fieldType = inst.field.type
                val prim = monoTypeToPrimitive(fieldType)

                when (inst.field.size) {
                    0 -> {
                        wasmFunc.instructions += WasmInst("drop")
                        wasmFunc.instructions += WasmInst("i32.const 0")
                    }

                    1 -> {
                        wasmFunc.instructions += WasmInst("i32.load8_s")
                    }

                    2 -> {
                        wasmFunc.instructions += WasmInst("i32.load16_s")
                    }

                    else -> {
                        wasmFunc.instructions += WasmInst("$prim.load")
                    }
                }
            }

            is MonoStoreField -> {
                val instancePrim = monoTypeToPrimitive(inst.instanceType)
                if (instancePrim !== WasmPrimitive.i32) {
                    error("Instance is not a pointer!")
                }

                val fieldType = inst.field.type
                val prim = monoTypeToPrimitive(fieldType)

                when (inst.field.size) {
                    0 -> {
                        wasmFunc.instructions += WasmInst("drop")
                        wasmFunc.instructions += WasmInst("drop")
                    }

                    1 -> {
                        wasmFunc.instructions += WasmInst("i32.store8", needsWrapping = false)
                    }

                    2 -> {
                        wasmFunc.instructions += WasmInst("i32.store16", needsWrapping = false)
                    }

                    else -> {
                        wasmFunc.instructions += WasmInst("$prim.store", needsWrapping = false)
                    }
                }
            }

            is MonoReturn -> {
                wasmFunc.instructions += WasmInst("return")
            }

            is MonoStartLoop -> {
                wasmFunc.instructions += WasmInst("loop", needsWrapping = false)
            }

            is MonoStartBlock -> {
                wasmFunc.instructions += WasmInst("block", needsWrapping = false)
            }

            is MonoJump -> {
                wasmFunc.instructions += WasmInst("br ${inst.depth}")
            }

            is MonoEndBlock -> {
                wasmFunc.instructions += WasmInst("end", needsWrapping = false)
            }
        }
    }

    private fun pushString(value: String, wasmFunc: WasmFunction) {
        // Align to 4 bytes for the 32bit length field
        module.sectionOffset = pad(module.sectionOffset)

        val bytes = value.encodeToByteArray()
        val start = module.sectionOffset
        val contentStart = module.sectionOffset + PTR_SIZE * 2
        val size = intToWasm(bytes.size)
        val contentsPtr = intToWasm(contentStart)

        module.addSection(WasmDataSection(start, byteArrayOf(*size, *contentsPtr), "String Instance"))
        module.addSection(WasmDataSection(contentStart, bytes, "String \"${value}\""))

        wasmFunc.instructions += WasmInst("i32.const $start")
    }

    fun monoTypeToPrimitive(mono: MonoType): WasmPrimitive {
        return when {
            mono.isFloat() -> WasmPrimitive.f32
            mono.isLong() -> WasmPrimitive.i64
            else -> WasmPrimitive.i32
        }
    }

    fun MonoType.getReferencePrimitive(): WasmPrimitive {
        return when {
            isFloat() -> WasmPrimitive.f32
            isLong() -> WasmPrimitive.i64
            isInt() -> WasmPrimitive.i32
            else -> WasmPrimitive.i32
        }
    }

    fun funcTypeToWasm(mono: MonoType, lambda: Boolean): String {
        if (!mono.isFunction() && !mono.isLambda()) error("Invalid type $mono")

        return buildString {
            if (lambda) {
                append("(param ")
                append(WasmPrimitive.i32)
                append(")")
                append(" ")
            }

            mono.params.dropLast(1).forEach { p ->
                append("(param ")
                append(monoTypeToPrimitive(p))
                append(")")
                append(" ")
            }

            append("(result ")
            append(monoTypeToPrimitive(mono.params.last()))
            append(")")
        }
    }
}

