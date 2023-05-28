option Optional<#T> {
    Some { value: #T }
    None {}
}

option Result<#Success, #Error> {
    Ok  { value: #Success }
    Err { value: #Error   }
}

option Boolean {
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

fun main(): Unit {}