package nitrolang.sm

import nitrolang.ANNOTATION_STACK_VALUE
import nitrolang.ANNOTATION_WASM_INLINE
import nitrolang.ast.*
import nitrolang.backend.wasm.PTR_SIZE
import nitrolang.backend.wasm.STRUCT_HEADER_SIZE
import nitrolang.util.ErrorCollector
import nitrolang.util.Span

class SmTransformer(
    val input: LstCode,
    val output: SmCode,
    val program: LstProgram,
    val collector: ErrorCollector,
) {

    private val validTypes = mutableMapOf<TypeTree, Int>()
    private val intType = findSimpleType("Int")

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
                output.variables += SmLocalVar(
                    name = it.finalName,
                    type = it.type ?: error("[${it.span}] Variable has not type: $it"),
                )
            }
        }
        input.nodes.forEach { transformNode(it) }

        val auxNames = mutableSetOf<String>()

        output.inst.indices.forEach { index ->

            when (val inst = output.inst[index]) {
                is SmSaveAux -> {
                    val name = "\$aux#${inst.ref.id}"

                    if (name !in auxNames) {
                        auxNames += name
                        output.variables += SmLocalVar(
                            name = name,
                            type = inst.type,
                        )
                    }

                    output.inst[index] = SmSaveVar(
                        span = inst.span,
                        name = name
                    ).comment(inst.comment)
                }

                is SmLoadAux -> {
                    val name = "\$aux#${inst.ref.id}"

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

    private fun mark() {
        output.inst += SmNop(span = Span.internal()).comment("---")
    }

    private fun transformNode(node: LstNode) {
        when (node) {
            is LstComment -> {
                output.inst += SmNop(
                    span = node.span,
                ).comment(node.comment)
                mark()
            }

            is LstBoolean -> {
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstBoolean(node.value),
                    type = type
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstFloat -> {
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstFloat(node.value),
                    type = type
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstInt -> {
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(node.value),
                    type = type
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstString -> {
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstString(node.value),
                    type = type
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstUnit -> {
                val type = node.type ?: error("Invalid state")
                output.inst += SmConst(
                    span = node.span,
                    value = ConstUnit,
                    type = type
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstAlloc -> {
                val type = node.type ?: error("Invalid state")
                val alloc = program.functions.values.find { it.fullName == "alloc" } ?: error("Missing alloc function")

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
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)

                // struct Type
                output.inst += SmLoadAux(span = node.span, ref = node.ref, type = intType)

                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(type.typeId),
                    type = intType
                )

                output.inst += SmWrite(
                    span = node.span,
                    type = intType
                )
                mark()
            }

            is LstFunCall -> {
                val func = node.function ?: error("Function not linked!")
                val result = func.returnType!!

                node.arguments.forEach { argRef ->
                    val arg = input.getNode(argRef) as LstExpression
                    output.inst += SmLoadAux(span = arg.span, ref = arg.ref, type = arg.type!!)
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
                    ).comment(node.toString())
                } else {
                    output.inst += SmCall(
                        span = node.span,
                        name = "$${func.finalName}",
                        args = func.params.size,
                        result = result
                    ).comment(node.toString())
                }

                if (!result.isUnit()) {
                    output.inst += SmSaveAux(span = node.span, ref = node.ref, type = result)
                }
                mark()
            }

            is LstChoose -> {
                // no-op
                mark()
            }

            is LstReturn -> {
                val arg = input.getNode(node.expr) as LstExpression
                output.inst += SmLoadAux(span = arg.span, ref = arg.ref, type = arg.type!!)

                output.inst += SmOpcode(
                    span = node.span,
                    name = "return",
                    args = 1,
                    result = null,
                ).comment(node.toString())
                mark()
            }

            is LstSizeOf -> {
                val type = node.typeTree!!
                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(type.heapSize),
                    type = type
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstIfStart -> {
                val arg = input.getNode(node.cond) as LstExpression
                output.inst += SmLoadAux(
                    span = node.span,
                    ref = arg.ref,
                    type = arg.type!!
                ).comment(node.toString())

                output.inst += SmIf(span = node.span)
                mark()
            }

            is LstIfElse -> {
                output.inst += SmElse(span = node.span).comment(node.toString())
                mark()
            }

            is LstIfEnd -> {
                output.inst += SmEnd(span = node.span).comment(node.toString())
                mark()
            }

            is LstLoopStart -> {
                output.inst += SmBlock(span = node.span)
                output.inst += SmLoop(span = node.span).comment(node.toString())
                mark()
            }

            is LstLoopEnd -> {
                output.inst += SmEnd(span = node.span)
                output.inst += SmEnd(span = node.span)
                mark()
            }

            is LstLoopJump -> {
                val diff = node.block.depth - node.loopBlock!!.depth

                output.inst += SmBranch(span = node.span, level = diff).comment(node.toString())
                mark()
            }

            is LstLoadVar -> {
                val type = node.type!!

                if (node.varRef is ConstRef) {
                    output.inst += SmLoadGlobal(
                        span = node.span,
                        name = node.finalName,
                        type = type,
                    ).comment(node.toString())
                } else {
                    output.inst += SmLoadVar(
                        span = node.span,
                        name = node.finalName,
                        type = type,
                    ).comment(node.toString())
                }
                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstStoreVar -> {
                val type = node.type!!

                output.inst += SmLoadAux(span = node.span, ref = node.expr, type = type)

                if (node.varRef is ConstRef) {
                    output.inst += SmSaveGlobal(
                        span = node.span,
                        name = node.finalName
                    ).comment(node.toString())
                } else {
                    output.inst += SmSaveVar(
                        span = node.span,
                        name = node.finalName
                    ).comment(node.toString())
                }
                mark()
            }

            is LstLoadField -> {
                val type = node.type!!
                val instance = input.getNode(node.instance) as LstExpression
                val field = node.field!!

                output.inst += SmLoadAux(
                    span = node.span,
                    ref = instance.ref,
                    type = instance.type!!,
                ).comment(node.toString())

                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(STRUCT_HEADER_SIZE + field.index * PTR_SIZE),
                    type = intType,
                ).comment(node.toString())

                output.inst += SmOpcode(
                    span = node.span,
                    name = "i32.add",
                    args = 2,
                    result = intType,
                ).comment(node.toString())

                output.inst += SmRead(
                    span = node.span,
                    type = type,
                ).comment(node.toString())

                output.inst += SmSaveAux(span = node.span, ref = node.ref, type = type)
                mark()
            }

            is LstStoreField -> {
                val type = node.type!!
                val instance = input.getNode(node.instance) as LstExpression
                val field = node.field!!
                val value = input.getNode(node.expr) as LstExpression

                // ptr
                output.inst += SmLoadAux(
                    span = node.span,
                    ref = instance.ref,
                    type = instance.type!!,
                ).comment(node.toString())

                output.inst += SmConst(
                    span = node.span,
                    value = ConstInt(STRUCT_HEADER_SIZE + field.index * PTR_SIZE),
                    type = intType,
                ).comment(node.toString())

                output.inst += SmOpcode(
                    span = node.span,
                    name = "i32.add",
                    args = 2,
                    result = intType,
                ).comment(node.toString())

                // value
                output.inst += SmLoadAux(
                    span = node.span,
                    ref = value.ref,
                    type = value.type!!,
                ).comment(node.toString())

                // ptr, value -> i32.store
                output.inst += SmWrite(
                    span = node.span,
                    type = type,
                ).comment(node.toString())
                mark()
            }

            is LstAsType -> {
                TODO()
            }

            is LstIsType -> {
                // val expr = code.getNode(stack.last()) as LstExpression
                //                val static = node.type!!.isStackBased() || expr.type!!.isStackBased()
                //
                //                if (static) {
                //                    // Statically checked that the type on the stack does not match the expected type
                //                    // We can doo this because the only valid values are primitives Int, Float, Unit, Boolean, Ptr
                //                    if (expr.type !== node.type) {
                //                        output += node("drop").comment(node.toString())
                //                        output += node("unreachable").comment(node.toString())
                //                    }
                //                } else {
                //                    val checkCast = program.functions.values.find { it.fullName == "check_cast" }
                //                        ?: error("Missing check_cast function")
                //
                //                    output += node("i32.const", wasmTypeId(node.type!!))
                //                    output += node("call", funcToWasm(checkCast.ref))
                //                        .comment(node.toString())
                //                }
                TODO()
            }
        }
    }

    private fun findSimpleType(name: String): TypeTree {
        val struct = program.structs.values.find { it.fullName == name } ?: error("Type '$name' not found")
        return TypeTree(base = StructType(struct))
    }

    private fun SmNode.comment(str: String): SmNode {
        this.comment = str
        return this
    }

    private val TypeTree.typeId: Int
        get() = validTypes.getOrPut(this) { validTypes.size }

    private val TypeTree.stackSize: Int
        get() = when (this.base) {
            InvalidType -> error("Invalid type!")
            is UnresolvedType -> error("Unresolved type!")
            is ParamType -> PTR_SIZE
            is StructType -> PTR_SIZE
            is OptionType -> PTR_SIZE
        }

    private val TypeTree.heapSize: Int
        get() = when (val base = this.base) {
            InvalidType -> error("Invalid type!")
            is UnresolvedType -> error("Unresolved type!")
            is ParamType -> PTR_SIZE
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

            // Minimum size is 32bit, for the type id for runtime checks
            var total = PTR_SIZE

            this.fields.values.forEach {
                total += it.type!!.stackSize
            }

            return total
        }
}