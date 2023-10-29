
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

// Creates an empty list
fun List::new<#Item>(): List<#Item> {
    ret List<#Item> $[
        capacity: 0
        len: 0
        items: null_ptr<Ptr<#Item>>().to_raw_array()
    ]
}

// Creates an empty list with the given capacity pre-allocated
fun List::new<#Item>(capacity: Int): List<#Item> {
    ret List<#Item> $[
        capacity: capacity
        len: 0
        items: RawArray::new<Ptr<#Item>>(capacity)
    ]
}

// Makes sure the list has capacity for the requested number of items
fun List<#Item>.ensure_capacity(capacity: Int) {
    if capacity <= this.capacity {
       ret
    }

    let new_capacity = max(this.capacity * 2, 16)

    while new_capacity < capacity {
        new_capacity = new_capacity * 2
    }

    let new_items: RawArray<Ptr<#Item>> = RawArray::new<Ptr<#Item>>(new_capacity)

    if (this.len > 0) {
        this.items.copy_into(new_items, this.capacity)
    }

    this.items = new_items
    this.capacity = new_capacity
}

// This function adds an item to the end of the list
// Can be called using the `list[] = value` syntax:
// `my_list[] = 1` is equivalent to `my_list.add(1)`
//
fun List<#Item>.add(item: #Item) {
    // Expand list if needed
    this.ensure_capacity(this.len + 1)

    let item_ptr = this.items.get_ptr(this.len)
    item_ptr.write(ptr_of<#Item>(item))
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
    ret Some(item_ptr.unsafe_as_ref())
}

// Set the item at the given index, if the index is out of bounds, the program crashes
// Can be called using the `list[index] = value` syntax:
// `my_list[0] = 1` is equivalent to `my_list.set(0, 1)`
fun List<#Item>.set(index: Int, item: #Item) {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    let item_ptr = this.items.get_ptr(index)
    item_ptr.write(ptr_of<#Item>(item))
}

// Removes the item at the given index, if the index is out of bounds, the program crashes
fun List<#Item>.remove_at(index: Int) {
    if index < 0 || index >= this.len {
        crash("Invalid list index: $index, len: ${this.len}")
    }

    // Shift all items after the index one position to the left
    repeat this.len - index - 1 {
        let ptr = this.items.get_ptr(index + it + 1).read()
        this.items.get_ptr(index + it).write(ptr)
    }

    this.len = this.len - 1
}

// Removes the given item from the list, if the item is not in the list, nothing happens
fun <#Item: GetOrdering> List<#Item>.remove_value(item: #Item) {
    let index = this.index_of(item)

    if index.is_some() {
        this.remove_at(index!!)
    }
}

// Returns the index of the given item, if the item is not in the list, returns `None`
fun <#Item: GetOrdering> List<#Item>.index_of(item: #Item): Optional<Int> {
    repeat this.len {
        if this[it]!! == item {
            ret Some(it)
        }
    }

    ret None()
}

// Returns the size of the list, meaning the number of items it contains
fun List<#Item>.len(): Int {
    ret this.len
}

// Checks if the list is empty
fun List<#Item>.is_empty(): Boolean = this.len == 0

// Checks if the list is not empty
fun List<#Item>.is_not_empty(): Boolean = this.len != 0

// Checks if the list contains the given item
fun <#Item: GetOrdering> List<#Item>.contains(item: #Item): Boolean {
    ret this.index_of(item).is_some()
}

// Returns a new list where each item is replaced by the result of the given function
fun List<#Item>.map(func: (#Item) -> #Other): List<#Other> {
    let result = List::new<#Other>(this.len)

    repeat this.len {
        result[] = func.invoke(this[it]!!)
    }

    ret result
}

// Each item is passed to the given function to get a new list, the result is a list will all the items
fun List<#Item>.flat_map(func: (#Item) -> List<#Other>): List<#Other> {
    let result = List::new<#Other>(this.len)

    repeat this.len {
        let items = func.invoke(this[it]!!)
        repeat items.len {
            result[] = items[it]!!
        }
    }

    ret result
}

