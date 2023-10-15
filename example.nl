struct BigStruct {
    a: Int
    b: Int
    c: Int
}

fun main() {
    let list = List::new<Int>()
    list[] = 42
    list[] = 69

    println(list)

    let list = List::new<BigStruct>()
    let str = BigStruct $[a: 42, b: 2, c: 12]
    list[] = str

    println(list.get(0).a)
    println(list.get(0).b)
    println(list.get(0).c)
}
// 1234567