import kotlin.test.Test

class ParseExpressionTest {
    @Test
    fun unit() {
        assertCompilationSuccess("tests/internal/expression/unit.nl")
    }

    @Test
    fun constant() {
        assertCompilationSuccess("tests/internal/expression/constant.nl")
    }

    @Test
    fun isType() {
        assertCompilationSuccess("tests/internal/expression/is_type.nl")
    }

    @Test
    fun asType() {
        assertCompilationSuccess("tests/internal/expression/as_type.nl")
    }

    @Test
    fun returnExpr() {
        assertCompilationSuccess("tests/internal/expression/return.nl")
    }

    @Test
    fun variable() {
        assertCompilationSuccess("tests/internal/expression/variable.nl")
    }

    @Test
    fun sizeOf() {
        assertCompilationSuccess("tests/internal/expression/size_of.nl")
    }

    @Test
    fun alloc() {
        assertCompilationSuccess("tests/internal/expression/alloc.nl")
    }

    @Test
    fun field() {
        assertCompilationSuccess("tests/internal/expression/field.nl")
    }

    @Test
    fun function() {
        assertCompilationSuccess("tests/internal/expression/function.nl")
    }

    @Test
    fun invalidFunction() {
        assertCompilationError("tests/internal/expression/invalid_function.nl")
    }

    @Test
    fun operators() {
        assertCompilationSuccess("tests/internal/expression/operators.nl")
    }
}