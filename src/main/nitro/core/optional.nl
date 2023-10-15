
// This type represents an optional value. It can either be Some(value) or None
// It's used to represent a value that may or may not be present, similar to null in other languages
option Optional<#Value> {
    Some { value: #Value }
    None
}

// Creates an instance of the Some variant of the Optional type
fun Some(some: #Value): Optional<#Value> {
    ret ptr_of(some).unsafe_cast().get_value<Optional<#Value>>()
}

// Creates an instance of the None variant of the Optional type
fun None<#Value>(): Optional<#Value> {
    ret null_ptr<Optional<#Value>>().get_value()
}

// Checks if the Optional is the Some variant
fun Optional<#Value>.is_some(): Boolean = not (ptr_of(this).is_null())

// Checks if the Optional is the None variant
fun Optional<#Value>.is_none(): Boolean = ptr_of(this).is_null()

// Returns the value of the Some variant, or the default value if the Optional is the None variant
fun Optional<#Value>.get_or_default(default: #Value): #Value {
    ret if this.is_some() { this!! } else { default }
}

// Returns the value of the Some variant, or crashes if the Optional is the None variant
// Can be called with the syntax `optional!!`
fun Optional<#Value>.get_or_crash(): #Value {
    if this.is_some() {
        ret ptr_of(this).unsafe_cast().get_value()
    }

    crash("Call to get_or_crash() on None variant")
}

// Returns the value of the Some variant, or panics if the Optional is the None variant
fun <#Value: ToString> Optional<#Value>.to_string(): String {
    ret when {
        this.is_some() -> "Some(${this!!})"
        else -> "None"
    }
}
