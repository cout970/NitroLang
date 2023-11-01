package nitrolang.ast

import nitrolang.typeinference.TType
import nitrolang.util.Span

class LstTypeAlias(
    val span: Span,
    val name: String,
    val path: Path,
    val typeParameters: List<LstTypeParameter>,
    val typeUsage: LstTypeUsage,
    val annotations: List<LstAnnotation>
) {
    var type: TType? = null
    val fullName: Path get() = createPath(path, name)
}