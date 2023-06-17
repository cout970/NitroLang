package nitrolang

import nitrolang.ast.LstProgram
import nitrolang.backend.wasm.WasmCompiler
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import java.io.File

fun main() {
    val errors = ErrorCollector()
    val program = LstProgram()

    AstParser.parseFile(SourceFile.load("src/main/nitro/core.nl"), errors, program)
    AstParser.parseFile(SourceFile.load("example.nl"), errors, program)

    if (!errors.isEmpty()) {
        System.err.println(errors.toString())
        return
    }

    val output = File("build/output.wat")

    output.bufferedWriter().use { WasmCompiler.compile(program, it, errors) }

    if (!errors.isEmpty()) {
        System.err.println(errors.toString())
        return
    }

    println("Calling wat2wasm")
    Runtime.getRuntime().exec("wat2wasm build/output.wat -o build/output.wasm").waitFor()
    println("Done")
}