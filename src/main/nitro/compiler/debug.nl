/*
struct A { i: Int }

struct B { i: Int }

interface ToA {
    fun to_a(s: This): A
}

fun B.to_a(): A {
    println("In B.to_a()")
    ret A $[i : this.i]
}

fun A.to_a(): A {
    println("In A.to_a()")
    ret A $[i : this.i]
}

fun Int.to_a(): A {
    println("In Int.to_a()")
    ret A $[i : this]
}

fun <#Item: ToA> convert(item: #Item): A {
  ret item.to_a()
}

fun debug() {
    let b: B = B $[i: 42]
    println(b.i)

    let a: A = convert(b)
    println(a.i)

    let a2: A = convert(a)
    println(a2.i)

    let i: A = convert(69)
    println(i.i)
}

*/

tag ToString {
    fun This.to_string(): String
}

// assert tag ToString for List<#Item>

fun <#Item: ToString> List<#Item>.to_string(): String {
    let items = ""
    repeat this.len {
        items = items.concat(this[it].to_string())

        if it.is_not_equal(this.len - 1) {
            items = items.concat(", ")
        }
    }
    ret "List($items)"
}

fun <#T: ToString> format(t: #T): String {
    ret t.to_string()
}

struct Box {
    i: Int
}

fun Box.to_string(): String {
    ret "Box(${this.i}|)|"
}

fun debug() {

    42 > 23

    println(true)
    println(3.1434)
    println("Hello world")
    println(Box $[i: 1234567])
    println #[1, 2, 3, 4]
    println(json { a: 1, b: true, c: [1,2,3]})

    get_memory().dump()

//  let value = #[1,2,3,4]
//  println(value.to_debug_string())
//  println(value.to_string())
}
