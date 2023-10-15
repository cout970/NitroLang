
// This type represents a heap-allocated sequence of items of type `#Item`
// The implementation is a dynamic array that doubles in size when it runs out of space.
// The array stores pointers to the items, so the items are not owned by the list
//
// A list can be created using the `#[...]` syntax:
// ```
// let my_list: List<Int> = #[1, 2, 3]
// ```
// or using the `List::new()` function:
// ```
// let my_list = List<Int>::new()
// my_list[] = 1
// my_list[] = 2
// my_list[] = 3
// ```
struct List<#Item> {
    capacity: Int
    len: Int
    items: RawArray<Ptr<#Item>>
}

// This is the default constructor for the list type, creates an empty list
fun List::new<#Item>(): List<#Item> {
    ret List<#Item> $[
        capacity: 0
        len: 0
        items: null_ptr<Ptr<#Item>>().to_raw_array()
    ]
}

// This function adds an item to the end of the list
// Can be called using the `list[] = value` syntax:
// `my_list[] = 1` is equivalent to `my_list.add(1)`
//
fun List<#Item>.add(item: #Item) {
    // Expand list if needed
    if this.len + 1 > this.capacity {
        // Double or initialize to 16
        let new_capacity: Int = max(this.capacity, 8) * 2
        let new_items: RawArray<Ptr<#Item>> = RawArray::new<Ptr<#Item>>(new_capacity)

        if (this.len > 0) {
            this.items.copy_into(new_items, this.capacity)
        }

        this.items = new_items
        this.capacity = new_capacity
    }

    this.items.get_ptr(this.len).write(ptr_of<#Item>(item))
    this.len = this.len + 1
}

// Get the item at the given index, if the index is out of bounds, returns `None`
// Can be called using the `list[index]` syntax:
// `my_list[0]` is equivalent to `my_list.get(0)`
//
fun List<#Item>.get(index: Int): Optional<#Item> {
    if index < 0 || index >= this.len {
        ret None<#Item>()
    }

    let item_ptr: Ptr<#Item> = this.items.get_ptr(index).read()
    ret Some(item_ptr.get_value())
}

// Set the item at the given index, if the index is out of bounds, the program crashes
// Can be called using the `list[index] = value` syntax:
// `my_list[0] = 1` is equivalent to `my_list.set(0, 1)`
fun List<#Item>.set(index: Int, item: #Item) {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    this.items.get_ptr(index).write(ptr_of<#Item>(item))
}

// Returns the size of the list, meaning the number of items it contains
fun List<#Item>.len(): Int {
    ret this.len
}

// Checks if the list is empty
fun List<#Item>.is_empty(): Boolean = this.len == 0

// Checks if the list is not empty
fun List<#Item>.is_not_empty(): Boolean = this.len != 0

// Generates a string with debug information about the list internals
fun List<#Item>.to_debug_string(): String {
    ret "List \$[capacity: ${this.capacity}, len: ${this.len}, data: ${this.items.to_debug_string()}]"
}

// Converts the list to a string
fun <#Item: ToString> List<#Item>.to_string(): String {
    let str = "#["

    repeat this.len {
        str = str.concat(this[it].to_string())

        if it != limit - 1 {
            str = str.concat(", ")
        }
    }

    ret str.concat("]")
}

// Iterates over the list and calls the given function for each item
// Can be called using the `list.for_each #{ ... }` syntax:
// ```
// #[1, 2, 3].for_each #{ item: Int ->
//     println(item)
// }
// ```
fun for_each(list: List<#Item>, func: (#Item) -> Nothing) {
    repeat list.len() {
        func.invoke(list[it]!!)
    }
}

// Iterates over the list and calls the given function for each item, passing the index as well
// Can be called using the `list.for_each_entry #{ ... }` syntax:
// ```
// #[1, 2, 3].for_each_entry #{ index: Int, item: Int ->
//     println("Item at index $index is $item")
// }
fun for_each_entry(list: List<#Item>, func: (Int, #Item) -> Nothing) {
    repeat list.len() {
        func.invoke(it, list[it]!!)
    }
}