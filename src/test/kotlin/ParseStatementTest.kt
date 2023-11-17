import kotlin.test.Test

class ParseStatementTest {
    @Test
    fun empty() {
        assertCompilationSuccess("tests/internal/statement/empty.nitro")
    }

    @Test
    fun ifStatement() {
        assertCompilationSuccess("tests/internal/statement/if_statement.nitro")
    }

    @Test
    fun loopStatement() {
        assertCompilationSuccess("tests/internal/statement/loop_statement.nitro")
    }

    @Test
    fun whileStatement() {
        assertCompilationSuccess("tests/internal/statement/while_statement.nitro")
    }

    @Test
    fun forStatement() {
        assertCompilationSuccess("tests/internal/statement/for_statement.nitro")
    }

    @Test
    fun letStatement() {
        assertCompilationSuccess("tests/internal/statement/let_statement.nitro")
    }

    @Test
    fun letScoping() {
        assertCompilationSuccess("tests/internal/statement/let_scoping.nitro")
    }

    @Test
    fun deferStatement() {
        assertCompilationSuccess("tests/internal/statement/defer.nitro")
    }

    @Test
    fun foreignBlockStatement() {
        assertCompilationSuccess("tests/internal/statement/foreign_block_statement.nitro")
    }

}