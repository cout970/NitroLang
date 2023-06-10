import kotlin.test.Test

class ParseDefinitionTest {

    @Test
    fun function() {
        assertCompilationSuccess("tests/internal/definition/function.nl")
    }

    @Test
    fun struct() {
        assertCompilationSuccess("tests/internal/definition/struct.nl")
    }

    @Test
    fun option() {
        assertCompilationSuccess("tests/internal/definition/option.nl")
    }

    @Test
    fun const() {
        assertCompilationSuccess("tests/internal/definition/const.nl")
    }

    @Test
    fun redeclaration() {
        assertCompilationError("tests/internal/definition/redeclaration.nl")
    }
}