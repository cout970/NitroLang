
// This type represents a map, a.k.a a dictionary or an associative array
// It is implemented as a hash table with open addressing and linear probing
// Any type that has the functions Type.get_hash() and Type.get_ordering(Type) can be used as a key
//
// A map can be created using the `@[...]` syntax:
// ```
// let map = @[
//     foo: 1,
//     bar: 2,
// ]
// ```
// If the keys are strings the " can be omitted is only ascii letters, numbers and _ are used
// Any arbitrary expression can be used as a key by surrounding it with ()
// ```
// let map = @[
//     regular_key: 0,
//     "key with spaces": 1,
//     "key with string templates $index => ${1 + 1}": 2,
//     (123.to_string()): 3,
//     (hash_of(item)): 4,
// ]
// ```
@Extern $[lib: "core", name: "Map"]
struct Map<#Key, #Value> {
    len: Int
    table: List<Optional<MapEntry<#Key, #Value>>>
    // TODO: Use a raw array instead of a list of optionals to save memory
    // table2: RawArray<Ptr<MapEntry<#Key, #Value>>>
}

// Slot in the map
struct MapEntry<#Key, #Value> {
    key: #Key
    value: #Value
}

// Functions required for keys
tag MapKey {
    fun This.get_hash(): Int
    fun This.get_ordering(other: This): Ordering
}

// Creates an empty map
fun <#Key: MapKey> Map::new(): Map<#Key, #Value> {
    ret Map<#Key, #Value> $[
        len: 0
        table: List::new<Optional<MapEntry<#Key, #Value>>>()
    ]
}

// Makes sure the map has capacity for the requested number of items
fun <#Key: MapKey> Map<#Key, #Value>.ensure_capacity(capacity: Int) {
    if max(this.len, capacity).to_float() < this.table.len().to_float() * 0.75 {
        // No resize needed
        ret
    }

    let new_size = (this.table.len() * 2).max(16)

    while new_size.to_float() < this.table.len().to_float() * 0.75 {
        new_size = new_size * 2
    }

    // Keep old table to move items later
    let old_table = this.table
    this.clear()

    repeat new_size {
        this.table.add(None<MapEntry<#Key, #Value>>())
    }

    repeat old_table.len() {
        let opt = old_table[it]!!

        if opt.is_some() {
            let entry: MapEntry<#Key, #Value> = opt!!
            this.set_entry(entry)
        }
    }
}

// Override an entry of the map with a new value
fun <#Key: MapKey> Map<#Key, #Value>.set(key: #Key, value: #Value) {
    this.ensure_capacity(this.len + 1)

    // Index where to insert the value, if available
    let index = key.get_hash().modulo(this.table.len())

    repeat this.table.len() {
        let slot = this.table[index]!!
        let available = slot.is_none()

        if not available {
            if slot!!.key == key {
                available = true
            }
        }

        if available {
            this.table[index] = Some(MapEntry<#Key, #Value> $[key, value])

            // Increment the length if the slot was empty before the modification
            if slot.is_none() {
                this.len = this.len + 1
            }

            ret
        }

        index = index + 1
    }

    unreachable()
}

// Equivalent to `Map.set(key, value)` but with a MapEntry
fun <#Key: MapKey> Map<#Key, #Value>.set_entry(entry: MapEntry<#Key, #Value>) {
    this.set(entry.key, entry.value)
}

// Finds a value in the map by key
fun <#Key: MapKey> Map<#Key, #Value>.get(key: #Key): Optional<#Value> {
    let opt = this.get_entry(key)

    if opt.is_none() {
        ret None()
    }

    ret Some(opt!!.value)
}

// Equivalent to `Map.get(key)` but returns a MapEntry
fun <#Key: MapKey> Map<#Key, #Value>.get_entry(key: #Key): Optional<MapEntry<#Key, #Value>> {
    if this.len == 0 || this.table.len() == 0 {
        ret None()
    }

    // Index where to find the value, if present
    let index = key.get_hash().modulo(this.table.len())

    repeat this.table.len() {
        let slot = this.table[index]!!

        // Not found
        if slot.is_none() {
            ret None()
        }

        // Found something
        let entry: MapEntry<#Key, #Value> = slot!!

        // Found it
        if entry.key == key {
            ret Some(entry)
        }

        // Check next slot
        index = index + 1
    }

    // Not found
    ret None()
}

// Length/size of the map, a.k.a the number of items in the map
fun <#Key: MapKey> Map<#Key, #Value>.len(): Int { ret this.len }

// Removes all values from the map
fun <#Key: MapKey> Map<#Key, #Value>.clear() {
    this.table = List::new<Optional<MapEntry<#Key, #Value>>>()
    this.len = 0
}

// Checks if the map has no items
fun <#Key: MapKey> Map<#Key, #Value>.is_empty(): Boolean = this.len == 0

// Checks if the map has at least one item
fun <#Key: MapKey> Map<#Key, #Value>.is_not_empty(): Boolean = this.len != 0

// Returns the list of keys in the map
fun <#Key: MapKey> Map<#Key, #Value>.keys_as_list(): List<#Key> {
    let res = List::new<#Key>()

    repeat this.table.len() {
        let slot = this.table[it]!!

        if slot.is_some() {
            res[] = slot!!.key
        }
    }

    ret res
}

// Returns the list of values in the map
fun <#Key: MapKey> Map<#Key, #Value>.values_as_list(): List<#Value> {
    let res = List::new<#Value>()

    repeat this.table.len() {
        let slot = this.table[it]!!

        if slot.is_some() {
            res[] = slot!!.value
        }
    }

    ret res
}

// Returns the list of entries in the map
fun <#Key: MapKey> Map<#Key, #Value>.entries_as_list(): List<MapEntry<#Key, #Value>> {
    let res = List::new<MapEntry<#Key, #Value>>()

    repeat this.table.len() {
        let slot = this.table[it]!!

        if slot.is_some() {
            res[] = slot!!
        }
    }

    ret res
}

// Converts the map to a string
fun <#Key: MapKey, ToString, #Value: ToString> Map<#Key, #Value>.to_string(): String {
    let str = "@["

    repeat this.len {
        let slot = this.table[it]!!

        if slot.is_none() {
            continue
        }

        let entry: MapEntry<#Key, #Value> = slot!!

        str = str.concat(entry.key.to_string())
        str = str.concat(" => ")
        str = str.concat(entry.value.to_string())

        if it != limit - 1 {
            str = str.concat(", ")
        }
    }

    ret str.concat("]")
}
