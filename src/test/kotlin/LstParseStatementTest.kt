import nitrolang.AstParser
import nitrolang.ast.LstProgram
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LstParseStatementTest {

    private fun parseFile(path: String): LstProgram {
        val errors = ErrorCollector()
        val file = SourceFile.load("tests/internal/statement/$path")
        val program = AstParser.parseFile(file, errors)

        assertEquals("", errors.toString(), "Parsing errors")
        assertNotEquals(null, program, "Program")
        println(program)
        return program!!
    }

    @Test
    fun empty() {
        parseFile("empty.nl")
    }

    @Test
    fun ifStatement() {
        parseFile("if_statement.nl")
    }

    @Test
    fun loopStatement() {
        parseFile("loop_statement.nl")
    }

    @Test
    fun whileStatement() {
        parseFile("while_statement.nl")
    }

    @Test
    fun forStatement() {
        parseFile("for_statement.nl")
    }

    @Test
    fun letStatement() {
        parseFile("let_statement.nl")
    }

    @Test
    fun foreignBlockStatement() {
        parseFile("foreign_block_statement.nl")
    }

}