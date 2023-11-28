import kotlin.test.Test

class ParseStatementTest {
    @Test
    fun empty() {
        assertCompilationSuccess("internal/statement/empty.nitro")
    }

    @Test
    fun ifStatement() {
        assertCompilationSuccess("internal/statement/if_statement.nitro")
    }

    @Test
    fun loopStatement() {
        assertCompilationSuccess("internal/statement/loop_statement.nitro")
    }

    @Test
    fun whileStatement() {
        assertCompilationSuccess("internal/statement/while_statement.nitro")
    }

    @Test
    fun forStatement() {
        assertCompilationSuccess("internal/statement/for_statement.nitro")
    }

    @Test
    fun letStatement() {
        assertCompilationSuccess("internal/statement/let_statement.nitro")
    }

    @Test
    fun letScoping() {
        assertCompilationSuccess("internal/statement/let_scoping.nitro")
    }

    @Test
    fun letShadowing() {
        assertCompilationSuccess("internal/statement/let_shadowing.nitro")
    }

    @Test
    fun lambdaLetShadowing() {
        assertCompilationSuccess("internal/statement/lambda_let_shadowing.nitro")
    }

    @Test
    fun deferStatement() {
        assertCompilationSuccess("internal/statement/defer.nitro")
    }

    @Test
    fun foreignBlockStatement() {
        assertCompilationSuccess("internal/statement/foreign_block_statement.nitro")
    }

    @Test
    fun operatorAssign() {
        assertCompilationSuccess("internal/statement/operator_assign.nitro")
    }

}