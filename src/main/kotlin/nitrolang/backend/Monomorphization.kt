package nitrolang.backend

import nitrolang.ast.*
import nitrolang.backend.wasm.heapSize
import nitrolang.parsing.ANNOTATION_EXTERN
import nitrolang.typeinference.TType
import nitrolang.util.Span

// Search if a given function signature matches an already converted MonoFunction, if not, create a new one and return it
fun MonoBuilder.getOrCreateMonoFunction(key: MonoFuncSignature, onInit: (MonoFunction) -> Unit): MonoFunction {
    if (key in funcCache) return funcCache[key]!!

    val nameIndex = globalNames.getOrDefault(key.fullName, 0)
    globalNames[key.fullName] = nameIndex + 1
    val finalName = if (nameIndex == 0) "$${key.fullName}" else "$${key.fullName}-${nameIndex}"

    val mono = MonoFunction(id = lastFuncId++, signature = key, finalName)
    funcCache[key] = mono
    try {
        onInit(mono)
    } catch (e: UnresolvedGenericTypeError) {
        throw RuntimeException(e)
    }
    return mono
}

// Given a function and a context return the corresponding MonoFunction, if it doesn't exist, we create it
fun MonoBuilder.getMonoFunction(func: LstFunction, ctx: MonoCtx): MonoFunction {
    if (func.tag != null) {
        error("Calling invalid function!")
    }

    // External functions cannot be duplicated, generics become plain pointers, represented with Int
    val key = if (func.isExternal) {
        val params = func.params.map {
            typeToMonoType(program.removeAllGenerics(it.type), ctx)
        }
        val returnType = typeToMonoType(program.removeAllGenerics(func.returnType), ctx)

        MonoFuncSignature(func.ref, func.fullName, params, returnType)
    } else {
        MonoFuncSignature(
            func.ref, func.fullName,
            func.params.map { typeToMonoType(it.type, ctx) },
            typeToMonoType(func.returnType, ctx)
        )
    }

    key.function = func

    return getOrCreateMonoFunction(key) { mono ->
        createMonoFunction(mono, func, key, ctx)
    }
}

// Given a lambda function and a context return the corresponding MonoFunction, if it doesn't exist, we create it
fun MonoBuilder.getMonoLambdaFunction(lambda: LstLambdaFunction, ctx: MonoCtx): MonoFunction {
    val key = MonoFuncSignature(
        lambda.ref, "lambda-${lambda.ref.id}",
        lambda.params.map { typeToMonoType(it.type, ctx) },
        typeToMonoType(lambda.returnType, ctx),
    )
    key.lambda = lambda

    return getOrCreateMonoFunction(key) { mono ->
        createMonoLambdaFunction(mono, lambda, key, ctx)
    }
}

// Fills a MonoFunction with the necessary components from the original LstFunction and the given context
fun MonoBuilder.createMonoFunction(
    mono: MonoFunction,
    function: LstFunction,
    signature: MonoFuncSignature,
    ctx: MonoCtx
) {
    val prev = current
    mono.code = MonoCode(function.body)
    mono.name = signature.fullName
    mono.returnType = signature.returnType
    mono.annotations += function.annotations
    mono.code.isExternal = function.isExternal

    current = mono.code
    processCode(mono.code, ctx)

    function.params.forEach {
        mono.params += mono.code.varMap[it.variable]!!
    }
    current = prev
}

// Fills a MonoFunction with the necessary components from the original LstLambdaFunction and the given context
fun MonoBuilder.createMonoLambdaFunction(
    mono: MonoFunction,
    function: LstLambdaFunction,
    signature: MonoFuncSignature,
    ctx: MonoCtx
) {
    mono.name = signature.fullName
    mono.returnType = signature.returnType
    mono.isLambda = true
    mono.code = MonoCode(function.body)

    val prev = current
    current = mono.code
    processCode(mono.code, ctx)

    function.params.forEach {
        mono.params += mono.code.varMap[it.variable]!!
    }

    builder.onCompileLambda(mono)
    current = prev
}

