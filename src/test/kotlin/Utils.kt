import nitrolang.parsing.AstParser
import nitrolang.ast.DeadCodeAnalyzer
import nitrolang.ast.LstProgram
import nitrolang.ast.LstProgram.Companion.toJson
import nitrolang.util.SourceFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val debug = System.getenv("override-tests") == "true"

fun assertCompilationSuccess(relPath: String): LstProgram {
    val path = "src/test/resources/$relPath"
    val file = SourceFile.load(path)
    val program = LstProgram()
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
    val program = LstProgram()
    AstParser.includeFile("core", "core.nitro", program, null)
    program.resetCounters(10000)
    AstParser.parseFile(file, program)

    println(program.collector.toString())
    assertNotEquals("", program.collector.toString(), "Parsed without the expected errors")
    assertCompilerOutput(program, path)
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