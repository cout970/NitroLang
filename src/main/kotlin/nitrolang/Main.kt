package nitrolang

import nitrolang.ast.DeadCodeAnalyzer
import nitrolang.ast.LstProgram
import nitrolang.backend.wasm.WasmBuilder
import nitrolang.parsing.AstParser
import nitrolang.util.Prof
import nitrolang.util.SourceFile
import java.io.File
import java.nio.file.*
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile
import kotlin.io.path.name


fun main(args: Array<String>) {
    val opts = CompilerOptions.fromArgs(args)
    if (opts == null) {
        CompilerOptions.showUsage()
        return
    }

    try {
        if (compile(opts) && opts.execute) {
            execute(File(opts.output))
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    // Listen to changes
    if (opts.listenChanges.isNotEmpty()) {
        val paths: MutableList<Path> = mutableListOf(
            File(opts.source).absoluteFile.parentFile.toPath()
        )
        opts.listenChanges.forEach {
            paths.add(File(it).toPath())
        }

        watchFolderForChanges(paths) {
            val fileName = it.fileName.toString()

            if (!fileName.endsWith(".nitro") && !fileName.endsWith(".ts") && !fileName.endsWith(".js")) {
                return@watchFolderForChanges
            }

            try {
                if (compile(opts) && opts.execute) {
                    execute(File(opts.output))
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

fun compile(opt: CompilerOptions): Boolean {
    Prof.start("compile")
    Prof.start("program")
    val program = LstProgram(opt)

    Prof.next("parse_core")
    AstParser.includeFile("core", "core.nitro", program, null)

    Prof.next("parse_source")
    AstParser.parseFile(SourceFile.load(opt.source), program)

    if (!opt.dumpIr) {
        Prof.next("dump_ir")
        program.functions.forEach { func ->
            if (func.isExternal) return@forEach

            println("------------------------------")
            println("${func.fullName}:")
            func.body.nodes.forEach {
                println("   $it")
            }
            println("------------------------------")
            println("")
        }
    }

    Prof.next("print_errors")
    if (program.collector.isNotEmpty()) {
        System.err.println(program.collector.toString())
        return false
    }

    Prof.next("mark_code")
    DeadCodeAnalyzer.markDeadCode(program)

    Prof.next("compile_wasm")
    File(opt.output).bufferedWriter().use { out ->
        WasmBuilder.compile(program, out)
    }

    if (program.compilerOptions.dumpWasm) {
        Prof.next("dump_wasm")
        println("------------------------------")
        File(opt.output).readLines().forEachIndexed { index, s ->
            println("${(index + 1).toString().padStart(4)} |$s")
        }
        println("------------------------------")
    }

    Prof.next("print_wasm_errors")
    if (program.collector.isNotEmpty()) {
        System.err.println(program.collector.toString())
        return false
    }

    Prof.end()
    return true
}

fun execute(watFile: File) {
    Prof.start("run")
    val wasmFile = File(watFile.parentFile, "compiled.wasm")

    Prof.start("wat2wasm")
    ProcessBuilder("wat2wasm", "--enable-code-metadata", watFile.path, "-o", wasmFile.path)
        .inheritIO()
        .start()
        .waitFor()
    Prof.end()

    Prof.start("deno")
    println("--- Running output.wasm")
    ProcessBuilder("./src/main/resources/deno_wrapper.ts")
        .inheritIO()
        .start()
        .waitFor()
    println("---")
    Prof.end()
    Prof.end()
}

fun getAbsDirsRec(dirs: List<Path>, result: MutableList<Path>) {
    for (dir in dirs) {
        result.add(dir.absolute())
        dir.toFile().listFiles()?.forEach {
            if (it.isDirectory) {
                getAbsDirsRec(listOf(it.toPath()), result)
            }
        }
    }
}

fun watchFolderForChanges(dirs: List<Path>, callback: (Path) -> Unit) {
    val watchService = FileSystems.getDefault().newWatchService()

    val absDirs = mutableListOf<Path>()
    getAbsDirsRec(dirs, absDirs)
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