package nitrolang.backend

// Monomorphized version of LstTypePattern
data class MonoTypePattern(
    val base: MonoTypeBasePattern,
    val params: List<MonoTypePattern>
) {

    // Matches a type against this pattern
    fun match(ty: MonoType): Boolean {
        // Match base
        when (base) {
            is MonoTypeBasePattern.PatternStruct -> {
                if (ty.isFunction() || ty.isLambda()) {
                    if (base.instance.fullName != "Function") return false
                } else {
                    if (ty.base !is MonoStruct) return false
                    if (base.instance != ty.base.instance) return false
                }
            }

            is MonoTypeBasePattern.PatternOption -> {
                if (ty.base !is MonoOption) return false
                if (base.instance != ty.base.instance) return false
            }

            is MonoTypeBasePattern.PatternAny -> {
                return true
            }
        }

        // Match params
        if (params.size != ty.params.size) return false

        return params.zip(ty.params).all { (p1, p2) -> p1.match(p2) }
    }

    fun isOptionItem(): Boolean = base is MonoTypeBasePattern.PatternStruct && base.instance.parentOption != null
    fun isOption(): Boolean = base is MonoTypeBasePattern.PatternOption
    fun isAny(): Boolean = base is MonoTypeBasePattern.PatternAny
    fun isOptionOrOptionItem(): Boolean = isOption() || isOptionItem()

    override fun toString(): String {
        return if (params.isNotEmpty()) "$base<${params.joinToString(", ")}>" else base.toString()
    }
}