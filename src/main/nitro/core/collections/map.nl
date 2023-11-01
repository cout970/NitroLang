
// This type represents a unordered map, a.k.a a dictionary or an associative array
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
    capacity: Int
    len: Int
    table: RawArray<Optional<Pair<#Key, #Value>>>
}

// Functions required for keys
tag MapKey {
    fun This.get_hash(): Int
    fun This.get_ordering(other: This): Ordering
}

// Creates an empty map
fun <#Key: MapKey> Map::new(): Map<#Key, #Value> {
    ret Map<#Key, #Value> $[
        capacity: 0
        len: 0
        table: RawArray::new<Optional<Pair<#Key, #Value>>>(0)
    ]
}

// Makes sure the map has capacity for the requested number of items
fun <#Key: MapKey> Map<#Key, #Value>.ensure_capacity(capacity: Int) {
    if max(this.len, capacity).to_float() < this.capacity.to_float() * 0.75 {
        // No resize needed
        ret
    }

    let new_size = max(this.capacity * 2, 8)

    while new_size.to_float() < this.capacity.to_float() * 0.75 {
        new_size = new_size * 2
    }


    // Keep old table to move items later
    let old_table = this.table
    let old_capacity = this.capacity

    let new_table = RawArray::new<Optional<Pair<#Key, #Value>>>(new_size)
    this.capacity = new_size
    this.table = new_table

    this.clear()

    // Move items to the new table
    repeat old_capacity {
        let opt: Optional<Pair<#Key, #Value>> = old_table[it]

        if opt.is_some() {
           this.set_entry(opt!!)
        }
    }
}

// Override an entry of the map with a new value
fun <#Key: MapKey> Map<#Key, #Value>.set(key: #Key, value: #Value) {
    this.ensure_capacity(this.len + 1)

    // Index where to insert the value, if available
    let index = key.get_hash().modulo(this.capacity)

    repeat this.capacity {
        let slot = this.table[index]
        let available = slot.is_none()

        if not available {
            if slot!!.first == key {
                available = true
            }
        }

        if available {
            // Increment the length if the slot was empty before the modification
            if slot.is_none() {
                this.len = this.len + 1
            }

            this.table[index] = Some(Pair::of(key, value))
            ret
        }

        index = index + 1
    }

    unreachable()
}

// Equivalent to `Map.set(key, value)` but with a Pair
fun <#Key: MapKey> Map<#Key, #Value>.set_entry(entry: Pair<#Key, #Value>) {
    this.set(entry.first, entry.second)
}

// Finds a value in the map by key
fun <#Key: MapKey> Map<#Key, #Value>.get(key: #Key): Optional<#Value> {
    let opt = this.get_entry(key)

    if opt.is_none() {
        ret None()
    }

    ret Some(opt!!.second)
}

// Equivalent to `Map.get(key)` but returns a Pair
fun <#Key: MapKey> Map<#Key, #Value>.get_entry(key: #Key): Optional<Pair<#Key, #Value>> {
    if this.len == 0 || this.capacity == 0 {
        ret None()
    }

    // Index where to find the value, if present
    let index = key.get_hash().modulo(this.capacity)

    repeat this.capacity {
        let slot = this.table[index]

        // Not found
        if slot.is_none() {
            ret None()
        }

        // Found something
        let entry: Pair<#Key, #Value> = slot!!

        // Found it
        if entry.first == key {
            ret Some(entry)
        }

        // Check next slot
        index = index + 1
    }

    // Not found
    ret None()
}

// Removes all values from the map
fun <#Key: MapKey> Map<#Key, #Value>.clear() {
    this.len = 0

    repeat this.capacity {
        this.table[it] = None<Pair<#Key, #Value>>()
    }
}

// Removes a value from the map by key
fun <#Key: MapKey> Map<#Key, #Value>.remove(key: #Key): Boolean {
    if this.len == 0 || this.capacity == 0 {
        ret false
    }

    // Index where to find the value, if present
    let index = key.get_hash().modulo(this.capacity)

    repeat this.capacity {
        let slot = this.table[index]

        // Not found
        if slot.is_none() {
            ret false
        }

        // Found something
        let entry: Pair<#Key, #Value> = slot!!

        // Found it
        if entry.first == key {
            this.table[index] = None<Pair<#Key, #Value>>()
            this.len = this.len - 1
            ret true
        }

        // Check next slot
        index = index + 1
    }

    // Not found
    ret false
}