fun MonoBuilder.processCode(monoCode: MonoCode, ctx: MonoCtx) {
    var varIndex = 0
    for (variable in monoCode.code.variables.values) {
        val type = typeToMonoType(
            if (monoCode.isExternal) program.removeAllGenerics(variable.type) else variable.type,
            ctx
        )

        val monoVar = MonoVar(varIndex, variable.name, type)
        monoCode.varMap[variable] = monoVar

        if (!variable.isParam) {
            varIndex++
            monoCode.variables += monoVar
        }
    }

    if (monoCode.isExternal) {
        return
    }

    for (inst in monoCode.code.nodes) {
        processInst(inst, monoCode, ctx)
    }

    for ((index, inst) in monoCode.instructions.toList().withIndex()) {
        if (inst is MonoProvider) {
            if (inst.consumers.isEmpty()) {
                if (index != monoCode.instructions.lastIndex || monoCode.code.returnType.isNothing()) {
                    monoCode.instructions[index] = MonoDrop(monoCode.nextId(), inst.span, inst.type)
                } else {
                    monoCode.instructions[index] = MonoNoop(monoCode.nextId(), inst.span)
                }
                continue
            }

            if (inst.consumers.size == 1 && inst.consumers.first().id == inst.id + 1) {
                monoCode.instructions[index] = MonoNoop(monoCode.nextId(), inst.span)
                continue
            }

            val variable = MonoVar(monoCode.variables.size, "tmp${inst.id}", inst.type)
            monoCode.variables += variable
            monoCode.instructions[index] = MonoStoreVar(monoCode.nextId(), inst.span, variable)
            inst.consumers.forEach { it.variable = variable }
        }

        if (inst is MonoConsumer) {
            if (inst.variable == null) {
                monoCode.instructions[index] = MonoNoop(monoCode.nextId(), inst.span)
                continue
            }

            monoCode.instructions[index] = MonoLoadVar(monoCode.nextId(), inst.span, inst.variable!!)
        }
    }

    if (monoCode.code.returnType.isNothing()) {
        monoCode.instructions += MonoInt(monoCode.nextId(), Span.internal(), 0)
    } else if (monoCode.instructions.lastOrNull() is MonoEndBlock) {
        val msg = "Added to suppress error, 'end' returns nothing"
        monoCode.instructions += MonoComment(monoCode.nextId(), Span.internal(), msg)
        monoCode.instructions += MonoInt(monoCode.nextId(), Span.internal(), 0)
    }
}

