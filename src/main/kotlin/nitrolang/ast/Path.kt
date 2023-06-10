package nitrolang.ast

const val MODULE_SEPARATOR = "::"

typealias Path = String

fun createPath(path: Path, name: String): Path {
    return if (path.isNotEmpty()) path + MODULE_SEPARATOR + name else name
}

fun createPathSegments(currentPath: Path, fullName: Path, @Suppress("UNUSED_PARAMETER") imports: List<Path> = emptyList()): List<Path> {
    return buildList {
//        for (typeImport in imports) {
//            if (typeImport.endsWith(fullName)) {
//                add(typeImport)
//            }
//        }

        if (currentPath.isEmpty()) {
            add(fullName)
            return@buildList
        }

        val parts = currentPath.split(MODULE_SEPARATOR)

        for (i in 0..parts.size) {
            add((parts.dropLast(i) + listOf(fullName)).joinToString(MODULE_SEPARATOR))
        }
    }
}