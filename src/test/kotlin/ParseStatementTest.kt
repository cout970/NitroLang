import kotlin.test.Test

class ParseStatementTest {
    @Test
    fun empty() {
        assertCompilationSuccess("tests/internal/statement/empty.nl")
    }

    @Test
    fun ifStatement() {
        assertCompilationSuccess("tests/internal/statement/if_statement.nl")
    }

    @Test
    fun loopStatement() {
        assertCompilationSuccess("tests/internal/statement/loop_statement.nl")
    }

    @Test
    fun whileStatement() {
        assertCompilationSuccess("tests/internal/statement/while_statement.nl")
    }

    @Test
    fun forStatement() {
        assertCompilationSuccess("tests/internal/statement/for_statement.nl")
    }

    @Test
    fun letStatement() {
        assertCompilationSuccess("tests/internal/statement/let_statement.nl")
    }

    @Test
    fun letScoping() {
        assertCompilationSuccess("tests/internal/statement/let_scoping.nl")
    }

    @Test
    fun foreignBlockStatement() {
        assertCompilationSuccess("tests/internal/statement/foreign_block_statement.nl")
    }

}