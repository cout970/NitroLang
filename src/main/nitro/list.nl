
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

fun List<#Item>.add(value: #Item) {
    // Expand list
    if this.len + 1 > this.capacity {
        // Double or initialize to 16
        let new_capacity: Int = max(this.capacity, 8) * 2
        let new_data: Int = memory_alloc(new_capacity * size_of<#Item>)

        if (this.capacity > 0) {
            memory_copy(this.data, new_data, this.capacity)
        }

        this.data = new_data
        this.capacity = new_capacity
    }

    let ptr_in_list = this.data + this.len * size_of<#Item>
    memory_write_int(ptr_in_list, ptr_of(value))
    this.len = this.len + 1
}

fun List<#Item>.get(index: Int): #Item {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    let ptr_in_list = this.data + index * size_of<#Item>
    let ptr = memory_read_int(ptr_in_list)
    ret ptr
}

fun List<#Item>.set(index: Int, value: #Item) {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    let ptr_in_list = this.data + index * size_of<#Item>
    memory_write_int(ptr_in_list, ptr_of(value))
}

fun List<#Item>.len(): Int {
    ret this.len
}

fun List<#Item>.is_empty(): Boolean = this.len == 0

fun List<#Item>.is_not_empty(): Boolean = this.len != 0

fun println(a: List<Int>) {
    println("List (${a.len()}):")
    repeat a.len() {
        println("  - ${a.get(it)}")
    }
}

fun println(a: List<String>) {
    println("List (${a.len()}):")
    repeat a.len() {
        println("  - ${a.get(it)}")
    }
}