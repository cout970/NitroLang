import nitrolang.AstParser
import nitrolang.ast.LstProgram
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LstParseExpressionTest {

    private fun parseFile(path: String): LstProgram {
        val errors = ErrorCollector()
        val file = SourceFile.load("tests/internal/expression/$path")
        val program = AstParser.parseFile(file, errors)

        assertEquals("", errors.toString(), "Parsing errors")
        assertNotEquals(null, program, "Program")
        println(program)
        return program!!
    }

    @Test
    fun unit() {
        parseFile("unit.nl")
    }

    @Test
    fun constant() {
        parseFile("constant.nl")
    }

    @Test
    fun isType() {
        parseFile("is_type.nl")
    }

    @Test
    fun asType() {
        parseFile("as_type.nl")
    }

    @Test
    fun returnExpr() {
        parseFile("return.nl")
    }

    @Test
    fun variable() {
        parseFile("variable.nl")
    }

    @Test
    fun sizeOf() {
        parseFile("size_of.nl")
    }

    @Test
    fun alloc() {
        parseFile("alloc.nl")
    }

    @Test
    fun field() {
        parseFile("field.nl")
    }

    @Test
    fun function() {
        parseFile("function.nl")
    }

    @Test
    fun operators() {
        parseFile("operators.nl")
    }
}