struct Box<#T> {
    value: #T
    value2: #T
}

option A {
    B {}
    C {
        a: Int
    }
}

fun println(a: List<Int>) {
    println("List (".concat(a.len().to_string()).concat("):"))
    repeat a.len() {
        println("  - ".concat(a[it].to_string()))
    }
}

fun main() {
    let config = json {
      firstName: "John"
      lastName: "Smith"
      isAlive: true
      age: 27
      address: {
        "streetAddress": "21 2nd Street"
        "city": "New York"
        "state": "NY"
        "postalCode": "10021-3100"
      }
    }

    println(config.to_string())

//    let a = #[1, 2, 3]
//
//    repeat 7 {
//        a[] = it
//    }
//    a[] = 42
//
//    println(a)
//
//    repeat a.len() {
//        println(a.get(it))
//    }

//    let cond = true
//    let c: Optional<Int> = if cond { Some(69) } else { None() }
//
//    println(c is Optional::Some)
//    println(c is Optional::None)
//
//    let d: Result<Int, Int> = if cond {
//        Ok(420)
//    } else {
//        Err(-1)
//    }
//
//    println(d is Result::Ok)
//    println(d is Result::Err)
//    println(d.ok_or_panic())
//    println(d.err_or_panic())

//    println(fib(40))
//    println(recFib(40))
}

fun test(a: #Value): #Value {
    ret a
}

fun test2(a: Box<#Value>): Box<#Value> {
    let b = a.value
    ret Box $[value: b]
}

//
//fun fib(count: Int): Int {
//    let i = 0
//    let j = 1
//
//    while count > 1 {
//        let aux = i + j
//        i = j
//        j = aux
//        count = count - 1
//    }
//
//    ret j
//}
//
//fun recFib(count: Int): Int {
//    if count <= 0 {
//        ret 0
//    }
//    if count <= 1 {
//        ret 1
//    }
//
//    ret recFib(count - 1) + recFib(count - 2)
//}