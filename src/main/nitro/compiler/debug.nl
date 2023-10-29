@ValueType
struct A {
    a0: Int
    a1: Int
}

struct B {
    b0: Optional<A>
}

struct C {
    c0: Int
    c1: Int
}

fun is_lambda(a: #A, lamb: (#A, #A) -> #A): #A {
    ret lamb.invoke(a, a)
}

fun debug() {
//    let j = @[
//        (1): true,
//        (1): true,
//        (0): true,
//        (0): true,
//        (2): true,
//        (2): true
//    ]
//    println("Map: ${j}")
//
//    let i = %[1, 1, 0, 0, 2, 2]
//    println("Set: ${i}")
//
//    get_memory().dump()

//      println(#[true] is List<*>)

//    let b = B $[b0: Some(A $[a0: 420, a1: 69])]
//    let array = RawArray::new<B>(4)
//
//    let item0 = array.get_ptr(0)
//
//    let b2 = item0.as_ref()
//
//    println(ptr_of(b2).get_address())
//    println(ptr_of(array[0]).get_address())
}
