package nitrolang.backend

import nitrolang.ast.LstOption
import nitrolang.ast.LstStruct

// Monomorphized version of LstTypeBasePattern
sealed class MonoTypeBasePattern {
    data object PatternAny : MonoTypeBasePattern()
    data class PatternStruct(val instance: LstStruct) : MonoTypeBasePattern()
    data class PatternOption(val instance: LstOption) : MonoTypeBasePattern()

    override fun toString(): String {
        return when (this) {
            is PatternAny -> "*"
            is PatternStruct -> instance.fullName
            is PatternOption -> instance.fullName
        }
    }
}