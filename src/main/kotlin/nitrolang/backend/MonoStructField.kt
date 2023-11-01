package nitrolang.backend

// Monomorphized version of LstStructField
data class MonoStructField(
    val name: String,
    val type: MonoType,
    val size: Int,
    val offset: Int,
)