import kotlin.test.Test

class ParseDefinitionTest {

    @Test
    fun function() {
        assertCompilationSuccess("internal/definition/function.nitro")
    }

    @Test
    fun struct() {
        assertCompilationSuccess("internal/definition/struct.nitro")
    }

    @Test
    fun option() {
        assertCompilationSuccess("internal/definition/option.nitro")
    }

    @Test
    fun enum() {
        assertCompilationSuccess("internal/definition/enum.nitro")
    }

    @Test
    fun const() {
        assertCompilationSuccess("internal/definition/const.nitro")
    }

    @Test
    fun redeclaration() {
        assertCompilationError("internal/definition/redeclaration.nitro")
    }
}