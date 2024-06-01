import nitrolang.CompilerOptions
import nitrolang.ast.DeadCodeAnalyzer
import nitrolang.ast.LstProgram
import nitrolang.ast.LstProgram.Companion.toJson
import nitrolang.backend.wasm.WasmBuilder
import nitrolang.compileToWasm
import nitrolang.parsing.AstParser
import nitrolang.util.SourceFile
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val debug = System.getenv("override-tests") == "true"

fun assertCompilationSuccess(relPath: String): LstProgram {
    val path = "src/test/resources/$relPath"
    val file = SourceFile.load(path)
    val program = LstProgram(CompilerOptions(source = path))
    AstParser.includeFile("core", "core.nitro", program, null)
    program.resetCounters(10000)
    AstParser.parseFile(file, program)

    assertEquals("", program.collector.toString(), "Parsing errors")
    assertCompilerOutput(program, path)
    return program
}

fun assertCompilationError(relPath: String) {
    val path = "src/test/resources/$relPath"
    val file = SourceFile.load(path)
    val program = LstProgram(CompilerOptions(source = path))
    AstParser.includeFile("core", "core.nitro", program, null)
    program.resetCounters(10000)
    AstParser.parseFile(file, program)

    println(program.collector.toString())
    assertNotEquals("", program.collector.toString(), "Parsed without the expected errors")
    assertCompilerOutput(program, path)
}

fun assertExecutionSuccess(relPath: String): LstProgram {
    val path = "src/test/resources/$relPath"
    val file = SourceFile.load(path)
    val program = LstProgram(CompilerOptions(source = path))
    AstParser.includeFile("core", "core.nitro", program, null)
    program.resetCounters(10000)
    AstParser.parseFile(file, program)

    assertEquals("", program.collector.toString(), "Parsing errors")

    DeadCodeAnalyzer.markDeadCode(program)

    val watFile = Files.createTempFile(null, ".wat").toFile()
    val wasmFile = Files.createTempFile(null, ".wasm").toFile()

    watFile.bufferedWriter().use { out ->
        WasmBuilder.compile(program, out)
    }

    assertEquals("", program.collector.toString(), "Compilation errors")
    val opt = CompilerOptions(source = path)
    compileToWasm(opt, watFile, wasmFile)

    val outputFile = Files.createTempFile(null, ".txt").toFile()
    ProcessBuilder("./src/main/resources/deno_wrapper.ts", "file://${wasmFile.absolutePath}")
        .inheritIO()
        .redirectOutput(outputFile)
        .start()
        .waitFor()

    val output = outputFile.readText()
    outputFile.delete()

    wasmFile.delete()
    watFile.delete()

    assertEquals("PASS", output.trim())

    return program
}

fun assertCompilerOutput(program: LstProgram?, programPath: String) {
    program ?: return

    val expectedOutputFile = File("$programPath.json")
    if (!expectedOutputFile.exists()) return

    DeadCodeAnalyzer.markDeadCode(program)
    val actualOutput = program.toJson()
    val expected = expectedOutputFile.readText()

    try {
        assertEquals(expected, actualOutput)
    } catch (e: Throwable) {
        // Gradle test diffs, are unusable, we override the text and use git to get a better diff
        if (debug) {
            expectedOutputFile.writeText(actualOutput)
        }
        throw e
    }
}