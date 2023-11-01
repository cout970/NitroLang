
// Represents a set of unique items, no duplicates are allowed.
// Internally uses a map to store the value as keys.
//
// A set can be created using the `%[...]` syntax:
// ```
// let set = %[
//     1
//     2
//     3
//     2 // The 2 will not be added to the set twice, since it already exists
// ]
// ```
@Extern $[lib: "core", name: "Set"]
struct Set<#Item> {
   map: Map<#Item, Boolean>
}

// Create a new empty set
fun <#Item: MapKey> Set::new(): Set<#Item> {
   ret Set<#Item> $[
      map: Map::new<#Item, Boolean>()
   ]
}

// Adds an item to the set
fun <#Item: MapKey> Set<#Item>.add(item: #Item) {
   map[item] = true
}

// Removes an item from the set
fun <#Item: MapKey> Set<#Item>.remove(item: #Item) {
   map.remove(item)
}

// Checks if the set contains an item
fun <#Item: MapKey> Set<#Item>.contains(item: #Item): Boolean {
   ret map.contains_key(item)
}

// Gets the length of the set, meaning the number of items in it
fun <#Item: MapKey> Set<#Item>.len(): Int {
   ret map.len()
}

// Checks if the set is empty
fun <#Item: MapKey> Set<#Item>.is_empty(): Boolean {
   ret map.is_empty()
}

// Checks if the set is not empty
fun <#Item: MapKey> Set<#Item>.is_not_empty(): Boolean {
   ret map.is_not_empty()
}

// Converts the set to a list
fun <#Item: MapKey> Set<#Item>.to_list(): List<#Item> {
    ret map.keys_to_list()
}

// Iterates over each item in the set
fun <#Item: MapKey> Set<#Item>.for_each(func: (#Item) -> Nothing) {
    map.for_each_key(func)
}

// Creates a new set from the union of this set and another set
fun <#Item: MapKey> Set<#Item>.union(other: Set<#Item>): Set<#Item> {
   let result = Set::new<#Item>()

   let list = this.to_list()
   repeat list.len() {
      result[] = list[it]!!
   }

   let list = other.to_list()
   repeat list.len() {
       result[] = list[it]!!
   }

   ret result
}

// Creates a new set from the intersection of this set and another set
fun <#Item: MapKey> Set<#Item>.intersection(other: Set<#Item>): Set<#Item> {
   let result = Set::new<#Item>()
   let list = this.to_list()

   repeat list.len() {
      let item = list[it]!!

      if other.contains(item) {
         result[] = item
      }
   }

   ret result
}

// Creates a new set from the difference of this set and another set,
// that is a set with all the items in this set that are not in the other set
fun <#Item: MapKey> Set<#Item>.difference(other: Set<#Item>): Set<#Item> {
   let result = Set::new<#Item>()
   let list = this.to_list()

   repeat list.len() {
      let item = list[it]!!

      if !other.contains(item) {
         result[] = item
      }
   }

   ret result
}

// Checks if this set is a subset of another set
fun <#Item: MapKey> Set<#Item>.is_subset(other: Set<#Item>): Boolean {
   let list = this.to_list()

   repeat list.len() {
      let item = list[it]!!

      if !other.contains(item) {
         ret false
      }
   }

   ret true
}

// Checks if this set is a superset of another set
fun <#Item: MapKey> Set<#Item>.is_superset(other: Set<#Item>): Boolean {
   let list = other.to_list()

   repeat list.len() {
      let item = list[it]!!

      if !this.contains(item) {
         ret false
      }
   }

   ret true
}

// Checks if this set is equal to another set
fun <#Item: MapKey> Set<#Item>.is_equal(other: Set<#Item>): Boolean {
   ret this.is_subset(other) && this.is_superset(other)
}

// Converts the set to a string
fun <#Item: MapKey, ToString> Set<#Item>.to_string(): String {
   let result = "%["
   let list = this.to_list()

   repeat list.len() {
      let value = list[it]!!.to_string()
      result = result.concat(value)

      if it < list.len() - 1 {
         result = result.concat(", ")
      }
   }

   ret result.concat("]")
}