fun MonoBuilder.processInst(
    inst: LstInstruction,
    code: MonoCode,
    ctx: MonoCtx,
) {
    comment(inst.span, inst.toString())

    when (inst) {
        is LstFunCall -> {
            val function = inst.function ?: error("Function not bound: $inst")

            processFunctionCall(code, function, inst, ctx)
        }

        is LstComment -> {
            code.instructions += MonoComment(code.nextId(), inst.span, inst.comment)
        }

        is LstBoolean -> {
            val type = typeToMonoType(inst.type, ctx)
            code.instructions += MonoBoolean(code.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstFloat -> {
            val type = typeToMonoType(inst.type, ctx)
            code.instructions += MonoFloat(code.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstInt -> {
            val type = typeToMonoType(inst.type, ctx)
            code.instructions += MonoInt(code.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstLong -> {
            val type = typeToMonoType(inst.type, ctx)
            code.instructions += MonoLong(code.nextId(), inst.span, inst.value)
            provider(inst.span, inst.ref, type)
        }

        is LstNothing -> {
            val type = typeToMonoType(inst.type, ctx)
            code.instructions += MonoNothing(code.nextId(), inst.span)
            provider(inst.span, inst.ref, type)
        }

        is LstString -> {
            val type = typeToMonoType(inst.type, ctx)
            code.instructions += MonoString(code.nextId(), inst.span, inst.value, type)
            provider(inst.span, inst.ref, type)
        }

        is LstSizeOf -> {
            val type = typeToMonoType(inst.type, ctx)
            val sizeType = typeToMonoType(inst.typeUsageBox!!.type, ctx)
            code.instructions += MonoInt(code.nextId(), inst.span, sizeType.heapSize())
            provider(inst.span, inst.ref, type)
        }

        is LstAlloc -> {
            val type = typeToMonoType(inst.type, ctx)

            // $1 = memory_alloc_internal(size_of<Type>)
            int(inst.span, type.heapSize())

            getMonoFunction(program.getFunction("memory_alloc_internal"), ctx)
            call(inst.span, "memory_alloc_internal", ctx)

            val monoStruct = type.base as? MonoStruct
            val option = monoStruct?.instance?.parentOption
            if (option != null) {
                val index = option.items.indexOf(type.base.instance)
                check(index != -1)

                // $1.variant = index
                dup(inst.span, type)
                int(inst.span, index + 1)
                code.instructions += MonoStoreField(code.nextId(), inst.span, type, monoStruct.fields.first())
            }

            provider(inst.span, inst.ref, type)
        }

        is LstLambdaInit -> {
            val type = typeToMonoType(inst.type, ctx)
            consumer(inst.span, inst.alloc)
            dup(inst.span, type)

            val monoLambda = getMonoLambdaFunction(inst.lambda, ctx)
            code.instructions += MonoLambdaInit(code.nextId(), inst.span, monoLambda)

            provider(inst.span, inst.ref, type)
        }

        is LstIsType -> {
            val type = typeToMonoType(inst.type, ctx)
            val typePattern = patternToMonoTypePattern(inst.typePattern, ctx)
            val expr = code.code.getInst(inst.expr) as LstExpression
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
                unreachable(inst.span)
                provider(inst.span, inst.ref, type)
            }
        }

        is LstAsType -> {
            val type = typeToMonoType(inst.type, ctx)
            val typeParam = typeToMonoType(inst.typeUsageBox!!.type, ctx)
            val expr = code.code.getInst(inst.expr) as LstExpression
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
                unreachable(inst.span)
                provider(inst.span, inst.ref, type)
            }
        }

        is LstIfChoose -> {
            val type = typeToMonoType(inst.type, ctx)
            consumer(inst.span, inst.ifTrue)
            consumer(inst.span, inst.ifFalse)
            consumer(inst.span, inst.cond)
            code.instructions += MonoIfChoose(code.nextId(), inst.span)
            provider(inst.span, inst.ref, type)
        }

        is LstReturn -> {
            consumer(inst.span, inst.expr)
            code.instructions += MonoReturn(code.nextId(), inst.span)
        }

        is LstLoadVar -> {
            val type = if (inst.constant != null) {
                val const = inst.constant!!
                val constType = typeToMonoType(const.type, ctx)

                if (const.isDeadCode) {
                    error("Internal error: attempt to access constant marked as dead code: ${const.fullName}")
                }

                val monoConst =
                    consts[const.ref.id] ?: error("Missing const: ${const.fullName}, with id: ${const.ref.id}")

                code.instructions += MonoLoadConst(code.nextId(), inst.span, monoConst)
                constType
            } else {
                val variable = code.varMap[inst.variable] ?: error("Missing variable: ${inst.variable}")

                code.instructions += MonoLoadVar(code.nextId(), inst.span, variable)
                variable.type
            }

            code.instructions += MonoProvider(code.nextId(), inst.span, type).also {
                code.providers[inst.ref] = it
            }
        }

        is LstStoreVar -> {
            if (inst.constant != null) {
                program.collector.report("Cannot override const ${inst.constant!!.fullName}", inst.span)
                return
            }
            val variable = code.varMap[inst.variable] ?: error("Missing variable: ${inst.variable}")
            consumer(inst.span, inst.expr)
            code.instructions += MonoStoreVar(code.nextId(), inst.span, variable)
        }

        is LstLoadField -> {
            val instance = code.code.getInst(inst.instance) as LstExpression
            val instanceType = typeToMonoType(instance.type, ctx)
            val type = typeToMonoType(inst.type, ctx)

            val struct = instanceType.base as MonoStruct
            val field = struct.fields[inst.field!!.index]

            consumer(inst.span, inst.instance)
            // Field offset
            code.instructions += MonoGetFieldAddress(code.nextId(), inst.span, struct, field)

            if (field.type.isValueType() && !field.type.isIntrinsic()) {
                // Field offset is already the pointer to the struct
                provider(inst.span, inst.ref, type)
            } else {
                // Load value from pointer
                code.instructions += MonoLoadField(code.nextId(), inst.span, instanceType, field)
                provider(inst.span, inst.ref, type)
            }
        }

        is LstStoreField -> {
            val instance = code.code.getInst(inst.instance) as LstExpression
            val instanceType = typeToMonoType(instance.type, ctx)

            val struct = instanceType.base as MonoStruct
            val field = struct.fields[inst.field!!.index]
            val fieldType = field.type

            if (fieldType.heapSize() == 0) {
                return
            }

            consumer(inst.span, inst.instance)

            // Field offset
            code.instructions += MonoGetFieldAddress(code.nextId(), inst.span, struct, field)

            // Value
            consumer(inst.span, inst.expr)

            if (fieldType.isValueType() && !fieldType.isIntrinsic()) {
                // Copy value
                int(inst.span, fieldType.heapSize())
                call(inst.span, "memory_copy_internal", ctx)
                drop(inst.span, fieldType)
            } else {
                // Copy pointer
                code.instructions += MonoStoreField(code.nextId(), inst.span, instanceType, field)
            }
        }

        is LstIfStart -> {
            consumer(inst.span, inst.cond)
            code.instructions += MonoIfStart(code.nextId(), inst.span)
        }

        is LstIfElse -> {
            code.instructions += MonoIfElse(code.nextId(), inst.span)
        }

        is LstIfEnd -> {
            code.instructions += MonoEndBlock(code.nextId(), inst.span, "if")
        }

        is LstWhenStart -> {
            val type = typeToMonoType(inst.type, ctx)
            val variable = MonoVar(code.variables.size, "when-${inst.ref.id}", type)
            code.helperVars[inst.ref] = variable
            code.variables += variable
            code.instructions += MonoStartBlock(code.nextId(), inst.span)
        }

        is LstWhenJump -> {
            code.instructions += MonoJump(code.nextId(), inst.span, inst.block.depth - inst.whenBlock.depth, false)
        }

        is LstWhenStore -> {
            val variable = code.helperVars[inst.start.ref]!!
            consumer(inst.span, inst.expr)
            code.instructions += MonoStoreVar(code.nextId(), inst.span, variable)
        }

        is LstWhenEnd -> {
            code.instructions += MonoEndBlock(code.nextId(), inst.span, "when")
            if (!inst.isStatement) {
                val whenVar = code.helperVars[inst.start.ref]!!
                code.instructions += MonoLoadVar(code.nextId(), inst.span, whenVar)
                provider(inst.span, inst.ref, whenVar.type)
            }
        }

        is LstLoopStart -> {
            code.instructions += MonoStartBlock(code.nextId(), inst.span)
            code.instructions += MonoStartLoop(code.nextId(), inst.span)
        }

        is LstLoopJump -> {
            code.instructions += MonoJump(
                code.nextId(),
                inst.span,
                inst.block.depth - inst.loopBlock!!.depth,
                inst.backwards
            )
        }

        is LstLoopEnd -> {
            code.instructions += MonoEndBlock(code.nextId(), inst.span, "loop")
            code.instructions += MonoEndBlock(code.nextId(), inst.span, "block")
        }
    }
}

fun MonoBuilder.processFunctionCall(mono: MonoCode, function: LstFunction, inst: LstFunCall, ctx: MonoCtx) {
    val finalType = typeToMonoType(inst.type, ctx)

    if (builder.onCompileFunctionCall(mono, function, inst, finalType)) {
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

        val args = inst.arguments.drop(1)
        args.forEach { ref -> consumer(inst.span, ref) }

        consumer(inst.span, lambdaParam)
        mono.instructions += MonoLambdaCall(mono.nextId(), inst.span, lambdaType, args.size)
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
        unreachable(inst.span)
    }
}
