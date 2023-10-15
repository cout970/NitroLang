
// This type is used to represent the result of an operation that may fail
//
// If the operation succeeds, the result is an Ok containing the value returned
// If the operation fails, the result is an Err containing the error returned
//
// This is the preferred way to handle errors in the language
// It is similar to the Option type, but with an error value instead of None
option Result<#Ok, #Err> {
    Ok { value: #Ok }
    Err { value: #Err }
}

// Creates a new Result with the Ok variant
fun Ok<#Ok, #Err>(ok: #Ok): Result<#Ok, #Err> = Result::Ok<#Ok, #Err> $[value: ok]

// Creates a new Result with the Err variant
fun Err<#Ok, #Err>(err: #Err): Result<#Ok, #Err> = Result::Err<#Ok, #Err> $[value: err]

// Checks if the result is Ok
fun Result<#Ok, #Err>.is_ok(): Boolean = this is Result::Ok<#Ok, #Err>

// Checks if the result is Err
fun Result<#Ok, #Err>.is_err(): Boolean = this is Result::Err<#Ok, #Err>

// Returns the value of the result if it is Ok, otherwise panics
// Can be called with the syntax `result!!`
fun Result<#Ok, #Err>.get_or_crash(): #Ok {
    if this is Result::Ok<#Ok, #Err> {
        ret (this as Result::Ok<#Ok, #Err>).value
    }

    panic()
}

// Get the ok value of the result if it is Ok, otherwise None
fun Result<#Ok, #Err>.get_ok(): Optional<#Ok> {
    if this is Result::Ok<#Ok, #Err> {
        ret Some((this as Result::Ok<#Ok, #Err>).value)
    }

    ret None()
}

// Get the error value of the result if it is Err, otherwise None
fun Result<#Ok, #Err>.get_err(): Optional<#Err> {
    if this is Result::Err<#Ok, #Err> {
        ret Some((this as Result::Err<#Ok, #Err>).value)
    }

    ret None()
}

fun <#Ok: ToString, #Err: ToString> Result<#Ok, #Err>.to_string(): String {
    if this is Result::Ok<#Ok, #Err> {
        ret "Ok(${(this as Result::Ok<#Ok, #Err>).value})"
    }

    ret "Err(${(this as Result::Err<#Ok, #Err>).value})"
}
