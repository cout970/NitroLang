import kotlin.test.Test

class ParseTypeTest {
    @Test
    fun structType() {
        assertCompilationSuccess("tests/internal/type/struct_type.nl")
    }

    @Test
    fun letType() {
        assertCompilationSuccess("tests/internal/type/let_type.nl")
    }

    @Test
    fun invalidLetType() {
        assertCompilationError("tests/internal/type/invalid_let_type.nl")
    }

    @Test
    fun letTypeInference() {
        assertCompilationSuccess("tests/internal/type/let_type_inference.nl")
    }

    @Test
    fun fieldTypeInference() {
        assertCompilationSuccess("tests/internal/type/field_type_inference.nl")
    }
}