// Checks if the map contains a key
fun <#Key: MapKey> Map<#Key, #Value>.contains_key(key: #Key): Boolean {
    ret this.get(key).is_some()
}

// Checks if the map contains a value
fun <#Value: GetOrdering> Map<#Key, #Value>.contains_value(value: #Value): Boolean {
    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            let entry: Pair<#Key, #Value> = slot!!

            if entry.second == value {
                ret true
            }
        }
    }

    ret false
}

// Length/size of the map, a.k.a the number of items in the map
fun <#Key: MapKey> Map<#Key, #Value>.len(): Int { ret this.len }

// Checks if the map has no items
fun <#Key: MapKey> Map<#Key, #Value>.is_empty(): Boolean = this.len == 0

// Checks if the map has at least one item
fun <#Key: MapKey> Map<#Key, #Value>.is_not_empty(): Boolean = this.len != 0

// Returns the value for a key or inserts a new value if the key is not present
fun <#Key: MapKey> Map<#Key, #Value>.get_or_insert(key: #Key, value: #Value): #Value {
    if this.contains_key(key) {
        ret this[key]!!
    }

    this[key] = value
    ret value
}

// Returns the value for a key or computes and inserts a new value if the key is not present
fun <#Key: MapKey> Map<#Key, #Value>.get_or_compute(key: #Key, func: () -> #Value): #Value {
    if this.contains_key(key) {
        ret this[key]!!
    }

    let value = func.invoke()
    this[key] = value
    ret value
}

// Convenience function to iterate over the each entry in the map
fun <#Key: MapKey> Map<#Key, #Value>.for_each(func: (#Key, #Value) -> Nothing) {
    this.for_each_entry(func)
}

// Iterates over the each entry in the map
fun <#Key: MapKey> Map<#Key, #Value>.for_each_entry(func: (#Key, #Value) -> Nothing) {
    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            let entry: Pair<#Key, #Value> = slot!!
            func.invoke(entry.first, entry.second)
        }
    }
}

// Iterates over the each value in the map
fun <#Key: MapKey> Map<#Key, #Value>.for_each_value(func: (#Value) -> Nothing) {
    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            let entry: Pair<#Key, #Value> = slot!!
            func.invoke(entry.second)
        }
    }
}

// Iterates over the each key in the map
fun <#Key: MapKey> Map<#Key, #Value>.for_each_key(func: (#Key) -> Nothing) {
    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            let entry: Pair<#Key, #Value> = slot!!
            func.invoke(entry.first)
        }
    }
}

// Returns the list of keys in the map
fun <#Key: MapKey> Map<#Key, #Value>.keys_to_list(): List<#Key> {
    let res = List::new<#Key>()

    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            res[] = slot!!.first
        }
    }

    ret res
}

// Returns the list of values in the map
fun <#Key: MapKey> Map<#Key, #Value>.values_to_list(): List<#Value> {
    let res = List::new<#Value>()

    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            res[] = slot!!.second
        }
    }

    ret res
}

// Returns the list of entries in the map
fun <#Key: MapKey> Map<#Key, #Value>.entries_to_list(): List<Pair<#Key, #Value>> {
    let res = List::new<Pair<#Key, #Value>>()

    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            res[] = slot!!
        }
    }

    ret res
}

// Converts the map to a string
fun <#Key: MapKey, ToString, #Value: ToString> Map<#Key, #Value>.to_string(): String {
    let mut str = "@["
    let mut count = 0

    repeat this.capacity {
        let slot = this.table[it]

        if slot.is_some() {
            let entry: Pair<#Key, #Value> = slot!!

            str = str.concat(entry.first.to_string())
            str = str.concat(" => ")
            str = str.concat(entry.second.to_string())

            if count != this.len - 1 {
                str = str.concat(", ")
            }
            count = count + 1
        }
    }

    ret str.concat("]")
}
