package nitrolang

import nitrolang.ast.DeadCodeAnalyzer
import nitrolang.ast.LstProgram
import nitrolang.backend.wasm.WasmBuilder
import nitrolang.backend.wasm.compile
import nitrolang.parsing.AstParser
import nitrolang.util.SourceFile
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.system.measureNanoTime

fun main() {
//    val source = File("example.nl")
    val source = File("src/main/nitro/compiler/main.nl")
    val core = File("src/main/nitro/core")
    val res = File("src/main/resources")
    try {
        compile(source.path)
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    watchFolderForChanges(listOf(source.parentFile.toPath(), core.toPath(), res.toPath())) {
        val fileName = it.fileName.toString()
        if (!fileName.endsWith(".nl") && !fileName.endsWith(".ts")) return@watchFolderForChanges

        try {
            compile(source.path)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

fun compile(path: String) {
    val elapsed = measureNanoTime {
        val program = LstProgram()

        AstParser.includeFile("core", "core.nl", program, null)
        AstParser.parseFile(SourceFile.load(path), program)

        program.functions.forEach { (_, func) ->
            if (func.fullName !in setOf("main", "to_a", "convert")) return@forEach

            println("${func.fullName}:")
            func.body.nodes.forEach {
                println("   $it")
            }
            println()
        }

        if (!program.collector.isEmpty()) {
            System.err.println(program.collector.toString())
            return
        }

        val assembly = File("src/main/resources/output/assembly.wat")
        val compiled = File("src/main/resources/output/compiled.wasm")

        DeadCodeAnalyzer.markDeadCode(program)

        assembly.bufferedWriter().use {
            WasmBuilder(program).compile(it)
        }

        println("------------------------------")
        assembly.readLines().forEachIndexed { index, s ->
            println("${(index + 1).toString().padStart(4)} |$s")
        }
        println("------------------------------")

        if (!program.collector.isEmpty()) {
            System.err.println(program.collector.toString())
            return
        }

//        println("Calling wat2wasm")
        ProcessBuilder("wat2wasm", assembly.path, "-o", compiled.path)
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

fun watchFolderForChanges(dirs: List<Path>, callback: (Path) -> Unit) {
    val watchService = FileSystems.getDefault().newWatchService()

    val absDirs = dirs.map { it.absolute() }
    absDirs.forEach {
        val dir = if (it.isRegularFile()) it.parent else it
        dir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
    }

    println("Watching directory: $absDirs")

    while (true) {
        val watchKey: WatchKey
        try {
            watchKey = watchService.take()

            // Required to avoid double fires
            Thread.sleep(50)
        } catch (ex: InterruptedException) {
            return
        }

        watchKey.pollEvents().forEach { event ->
            val kind = event.kind()

            @Suppress("UNCHECKED_CAST")
            val ev = event as WatchEvent<Path>
            val filename = ev.context()
            val parentDirectory = watchKey.watchable() as Path
            val fullPath = parentDirectory.resolve(filename)

            if (filename.name.endsWith('~')) {
                return@forEach
            }

            var matches = false
            for (absDir in absDirs) {
                if (fullPath.startsWith(absDir)) {
                    matches = true
                }
            }
            if (!matches) return@forEach

            when (kind) {
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    println("File created: $filename")
                    callback(fullPath)
                }

                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    println("File modified: $filename")
                    callback(fullPath)
                }
            }
        }

        if (!watchKey.reset()) {
            break
        }
    }
}