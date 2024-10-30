# Collections

## Lists

Lists are ordered collections of items. They are created by enclosing the items in square brackets `[]`.

```nitro
let my_numbers = [1, 2, 3, 4, 5]
```

Lists can be indexed using square brackets `[]`. Indexing starts at 0.

```nitro
let first_number = my_numbers[0]!!
let last_number = my_numbers[my_numbers.len - 1]!!
```

Notice the `!!` operator. List indexing returns an optional value, because the index might be out of bounds.

```nitro
let maybe_or_maybe_not: Optional<Int> = my_numbers[10]
```

The variable `maybe_or_maybe_not` will be `Optional::None` if the index is out of bounds.

There are also methods to get the first and last elements of a list.

```nitro
let first_number = my_numbers.first()!!
let last_number = my_numbers.last()!!
```

Alternatively, you can use the `get` function.

```nitro
let maybe_or_maybe_not: Optional<Int> = my_numbers.get(10)
```

You can add a value to the end of a list using the `[]=` operator.

```nitro
my_numbers[] = 6
```

This is equivalent to the `add` method.

```nitro
my_numbers.add(6)
```

You can check if a value is inside a list using the `in` operator.

```nitro
// Will print "true"
println(3 in my_numbers)
```

## Maps

Maps are collections of key-value pairs. They are created by enclosing the pairs in `#[]`

```nitro
let my_map = #[name: "John", surname: "Smith"]
```

Maps are homogeneous, meaning that all keys and values must have the same type.

```nitro
// Will emit a compile-time error
let my_map: Map<String, String> = #[name: "John", age: 42]
```

Maps can be indexed using square brackets `[]`.

```nitro
let name = my_map["name"]!!
```

Notice the `!!` operator. Map indexing also returns an optional value, because the key might not exist.

You can set a value in a map using `[]`

```nitro
my_map["name"] = "Alice"
```

## Sets

Sets are collections of unique items. They are created by enclosing the items in curly brackets `%[]`

```nitro
let my_set = %["apple", "banana", "cherry"]
```

If you try to add a duplicate item to a set, it will be ignored.

```nitro
my_set[] = "apple"
// Will print %["apple", "banana", "cherry"]
println(my_set)
```

You can check if a value is inside a set using the `in` operator.

```nitro
// Will print "true"
println("apple" in my_set)
```

## Pair

A pair is a tuple with two elements.

```nitro
let friends = Pair::of("Alice", "Bob")
```

You can access the elements of a pair using the `first` and `second` methods.

```nitro
let first_friend = friends.first
let second_friend = friends.second
```

Pairs are useful when you need to return two values from a function.

```nitro
fun get_name_and_age(): Pair<String, Int> {
    ret Pair::of("Alice", 42)
}
```

## ArrayDeque

An `ArrayDeque` is a double-ended queue. Acts as list, stack and queue at the same time.

```nitro
let deque = ArrayDeque::new()

deque.add_first(1) // => [1]
deque.add_first(2) // => [2, 1] 
deque.add_last(3) // => [2, 1, 3]
deque.add_last(4) // => [2, 1, 3, 4]

let first = deque.remove_first()!!
// Will print "2"
println(first)

let last = deque.remove_last()!!
// Will print "4"
println(last)

// Will print "[1, 3]"
println(deque)
```

## Json

The `Json` type is a representation of a JSON object. You can use the `json!` keyword to create a value using the JSON
syntax.

```nitro
let my_json_value = json! {"name": "Alice", "age": 42}
let my_json_array = json! ["apple", 42.5, false, null]
```

It supports more syntax than the standard JSON notation, for example, you can omit the `"` around keys and evaluate
expressions in parentheses.

```nitro
let my_json_map = json! { name: "Alice", age: (34 + 43) }
```
