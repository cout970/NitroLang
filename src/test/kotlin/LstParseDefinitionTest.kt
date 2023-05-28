import nitrolang.AstParser
import nitrolang.ast.LstProgram
import nitrolang.util.ErrorCollector
import nitrolang.util.SourceFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LstParseDefinitionTest {

    private fun parseFile(path: String): LstProgram {
        val errors = ErrorCollector()
        val file = SourceFile.load("tests/internal/definition/$path")
        val program = AstParser.parseFile(file, errors)

        assertEquals("", errors.toString(), "Parsing errors")
        assertNotEquals(null, program, "Program")
        println(program)
        return program!!
    }

    @Test
    fun function() {
        parseFile("function.nl")
    }

    @Test
    fun struct() {
        parseFile("struct.nl")
    }

    @Test
    fun option() {
        parseFile("option.nl")
    }

    @Test
    fun const() {
        parseFile("const.nl")
    }
}