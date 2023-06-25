package nitrolang

import nitrolang.ast.LstProgram
import nitrolang.backend.wasm.WasmCompiler
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import kotlin.system.measureNanoTime

fun main() {
    val source = File("example.nl")
    compile(source.path)
    watch(source) { compile(source.path) }
}

fun watch(file: File, changed: () -> Unit) {
    val fs = FileSystems.getDefault()
    fs.newWatchService().use { ws ->
        val key = fs.getPath(file.absoluteFile.parent).register(ws, StandardWatchEventKinds.ENTRY_MODIFY)

        while (true) {
            val curr = ws.take()

            // Required to avoid double fires
            Thread.sleep(50)

            val events = curr.pollEvents()

            events.forEach { event ->
                val changedPath = event.context() as Path

                if (changedPath == file.toPath()) {
                    println("[] Recompiling $changedPath")
                    changed()
                }
            }

            key.reset()
        }
    }
}

fun compile(path: String) {
    val elapsed = measureNanoTime {
        val errors = ErrorCollector()
        val program = LstProgram()

        AstParser.parseFile(SourceFile.load("src/main/nitro/core.nl"), errors, program)
        AstParser.parseFile(SourceFile.load(path), errors, program)

        if (!errors.isEmpty()) {
            System.err.println(errors.toString())
            return
        }

        program.functions.forEach { (_, func) ->
            if (func.fullName != "main") return@forEach
            println("${func.fullName}:")
            func.body.nodes.forEach {
                println("   $it")
            }
            println()
        }

        val output = File("build/output.wat")

        output.bufferedWriter().use { WasmCompiler.compile(program, it, errors) }

        if (!errors.isEmpty()) {
            System.err.println(errors.toString())
            return
        }

//        println("Calling wat2wasm")
        ProcessBuilder("wat2wasm", "build/output.wat", "-o", "src/main/resources/output.wasm")
            .inheritIO()
            .start()
            .waitFor()
    }
    println("[] Compiled in ${elapsed / 1_000_000} ms")

    val rtElapsed = measureNanoTime {
        println("--- Running output.wasm")
        ProcessBuilder("./src/main/resources/wrapper.ts")
            .inheritIO()
            .start()
            .waitFor()
        println("---")
    }
    println("[] Executed in ${rtElapsed / 1_000_000} ms")
}