package nitrolang.util


class ErrorCollector {
    private val errors = mutableListOf<ErrorInfo>()

    fun report(message: String, span: Span) {
        errors += ErrorInfo(message, span, Thread.currentThread().stackTrace[2].toString())
    }

    fun reportError(error: ErrorInfo) {
        errors += error
    }

    fun isEmpty(): Boolean = errors.isEmpty()
    fun isNotEmpty(): Boolean = errors.isNotEmpty()

    override fun toString(): String {
        return errors.joinToString("\n")
    }
}

open class ErrorInfo(
    val message: String,
    var span: Span = Span.internal(),
    val source: String = getErrorSource()
) {

    companion object {
        fun getErrorSource(): String {
            return Thread.currentThread().stackTrace[4].toString()
        }
    }

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

        while (lineStart > 1 && span.file.readChar(lineStart - 1) != '\n') {
            lineStart--
        }

        while (lineEnd < span.file.length && span.file.readChar(lineEnd) != '\n') {
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