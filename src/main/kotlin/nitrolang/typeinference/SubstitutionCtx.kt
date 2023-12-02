package nitrolang.typeinference

class SubstitutionCtx(val typeEnv: TypeEnv) {
    val unresolvedTypes: MutableMap<TUnresolved, TType> = mutableMapOf()
    val unresolvedFunctions: MutableMap<TUnresolvedFunction, TType> = mutableMapOf()
    val unions: MutableMap<TUnion, TType> = mutableMapOf()

    fun clear() {
        unresolvedTypes.clear()
        unresolvedFunctions.clear()
        unions.clear()
    }

    fun mergeIn(other: SubstitutionCtx) {
        for ((subKey, subValue) in other.unresolvedTypes) {
            if (subKey !in unresolvedTypes) {
                unresolvedTypes[subKey] = subValue
                continue
            }

            unresolvedTypes[subKey] = typeEnv.unionOf(unresolvedTypes[subKey]!!, subValue)
        }

        for ((subKey, subValue) in other.unresolvedFunctions) {
            if (subKey !in unresolvedFunctions) {
                unresolvedFunctions[subKey] = subValue
                continue
            }

            unresolvedFunctions[subKey] = typeEnv.unionOf(unresolvedFunctions[subKey]!!, subValue)
        }

        for ((subKey, subValue) in other.unions) {
            if (subKey !in unions) {
                unions[subKey] = subValue
                continue
            }

            unions[subKey] = typeEnv.unionOf(unions[subKey]!!, subValue)
        }
    }
}