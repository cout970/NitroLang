import nitrolang.AstParser
import nitrolang.ast.LstProgram
import nitrolang.ast.LstProgram.Companion.toJson
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

fun assertCompilationSuccess(path: String): LstProgram {
    val errors = ErrorCollector()
    val file = SourceFile.load(path)
    val program = AstParser.parseFile(file, errors)

    assertEquals("", errors.toString(), "Parsing errors")
    assertNotEquals(null, program, "Program")
    assertCompilerOutput(program, path)
    return program!!
}

fun assertCompilationError(path: String) {
    val errors = ErrorCollector()
    val file = SourceFile.load(path)
    val program = AstParser.parseFile(file, errors)

    println(errors.toString())
    assertNotEquals("", errors.toString(), "Parsed without the expected errors")

    if (program != null) {
        assertCompilerOutput(program, path)
    }
}

fun assertCompilerOutput(program: LstProgram?, programPath: String) {
    val expectedOutputFile = File("$programPath.json")
    val actualOutput = program?.toJson() ?: "{}"

    // For debugging purposes
    File("build/output.json").writeText(actualOutput)

    if (expectedOutputFile.exists()) {
        val expected = expectedOutputFile.readText()
        assertEquals(expected, actualOutput)
    }
}