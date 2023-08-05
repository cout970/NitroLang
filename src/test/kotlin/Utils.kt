import nitrolang.parsing.AstParser
import nitrolang.ast.DeadCodeAnalyzer
import nitrolang.ast.LstProgram
import nitrolang.ast.LstProgram.Companion.toJson
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val debug = System.getenv("override-tests") == "true"

fun assertCompilationSuccess(path: String): LstProgram {
    val errors = ErrorCollector()
    val file = SourceFile.load(path)
    val program = LstProgram()
    AstParser.parseFile(SourceFile.load("src/main/nitro/core.nl"), errors, program)
    program.resetCounters(10000)
    AstParser.parseFile(file, errors, program)

    assertEquals("", errors.toString(), "Parsing errors")
    assertCompilerOutput(program, path)
    return program
}

fun assertCompilationError(path: String) {
    val errors = ErrorCollector()
    val file = SourceFile.load(path)
    val program = LstProgram()
    AstParser.parseFile(SourceFile.load("src/main/nitro/core.nl"), errors, program)
    program.resetCounters(10000)
    AstParser.parseFile(file, errors, program)

    println(errors.toString())
    assertNotEquals("", errors.toString(), "Parsed without the expected errors")
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