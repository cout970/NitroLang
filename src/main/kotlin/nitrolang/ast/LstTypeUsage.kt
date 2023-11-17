package nitrolang.ast

import com.google.gson.JsonElement
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump

data class LstTypeUsage(
    val span: Span,
    val name: String,
    val path: Path,
    val sub: List<LstTypeUsage>,
    val currentPath: Path,
    val typeParameter: LstTypeParameter?,
    val unresolvedTypeRef: UnresolvedTypeRef? = null,
    val lambda: LstLambdaFunction? = null,
) : Dumpable {
    var resolvedType: TypeBox? = null
    var hasReceiver: Boolean = false
    val fullName: Path get() = createPath(path, name)

    override fun toString(): String {
        val prefix = if (path.isNotEmpty()) "$path::" else ""
        val children = if (sub.isNotEmpty()) "<${sub.joinToString(", ")}>" else ""
        return "$prefix$name$children"
    }

    override fun dump(): JsonElement = this.toString().dump()

    companion object {
        fun simple(name: String) = LstTypeUsage(
            span = Span.internal(),
            name = name,
            path = "",
            sub = mutableListOf(),
            typeParameter = null,
            currentPath = ""
        )

        fun nothing() = simple("Nothing")

        fun int() = simple("Int")

        fun float() = simple("Float")

        fun string() = simple("String")

        fun unresolved(unresolvedTypeRef: UnresolvedTypeRef, span: Span? = null) = LstTypeUsage(
            span = span ?: Span.internal(),
            name = "<unresolved>",
            path = "",
            sub = listOf(),
            typeParameter = null,
            currentPath = "",
            unresolvedTypeRef = unresolvedTypeRef,
        )

        fun typeParam(param: LstTypeParameter) = LstTypeUsage(
            span = Span.internal(),
            name = param.name,
            path = "",
            sub = listOf(),
            typeParameter = param,
            currentPath = ""
        )

        fun optional(other: LstTypeUsage) = LstTypeUsage(
            span = Span.internal(),
            name = "Optional",
            path = "",
            sub = listOf(other),
            typeParameter = null,
            currentPath = ""
        )

        fun list(other: LstTypeUsage) = LstTypeUsage(
            span = Span.internal(),
            name = "List",
            path = "",
            sub = listOf(other),
            typeParameter = null,
            currentPath = ""
        )

        fun map(key: LstTypeUsage, value: LstTypeUsage) = LstTypeUsage(
            span = Span.internal(),
            name = "Map",
            path = "",
            sub = listOf(key, value),
            typeParameter = null,
            currentPath = ""
        )

        fun set(other: LstTypeUsage) = LstTypeUsage(
            span = Span.internal(),
            name = "Set",
            path = "",
            sub = listOf(other),
            typeParameter = null,
            currentPath = ""
        )

        fun lambda(lambda: LstLambdaFunction) = LstTypeUsage(
            span = lambda.span,
            name = "Lambda",
            path = "",
            sub = listOf(),
            typeParameter = null,
            currentPath = "",
            lambda = lambda,
        )
    }

    enum class Modifier {
        MUT, REF, NONE
    }
}