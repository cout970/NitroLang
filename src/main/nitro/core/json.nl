
// This type represents a json value. It can be a boolean, number, string, array, object or null.
// Can be created inline using the `json` literal:
// ```
// let value: Json = json {
//    "key1": "value",
//    "key2": 123,
//    "key3": true,
//    "key4": null,
//    "key5": [1, 2, 3],
//    "key6": [{ "key": "value" }]
// }
// ```
option Json {
    Boolean { value: Boolean }
    Number { value: Float }
    String { value: String }
    Array { value: List<Json> }
    Object { value: Map<String, Json> }
    Null {}
}

// Cast this value to a Json::Number and return it's value as an int.
// If this value is not a Json::Number, returns Optional::None
fun Json.as_int(): Optional<Int> {
    ret if this is Json::Number {
        Some((this as Json::Number).value.to_int())
    } else {
        None()
    }
}

// Cast this value to a Json::Number and return it's value as a float.
// If this value is not a Json::Number, returns Optional::None
fun Json.as_float(): Optional<Float> {
    ret if this is Json::Number {
        Some((this as Json::Number).value)
    } else {
        None()
    }
}

// Cast this value to a Json::Boolean and return it's value as a float.
// If this value is not a Json::Boolean, returns Optional::None
fun Json.as_boolean(): Optional<Boolean> {
    ret if this is Json::Boolean {
        Some((this as Json::Boolean).value)
    } else {
        None()
    }
}

// Cast this value to a Json::String and return it's value.
// If this value is not a Json::String, returns Optional::None
fun Json.as_string(): Optional<String> {
    ret if this is Json::String {
        Some((this as Json::String).value)
    } else {
        None()
    }
}

// Cast this value to a Json::Array and return a list of the elements in the array.
// If this value is not a Json::Array, returns Optional::None
fun Json.as_list(): Optional<List<Json>> {
    ret if this is Json::Array {
        Some((this as Json::Array).value)
    } else {
        None()
    }
}

// Cast this value to a Json::Object and return a map of the properties in the object.
// If this value is not a Json::Object, returns Optional::None
fun Json.as_map(): Optional<Map<String, Json>> {
    ret if this is Json::Object {
        Some((this as Json::Object).value)
    } else {
        None()
    }
}

// Checks if this value is a Json::Null
fun Json.is_null(): Boolean = this is Json::Null

// Converts this value to a string.
fun Json::Null.to_string(): String = "null"

// Converts this value to a string.
fun Json::Boolean.to_string(): String = this.value.to_string()

// Converts this value to a string.
fun Json::Number.to_string(): String = this.value.to_string()

// Converts this value to a string.
fun Json::String.to_string(): String = "\"".concat(this.value).concat("\"")

// Converts this value to a string.
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

// Converts this value to a string.
fun Json::Object.to_string(): String {
    let map: Map<String, Json> = this.value

    if map.is_empty() {
        ret "{}"
    }

    let keys: List<String> = map.keys_to_list()
    let str = "{"

    str = str.concat("\n")
    repeat keys.len() {
        let key: String = keys[it]!!
        let value: Optional<Json> = map[key]

        str = str.concat("  \"")
        str = str.concat(key)
        str = str.concat("\": ")
        str = str.concat(value!!.to_string().replace("\n", "\n  "))

        if it + 1 < limit {
            str = str.concat(", ")
            str = str.concat("\n")
        }
    }
    str = str.concat("\n")
    str = str.concat("}")
    ret str
}

// Converts this json value to a string.
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

