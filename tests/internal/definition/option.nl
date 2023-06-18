
option Bool {
    True  {}
    False {}
}

option NodeList<#T> {
    Cons { value: #T, next: NodeList<#T> }
    Nil  {}
}

option Error {
    InvalidValue {}
    MissingInput {}
    ReachedEndOfFile {}
    OsError { error: OsError }
}

struct OsError { os_code: Int, message: String }

fun main() {
    let a: Optional<Int> = Optional::Some $[value: 42]
    let b: Optional<Int> = Optional::None $[]

    let c: Result<Int, String> = Result::Ok  $[value: 420]
    let d: Result<Int, String> = Result::Err $[value: "Not found"]

    let e: Bool = Bool::True $[]
    let f: Bool = Bool::False $[]

    let n: NodeList<Int> = NodeList::Cons $[
        value: 0
        next: NodeList::Cons $[
            value: 1
            next: NodeList::Nil $[]
        ]
    ]

    let err = Error::OsError $[
        error: OsError $[
            os_code: 1000
            message: "Error with code 1000"
        ]
    ]
}