
struct List<#Item> {
    data: Int
    len: Int
    capacity: Int
}

fun create_list<#Item>(): List<#Item> {
    ret List<#Item> $[
        data: 0
        len: 0
        capacity: 0
    ]
}

fun List<#Item>.add(item: #Item) {
    // Expand list
    if this.len + 1 > this.capacity {
        // Double or initialize to 16
        let new_capacity: Int = max(this.capacity, 8) * 2
        let new_data: Int = memory_alloc(new_capacity * size_of<#Item>)

        memory_copy(this.data, new_data, this.capacity)

        this.data = new_data
        this.capacity = new_capacity
    }

    memory_write(this.data + this.len * size_of<#Item>, item)
    this.len = this.len + 1
}

fun List<#Item>.get(index: Int): #Item {
    if index < 0 || index >= this.len {
        crash("Invalid list index: ".concat(index.to_string()).concat(" len: ").concat(this.len.to_string()))
    }
    ret memory_read<#Item>(this.data +  index * size_of<#Item>)
}

fun List<#Item>.set(index: Int, value: #Item) {
    if index < 0 || index >= this.len {
        crash("Invalid list index: ".concat(index.to_string()).concat(" len: ").concat(this.len.to_string()))
    }
    memory_write<#Item>(this.data +  index * size_of<#Item>, value)
}

fun List<#Item>.len(): Int {
    ret this.len
}

fun List<#Item>.is_empty(): Boolean = this.len == 0

fun List<#Item>.is_not_empty(): Boolean = this.len != 0

fun println(a: List<Int>) {
    println("List (".concat(a.len().to_string()).concat("):"))
    repeat a.len() {
        println("  - ".concat(a.get(it).to_string()))
    }
}

fun println(a: List<String>) {
    println("List (".concat(a.len().to_string()).concat("):"))
    repeat a.len() {
        println("  - ".concat(a.get(it)))
    }
}