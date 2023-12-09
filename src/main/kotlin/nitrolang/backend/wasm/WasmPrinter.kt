package nitrolang.backend.wasm

import nitrolang.backend.CodePrinter

// Utility to convert a WasmModule to a string in WebAssembly WAT format
fun CodePrinter.wasmModule(module: WasmModule) {
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
    +(16 * 10).toString() // 10 MB
    +")"
    nl()

    for (section in module.sections) {
        +"(data (i32.const ${section.offset})"
        sep()
        string(section.data.joinToString("") {
            "\\" + it.toUByte().toString(16).padStart(2, '0')
        })
        +") "
        +"(; "
        +section.comment
        +" ;)"
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
            if (local.monoType != null) {
                +" (; "
                +local.monoType.toString()
                +" ;)"
            }
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

fun CodePrinter.functionDef(def: WasmFunction, end: Boolean) {
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

    +"(result "
    +def.result.toString()
    +")"
    sep()

    if (end) {
        deIndent()
        +")"
    }
}
