
option Result<#Ok, #Err> {
    Ok { value: #Ok }
    Err { value: #Err }
}

fun Ok<#Ok, #Err>(ok: #Ok): Result<#Ok, #Err> = Result::Ok<#Ok, #Err> $[value: ok]
fun Err<#Ok, #Err>(err: #Err): Result<#Ok, #Err> = Result::Err<#Ok, #Err> $[value: err]

fun Result<#Ok, #Err>.is_ok(): Boolean = this is Result::Ok<#Ok, #Err>
fun Result<#Ok, #Err>.is_err(): Boolean = this is Result::Err<#Ok, #Err>

fun Result<#Ok, #Err>.ok_or_panic(): #Ok {
    if this is Result::Ok<#Ok, #Err> {
        ret (this as Result::Ok<#Ok, #Err>).value
    }

    panic()
}

fun Result<#Ok, #Err>.err_or_panic(): #Err {
    if this is Result::Err<#Ok, #Err> {
        ret (this as Result::Err<#Ok, #Err>).value
    }

    panic()
}