// List of items stored in heap
// The list stores references to the items, not the items themselves
struct List<#Item> {
    capacity: Int
    len: Int
    items: RawArray<Ptr<#Item>>
}

// Creates an empty list
fun create_list<#Item>(): List<#Item> {
    ret List<#Item> $[
        capacity: 0
        len: 0
        items: null_ptr<Ptr<#Item>>().to_raw_array()
    ]
}

// Adds an item to the end of the list
fun List<#Item>.add(item: #Item) {
    // Expand list if needed
    if (this.len + 1).greater_than_signed(this.capacity) {
        // Double or initialize to 16
        let new_capacity: Int = max(this.capacity, 8) * 2
        let new_items: RawArray<Ptr<#Item>> = create_raw_array<Ptr<#Item>>(new_capacity)

        if (this.capacity.greater_than_signed(0)) {
            this.items.copy_into(new_items, this.capacity)
        }

        this.items = new_items
        this.capacity = new_capacity
    }

    this.items.get_ptr(this.len).write(ptr_of<#Item>(item))
    this.len = this.len + 1
}

fun List<#Item>.get(index: Int): #Item {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    let item_ptr: Ptr<#Item> = this.items.get_ptr(index).read()
    ret item_ptr.get_value()
}

fun List<#Item>.set(index: Int, item: #Item) {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    this.items.get_ptr(index).write(ptr_of<#Item>(item))
}

fun List<#Item>.len(): Int {
    ret this.len
}

fun List<#Item>.is_empty(): Boolean = this.len == 0

fun List<#Item>.is_not_empty(): Boolean = this.len != 0

fun List<#Item>.to_debug_string(): String {
    ret "List \$[capacity: ${this.capacity}, len: ${this.len}, data: ${this.items.to_debug_string()}]"
}
