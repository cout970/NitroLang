package nitrolang.backend.wasm

import nitrolang.ast.*
import nitrolang.backend.*
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_WASM_INLINE
import nitrolang.parsing.ANNOTATION_WASM_NAME

const val PTR_SIZE: Int = 4

// Utility to convert an already type-checked LstProgram into a WasmModule
class WasmBuilder(
    val program: LstProgram,
    val module: WasmModule,
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

    companion object {
        fun compile(program: LstProgram, out: Appendable) {
            val module = WasmModule()
            WasmBuilder(program, module).compileAll()
            WasmPrinter(out).module(module)
        }
    }
}

fun WasmBuilder.compileAll() {
    // Null value
    module.sections += module.sectionOffset to intToWasmHex(0)
    module.sectionOffset += 4

    // Memory instance
    val memoryAddr = module.sectionOffset
    module.sections += module.sectionOffset to ""
    module.sectionOffset += 12

    compileImports()
    compileConsts()
    compileFunctions()

    // Override section with address where the heap starts
    // @formatter:off
    module.sectionOffset = pad(module.sectionOffset)
    val memoryInstance =
        /* capacity */ intToWasmHex(16 * 64 * 1024 - module.sectionOffset) +
            /* len      */ intToWasmHex(0) +
            /* bytes    */ intToWasmHex(module.sectionOffset)
    // @formatter:on
    module.sections[1] = memoryAddr to memoryInstance
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

        val def = WasmFunction(
            name = mono.finalName,
            params = params,
            results = results,
            comment = func.toString()
        )

        val name = external.args["name"] as? ConstString ?: error("Missing external name")
        val lib = external.args["lib"] as? ConstString ?: error("Missing external lib name")

        module.imports += WasmImport(
            module = lib.value,
            functionName = name.value,
            function = def
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
    program.functions.filter { it.isRequired }.forEach {
        getMonoFunction(it, MonoCtx())
    }
    getMonoFunction(program.getFunction("main"), MonoCtx())

    for (it in funcCache.values) {
        if (it.isExternal) {
            continue
        }
        compileMonoFunction(it)
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
        name = func.finalName,
        params = params,
        results = monoTypeToPrimitive(func.returnType),
        comment = func.signature.toString(),
        exportName = exportName
    )

    func.variables.forEach { variable ->
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
        compileMonoInstruction(inst, wasmFunc)
    }

    module.functions += wasmFunc
}

fun WasmBuilder.compileMonoInstruction(inst: MonoInstruction, wasmFunc: WasmFunction) {
    when (inst) {
        is MonoConsumer -> error("MonoConsumer")
        is MonoProvider -> error("MonoProvider")
        is MonoNoop -> Unit
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
            module.sectionOffset = pad(module.sectionOffset)

            val bytes = inst.value.encodeToByteArray()
            val start = module.sectionOffset
            // size
            module.sectionOffset += PTR_SIZE
            // pointer to string characters in heap or data section
            module.sectionOffset += PTR_SIZE

            // content
            val contentStart = module.sectionOffset
            module.sectionOffset += bytes.size

            val size = intToWasmHex(bytes.size)
            val contentsPtr = intToWasmHex(contentStart)
            val contents = bytes.joinToString("") {
                "\\" + it.toUByte().toString(16).padStart(2, '0')
            }

            module.sections += start to "$size$contentsPtr$contents"

            wasmFunc.instructions += WasmInst("i32.const $start")
        }

        is MonoIndirectCall -> {
            val wasmFuncType = funcTypeToWasm(inst.functionType)
            wasmFunc.instructions += WasmInst("call_indirect $wasmFuncType")
        }

        is MonoFunCall -> {
            wasmFunc.instructions += WasmInst("call ${inst.function.finalName}")
        }

        is MonoOpcode -> {
            wasmFunc.instructions += WasmInst(inst.opcode)
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

            val fieldType = inst.field.type
            val fieldPrim = monoTypeToPrimitive(fieldType)
            if (fieldPrim.size != 1) error("Handle multi value fields")

            fieldPrim.forEach { prim ->
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
        }

        is MonoStoreField -> {
            val instancePrim = monoTypeToPrimitive(inst.instanceType)
            if (instancePrim.size != 1 || instancePrim.first() !== WasmPrimitive.i32) {
                error("Instance is not a pointer!")
            }

            val fieldType = inst.field.type
            val fieldPrim = monoTypeToPrimitive(fieldType)
            if (fieldPrim.size != 1) TODO("Handle multi value fields")

            fieldPrim.forEach { prim ->
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

