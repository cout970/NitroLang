package nitrolang.typeinference

import nitrolang.util.Span

data class TypeBox(val env: TypeEnv, var type: TType, val span: Span) {
    var hasUnresolved: Boolean = false
    var unresolvedFunction: Boolean = false

    fun replace(find: TType, replacement: TType): Boolean {
        return env.run {
            val prev = this@TypeBox.type
            this@TypeBox.type = this@TypeBox.type.replace(find, replacement)
            prev != this@TypeBox.type
        }
    }

    override fun toString(): String = type.toString()
}