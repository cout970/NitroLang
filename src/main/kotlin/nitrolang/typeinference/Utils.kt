package nitrolang.typeinference

fun deepCompareTypes(a: List<TType>, b: List<TType>, diff: MutableList<Pair<TType, TType>>) {
    for ((aT, bT) in a.zip(b)) {
        deepCompareTypes(aT, bT, diff)
    }
}

fun deepCompareTypes(a: TType, b: TType, diff: MutableList<Pair<TType, TType>>) {
    if (a == b) return

    if (a is TComposite && b is TComposite) {
        if (a.base == b.base) {
            deepCompareTypes(a.params, b.params, diff)
            return
        }
    }

    diff += a to b
}
