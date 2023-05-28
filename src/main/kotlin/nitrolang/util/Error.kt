package nitrolang.util


class ErrorCollector {
    private val errors = mutableListOf<ErrorInfo>()

    fun report(message: String, span: Span) {
        val caller = Thread.currentThread().stackTrace[2]
        errors += ErrorInfo(message, span, caller.toString())
    }

    fun isEmpty(): Boolean = errors.isEmpty()
    fun isNotEmpty(): Boolean = errors.isNotEmpty()

    override fun toString(): String {
        return errors.joinToString("\n")
    }
}

class ErrorInfo(
    val message: String,
    val span: Span,
    val source: String
) {
    override fun toString(): String = buildString {
        if (span.file.path == "<internal>") {
            appendLine(message)
            return@buildString
        }

        appendLine("Report source: $source")
        appendLine(span)
        appendLine(message)

        var lineStart = span.start
        var lineEnd = span.start

        while (lineStart > 1 && span.file.read(lineStart - 1, lineStart - 1) != "\n") {
            lineStart--
        }

        while (lineEnd < span.file.length && span.file.read(lineEnd, lineEnd) != "\n") {
            lineEnd++
        }

        val lineText = span.file.read(lineStart, lineEnd)
        val pad = span.start - lineStart

        appendLine("│ ")
        append("│ ")
        appendLine(lineText)
        append("│ ")
        repeat(pad) {
            append(" ")
        }
        append("▲")
        appendLine()
        append("│ ")
        repeat(pad) {
            append("─")
        }
        append("┘")
        appendLine()
    }
}