package nitrolang.util

import kotlin.math.max
import kotlin.math.min


data class Span(
    val start: Int,
    val last: Int,
    val file: SourceFile
) {
    val value: String by lazy {
        file.read(start, last + 1)
    }

    val line: Int by lazy {
        file.read(0, start).count { it == '\n' }
    }

    val column: Int by lazy {
        val lastNl = file.read(0, start).indexOfLast { it == '\n' }
        file.read(lastNl + 1, start).length
    }

    fun merge(other: Span): Span {
        return Span(min(this.start, other.start), max(this.last, other.last), file)
    }

    override fun toString(): String {
        // Make Intellij links clickable
        val path = if (file.path.contains("/src/")) {
            file.path.substringAfter("/src/")
        } else {
            file.path
        }
        return "$path(${file.path.substringAfterLast("/")}:${line + 1})"
    }

    fun toLinkString(): String {
        val filename = file.path.substringAfterLast("/")
        return "$filename($filename:${line + 1})"
    }

    fun isInternal(): Boolean = file.length == 0

    companion object {
        fun internal() = Span(0, 0, SourceFile.fromString(""))
    }
}

