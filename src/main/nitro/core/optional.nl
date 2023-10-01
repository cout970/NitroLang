
option Optional<#Value> {
    None {}
    Some { value: #Value }
}

fun Some(some: #Value): Optional<#Value> = Optional::Some<#Value> $[value: some]
fun None<#Value>(): Optional<#Value> = Optional::None<#Value> $[]

fun Optional<#Value>.is_some(): Boolean = this is Optional::Some<#Value>
fun Optional<#Value>.is_none(): Boolean = this is Optional::None<#Value>

fun Optional<#Value>.get_or_crash(): #Value {
    if this is Optional::Some<#Value> {
        ret (this as Optional::Some<#Value>).value
    }

    crash("Call to get_or_crash() on None variant")
}

//fun <#Value: ToString> Optional<#Value>.to_string(): String {
//    if this is Optional::Some<#Value> {
//        let value: #Value = (this as Optional::Some<#Value>).value
//        ret "Some($value)"
//    }
//
//    ret "None"
//}
