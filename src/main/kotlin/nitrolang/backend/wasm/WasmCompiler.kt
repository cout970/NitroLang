package nitrolang.backend.wasm

import nitrolang.ANNOTATION_EXTERN
import nitrolang.MAIN_FUNCTION_NAME
import nitrolang.ANNOTATION_WASM_INLINE
import nitrolang.ast.*
import nitrolang.sm.*
import nitrolang.util.ErrorCollector
import nitrolang.util.indent
import java.lang.Appendable

const val INT_TYPE = "i32"
const val INT_SIZE: Int = 4

const val PTR_TYPE = "i32"
const val PTR_SIZE: Int = 4

const val GENERIC_TYPE = "i32"
const val GENERIC_SUFFIX = "_ty"
const val GENERIC_SIZE: Int = 8
const val STRUCT_HEADER_SIZE: Int = 0

class WasmCompiler(
    private val program: LstProgram,
    private val root: WasmNode,
    private val collector: ErrorCollector,
) {

    private val stack = ArrayDeque<Ref>()
    private val validTypes = mutableMapOf<TypeTree, Int>()
    private var dataOffset = 0

    companion object {
        fun compile(program: LstProgram, output: Appendable, collector: ErrorCollector) {
            val root = node("module")
            val compiler = WasmCompiler(program, root, collector)

            DeadCodeAnalyzer.markDeadCode(program)

            compiler.init()

            output.append(root.toString())
        }
    }

    private fun init() {
        SmTransformer.transform(program, collector)

        // Imports
        program.functions.values.forEach { func ->
            if (func.isExternal) {
                compileFunction(func)
            }
        }

        // Memory
        root += node("memory", node("export", "memory".surround()), 1)

        // Null pointer points to 0, lets not have something important there
        root += node("data", node("$INT_TYPE.const", dataOffset), intToWasmHex(0).surround()).comment("null")
        dataOffset += PTR_SIZE
        val heapStart = node("data", node("$INT_TYPE.const", dataOffset)).comment("end of data region")
        dataOffset += PTR_SIZE
        root += heapStart

        // Functions
        program.functions.values.forEach { func ->
            if (!func.isExternal) {
                compileFunction(func)
            }
        }

        padOffset()
        heapStart += node(intToWasmHex(dataOffset).surround())
    }

    private fun compileFunction(func: LstFunction) {
        if (func.isDeadCode || func.getAnnotation(ANNOTATION_WASM_INLINE) != null) {
            return
        }

        val extern = func.getAnnotation(ANNOTATION_EXTERN)
        val output = node("func")

        output += node("\$${func.ref}").comment(func.fullName)

        if (func.fullName == MAIN_FUNCTION_NAME) {
            output += node("export", "main".surround())
        } else if(extern == null) {
            output += node("export", "fun_${func.ref.id}".surround())
        }

        if (extern == null) {
            func.params.forEach { param ->
                if (!param.type!!.isGeneric()) {
                    output += node("param", param.variable!!.finalName, typeToWasm(param.type))
                } else {
                    output += node("param", param.variable!!.finalName, typeToWasm(param.type))
                    output += node("param", param.variable!!.finalName + GENERIC_SUFFIX, "i32")
                }
            }
        } else {
            val params = node("param")

            func.params.forEach { param ->
                if (!param.type!!.isGeneric()) {
                    params += node(typeToWasm(param.type))
                } else {
                    params += node(typeToWasm(param.type))
                    params += node("i32")
                }
            }

            if (params.children.isNotEmpty()) {
                output += params
            }
        }

        if (!func.returnType!!.isUnit()) {
            output += if (!func.returnType!!.isGeneric()) {
                node("result", typeToWasm(func.returnType))
            } else {
                node("result", typeToWasm(func.returnType), "i32")
            }
        }

        if (extern != null) {
            val name = (extern.args["name"] as? ConstString)?.value

            if (name == null) {
                collector.report(
                    "Function '${func.fullName}' is annotated with @Extern but the arg 'name' is missing",
                    extern.span
                )
                return
            }

            root += node("import", "core".surround(), name.surround(), output)
            return
        }

        compileSmCode(func.smBody, output)

        root += output
    }

    private fun compileSmCode(code: SmCode, output: WasmNode) {
        code.variables.forEach { variable ->
            output += node("local", variable.name, typeToWasm(variable.type)).comment(variable.name)
        }
        code.inst.forEach { inst ->
            compileInst(inst, output)
        }
    }

    private fun compileInst(inst: SmNode, output: WasmNode) {
        when (inst) {
            is SmNop -> {
                output += node("").comment(inst.comment)
            }

            is SmConst -> {
                output += when (val v = inst.value) {
                    is ConstBoolean -> node("i32.const", if (v.value) 1 else 0)
                    is ConstFloat -> node("f32.const", v.value)
                    is ConstInt -> node("i32.const", v.value)
                    is ConstString -> compileString(v.value)
                    ConstUnit -> node("i32.const", 0)
                }.comment(inst.comment)
            }

            is SmIf -> {
                output += node("if").comment(inst.comment)
            }

            is SmElse -> {
                output += node("else").comment(inst.comment)
            }

            is SmEnd -> {
                output += node("end").comment(inst.comment)
            }

            is SmBlock -> {
                output += node("block").comment(inst.comment)
            }

            is SmLoop -> {
                output += node("loop").comment(inst.comment)
            }

            is SmBranch -> {
                output += node("br", inst.level).comment(inst.comment)
            }

            is SmCall -> {
                output += node("call", inst.name).comment(inst.comment)
            }

            is SmOpcode -> {
                output += node(inst.name).comment(inst.comment)
            }

            is SmLoadAux -> error("Unreachable")
            is SmSaveAux -> error("Unreachable")

            is SmLoadVar -> {
                output += node("local.get", inst.name).comment(inst.comment)
            }

            is SmLoadGlobal -> {
                output += node("global.get", inst.name).comment(inst.comment)
            }

            is SmSaveVar -> {
                output += node("local.set", inst.name).comment(inst.comment)
            }

            is SmSaveGlobal -> {
                output += node("global.set", inst.name).comment(inst.comment)
            }

            is SmRead -> {
                output += node("${typeToWasm(inst.type)}.load").comment(inst.comment)
            }

            is SmWrite -> {
                output += node("${typeToWasm(inst.type)}.store").comment(inst.comment)
            }
        }
    }

    private fun compileString(value: String): WasmNode {
        // Align to 4 bytes for the 32bit length field
        padOffset()

        val bytes = value.encodeToByteArray()
        val start = dataOffset
        dataOffset += PTR_SIZE
        dataOffset += bytes.size

        val size = intToWasmHex(bytes.size)
        val contents = bytes.joinToString("") {
            "\\" + it.toUByte().toString(16).padStart(2, '0')
        }

        root += node("data", node("i32.const", start), "$size$contents".surround())
            .comment("offset: 0x${start.toString(16)}")

        return node("$PTR_TYPE.const", start)
    }

    private fun padOffset() {
        // Align to 4 bytes for the 32bit length field
        val pad = (PTR_SIZE - dataOffset) % PTR_SIZE
        dataOffset += if (pad < 0) pad + PTR_SIZE else pad
    }

    private fun typeToWasm(type: TypeTree?): String {
        if (type == null) error("Type is null!")

        if (type.base is StructType && type.base.struct.name == "Float") {
            return "f32"
        }

        if (type.base is StructType) {
            return PTR_TYPE
        }

        if (type.base is ParamType) {
            return GENERIC_TYPE
        }

        return INT_TYPE
    }

    private fun intToWasmHex(num: Int): String {
        val hex = num.toString(16).padStart(8, '0')
        val a0 = hex.substring(0, 2)
        val a1 = hex.substring(2, 4)
        val a2 = hex.substring(4, 6)
        val a3 = hex.substring(6, 8)

        return "\\$a3\\$a2\\$a1\\$a0"
    }

    private fun wasmValueTypeSize(type: TypeTree?): Int {
        if (type == null) error("Type is null!")

        return when (val base = type.base) {
            InvalidType -> error("Invalid type!")
            is UnresolvedType -> error("Unresolved type!")
            is ParamType -> PTR_SIZE
            is StructType -> wasmStructSize(base.struct)
            is OptionType -> {
                base.option.items.maxOf { wasmStructSize(program.structs[it]!!) }
            }
        }
    }

    private fun wasmStructSize(str: LstStruct): Int {
        if (str.getAnnotation(ANNOTATION_EXTERN) != null) {
            return PTR_SIZE
        }

        // Minimum size is 32bit, for the type id for runtime checks
        var total = PTR_SIZE

        str.fields.values.forEach {
            total += wasmValueTypeSize(it.type)
        }

        return total
    }

    private fun wasmTypeId(type: TypeTree): Int {
        return validTypes.getOrPut(type) { validTypes.size }
    }

    private fun TypeTree.isStackBased(): Boolean {
        return base is StructType && base.struct.getAnnotation("StackValue") != null
    }

    private fun String.surround(): String = "\"$this\""
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
            if (name == "i32.store" || name == "i64.store" || name == "f32.store" || name == "f64.store" || name == "unreachable") {
                return "($name)$postfix"
            }

            return "$name$postfix"
        }

        if (name == "func") {
            var code = false
            return buildString {
                append("(")
                append(name)
                children.forEach { child ->
                    if (child.name == "local") code = true
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