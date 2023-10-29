package nitrolang.backend.wasm

class WasmPrint(val out: Output)

fun WasmPrint.module(module: WasmModule) {
    out += "("
    out.indent()
    out += "module"
    out.nl()

    for (import in module.imports) {
        out += "(import "
        out.string(import.module)
        out.sep()
        out.string(import.function)
        out.sep()
        functionDef(import.functionDefinition, end = true)
        out += ")"
        out.nl()
    }

    if (module.lambdaLabels.isNotEmpty()) {
        out += "(table \$lambdas funcref"
        out.sep()
        out += "(elem"
        for (label in module.lambdaLabels) {
            out.sep()
            out += "$"
            out += label
        }
        out += ")"
        out += ")"
        out.nl()
    }

    out += "(memory (export"
    out.sep()
    out.string("memory")
    out += ")"
    out.sep()
    out += "16" // 1 MB
    out += ")"
    out.nl()

    for ((offset, content) in module.sections) {
        out += "(data (i32.const $offset)"
        out.sep()
        out.string(content)
        out += ")"
        out.nl()
    }

    for (func in module.functions) {
        functionDef(func.definition, end = false)
        out.nl()

        for (local in func.locals) {
            out += "(local"
            out.sep()
            if (local.name != null) {
                out += local.name
                out.sep()
            }
            out += local.type
            out += ")"
            out.nl()
        }

        for (inst in func.instructions) {
            if (inst.needsWrapping) {
                out += "("
                out += inst.opcode
                out += ")"
            } else {
                if (inst.opcode == "end" || inst.opcode == "else") {
                    out.deIndent()
                }

                out += inst.opcode

                if (inst.opcode == "if" || inst.opcode == "else" || inst.opcode == "loop" || inst.opcode == "block") {
                    out.indent()
                }
            }

            if (inst.comment != null) {
                out.sep()
                out += "(; "
                out += inst.comment!!
                out += " ;)"
            }

            out.nl()
        }

        out.deIndent()
        out += ")"
        out.nl()
    }

    out.deIndent()
    out += ")"
}

fun WasmPrint.functionDef(def: WasmFunctionDefinition, end: Boolean) {
    out += "(func "
    out.indent()
    out += def.name
    out.sep()

    if (def.exportName.isNotEmpty()) {
        out += "(export"
        out.sep()
        out.string(def.exportName)
        out += ")"
        out.sep()
    }

    out += "(; "
    out += def.comment
    out += " ;)"
    out.sep()

    for (param in def.params) {
        out += "(param "
        if (param.name != null) {
            out += param.name
            out += " "
        }
        out += param.type.toString()
        out += ")"
        out.sep()
    }

    for (result in def.results) {
        out += "(result "
        out += result
        out += ")"
        out.sep()
    }

    if (end) {
        out.deIndent()
        out += ")"
    }
}

class Output(private val appendable: Appendable) {
    private var indent: Int = 0
    private var nl = false
    private var sep = false

    operator fun plusAssign(str: String) {
        if (str == "\n") {
            nl = true
            return
        }

        if (nl) {
            nl = false
            sep = false
            appendable.append("\n")
            repeat(indent) {
                appendable.append("  ")
            }
        }

        if (sep) {
            sep = false
            appendable.append(" ")
        }
        appendable.append(str)
    }

    operator fun plusAssign(prim: WasmPrimitive) {
        this += prim.toString()
    }

    fun sep() {
        sep = true
    }

    fun string(str: String) {
        this += "\""
        this += str
        this += "\""
    }

    fun indent() {
        indent++
    }

    fun deIndent() {
        if (indent > 0) indent--
    }

    fun nl() {
        nl = true
    }
}