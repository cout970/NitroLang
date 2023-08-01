package nitrolang.sm

import nitrolang.ANNOTATION_STACK_VALUE
import nitrolang.ANNOTATION_WASM_INLINE
import nitrolang.ast.*
import nitrolang.backend.wasm.*
import nitrolang.util.ErrorCollector
import nitrolang.util.Span

class SmTransformer(
    val input: LstCode,
    val output: SmCode,
    val program: LstProgram,
    val collector: ErrorCollector,
) {

    private val validTypes = mutableMapOf<TypeTree, Int>()
    private val neverType = findSimpleType("Never")
    private val intType = findSimpleType("Int")
    private val booleanType = findSimpleType("Boolean")

    companion object {
        fun transform(program: LstProgram, collector: ErrorCollector) {
            program.functions.forEach { (_, func) ->
                SmTransformer(func.body, func.smBody, program, collector).run()
            }
        }
    }

    private fun run() {
        // Populate type ids
        input.nodes.filterIsInstance<LstExpression>().forEach { expr ->
            expr.mapTypes { it.typeId; it }
        }

        input.variables.values.forEach {
            if (!it.isParam) {
                val type = it.type ?: error("[${it.span}] Variable has not type: $it")

                output.variables += SmLocalVar(
                    name = it.finalName,
                    type = type,
                )

                if (type.isGeneric()) {
                    output.variables += SmLocalVar(
                        name = it.finalName + GENERIC_SUFFIX,
                        type = intType,
                    )
                }
            }
        }
        input.nodes.forEach { transformNode(it) }

        if (!input.returnType!!.isNothing() && input.lastExpression != null) {
            val lastExpr = input.getNode(input.lastExpression!!) as LstExpression
            load(span = lastExpr.span, expr = lastExpr)
        }

        val auxNames = mutableSetOf<String>()

        output.inst.indices.forEach { index ->

            when (val inst = output.inst[index]) {
                is SmSaveAux -> {
                    var name = "\$aux#${inst.ref.id}"
                    if (inst.isGeneric) name += GENERIC_SUFFIX

                    if (name !in auxNames) {
                        auxNames += name
                        output.variables += SmLocalVar(
                            name = name,
                            type = inst.type,
                        )
                    }

                    output.inst[index] = SmSaveVar(
                        span = inst.span,
                        name = name,
                        type = inst.type,
                    ).comment(inst.comment)
                }

                is SmLoadAux -> {
                    var name = "\$aux#${inst.ref.id}"
                    if (inst.isGeneric) name += GENERIC_SUFFIX

                    if (name !in auxNames) {
                        auxNames += name
                        output.variables += SmLocalVar(
                            name = name,
                            type = inst.type,
                        )
                    }

                    output.inst[index] = SmLoadVar(
                        span = inst.span,
                        name = name,
                        type = inst.type,
                    ).comment(inst.comment)
                }

                else -> {}
            }
        }
    }

    private fun mark(comment: String) {
        output.inst += SmNop(span = Span.internal()).comment("--------------------------- $comment")
    }

    private fun transformNode(node: LstNode) {
        when (node) {
            is LstComment -> {
                mark(node.comment)
                output.inst += SmNop(
                    span = node.span,
                )
            }

            is LstBoolean -> {
                mark(node.toString())
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstBoolean(node.value),
                    type = type
                )

                store(node, type)
            }

            is LstFloat -> {
                mark(node.toString())
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstFloat(node.value),
                    type = type
                )

                store(node, type)
            }

            is LstInt -> {
                mark(node.toString())
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(node.value),
                    type = type
                )

                store(node, type)
            }

            is LstString -> {
                mark(node.toString())
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstString(node.value),
                    type = type
                )

                store(node, type)
            }

            is LstNothing -> {
                mark(node.toString())
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstNothing,
                    type = type
                )

                store(node, type)
            }

            is LstAlloc -> {
                mark(node.toString())
                val type = node.type ?: error("Invalid state")
                val alloc = program.getFunction("memory_alloc")

                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(type.heapSize),
                    type = intType
                )

                output.inst += SmCall(
                    span = node.span,
                    name = "$${alloc.finalName}",
                    args = 1,
                    result = intType
                )

                store(node, type)

                if (type.base is StructType && type.isOptionItem()) {
                    val option = program.options[type.base.struct.parentOption]!!
                    val index = option.items.indexOf(type.base.struct.ref)

                    load(span = node.span, expr = node, type = intType)

                    output.inst += SmConst(
                        span = node.span,
                        value = ConstInt(index),
                        type = intType
                    )

                    output.inst += SmWrite(
                        span = node.span,
                        type = intType
                    ).comment("Int")
                }
            }

            is LstFunCall -> {
                mark(node.toString())
                val func = node.function ?: error("Function not linked!")
                val result = func.returnType!!

                node.arguments.forEachIndexed { index, argRef ->
                    val arg = input.getNode(argRef) as LstExpression
                    val param = func.params[index]

                    load(span = node.span, expr = arg, generic = param.type!!.isGeneric())
                }

                val annotation = func.getAnnotation(ANNOTATION_WASM_INLINE)

                if (annotation != null) {
                    val opcode = (annotation.args["opcode"] as? ConstString)?.value

                    if (opcode == null) {
                        collector.report("Missing value for opcode", func.span)
                        return
                    }

                    output.inst += SmOpcode(
                        span = node.span,
                        name = opcode,
                        args = func.params.size,
                        result = result
                    )
                } else {
                    output.inst += SmCall(
                        span = node.span,
                        name = "$${func.finalName}",
                        args = func.params.size,
                        result = result
                    )
                }

                if (!result.isNothing() && !result.isNever()) {
                    store(node, result)
                }

                if (result.isNever()) {
                    output.inst += SmOpcode(
                        span = node.span,
                        name = "unreachable",
                        args = 0,
                        result = neverType,
                    )
                }
            }

            is LstReturn -> {
                mark(node.toString())
                val arg = input.getNode(node.expr) as LstExpression
                load(span = node.span, expr = arg, generic = arg.type!!.isGeneric())

                output.inst += SmOpcode(
                    span = node.span,
                    name = "return",
                    args = 1,
                    result = null,
                )
            }

            is LstSizeOf -> {
                mark(node.toString())
                val type = node.typeTree!!
                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(type.heapSize),
                    type = type
                )

                store(node, node.type!!)
            }

            is LstIfStart -> {
                mark(node.toString())
                val arg = input.getNode(node.cond) as LstExpression
                load(span = node.span, expr = arg)

                output.inst += SmIf(span = node.span)
            }

            is LstIfElse -> {
                mark(node.toString())
                output.inst += SmElse(span = node.span)
            }

            is LstIfEnd -> {
                mark(node.toString())
                output.inst += SmEnd(span = node.span)
            }

            is LstIfChoose -> {
                mark(node.toString())
                val type = node.type!!

                val ifTrue = input.getNode(node.ifTrue) as LstExpression
                val ifFalse = input.getNode(node.ifFalse) as LstExpression
                val cond = input.getNode(node.cond) as LstExpression

                // TODO generics
                load(span = node.span, expr = ifTrue, type = type)
                load(span = node.span, expr = ifFalse, type = type)
                load(span = node.span, expr = cond, type = booleanType)

                output.inst += SmOpcode(
                    span = node.span,
                    name = "select",
                    args = 3,
                    result = type
                )

                store(node, type)
            }

            is LstLoopStart -> {
                mark(node.toString())
                output.inst += SmBlock(span = node.span)
                output.inst += SmLoop(span = node.span)
            }

            is LstLoopEnd -> {
                mark(node.toString())
                output.inst += SmEnd(span = node.span)
                output.inst += SmEnd(span = node.span)
            }

            is LstLoopJump -> {
                mark(node.toString())
                val diff = node.block.depth - node.loopBlock!!.depth

                output.inst += SmBranch(span = node.span, level = diff)
            }

            is LstWhenStart -> {
                mark(node.toString())
                output.inst += SmBlock(span = node.span)
            }

            is LstWhenEnd -> {
                mark(node.toString())
                output.inst += SmEnd(span = node.span)
            }

            is LstWhenJump -> {
                mark(node.toString())
                val diff = node.block.depth - node.whenBlock.depth

                output.inst += SmBranch(span = node.span, level = diff)
            }

            is LstLoadVar -> {
                mark(node.toString())
                val type = node.type!!

                if (node.varRef is ConstRef) {
                    output.inst += SmLoadGlobal(
                        span = node.span,
                        name = node.finalName,
                        type = type,
                    )
                } else {
                    output.inst += SmLoadVar(
                        span = node.span,
                        name = node.finalName,
                        type = type,
                    )
                }

                if (type.isGeneric()) {
                    if (node.varRef is ConstRef) {
                        output.inst += SmLoadGlobal(
                            span = node.span,
                            name = node.finalName + GENERIC_SUFFIX,
                            type = intType,
                        )
                    } else {
                        output.inst += SmLoadVar(
                            span = node.span,
                            name = node.finalName + GENERIC_SUFFIX,
                            type = intType,
                        )
                    }
                }

                store(node, type)
            }

            is LstStoreVar -> {
                mark(node.toString())
                val type = node.type!!
                val value = input.getNode(node.expr) as LstExpression

                load(span = node.span, expr = value, generic = type.isGeneric())
                if (type.isGeneric()) {
                    if (node.varRef is ConstRef) {
                        output.inst += SmSaveGlobal(
                            span = node.span,
                            name = node.finalName + GENERIC_SUFFIX,
                            type = intType,
                        )
                    } else {
                        output.inst += SmSaveVar(
                            span = node.span,
                            name = node.finalName + GENERIC_SUFFIX,
                            type = intType,
                        )
                    }
                }

                if (node.varRef is ConstRef) {
                    output.inst += SmSaveGlobal(
                        span = node.span,
                        name = node.finalName,
                        type = type,
                    )
                } else {
                    output.inst += SmSaveVar(
                        span = node.span,
                        name = node.finalName,
                        type = type,
                    )
                }
            }

            is LstLoadField -> {
                mark(node.toString())
                val instance = input.getNode(node.instance) as LstExpression
                val field = node.field!!

                val offset = getFieldOffset(field, instance.type!!)

                load(span = node.span, expr = instance)

                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(offset),
                    type = intType,
                )

                output.inst += SmOpcode(
                    span = node.span,
                    name = "$INT_TYPE.add",
                    args = 2,
                    result = intType,
                )

                output.inst += SmRead(
                    span = node.span,
                    type = node.type!!,
                )

                if (node.type!!.isGeneric()) {
                    load(span = node.span, expr = instance)

                    output.inst += SmConst(
                        span = node.span,
                        value = ConstInt(offset + PTR_SIZE),
                        type = intType,
                    )

                    output.inst += SmOpcode(
                        span = node.span,
                        name = "$INT_TYPE.add",
                        args = 2,
                        result = intType,
                    )

                    output.inst += SmRead(
                        span = node.span,
                        type = node.type!!,
                    )
                }

                store(node, node.type!!)
            }

            is LstStoreField -> {
                mark(node.toString())
                val instance = input.getNode(node.instance) as LstExpression
                val field = node.field!!
                val value = input.getNode(node.expr) as LstExpression

                val offset = getFieldOffset(field, instance.type!!)

                if (!node.field!!.type!!.isGeneric()) {
                    // ptr
                    load(span = node.span, expr = instance)

                    output.inst += SmConst(
                        span = node.span,
                        value = ConstInt(offset),
                        type = intType,
                    )

                    output.inst += SmOpcode(
                        span = node.span,
                        name = "$INT_TYPE.add",
                        args = 2,
                        result = intType,
                    )

                    load(span = node.span, expr = value)

                    // ptr, value -> $INT_TYPE.store
                    output.inst += SmWrite(
                        span = node.span,
                        type = field.type!!,
                    ).comment(field.type!!.toString())
                } else {
                    // ptr
                    load(span = node.span, expr = instance)

                    // value offset
                    output.inst += SmConst(
                        span = node.span,
                        value = ConstInt(offset),
                        type = intType,
                    )

                    output.inst += SmOpcode(
                        span = node.span,
                        name = "$INT_TYPE.add",
                        args = 2,
                        result = intType,
                    )

                    // value
                    load(span = node.span, expr = value, generic = false)

                    // ptr, value -> $INT_TYPE.store
                    output.inst += SmWrite(
                        span = node.span,
                        type = field.type!!,
                    ).comment(field.type!!.toString())

                    // ptr
                    load(span = node.span, expr = instance)

                    // ty offset
                    output.inst += SmConst(
                        span = node.span,
                        value = ConstInt(offset + PTR_SIZE),
                        type = intType,
                    )

                    output.inst += SmOpcode(
                        span = node.span,
                        name = "$INT_TYPE.add",
                        args = 2,
                        result = intType,
                    )

                    // ty
                    loadGenericTy(value, value.type!!)

                    // ptr+4, ty -> $INT_TYPE.store
                    output.inst += SmWrite(
                        span = node.span,
                        type = intType,
                    ).comment("Int")
                }
            }

            is LstAsType -> {
                // TODO generics
                mark(node.toString())
                val expr = input.getNode(node.expr) as LstExpression

                val exprBase = expr.type!!.base
                if (exprBase !is OptionType) {
                    collector.report("The 'as' operator is only supported for option types", node.span)
                    return
                }
                val typeBase = node.type!!.base
                if (typeBase !is StructType) {
                    collector.report("The 'as' operator must be used with an Option and an Option variant", node.span)
                    return
                }

                val index = exprBase.option.items.indexOf(typeBase.struct.ref)

                if (index == -1) {
                    collector.report(
                        "The struct '${typeBase.struct.fullName}' is not a valid variant " +
                                "of the '${exprBase.option.fullName}' option",
                        node.span
                    )
                    return
                }

                load(span = node.span, expr = expr)
                store(node, node.type!!)
            }

            is LstIsType -> {
                // TODO generics
                mark(node.toString())
                val expr = input.getNode(node.expr) as LstExpression

                val exprBase = expr.type!!.base
                if (exprBase !is OptionType) {
                    collector.report("The 'is' operator is only supported for option types", node.span)
                    return
                }
                val typeBase = node.typeTree!!.base
                if (typeBase !is StructType) {
                    collector.report("The 'is' operator must be used with an Option and an Option variant", node.span)
                    return
                }

                val index = exprBase.option.items.indexOf(typeBase.struct.ref)

                if (index == -1) {
                    collector.report(
                        "The struct '${typeBase.struct.fullName}' is not a valid variant " +
                                "of the '${exprBase.option.fullName}' option",
                        node.span
                    )
                    return
                }

                load(span = node.span, expr = expr)

                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(index),
                    type = type
                ).comment("Variant $index => ${exprBase.option.items[index]}")

                val func = program.getFunction("internal_is_variant")

                output.inst += SmCall(
                    span = node.span,
                    name = "$${func.finalName}",
                    args = func.params.size,
                    result = booleanType
                )

                store(node, booleanType)
            }
        }
    }

    private fun load(
        expr: LstExpression,
        type: TypeTree? = null,
        generic: Boolean = false,
        span: Span = expr.span,
    ): SmNode {
        val ty = type ?: expr.type!!
        if (generic) {
            return loadGeneric(expr, ty)
        }

        val inst = SmLoadAux(span = span, ref = expr.ref, type = ty)
        output.inst += inst
        return inst
    }

    private fun loadGeneric(expr: LstExpression, type: TypeTree): SmNode {
        val inst = SmLoadAux(span = expr.span, ref = expr.ref, type = type)
        output.inst += inst

        loadGenericTy(expr, type)
        return inst
    }

    private fun loadGenericTy(expr: LstExpression, type: TypeTree) {
        if (type.isGeneric()) {
            output.inst += SmLoadAux(span = expr.span, ref = expr.ref, type = intType, isGeneric = true)
        } else {
            output.inst += SmConst(span = expr.span, value = ConstInt(type.typeId), type = intType)
        }
    }

    private fun store(node: LstNode, type: TypeTree): SmNode {
        if (type.isGeneric()) {
            return storeGeneric(node, type)
        }
        val inst = SmSaveAux(span = node.span, ref = node.ref, type = type)
        output.inst += inst
        return inst
    }

    private fun storeGeneric(node: LstNode, type: TypeTree): SmNode {
        output.inst += SmSaveAux(span = node.span, ref = node.ref, type = intType, isGeneric = true)
        val inst = SmSaveAux(span = node.span, ref = node.ref, type = type)
        output.inst += inst
        return inst
    }

    private fun findSimpleType(name: String): TypeTree {
        val struct = program.structs.values.find { it.fullName == name } ?: error("Type '$name' not found")
        return TypeTree(base = StructType(struct))
    }

    private fun SmNode.comment(str: String): SmNode {
        this.comment = str
        return this
    }

    private fun getFieldOffset(field: LstStructureField, structType: TypeTree): Int {
        var offset = 0

        if (structType.isOptionItem()) {
            offset += PTR_SIZE
        }

        val struct = (structType.base as StructType).struct
        val allFields = struct.fields.values.sortedBy { it.index }

        repeat(field.index) { index ->
            val ty = allFields[index].type!!

            offset += ty.stackSize
            // stackSize already computes the size as a fat pointer
//            if (ty.isGeneric()) {
//                offset += PTR_SIZE
//            }
        }

        return offset
    }

    private val TypeTree.typeId: Int
        get() = validTypes.getOrPut(this) { validTypes.size }

    private val TypeTree.stackSize: Int
        get() = when (this.base) {
            InvalidType -> error("Invalid type!")
            is UnresolvedType -> error("Unresolved type!")
            is ParamType -> GENERIC_SIZE
            is StructType -> PTR_SIZE
            is OptionType -> PTR_SIZE
        }

    private val TypeTree.heapSize: Int
        get() = when (val base = this.base) {
            InvalidType -> error("Invalid type!")
            is UnresolvedType -> error("Unresolved type!")
            is ParamType -> GENERIC_SIZE
            is StructType -> base.struct.heapSize
            is OptionType -> {
                base.option.items.maxOf { program.structs[it]!!.heapSize }
            }
        }

    private val LstStruct.heapSize: Int
        get() {
            if (this.getAnnotation(ANNOTATION_STACK_VALUE) != null) {
                return PTR_SIZE
            }

            var total = STRUCT_HEADER_SIZE

            // Store the option variant index
            if (this.parentOption != null) {
                total += PTR_SIZE
            }

            this.fields.values.forEach {
                total += it.type!!.stackSize
            }

            return total
        }
}