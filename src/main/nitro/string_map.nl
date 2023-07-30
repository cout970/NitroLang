
struct StringMap<#Value> {
    len: Int
    table: List<Optional<StringMapEntry<#Value>>>
}

struct StringMapEntry<#Value> {
    key: String
    value: #Value
}

fun create_string_map(): StringMap<#Value> {
    ret StringMap<#Value> $[
        len: 0
        table: create_list<Optional<StringMapEntry<#Value>>>()
    ]
}

fun StringMap<#Value>.set(key: String, value: #Value) {
    // Grow table
    if this.len.to_float() >= this.table.len().to_float() * 0.75 {
        let old_table = this.table
        let new_size = (old_table.len() * 2).max(16)

        this.clear()

        repeat new_size {
            this.table.add(None<StringMapEntry<#Value>>())
        }

        repeat old_table.len() {
            let opt = old_table[it]

            if opt.is_some() {
                let entry: StringMapEntry<#Value> = opt.get_or_crash()
                this.set_entry(entry)
            }
        }
    }

    // Insert item
    let index = key.get_hash().modulo(this.table.len())

    repeat this.table.len() {
        let opt = this.table[index]
        let empty = opt.is_none()

        if not empty {
            if opt.get_or_crash().key == key {
                empty = true
            }
        }

        if empty {
            this.table[index] = Some(StringMapEntry<#Value> $[
                key: key,
                value: value
            ])

            if opt.is_none() {
                this.len = this.len + 1
            }
            ret nothing
        }

        index = index + 1
    }

    crash("No empty entry in table!")
}

fun StringMap<#Value>.set_entry(entry: StringMapEntry<#Value>) {
    this.set(entry.key, entry.value)
}

fun StringMap<#Value>.get(key: String): Optional<#Value> {
    let opt = this.get_entry(key)

    if opt.is_none() {
        ret None()
    }

    ret Some(opt.get_or_crash().value)
}

fun StringMap<#Value>.get_entry(key: String): Optional<StringMapEntry<#Value>> {
    if this.len == 0 || this.table.len() == 0 {
        ret None()
    }

    // First index to try
    let index = key.get_hash().modulo(this.table.len())

    repeat this.table.len() {
        let opt = this.table[index]

        if opt.is_none() {
            ret None()
        }

        let entry: StringMapEntry<#Value> = opt.get_or_crash()

        if entry.key == key {
            ret Some(entry)
        }

        index = index + 1
    }

    ret None()
}

fun StringMap<#Value>.len(): Int { ret this.len }

fun StringMap<#Value>.clear() {
    this.table = create_list<Optional<StringMapEntry<#Value>>>()
    this.len = 0
}

fun StringMap<#Value>.is_empty(): Boolean = this.len == 0

fun StringMap<#Value>.is_not_empty(): Boolean = this.len != 0

fun StringMap<#Value>.keys_as_list(): List<String> {
    let res = create_list<String>()

    repeat this.table.len() {
        let opt = this.table[it]

        if opt.is_some() {
            res[] = opt.get_or_crash().key
        }
    }

    ret res
}
