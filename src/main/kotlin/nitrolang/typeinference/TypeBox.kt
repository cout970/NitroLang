package nitrolang.typeinference

import nitrolang.util.Span

data class TypeBox(val env: TypeEnv, var type: TType, val span: Span) {
    var hasUnresolved: Boolean = true
    var unresolvedFunction: Boolean = true

    fun replace(find: TType, replacement: TType): TypeBox {
        env.apply {
            this@TypeBox.type = this@TypeBox.type.replace(find, replacement)
        }
        return this
    }

    override fun toString(): String = type.toString()
}