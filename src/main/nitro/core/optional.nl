
option Optional<#Value> {
    Some { value: #Value }
    None
}

fun Some(some: #Value): Optional<#Value> {
    ret Optional::Some<#Value> $[value: some]
}

fun None<#Value>(): Optional<#Value> {
    ret Optional::None<#Value> $[]
}

fun Optional<#Value>.is_some(): Boolean = this is Optional::Some<#Value>
fun Optional<#Value>.is_none(): Boolean = this is Optional::None<#Value>

fun Optional<#Value>.get_or_default(default: #Value): #Value {
    ret if this.is_some() { this.get_or_crash() } else { default }
}

fun Optional<#Value>.get_or_crash(): #Value {
    if this is Optional::Some<#Value> {
        ret (this as Optional::Some<#Value>).value
    }

    crash("Call to get_or_crash() on None variant")
}

fun <#Value: ToString> Optional<#Value>.to_string(): String {
    ret when {
        this is Optional::Some<#Value> -> "Some(${(this as Optional::Some<#Value>).value})"
        else -> "None"
    }
}
