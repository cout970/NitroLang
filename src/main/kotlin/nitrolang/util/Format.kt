package nitrolang.util

fun formatItem(item: Any?): String = when (item) {
    null -> {
        "null"
    }

    is Collection<*> -> {
        @Suppress("UNCHECKED_CAST")
        formatList(item as Collection<Any>)
    }

    is String -> {
        "\"${item.replace("\n", "\\n").replace("\"", "\\\"")}\""
    }

    else -> {
        val result = item.toString()
        indent(result)
    }
}

fun formatList(values: Collection<Any>, inline: Boolean = true): String {
    if (values.isEmpty()) return "[]"

    val lines = values.joinToString("\n") { indent(formatItem(it)) }
    val result = "[\n$lines\n]"

    return if (inline) indent(result).trimStart() else result
}

fun indent(value: String, indent: String = "  ") = indent + value.replace("\n", "\n  ")
