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

        val start = WasmFunction(
            name = "\$_start_main",
            params = emptyList(),
            result = WasmPrimitive.i32,
            comment = "Init constants and call main",
            exportName = "_start_main",
        )

        module.initializers.forEach { (mono, wasmFunc) ->
            // memory_copy_internal(target: Int, value: Int, len: Int)
            start.instructions += WasmInst("i32.const ${mono.offset}")
            start.instructions += WasmInst("call ${wasmFunc.name}")
            if (mono.type.isFloat()) {
                start.instructions += WasmInst("f32.store")
            } else if (mono.type.isIntrinsic()) {
                start.instructions += WasmInst("i32.store")
            } else {
                start.instructions += WasmInst("i32.const ${mono.size}")
                start.instructions += WasmInst("call \$memory_copy_internal")
                start.instructions += WasmInst("drop")
            }
        }

        start.instructions += WasmInst("call \$main")
        module.functions += start
    }

    override fun compileImport(func: LstFunction, mono: MonoFunction, name: ConstString, lib: ConstString) {
        val params = mutableListOf<WasmVar>()

        for (param in mono.params) {
            params += WasmVar(
                kind = WasmVarKind.Param,
                name = param.finalName(),
                type = monoTypeToPrimitive(param.type)
            )
        }

        val def = WasmFunction(
            name = mono.finalName,
            params = params,
            result = monoTypeToPrimitive(mono.returnType),
            comment = func.toString()
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
            comment = "${const.fullName} at ${mono.offset}"
        )

        compileConstInit(mono, wasmFunction)

        module.initializers += mono to wasmFunction
    }

    fun compileConstInit(const: MonoConst, wasmFunc: WasmFunction) {
        const.code.variables.forEach { variable ->
            wasmFunc.locals += WasmVar(
                kind = WasmVarKind.Local,
                name = variable.finalName(),
                type = monoTypeToPrimitive(variable.type)
            )
        }

        for (inst in const.code.instructions) {
            compileInstruction(inst, wasmFunc)
        }

        module.functions += wasmFunc
    }

    override fun compileFunction(lst: LstFunction, func: MonoFunction) {
        val params = mutableListOf<WasmVar>()

        func.params.map { variable ->
            params += WasmVar(
                kind = WasmVarKind.Param,
                name = variable.finalName(),
                type = monoTypeToPrimitive(variable.type)
            )
        }

        val main = if (func.name == "main") "main" else null

        val wasmName = func.annotations.find { it.name == ANNOTATION_WASM_NAME }
        val wasmNameValue = (wasmName?.args?.get("name") as? ConstString)?.value

        val extern = func.annotations.find { it.name == ANNOTATION_EXTERN }
        val externName = (extern?.args?.get("name") as? ConstString)?.value

        val exportName = main ?: wasmNameValue ?: externName ?: ""

        val wasmFunc = WasmFunction(
            name = func.finalName,
            params = params,
            result = monoTypeToPrimitive(func.returnType),
            comment = func.signature.toString(),
            exportName = exportName
        )

        func.code.variables.forEach { variable ->
            wasmFunc.locals += WasmVar(
                kind = WasmVarKind.Local,
                name = variable.finalName(),
                type = monoTypeToPrimitive(variable.type)
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

            is MonoNothing -> {
                wasmFunc.instructions += WasmInst("i32.const 0")
            }

            is MonoString -> {
                // Align to 4 bytes for the 32bit length field
                module.sectionOffset = pad(module.sectionOffset)

                val bytes = inst.value.encodeToByteArray()
                val start = module.sectionOffset
                val contentStart = module.sectionOffset + PTR_SIZE * 2
                val size = intToWasm(bytes.size)
                val contentsPtr = intToWasm(contentStart)

                module.addSection(WasmDataSection(start, byteArrayOf(*size, *contentsPtr), "String Instance"))
                module.addSection(WasmDataSection(contentStart, bytes, "String \"${inst.value}\""))

                wasmFunc.instructions += WasmInst("i32.const $start")
            }

            is MonoLambdaCall -> {
                val wasmFuncType = funcTypeToWasm(inst.functionType)
                wasmFunc.instructions += WasmInst("i32.const 4")
                wasmFunc.instructions += WasmInst("i32.add")
                wasmFunc.instructions += WasmInst("i32.load")
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
                val index = module.lambdaLabels.indexOf(inst.lambda.name)
                val msg = "Lambda function \$${inst.lambda.name} at index $index in \$lambdas"
                wasmFunc.instructions += WasmInst("; $msg ;")
                wasmFunc.instructions += WasmInst("i32.const $index")
                wasmFunc.instructions += WasmInst("i32.store")
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

    fun monoTypeToPrimitive(mono: MonoType): WasmPrimitive {
        return if (mono.isFloat()) WasmPrimitive.f32 else WasmPrimitive.i32
    }

    fun funcTypeToWasm(mono: MonoType): String {
        if (!mono.isFunction() && !mono.isLambda()) error("Invalid type $mono")

        return buildString {
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

