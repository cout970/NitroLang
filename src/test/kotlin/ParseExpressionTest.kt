import kotlin.test.Test

class ParseExpressionTest {
    @Test
    fun unit() {
        assertCompilationSuccess("tests/internal/expression/nothing.nitro")
    }

    @Test
    fun constant() {
        assertCompilationSuccess("tests/internal/expression/constant.nitro")
    }

    @Test
    fun isType() {
        assertCompilationSuccess("tests/internal/expression/is_type.nitro")
    }

    @Test
    fun asType() {
        assertCompilationSuccess("tests/internal/expression/as_type.nitro")
    }

    @Test
    fun returnExpr() {
        assertCompilationSuccess("tests/internal/expression/return.nitro")
    }

    @Test
    fun variable() {
        assertCompilationSuccess("tests/internal/expression/variable.nitro")
    }

    @Test
    fun sizeOf() {
        assertCompilationSuccess("tests/internal/expression/size_of.nitro")
    }

    @Test
    fun alloc() {
        assertCompilationSuccess("tests/internal/expression/alloc.nitro")
    }

    @Test
    fun field() {
        assertCompilationSuccess("tests/internal/expression/field.nitro")
    }

    @Test
    fun function() {
        assertCompilationSuccess("tests/internal/expression/function.nitro")
    }

    @Test
    fun invalidFunction() {
        assertCompilationError("tests/internal/expression/invalid_function.nitro")
    }

    @Test
    fun operators() {
        assertCompilationSuccess("tests/internal/expression/operators.nitro")
    }
}