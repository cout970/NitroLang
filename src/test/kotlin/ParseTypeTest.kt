import kotlin.test.Test

class ParseTypeTest {
    @Test
    fun structType() {
        assertCompilationSuccess("tests/internal/type/struct_type.nitro")
    }

    @Test
    fun letType() {
        assertCompilationSuccess("tests/internal/type/let_type.nitro")
    }

    @Test
    fun invalidLetType() {
        assertCompilationError("tests/internal/type/invalid_let_type.nitro")
    }

    @Test
    fun letTypeInference() {
        assertCompilationSuccess("tests/internal/type/let_type_inference.nitro")
    }

    @Test
    fun fieldTypeInference() {
        assertCompilationSuccess("tests/internal/type/field_type_inference.nitro")
    }
}