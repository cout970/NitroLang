package nitrolang.typeinference

import nitrolang.ast.LstTag
import nitrolang.util.ErrorInfo

interface TypeError {
    val message: String
    val constraint: TConstraint

    fun replace(env: TypeEnv, key: TType, replacement: TType)
}

class TypeMismatchError(
    var left: TType,
    var right: TType,
    override val constraint: TConstraint,
) : ErrorInfo(
    message = "Type mismatch, expected '$left', found '$right'",
    span = constraint.span,
), TypeError {

    override fun replace(env: TypeEnv, key: TType, replacement: TType) {
        with(env) {
            left = left.replace(key, replacement)
            right = right.replace(key, replacement)
        }
    }
}

class TypeBoundsError(
    var left: TType,
    val tag: LstTag,
    override val constraint: TConstraint
) : ErrorInfo(
    message = "Unsatisfied type bounds, type '$left' is missing the tag '${tag.fullName}'",
    span = constraint.span
), TypeError {

    override fun replace(env: TypeEnv, key: TType, replacement: TType) {
        with(env) {
            left = left.replace(key, replacement)
        }
    }
}