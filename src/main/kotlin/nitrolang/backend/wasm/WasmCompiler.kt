package nitrolang.backend.wasm

import nitrolang.EXTERN_NAME
import nitrolang.MAIN_FUNCTION_NAME
import nitrolang.WASM_INLINE_NAME
import nitrolang.ast.*
import nitrolang.util.ErrorCollector
import nitrolang.util.indent
import java.lang.Appendable

class WasmCompiler(
    private val program: LstProgram,
    private val root: WasmNode,
    private val collector: ErrorCollector,
) {

    companion object {
        fun compile(program: LstProgram, output: Appendable, collector: ErrorCollector) {
            val root = node("module")
            val compiler = WasmCompiler(program, root, collector)

            DeadCodeAnalyzer.markDeadCode(program)

            program.functions.values.forEach { func ->
                compiler.compileFunction(func)
            }

            output.append(root.toString())
        }
    }

    private fun compileFunction(func: LstFunction) {
        if (func.isDeadCode || func.annotations.any { it.name == WASM_INLINE_NAME }) {
            return
        }

        val output = node("func")

        output += node("\$${func.ref}").comment(func.fullName)

        if (func.fullName == MAIN_FUNCTION_NAME) {
            output += node("export", "\"main\"")
        }

        val params = node("param")

        func.params.forEach {
            params += node(typeToWasm(it.type))
        }

        if (params.children.isNotEmpty()) {
            output += params
        }

        output += node("result", typeToWasm(func.returnType))

        val extern = func.annotations.find { it.name == EXTERN_NAME }

        if (extern != null) {
            val name = (extern.args["name"] as? LstConstString)?.value

            if (name == null) {
                collector.report(
                    "Function '${func.fullName}' is annotated with @Extern but the arg 'name' is missing",
                    extern.span
                )
                return
            }

            root += node("import", "\"core\"", "\"$name\"", output)
            return
        }

        compileCode(func.body, output)

        root += output
    }

    private fun compileCode(code: LstCode, output: WasmNode) {
        code.variables.forEach { (_, variable) ->
            output += node("local", "\$var_${variable.ref.id}", typeToWasm(variable.type))
        }

        code.nodes.forEach { node ->
            compileNode(node, output)
        }
    }

    private fun compileNode(node: LstNode, output: WasmNode) {
        return when (node) {
            is LstComment -> {
                output += node("").comment(node.comment)
            }

            is LstDup -> {
                output += node("").comment(node.toString())
            }

            is LstAlloc -> {
                output += node("i32.const", wasmTypeSize(node.type))
                output += node("call", "\$alloc")
                    .comment("Alloc ${node.type}")
            }

            is LstAsType -> {
                // No-op
                output += node("").comment(node.toString())
            }

            is LstChoose -> TODO()
            is LstBoolean -> {
                output += node("i32.const", if (node.value) 1 else 0)
                    .comment(node.toString())
            }

            is LstFloat -> {
                output += node("f32.const", node.value)
                    .comment(node.toString())
            }

            is LstInt -> {
                output += node("i32.const", node.value)
                    .comment(node.toString())
            }

            is LstString -> TODO("LstString")
            is LstUnit -> {
                output += node("i32.const", 0)
                    .comment(node.toString())
            }

            is LstFunArg -> {
                output += node("").comment(node.toString())
            }

            is LstFunCall -> {
                val annotation = node.function!!.annotations.find { it.name == WASM_INLINE_NAME }
                if (annotation != null) {
                    val opcode = (annotation.args["opcode"] as? LstConstString)?.value

                    if (opcode == null) {
                        collector.report("Missing value for opcode", node.function!!.span)
                        return
                    }

                    output += node(opcode).comment(node.toString())
                    return
                }

                output += node("call", "\$${node.funRef}")
                    .comment(node.toString())
            }

            is LstIsType -> TODO()
            is LstLoadField -> TODO()
            is LstLoadVar -> TODO()
            is LstReturn -> TODO()
            is LstSizeOf -> TODO()
            is LstStoreField -> TODO()
            is LstStoreVar -> TODO()
            is LstIfJump -> TODO()
            is LstJumpTo -> TODO()
            is LstMarker -> {
                output += node("").comment(node.toString())
            }
        }
    }

    private fun typeToWasm(type: TypeTree?): String {
        if (type == null) error("Type is null!")

        if (type.base is StructType && type.base.struct.name == "Float") {
            return "f32"
        }

        return "i32"
    }

    private fun wasmTypeSize(type: TypeTree?): Int {
        if (type == null) error("Type is null!")

        return 4
    }
}

data class WasmNode(
    val name: String,
    val children: MutableList<WasmNode> = mutableListOf()
) {
    var comment: String? = null

    fun comment(c: String): WasmNode {
        comment = c
        return this
    }

    operator fun plusAssign(other: WasmNode) {
        children += other
    }

    override fun toString(): String {
        val comm = if (comment != null) "(; $comment ;)" else ""
        if (name == "") {
            return comm
        }

        val postfix = if (comm.isNotEmpty()) " $comm" else ""

        if (children.isEmpty()) {
            return "$name$postfix"
        }

        if (name == "func") {
            var code = false
            return buildString {
                append("(")
                append(name)
                children.forEach { child ->
                    append(if (code) "\n  " else " ")
                    append(child.toString())
                    if (child.name == "result") code = true
                }
                append(")")
                append(postfix)
            }
        }

        val childrenStr = children.map { it.toString() }

        if (name == "module") {
            return "($name\n${indent(childrenStr.joinToString("\n"))})$postfix"
        }

        return "($name ${childrenStr.joinToString(" ")})$postfix"
    }
}

fun node(name: String, vararg other: Any): WasmNode {
    return WasmNode(
        name = name,
        children = other.map { if (it is WasmNode) it else WasmNode(it.toString()) }.toMutableList()
    )
}