package nitrolang.util

import java.io.File

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