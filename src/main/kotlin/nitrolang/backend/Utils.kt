package nitrolang.backend

import nitrolang.ast.LstFunction
import nitrolang.ast.LstTag
import nitrolang.ast.LstTypeParameter
import nitrolang.backend.wasm.PTR_SIZE
import nitrolang.typeinference.*

fun findTagImplementation(tag: LstTag, paramType: MonoType, fullName: String): Pair<TType, LstFunction>? {
    for ((ty, map) in tag.functionImplementations) {
        if (fullName in map && monoTypeMatches(ty, paramType)) {
            return ty to map[fullName]!!
        }
    }

    return null
}

fun monoTypeMatches(a: TType, b: MonoType): Boolean {
    if (a is TGeneric) return true

    if (a is TComposite) {
        if (a.params.size != b.params.size) return false

        repeat(a.params.size) {
            if (!monoTypeMatches(a.params[it], b.params[it])) return false
        }

        if (a.base is TStruct && b.base is MonoStruct && a.base.instance == b.base.instance) return true
        if (a.base is TOption && b.base is MonoOption && a.base.instance == b.base.instance) return true
        if (a.base is TOptionItem && b.base is MonoStruct && a.base.instance == b.base.instance) return true
    }
    return false
}

fun findReplacements(a: TType, b: MonoType, map: MutableMap<LstTypeParameter, MonoType>) {
    if (a is TGeneric) {
        map[a.instance] = b
        return
    }

    if (a !is TComposite) error("Invalid type: $a")
    if (a.params.size != b.params.size) return

    repeat(a.params.size) {
        findReplacements(a.params[it], b.params[it], map)
    }
}

fun pad(value: Int, alignment: Int = PTR_SIZE): Int {
    val extra = value % alignment
    if (extra != 0) {
        return value + (alignment - extra)
    }
    return value
}