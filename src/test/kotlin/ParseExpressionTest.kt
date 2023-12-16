import kotlin.test.Test

class ParseExpressionTest {
    @Test
    fun unit() {
        assertCompilationSuccess("internal/expression/nothing.nitro")
    }

    @Test
    fun constant() {
        assertCompilationSuccess("internal/expression/constant.nitro")
    }

    @Test
    fun isType() {
        assertCompilationSuccess("internal/expression/is_type.nitro")
    }

    @Test
    fun asType() {
        assertCompilationSuccess("internal/expression/as_type.nitro")
    }

    @Test
    fun returnExpr() {
        assertCompilationSuccess("internal/expression/return.nitro")
    }

    @Test
    fun variable() {
        assertCompilationSuccess("internal/expression/variable.nitro")
    }

    @Test
    fun sizeOf() {
        assertCompilationSuccess("internal/expression/size_of.nitro")
    }

    @Test
    fun alloc() {
        assertCompilationSuccess("internal/expression/alloc.nitro")
    }

    @Test
    fun field() {
        assertCompilationSuccess("internal/expression/field.nitro")
    }

    @Test
    fun function() {
        assertCompilationSuccess("internal/expression/function.nitro")
    }

    @Test
    fun invalidFunction() {
        assertCompilationError("internal/expression/invalid_function.nitro")
    }

    @Test
    fun operators() {
        assertCompilationSuccess("internal/expression/operators.nitro")
    }

    @Test
    fun operatorPrecedence() {
        assertExecutionSuccess("internal/expression/operator_precedence.nitro")
    }

    @Test
    fun lambda() {
        assertCompilationSuccess("internal/expression/lambda.nitro")
    }

    @Test
    fun lambda_chain() {
        assertCompilationSuccess("internal/expression/lambda_chain.nitro")
    }

    @Test
    fun html_dsl() {
        assertCompilationSuccess("internal/expression/html_dsl.nitro")
    }
}