# Collections

Nitro provides several built-in collection types for storing and organizing data. Each collection type is optimized for
different use cases and access patterns.

## Lists

Lists are ordered, mutable collections that can grow and shrink dynamically.

### Creating Lists

```nitro
// Empty list
let empty_list = List::new<Int>()

// List with initial values
let numbers = [1, 2, 3, 4, 5]
let fruits = ["apple", "banana", "cherry"]

// Mixed types not allowed
let mixed = [1, "hello"] // Compile error: Mismatched types Int and String
```

### Accessing Elements

```nitro
let numbers = [10, 20, 30, 40, 50]

// Safe indexing (returns Optional)
let first: Optional<Int> = numbers[0]      // Some(10)
let invalid: Optional<Int> = numbers[10]   // None

// Unsafe indexing (crashes if out of bounds)
let first_value = numbers[0]!!             // 10
let last_value = numbers[numbers.len - 1]!! // 50

// Convenience methods
let first_safe = numbers.first()           // Optional<Int>
let last_safe = numbers.last()             // Optional<Int>
```

### Modifying Lists

```nitro
let my_list = [1, 2, 3]

// Add elements
my_list[] = 4              // Append: [1, 2, 3, 4]
my_list.add(5)             // Same as above: [1, 2, 3, 4, 5]
my_list.insert(2, 0)       // Insert at index: [1, 2, 0, 3, 4, 5]

// Modify elements
my_list[1] = 10            // [0, 10, 2, 3, 4, 5]

// Remove elements
let removed = my_list.remove_at(1)!!  // Removes and returns 10
let last = my_list.remove_last()!!    // Removes and returns last element (5)
let first = my_list.remove_first()!!  // Removes and returns first element (0)
my_list.clear()                       // Empty list: []
```

### List Operations

```nitro
let numbers = [1, 2, 3, 4, 5]

// Check membership
let contains_three = 3 in numbers      // true
let not_contains_ten = 10 !in numbers  // true

// Size and properties
let size = numbers.len                 // 5
let is_empty = numbers.is_empty()      // false
let opposite = numbers.is_not_empty()  // true

let first_index = numbers.index_of(3)        // Some(2) (returns index of first occurrence)
let last_index = numbers.last_index_of(4)    // Some(3) (returns index of last occurrence)

// Iteration
for number in numbers {
    println(number)
}
```

## Maps

Maps store key-value pairs, providing fast lookups by key. All keys must be the same type, and all values must be the
same type.

### Creating Maps

```nitro
// Empty map
let empty_map = Map::new<String, Int>()

// Map with initial values
let person = #[name: "John", surname: "Smith"]
let scores = #["alice": 95, "bob": 87, "charlie": 92]

// Type annotations when needed
let config: Map<String, Int> = #["timeout": 30, "retries": 3]
```

### Accessing Values

```nitro
let person = #[name: "John", age: "30", city: "New York"]

// Safe access (returns Optional)
let name: Optional<String> = person["name"]       // Some("John")
let country: Optional<String> = person["country"] // None

// Unsafe access (crashes if key doesn't exist)
let name_direct = person["name"]!!              // "John"

// Alternative access methods
let has_age = "age" in person                   // true
```

### Modifying Maps

```nitro
let person = #[name: "John", age: "30"]

// Add or update values
person["city"] = "Boston"               // Add new key-value pair
person["age"] = "31"                    // Update existing value
person[] = Pair::of("country", "USA")   // Alternative way

// Remove values
let removed_age = person.remove("age")  // Returns Optional<String>
person.clear()                          // Remove all entries
```

### Map Operations

```nitro
let scores = #["alice": 95, "bob": 87, "charlie": 92]

// Size and properties
let size = scores.len                    // 3
let is_empty = scores.is_empty()         // false
let is_not_empty = scores.is_not_empty() // true

// Get all keys or values
let players = scores.keys_to_list()              // List of keys
let all_scores = scores.values_to_list()         // List of values
let entries = scores.to_list()                   // List of key-value pairs

// Check membership
let has_alice = "alice" in scores        // true
let no_david = "david" !in scores        // true
```

