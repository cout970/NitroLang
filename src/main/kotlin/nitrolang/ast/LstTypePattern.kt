package nitrolang.ast

import nitrolang.typeinference.TGeneric
import nitrolang.typeinference.TTypeBase
import nitrolang.util.Span

class LstTypePattern(
    val span: Span,
    val name: String,
    val path: Path,
    val sub: List<LstTypePattern>,
    val currentPath: Path,
    val typeParameter: LstTypeParameter?,
    val isAny: Boolean = false,
) {
    val fullName: Path get() = createPath(path, name)
    var base: TTypeBase? = null
    var generic: TGeneric? = null

    override fun toString(): String {
        val prefix = if (path.isNotEmpty()) "$path::" else ""
        val children = if (sub.isNotEmpty()) "<${sub.joinToString(", ")}>" else ""
        return "$prefix$name$children"
    }
}