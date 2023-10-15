
fun debug1(func: (Int) -> Nothing) {
    println("Before")
    func.invoke(42)
    println("After")
}

fun debug() {
    // Integers from 0 to 20 in random order, to sort later
    let list = #[20, 18, 7, 11, 14, 10, 4, 16, 3, 5, 12, 15, 17, 6, 2, 8, 1, 9, 13, 19]
    println("Before: $list")
    list.sort_in_place()

    println("After: $list")
    println("Item at 10 is ${list[10]!!}")

    let num = list
                .map #{ n -> n * 2 }
                .filter #{ n -> n % 2 == 0 }
                .count #{ n -> n % 12 == 0 }

    println("Found $num matches")
}