## Sets

Sets store unique elements with no duplicates. They're ideal for membership testing and eliminating duplicates.

### Creating Sets

```nitro
// Empty set
let empty_set = Set::new<Int>()

// Set with initial values
let fruits = %["apple", "banana", "cherry"]
let numbers = %[1, 2, 3, 4, 5]

// Type annotation when needed
let unique_ids: Set<Int> = %[101, 102, 103]
```

### Set Operations

```nitro
let fruits = %["apple", "banana"]

// Add elements (duplicates ignored)
fruits[] = "cherry"     // Add "cherry"
fruits[] = "apple"      // Ignored (already exists)
fruits.add("date")      // Desugared form

// Check size
let count = fruits.len                     // Number of unique elements

// Check membership (very fast)
let has_apple = "apple" in fruits          // true
let no_grape = "grape" !in fruits          // true

// Remove elements
let removed = fruits.remove("banana")      // Returns bool
fruits.clear()                             // Remove all elements

// Convert to list
let fruit_list = fruits.to_list()          // List of unique elements
```

### Set Mathematics

```nitro
let set_a = %[1, 2, 3, 4]
let set_b = %[3, 4, 5, 6]

// Union (all elements from both sets)
let union = set_a.union(set_b)             // %[1, 2, 3, 4, 5, 6]

// Intersection (common elements)
let intersection = set_a.intersection(set_b) // %[3, 4]

// Difference (elements in A but not in B)
let difference = set_a.difference(set_b)   // %[1, 2]
```

## Other Collections

### Pair

Pairs store exactly two values, useful for returning multiple values or creating simple associations:

```nitro
// Creating pairs
let coordinates = Pair::of(10, 20)
let name_age = Pair::of("Alice", 25)

// Accessing elements
let x = coordinates.first    // 10
let y = coordinates.second   // 20

// Functions returning pairs
fun get_both_ends(numbers: List<Int>): Pair<Int, Int> {
    let first = numbers.first()!!
    let last = numbers.last()!!
    return Pair::of(first, last)
}
```

### ArrayDeque

ArrayDeque is a resizable array implementation of a double-ended queue (deque) that provides efficient insertion and
removal operations at both the front and back of the collection. This versatility allows ArrayDeque to function as both
a Queue (FIFO - first in, first out) and a Stack (LIFO - last in, first out) data structure.

```nitro
let deque = ArrayDeque::new()

// Add to both ends
deque.add_first(2)     // [2]
deque.add_first(1)     // [1, 2]
deque.add_last(3)      // [1, 2, 3]
deque.add_last(4)      // [1, 2, 3, 4]

// Remove from both ends
let first = deque.remove_first()!!   // 1, deque = [2, 3, 4]
let last = deque.remove_last()!!     // 4, deque = [2, 3]
```

### JSON Collections

Nitro provides built-in JSON support for working with dynamic data:

```nitro
// JSON objects and arrays
let user_data: Json = json! {"name": "Alice", "age": 42, "active": true}
let shopping_list: Json = json! ["apple", "banana", "cherry"]

// Enhanced JSON syntax
let computed_data = json! {
    name: "Bob",             // Quotes optional for keys
    age: (30 + 5),           // Expressions in parentheses
    birth_year: (2024 - 35),
    hobbies: ["reading", "coding"],
}

// Accessing JSON values
let name = user_data["name"]?.as_string()!!
let age = user_data["age"]?.as_int()!!
```

### Collection Conversion

```nitro
// Converting between collection types
let list = [1, 2, 3, 2, 1]
let unique_set = list.to_set()        // %[1, 2, 3]
let back_to_list = unique_set.to_list() // [1, 2, 3] (order may vary)

// List to map (with transformation)
let names = ["alice", "bob", "charlie"]
let name_lengths = names.map @{ name -> Pair::of(name, name.len) }.to_map()
// Result: #["alice": 5, "bob": 3, "charlie": 7]
```

## Other Collection Types

Check the standard library at `src/main/nitro/core/collections` where all the collection types are defined.
