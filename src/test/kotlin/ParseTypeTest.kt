import kotlin.test.Test

class ParseTypeTest {
    @Test
    fun structType() {
        assertCompilationSuccess("internal/type/struct_type.nitro")
    }

    @Test
    fun letType() {
        assertCompilationSuccess("internal/type/let_type.nitro")
    }

    @Test
    fun invalidLetType() {
        assertCompilationError("internal/type/invalid_let_type.nitro")
    }

    @Test
    fun letTypeInference() {
        assertCompilationSuccess("internal/type/let_type_inference.nitro")
    }

    @Test
    fun fieldTypeInference() {
        assertCompilationSuccess("internal/type/field_type_inference.nitro")
    }
}