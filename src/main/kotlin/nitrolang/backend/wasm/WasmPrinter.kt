package nitrolang.backend.wasm

// Utility to convert a WasmModule to a string in WebAssembly WAT format
class WasmPrinter(private val appendable: Appendable) {
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

    operator fun String.unaryPlus() {
        this@WasmPrinter += this
    }
}

fun WasmPrinter.module(module: WasmModule) {
    +"("
    indent()
    +"module"
    nl()

    for (import in module.imports) {
        +"(import "
        string(import.module)
        sep()
        string(import.functionName)
        sep()
        functionDef(import.function, end = true)
        +")"
        nl()
    }

    if (module.lambdaLabels.isNotEmpty()) {
        +"(table \$lambdas funcref"
        sep()
        +"(elem"
        for (label in module.lambdaLabels) {
            sep()
            +"$"
            +label
        }
        +")"
        +")"
        nl()
    }

    +"(memory (export"
    sep()
    string("memory")
    +")"
    sep()
    +"16" // 1 MB
    +")"
    nl()

    for ((offset, content) in module.sections) {
        +"(data (i32.const $offset)"
        sep()
        string(content)
        +")"
        nl()
    }

    for (func in module.functions) {
        functionDef(func, end = false)
        nl()

        for (local in func.locals) {
            +"(local"
            sep()
            if (local.name != null) {
                +local.name
                sep()
            }
            +local.type.toString()
            +")"
            nl()
        }

        for (inst in func.instructions) {
            if (inst.needsWrapping) {
                +"("
                +inst.opcode
                +")"
            } else {
                if (inst.opcode == "end" || inst.opcode == "else") {
                    deIndent()
                }

                +inst.opcode

                if (inst.opcode == "if" || inst.opcode == "else" || inst.opcode == "loop" || inst.opcode == "block") {
                    indent()
                }
            }

            if (inst.comment != null) {
                sep()
                +"(; "
                +inst.comment!!
                +" ;)"
            }

            nl()
        }

        deIndent()
        +")"
        nl()
    }

    deIndent()
    +")"
}

fun WasmPrinter.functionDef(def: WasmFunction, end: Boolean) {
    +"(func "
    indent()
    +def.name
    sep()

    if (def.exportName.isNotEmpty()) {
        +"(export"
        sep()
        string(def.exportName)
        +")"
        sep()
    }

    +"(; "
    +def.comment
    +" ;)"
    sep()

    for (param in def.params) {
        +"(param "
        if (param.name != null) {
            +param.name
            +" "
        }
        +param.type.toString()
        +")"
        sep()
    }

    for (result in def.results) {
        +"(result "
        +result.toString()
        +")"
        sep()
    }

    if (end) {
        deIndent()
        +")"
    }
}
