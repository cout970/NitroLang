# Types

Nitro's type system provides powerful tools for creating custom types, handling errors safely, and writing generic code.
This chapter covers structs, options, enums, and the trait system.

## Structs

Structs group related data together into custom types. They're similar to classes in object-oriented languages but focus
purely on data storage.

### Defining Structs

```nitro
// Basic struct definition
struct Book {
    title: String
    author: String
    isbn: String
    pages: Int
    published_year: Int
}

// Comas are optional
struct Point3D {
    x: Float,
    y: Float,
    z: Float,
}

// Struct with optional fields
struct User {
    name: String
    email: String
    age: Optional<Int>        // Age might not be provided
    profile_picture: Optional<String>
}
```

### Creating and Using Structs

```nitro
// Create struct instances
let book = Book @[
    title: "The Rust Programming Language",
    author: "Steve Klabnik",
    isbn: "978-1-59327-828-1",
    pages: 560,
    published_year: 2018,
]

let origin = Point3D @[x: 0.0, y: 0.0, z: 0.0]

// Access fields
let book_title = book.title           // "The Rust Programming Language"
let page_count = book.pages           // 560
let coordinates = "(${origin.x}, ${origin.y}, ${origin.z})"

// Modify fields (if struct is mutable)
let user = User @[
    name: "Alice",
    email: "alice@example.com",
    age: Some(25),
    profile_picture: None(),
]
user.age = Some(26)                   // Update age
```

### Struct Methods

You can define methods for structs, the same way that any other type:

```nitro
// Method syntax
fun Book.is_long(): Boolean {
    return this.pages > 300
}

// 100% the same as:
fun is_long(book: Book): Boolean {
    return book.pages > 300
}

// You can omit the `this` keyword and access fields directly
fun Point3D.distance_from_origin(): Float {
    return sqrt(x * x + y * y + z * z)
}

// Using methods
let is_thick_book = book.is_long()    // true (560 > 300)
let distance = origin.distance_from_origin()  // 0.0
```

Methods and functions are interchangeable in Nitro, they are only a syntactic difference.
You can use the syntax that is more convenient in each case, for example, `max(a, b)` and `book.is_long()` are more
readable than `a.max(b)` and `is_long(book)`, is up to your preference.

## Options and Enums

### Option Types

Option types represent values that may have multiple variants, with each variant potentially carrying different data.

```nitro
// Custom option type
option IceCreamChoice {
    Yes { flavor: String, scoops: Int }
    No { reason: String }
    Maybe { deadline: String }
}

// Creating option instances
let choice1 = IceCreamChoice::Yes @[flavor: "chocolate", scoops: 2]
let choice2 = IceCreamChoice::No @[reason: "I'm on a diet"]
let choice3 = IceCreamChoice::Maybe @[deadline: "after dinner"]
```

### Built-in Option Types

Nitro provides two essential option types for common patterns:

#### Optional Type

Represents values that may or may not exist:

```nitro
// Long form
let has_value = Optional::Some @[value: 42]
let no_value = Optional::None @[]

// Convenient shorthand
let has_value = Some(42)
let no_value = None()

// Working with Optional values
fun find_user(id: Int): Optional<User> {
    // Search logic here...
    return if user_exists(id) {
        Some(load_user(id))
    } else {
        None()
    }
}

// Safe access
let user = find_user(123)
let user_name = user?.name ?? "Unknown User"  // Safe navigation, returns "Unknown User" if None

// Unsafe access (crashes if None)
let user_name = find_user(123)!!.name
```

#### Result Type

Represents operations that can succeed or fail:

```nitro
// Long form
let success = Result::Ok @[value: "Operation completed"]
let failure = Result::Err @[value: "Network timeout"]

// Convenient shorthand
let success = Ok("Data loaded successfully")
let failure = Err("File not found")

// Functions returning Results
fun divide(a: Float, b: Float): Result<Float, String> {
    if b == 0.0 {
        return Err("Division by zero")
    } else {
        return Ok(a / b)
    }
}

// Error handling
let result = divide(10.0, 2.0)
let value = result? // '?' will give the value if Ok, or return the function with the Err() variant, propagating the error
```

### Enums

Enums define a fixed set of named constants, optionally with associated data:

#### Simple Enums

