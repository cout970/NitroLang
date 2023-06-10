package nitrolang.util

import java.io.File
import kotlin.math.max
import kotlin.math.min


interface HasSpan {
    val span: Span
}

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
        return "${file.path}(${file.path.substringAfterLast("/")}:${line + 1})"
    }

    companion object {
        fun internal() = Span(0, 0, SourceFile.fromString(""))
    }
}

data class SourceFile(val path: String, val length: Int, val contents: String) {
    fun read(from: Int, to: Int): String = contents.substring(from, to)
    fun readChar(index: Int): Char = contents[index]

    companion object {

        fun load(path: String): SourceFile {
            val file = File(path)

            if (!file.exists()) {
                error("File doesn't exists: '$path'")
            }

            val code = file.readText()
            return SourceFile(path = path, length = code.length, contents = code)
        }

        fun fromString(code: String) = SourceFile(path = "<internal>", length = code.length, contents = code)
    }
}