// Returns a new list with only the items that match the given predicate
fun List<#Item>.filter(func: (#Item) -> Boolean): List<#Item> {
    let result = List::new<#Item>(this.len)

    repeat this.len {
        if func.invoke(this[it]!!) {
            result[] = this[it]!!
        }
    }

    ret result
}

// Given a list of lists, returns a new list with all the items in the sublists
fun List<List<#Item>>.flatten(): List<#Item> {
    let result = List::new<#Item>(this.len)

    repeat this.len {
        let items = this[it]!!
        repeat items.len {
            result[] = items[it]!!
        }
    }

    ret result
}

// Counts the number of items in the list that match the given predicate
fun <#Item: GetOrdering> List<#Item>.count(predicate: (#Item) -> Boolean): Int {
    let count = 0

    repeat this.len {
        if predicate.invoke(this[it]!!) {
            count = count + 1
        }
    }

    ret count
}

// Returns true if all the items in the list match the given predicate
// If the list is empty, returns true
fun <#Item: GetOrdering> List<#Item>.all(predicate: (#Item) -> Boolean): Boolean {
    repeat this.len {
        if !predicate.invoke(this[it]!!) {
            ret false
        }
    }

    ret true
}

// Returns true if any of the items in the list match the given predicate
// If the list is empty, returns false
fun <#Item: GetOrdering> List<#Item>.any(predicate: (#Item) -> Boolean): Boolean {
    repeat this.len {
        if predicate.invoke(this[it]!!) {
            ret true
        }
    }

    ret false
}

// Returns true if none of the items in the list match the given predicate
// If the list is empty, returns true
fun <#Item: GetOrdering> List<#Item>.none(predicate: (#Item) -> Boolean): Boolean {
    repeat this.len {
        if predicate.invoke(this[it]!!) {
            ret false
        }
    }

    ret true
}

// Returns the first item in the list, if the list is empty, returns `None`
fun <#Item: GetOrdering> List<#Item>.first(): Optional<#Item> {
    if this.is_empty() {
        ret None()
    }

    ret Some(this[0]!!)
}

// Returns a new list with all the items except the first one
fun <#Item: GetOrdering> List<#Item>.rest(): List<#Item> {
    let result = List::new<#Item>(this.len - 1)

    repeat this.len - 1 {
        result[] = this[it + 1]!!
    }

    ret result
}

// Returns the last item in the list, if the list is empty, returns `None`
fun <#Item: GetOrdering> List<#Item>.last(): Optional<#Item> {
    if this.is_empty() {
        ret None()
    }

    ret Some(this[this.len - 1]!!)
}

// Finds the first item in the list that matches the given predicate, if no item matches, returns `None`
fun <#Item: GetOrdering> List<#Item>.find(predicate: (#Item) -> Boolean): Optional<#Item> {
    repeat this.len {
        if predicate.invoke(this[it]!!) {
            ret Some(this[it]!!)
        }
    }

    ret None()
}

// Finds the index of the first item in the list that matches the given predicate, if no item matches, returns `None`
fun <#Item: GetOrdering> List<#Item>.find_index(predicate: (#Item) -> Boolean): Optional<Int> {
    repeat this.len {
        if predicate.invoke(this[it]!!) {
            ret Some(it)
        }
    }

    ret None()
}

// Generates a string with debug information about the list internals
fun List<#Item>.to_debug_string(): String {
    ret "List \$[capacity: ${this.capacity}, len: ${this.len}, data: ${this.items}]"
}

// Converts the list to a string
fun <#Item: ToString> List<#Item>.to_string(): String {
    let str = "#["

    repeat this.len {
        let item = this[it]!!.to_string()
        str = str.concat(item)

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
fun List<#Item>.for_each(func: (#Item) -> Nothing) {
    repeat this.len() {
        func.invoke(this[it]!!)
    }
}

// Iterates over the list and calls the given function for each item, passing the index as well
// Can be called using the `list.for_each_entry #{ ... }` syntax:
// ```
// #[1, 2, 3].for_each_entry #{ index: Int, item: Int ->
//     println("Item at index $index is $item")
// }
fun List<#Item>.for_each_entry(func: (Int, #Item) -> Nothing) {
    repeat this.len() {
        func.invoke(it, this[it]!!)
    }
}

