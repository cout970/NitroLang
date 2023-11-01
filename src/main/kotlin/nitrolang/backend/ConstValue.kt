package nitrolang.backend

// Constant value known at compile time
sealed class ConstValue

// Boolean
data class ConstBoolean(val value: Boolean) : ConstValue()

// Float
data class ConstFloat(val value: Float) : ConstValue()

// Int
data class ConstInt(val value: Int) : ConstValue()

// String
data class ConstString(val value: String) : ConstValue()

// Nothing, 0 byte value, sometimes represented as 0
data object ConstNothing : ConstValue()
