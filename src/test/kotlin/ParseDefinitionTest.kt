import kotlin.test.Test

class ParseDefinitionTest {

    @Test
    fun function() {
        assertCompilationSuccess("tests/internal/definition/function.nitro")
    }

    @Test
    fun struct() {
        assertCompilationSuccess("tests/internal/definition/struct.nitro")
    }

    @Test
    fun option() {
        assertCompilationSuccess("tests/internal/definition/option.nitro")
    }

    @Test
    fun enum() {
        assertCompilationSuccess("tests/internal/definition/enum.nitro")
    }

    @Test
    fun const() {
        assertCompilationSuccess("tests/internal/definition/const.nitro")
    }

    @Test
    fun redeclaration() {
        assertCompilationError("tests/internal/definition/redeclaration.nitro")
    }
}