// Concatenates all the items in the list into a string, separated by the given separator
fun <#Item: ToString> List<#Item>.join(separator: String): String {
    let str = ""

    repeat this.len {
        str = str.concat(this[it]!!.to_string())

        if it != this.len - 1 {
            str = str.concat(separator)
        }
    }

    ret str
}

// Creates a new list with items between the given start and end indexes
// If start or end are negative, they are counted from the end of the list
fun List<#Item>.to_sublist(start: Int, end: Int): List<#Item> {
    if end < 0 {
       end = this.len + end
    }
    if end >= this.len {
        end = this.len - 1
    }

    if start < 0 {
       start = this.len + start
    }
    if start < 0 {
        start = 0
    }

    let len = end - start
    if len <= 0 {
        ret List::new<#Item>()
    }

    let sublist = List::new<#Item>(len)
    let i: Int = start

    while i < end {
        sublist[] = this[i]!!
        i = i + 1
    }

    ret sublist
}

// Creates a copy of this list
fun List<#Item>.to_list(): List<#Item> {
    let list = List::new<#Item>(this.len)

    repeat this.len {
        list[] = this[it]!!
    }

    ret list
}

// Swaps the items at the given indexes
fun <#Item: GetOrdering> List<#Item>.swap(index1: Int, index2: Int) {
    let temp = this[index1]!!
    this[index1] = this[index2]!!
    this[index2] = temp
}

// Sorts the list in place, without allocating a new list
fun <#Item: GetOrdering> List<#Item>.sort_in_place() {
    // Already sorted
    if this.len <= 1 {
        ret
    }

    this.quicksort_in_place()
}

// Returns a sorted copy of the list
fun <#Item: GetOrdering> List<#Item>.to_sorted_list(): List<#Item> {
    let sorted = this.to_list()
    sorted.sort_in_place()
    ret sorted
}

// Sorts the list using the quicksort algorithm
fun <#Item: GetOrdering> List<#Item>.quicksort_in_place() {
    this.quicksort_rec(0, this.len - 1, false)
}

// Recursive quicksort function, the partition parameter is used to toggle between finding the partition index and
// sorting the two sublists
fun <#Item: GetOrdering> List<#Item>.quicksort_rec(low: Int, high: Int, partition: Boolean): Int {
    // Find the partition index
    if partition {
        let pivot = this[high]!!
        let i = low - 1
        let j = low

        while j < high {
            if this[j]!! <= pivot {
                i = i + 1
                this.swap(i, j)
            }
            j = j + 1
        }

        this.swap(i + 1, high)

        ret i + 1
    }

    // Sort sublists
    if low >= high {
        ret 0
    }

    // Number of items to sort
    let len = high - low

    // fast path to reduce recursion depth
    if len == 2 {
        if this[low]!! > this[high]!! {
            this.swap(low, high)
        }
        ret 0
    }

    // fast path to reduce recursion depth
    if len == 3 {
        if this[low]!! > this[high]!! {
            this.swap(low, high)
        }
        if this[low]!! > this[low + 1]!! {
            this.swap(low, low + 1)
        }
        if this[low + 1]!! > this[high]!! {
            this.swap(low + 1, high)
        }
        ret 0
    }

    // fast path to reduce recursion depth
    if len == 4 {
        if this[low]!! > this[high]!! {
            this.swap(low, high)
        }
        if this[low]!! > this[low + 1]!! {
            this.swap(low, low + 1)
        }
        if this[low + 1]!! > this[high]!! {
            this.swap(low + 1, high)
        }
        if this[low + 1]!! > this[low + 2]!! {
            this.swap(low + 1, low + 2)
        }
        if this[low + 2]!! > this[high]!! {
            this.swap(low + 2, high)
        }
        ret 0
    }

    // regular recursive quicksort
    let pivot = this.quicksort_rec(low, high, true)

    this.quicksort_rec(low, pivot - 1, false)
    this.quicksort_rec(pivot + 1, high, false)
    ret 0
}