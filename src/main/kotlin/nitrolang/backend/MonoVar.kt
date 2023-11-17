package nitrolang.backend

// Monomorphized version of LstVar
data class MonoVar(
    val id: MonoRef,
    val name: String,
    val type: MonoType
) {
    var auxName: String? = null

    fun finalName(): String = "$$name-$id"
}