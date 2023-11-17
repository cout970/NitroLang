package nitrolang.backend

import nitrolang.backend.wasm.WasmPrimitive

class CodePrinter(private val appendable: Appendable) {
    private var indent: Int = 0
    private var nl = false
    private var sep = false

    operator fun plusAssign(str: String) {
        if (str == "\n") {
            nl = true
            return
        }

        if (nl) {
            forceNl()
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

    fun forceNl() {
        nl = false
        sep = false
        appendable.append("\n")
        repeat(indent) {
            appendable.append("  ")
        }
    }

    operator fun String.unaryPlus() {
        this@CodePrinter += this
    }
}
