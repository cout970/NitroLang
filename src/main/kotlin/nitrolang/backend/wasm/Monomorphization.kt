package nitrolang.backend.wasm

import nitrolang.ast.*
import nitrolang.backend.*
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.parsing.ANNOTATION_WASM_INLINE
import nitrolang.typeinference.TType
import nitrolang.util.Span

// Search if a given function signature matches an already converted MonoFunction, if not, create a new one and return it
fun WasmBuilder.getOrCreateMonoFunction(key: MonoFuncSignature, onInit: (MonoFunction) -> Unit): MonoFunction {
    if (key in funcCache) return funcCache[key]!!

    val nameIndex = globalNames.getOrDefault(key.fullName, 0)
    globalNames[key.fullName] = nameIndex + 1
    val finalName = if (nameIndex == 0) "$${key.fullName}" else "$${key.fullName}-${nameIndex}"

    val mono = MonoFunction(id = lastFuncId++, signature = key, finalName)
    funcCache[key] = mono
    try {
        onInit(mono)
    } catch (e: WasmUnresolvedGenericTypeError) {
        throw RuntimeException(e)
    }
    return mono
}

// Given a function and a context return the corresponding MonoFunction, if it doesn't exist, we create it
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

// Given a lambda function and a context return the corresponding MonoFunction, if it doesn't exist, we create it
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

// Fills a MonoFunction with the necessary components from the original LstFunction and the given context
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

        val monoVar = MonoVar(mono.variables.size, variable.name, type)
        varMap[variable] = monoVar

        if (!variable.isParam) {
            mono.variables += monoVar
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
        processInst(inst, mono, code, ctx, providers, helperVars, varMap)
    }

    for ((index, inst) in mono.instructions.toList().withIndex()) {
        if (inst is MonoProvider) {
            if (inst.consumers.isEmpty()) {
                if (index != mono.instructions.lastIndex || signature.returnType.isNothing()) {
                    mono.instructions[index] = MonoDrop(mono.nextId(), inst.span, inst.type)
                } else {
                    mono.instructions[index] = MonoNoop(mono.nextId(), inst.span)
                }
                continue
            }

            if (inst.consumers.size == 1 && inst.consumers.first().id == inst.id + 1) {
                mono.instructions[index] = MonoNoop(mono.nextId(), inst.span)
                continue
            }

            val variable = MonoVar(mono.variables.size, "tmp${inst.id}", inst.type)
            mono.variables += variable
            mono.instructions[index] = MonoStoreVar(mono.nextId(), inst.span, variable)
            inst.consumers.forEach { it.variable = variable }
        }

        if (inst is MonoConsumer) {
            if (inst.variable == null) {
                mono.instructions[index] = MonoNoop(mono.nextId(), inst.span)
                continue
            }

            mono.instructions[index] = MonoLoadVar(mono.nextId(), inst.span, inst.variable!!)
        }
    }

    if (signature.returnType.isNothing()) {
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    } else if (mono.instructions.lastOrNull() is MonoEndBlock) {
        val msg = "Added to suppress error, 'end' returns nothing"
        mono.instructions += MonoComment(mono.nextId(), Span.internal(), msg)
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    }

    current = prev
}

// Fills a MonoFunction with the necessary components from the original LstLambdaFunction and the given context
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

        val monoVar = MonoVar(mono.variables.size, variable.name, type)
        varMap[variable] = monoVar

        if (!variable.isParam) {
            mono.variables += monoVar
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
        processInst(inst, mono, code, ctx, providers, helperVars, varMap)
    }

    for ((index, inst) in mono.instructions.toList().withIndex()) {
        if (inst is MonoProvider) {
            if (inst.consumers.isEmpty()) {
                if (index != mono.instructions.lastIndex || signature.returnType.isNothing()) {
                    mono.instructions[index] = MonoDrop(mono.nextId(), inst.span, inst.type)
                } else {
                    mono.instructions[index] = MonoNoop(mono.nextId(), inst.span)
                }
                continue
            }

            if (inst.consumers.size == 1 && inst.consumers.first().id == inst.id + 1) {
                mono.instructions[index] = MonoNoop(mono.nextId(), inst.span)
                continue
            }

            val variable = MonoVar(mono.variables.size, "tmp${inst.id}", inst.type)
            mono.variables += variable
            mono.instructions[index] = MonoStoreVar(mono.nextId(), inst.span, variable)
            inst.consumers.forEach { it.variable = variable }
        }

        if (inst is MonoConsumer) {
            if (inst.variable == null) {
                mono.instructions[index] = MonoNoop(mono.nextId(), inst.span)
                continue
            }

            mono.instructions[index] = MonoLoadVar(mono.nextId(), inst.span, inst.variable!!)
        }
    }

    if (signature.returnType.isNothing()) {
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    } else if (mono.instructions.lastOrNull() is MonoEndBlock) {
        val msg = "Added to suppress error, 'end' returns nothing"
        mono.instructions += MonoComment(mono.nextId(), Span.internal(), msg)
        mono.instructions += MonoInt(mono.nextId(), Span.internal(), 0)
    }

    current = prev
}

