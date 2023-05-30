package nitrolang.ast

const val MODULE_SEPARATOR = "::"

typealias Path = String

fun createPath(path: Path, name: String): Path {
    return if (path.isNotEmpty()) path + MODULE_SEPARATOR + name else name
}