```nitro
// Basic enum without data
enum Direction {
    North
    South
    East
    West
}

// Using enum values
let current_direction = Direction::North
let opposite = Direction::South
```

#### Enums with Data

```nitro
// Enum with associated data
enum Direction {
    let x: Int
    let y: Int
    
    North @[x:  0, y:  1]
    South @[x:  0, y: -1]
    East  @[x:  1, y:  0]
    West  @[x: -1, y:  0]
}

// Accessing enum data
let north = Direction::North
let north_x = Direction::North.x      // 0
let north_y = Direction::North.y      // 1

// Pattern matching with enums
fun move_character(direction: Direction, distance: Int): Point {
    return match direction {
        Direction::North -> Point @[x: 0, y: distance]
        Direction::South -> Point @[x: 0, y: -distance]
        Direction::East -> Point @[x: distance, y: 0]
        Direction::West -> Point @[x: -distance, y: 0]
    }
}
```

#### Enum Utilities

Enums automatically provide useful methods:

```nitro
// Get all enum values
let all_directions: List<Direction> = Direction::values()

// Convert from string name
let north_from_name = Direction::from_name("North")  // Optional<Direction>

// Convert from index
let north_from_index = Direction::from_variant(0)   // Optional<Direction>

// Get variant index
let north_index = Direction::North.variant          // 0

// Iterate over all values
for direction in Direction::values() {
    // to_string() is automatically generated
    println("Direction: ${direction}, Vector: (${direction.x}, ${direction.y})")
}
```

## Tags

Tags (similar to traits in other languages) define a set of methods that types must implement. They enable
polymorphism and code reuse through shared behavior.

The main difference in Nitro is that tags are automatically assigned to types that implement the required methods,
instead of explicitly declaring that a type implements a tag.

### Defining Tags

```nitro
// Define a tag for types that can be printed
// `This` refers to the type that implements the tag
tag Printable {
    fun This.print()           // Method that implementers must provide
    fun This.print_debug()     // Another required method
}

// Define a tag for types that can be compared
tag Comparable {
    fun This.get_ordering(other: This): Ordering
}

// Define a tag with default implementations
tag Drawable {
    fun This.draw()                    // Required method
}

// This acts as a default implementation, since any Drawable type will have this function available, and any override will have higher priority
fun <#T: Drawable> #T.draw_with_color(color: String) {
    println("Drawing with color: $color")
    this.draw()
}
```

### Implementing Tags

Types automatically implement tags when they have the required methods:

```nitro
// Implement Printable for Int
fun Int.print() {
    println("Int value: $this")
}

fun Int.print_debug() {
    println("Debug - Int: $this (size: ${size_of<Int>()})")
}

// Implement Printable for String
fun String.print() {
    println("String: '$this'")
}

fun String.print_debug() {
    println("Debug - String: '$this' (length: ${this.len})")
}

// Now Int and String automatically implement Printable
```

### Using Tags

#### Generic Functions with Tag Constraints

```nitro
// Function that works with any Printable type
fun <#T: Printable> print_and_debug(value: #T) {
    value.print()
    value.print_debug()
}

// Function with multiple tag constraints
fun <#T: Printable & Comparable> process_item(item: #T) {
    // Can also use comparison methods
    if item > 10 {
        item.print()
    }
}

// Using the generic functions
fun main() {
    print_and_debug(42)              // Works with Int
    print_and_debug("Hello")         // Works with String
    
    let numbers = [3, 1, 4, 1, 5]
    for num in numbers {
        process_item(num)
    }
}
```

#### Built-in Tags

Nitro provides several built-in tags:

- GetOrdering
- ToString
- ToBoolean

You can add tags to existing types and apply tags to new types you define, there is no limit. This allows
easier interoperability with libraries, using library functions with your types and using your functions with library types.

#### GetOrdering Example

```nitro
// Provides comparison between values of the same type
tag GetOrdering {
    fun This.get_ordering(other: This): Ordering
}

struct MyType { ... }

// Implementing GetOrdering for MyType
fun MyType.get_ordering(other: MyType): Ordering { ... }

fun main() {
    let a = MyType @[ ... ]
    let b = MyType @[ ... ]
    
    when {
        a < b -> println("a is less than b")
        a > b -> println("a is greater than b")
        else -> println("a is equal to b")
    }
}
```
