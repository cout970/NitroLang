package nitrolang.backend

// Monomorphized version of LstVar
data class MonoVar(
    val id: MonoRef,
    val name: String,
    val type: MonoType
) {

    fun finalName(index: Int): String {
        return if (index == 0) "$$name-$id" else "$$name-$id-$index"
    }
}