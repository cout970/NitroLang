
option Json {
    Boolean { value: Boolean }
    Number { value: Float }
    String { value: String }
    Array { value: List<Json> }
    Object { value: StringMap<Json> }
    Null {}
}

fun Json.as_int(): Optional<Int> {
    ret if this is Json::Number {
        Some((this as Json::Number).value.to_int())
    } else {
        None()
    }
}

fun Json.as_float(): Optional<Float> {
    ret if this is Json::Number {
        Some((this as Json::Number).value)
    } else {
        None()
    }
}

fun Json.as_string(): Optional<String> {
    ret if this is Json::String {
        Some((this as Json::String).value)
    } else {
        None()
    }
}

fun Json.as_list(): Optional<List<Json>> {
    ret if this is Json::Array {
        Some((this as Json::Array).value)
    } else {
        None()
    }
}

fun Json.as_string_map(): Optional<StringMap<Json>> {
    ret if this is Json::Object {
        Some((this as Json::Object).value)
    } else {
        None()
    }
}

fun Json.is_null(): Boolean = this is Json::Null

fun Json::Null.to_string(): String = "null"

fun Json::Boolean.to_string(): String = this.value.to_string()

// TODO remove once floats work
fun Json::Number.to_string(): String = this.value.to_int().to_string()

fun Json::String.to_string(): String = "\"".concat(this.value).concat("\"")

fun Json::Array.to_string(): String {
    let list: List<Json> = this.value

    if list.is_empty() {
        ret "[]"
    }

    let str = "["

    str = str.concat("\n")
    repeat list.len() {
        str = str.concat("  ")
        str = str.concat(list[it].to_string().replace("\n", "\n  "))

        if it + 1 < limit {
            str = str.concat(", ")
            str = str.concat("\n")
        }
    }
    str = str.concat("\n")
    str = str.concat("]")
    ret str
}

fun Json::Object.to_string(): String {
    let map: StringMap<Json> = this.value

    if map.is_empty() {
        ret "{}"
    }

    let keys: List<String> = map.keys_as_list()
    let str = "{"

    str = str.concat("\n")
    repeat keys.len() {
        let key: String = keys[it]
        let value: Optional<Json> = map[key]

        str = str.concat("  \"")
        str = str.concat(key)
        str = str.concat("\": ")
        str = str.concat(value.get_or_crash().to_string().replace("\n", "\n  "))

        if it + 1 < limit {
            str = str.concat(", ")
            str = str.concat("\n")
        }
    }
    str = str.concat("\n")
    str = str.concat("}")
    ret str
}

fun Json.to_string(): String {
    if this is Json::Null {
        ret (this as Json::Null).to_string()
    }
    if this is Json::Boolean {
        ret (this as Json::Boolean).to_string()
    }
    if this is Json::Number {
        ret (this as Json::Number).to_string()
    }
    if this is Json::String {
        ret (this as Json::String).to_string()
    }
    if this is Json::Array {
        ret (this as Json::Array).to_string()
    }
    if this is Json::Object {
        ret (this as Json::Object).to_string()
    }
    crash("Unknown json value")
}