fun WasmBuilder.processInst(
    inst: LstInstruction,
    mono: MonoFunction,
    code: LstCode,
    ctx: MonoCtx,
    providers: MutableMap<Ref, MonoProvider>,
    helperVars: MutableMap<Ref, MonoVar>,
    varMap: MutableMap<LstVar, MonoVar>
) {
    comment(inst.span, inst.toString())

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

            val option = (type.base as? MonoStruct)?.instance?.parentOption
            if (option != null) {
                val index = option.items.indexOf(type.base.instance)
                check(index != -1)

                // $1.variant = index
                dup(inst.span, type)
                int(inst.span, index + 1)
                opcode(inst.span, "i32.store")
            }

            provider(inst.span, inst.ref, type)
        }

        is LstLambdaInit -> {
            val type = typeToMonoType(inst.type, ctx)
            consumer(inst.span, inst.alloc)
            dup(inst.span, type)

            val monoLambda = getMonoLambdaFunction(inst.lambda, ctx)
            val index = module.lambdaLabels.indexOf(monoLambda.name)
            comment(inst.span, "Lambda function \$${monoLambda.name} at index $index in \$lambdas")
            int(inst.span, index)
            opcode(inst.span, "i32.store")

            provider(inst.span, inst.ref, type)
        }

        is LstIsType -> {
            val type = typeToMonoType(inst.type, ctx)
            val typePattern = patternToMonoTypePattern(inst.typePattern, ctx)
            val expr = code.getInst(inst.expr) as LstExpression
            val exprType = typeToMonoType(expr.type, ctx)

            if (!typePattern.isOptionOrOptionItem() || !exprType.isOptionOrOptionItem()) {
                // Compile type check
                int(inst.span, if (typePattern.match(exprType)) 1 else 0)
                provider(inst.span, inst.ref, type)
            } else if (typePattern.isOptionItem() && exprType.isOptionOrOptionItem()) {
                // Runtime check
                val struct = (typePattern.base as MonoTypeBasePattern.PatternStruct).instance
                val index = struct.parentOption!!.items.indexOf(typePattern.base.instance)
                check(index != -1)

                consumer(inst.span, inst.expr)
                int(inst.span, index + 1)
                call(inst.span, "is_type_internal", ctx)
                provider(inst.span, inst.ref, type)
            } else {
                // Invalid check
                this.program.collector.report(
                    "Invalid type check, types must be known at compile type or be options",
                    inst.span
                )
                opcode(inst.span, "unreachable")
                provider(inst.span, inst.ref, type)
            }
        }

        is LstAsType -> {
            val type = typeToMonoType(inst.type, ctx)
            val typeParam = typeToMonoType(inst.typeUsageBox!!.type, ctx)
            val expr = code.getInst(inst.expr) as LstExpression
            val exprType = typeToMonoType(expr.type, ctx)

            if (typeParam.isValueType() || exprType.isValueType()) {
                // Compile type cast
                if (exprType == typeParam) {
                    consumer(inst.span, inst.expr)
                    provider(inst.span, inst.ref, type)
                    return
                }
            } else if (typeParam.isOptionItem() && (exprType.isOptionItem() || exprType.isOption())) {
                // Runtime cast
                val struct = (typeParam.base as MonoStruct).instance
                val index = struct.parentOption!!.items.indexOf(typeParam.base.instance)
                check(index != -1)

                consumer(inst.span, inst.expr)
                int(inst.span, index + 1)
                call(inst.span, "as_type_internal", ctx)
                provider(inst.span, inst.ref, type)
            } else {
                // Invalid cast
                this.program.collector.report("Invalid type cast", inst.span)
                opcode(inst.span, "unreachable")
                provider(inst.span, inst.ref, type)
            }
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
            val instance = code.getInst(inst.instance) as LstExpression
            val instanceType = typeToMonoType(instance.type, ctx)
            val type = typeToMonoType(inst.type, ctx)

            val struct = instanceType.base as MonoStruct
            val field = struct.fields[inst.field!!.index]

            consumer(inst.span, inst.instance)
            // Field offset
            int(inst.span, field.offset)
            opcode(inst.span, "i32.add")

            if (field.type.isValueType() && !field.type.isIntrinsic()) {
                // Field offset is already the pointer to the struct
                provider(inst.span, inst.ref, type)
            } else {
                // Load value from pointer
                mono.instructions += MonoLoadField(mono.nextId(), inst.span, instanceType, field)
                provider(inst.span, inst.ref, type)
            }
        }

        is LstStoreField -> {
            val instance = code.getInst(inst.instance) as LstExpression
            val instanceType = typeToMonoType(instance.type, ctx)

            val struct = instanceType.base as MonoStruct
            val field = struct.fields[inst.field!!.index]
            val fieldType = field.type

            if (fieldType.heapSize() == 0) {
                return
            }

            consumer(inst.span, inst.instance)

            // Field offset
            int(inst.span, field.offset)
            opcode(inst.span, "i32.add")

            // Value
            consumer(inst.span, inst.expr)

            if (fieldType.isValueType() && !fieldType.isIntrinsic()) {
                // Copy value
                int(inst.span, fieldType.heapSize())
                call(inst.span, "memory_copy_internal", ctx)
                drop(inst.span, fieldType)
            } else {
                // Copy pointer
                mono.instructions += MonoStoreField(mono.nextId(), inst.span, instanceType, field)
            }
        }

        is LstIfStart -> {
            consumer(inst.span, inst.cond)
            mono.instructions += MonoIfStart(mono.nextId(), inst.span)
        }

        is LstIfElse -> {
            mono.instructions += MonoIfElse(mono.nextId(), inst.span)
        }

        is LstIfEnd -> {
            mono.instructions += MonoEndBlock(mono.nextId(), inst.span)
        }

        is LstWhenStart -> {
            val type = typeToMonoType(inst.type, ctx)
            val variable = MonoVar(mono.variables.size, "when-${inst.ref.id}", type)
            helperVars[inst.ref] = variable
            mono.variables += variable
            mono.instructions += MonoStartBlock(mono.nextId(), inst.span)
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
            mono.instructions += MonoEndBlock(mono.nextId(), inst.span)
            mono.instructions += MonoLoadVar(mono.nextId(), inst.span, helperVars[inst.start.ref]!!)
        }

        is LstLoopStart -> {
            mono.instructions += MonoStartBlock(mono.nextId(), inst.span)
            mono.instructions += MonoStartLoop(mono.nextId(), inst.span)
        }

        is LstLoopJump -> {
            mono.instructions += MonoJump(mono.nextId(), inst.span, inst.block.depth - inst.loopBlock!!.depth)
        }

        is LstLoopEnd -> {
            mono.instructions += MonoEndBlock(mono.nextId(), inst.span)
            mono.instructions += MonoEndBlock(mono.nextId(), inst.span)
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

        inst.arguments.drop(1).forEach { ref -> consumer(inst.span, ref) }
        consumer(inst.span, lambdaParam)
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "i32.const 4")
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "i32.add")
        mono.instructions += MonoOpcode(mono.nextId(), inst.span, "i32.load")
        mono.instructions += MonoIndirectCall(mono.nextId(), inst.span, lambdaType)
        provider(inst.span, inst.ref, finalType)
        return
    }

    // Regular function call
    val replacements = mutableMapOf<LstTypeParameter, TType>()
    val generics = mutableMapOf<LstTypeParameter, MonoType>()

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

    if (monoFunction.returnType.isNever()) {
        opcode(inst.span, "unreachable")
    }
}
