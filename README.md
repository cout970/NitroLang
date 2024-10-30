# NitroLang

NitroLang is a modern procedural programming language, designed for simplicity with a focus on readability,
maintainability and flexibility.
Inspired by C, Rust, Kotlin and JS, combining the best features of modern languages while avoiding the pitfalls.

Right now it's in alpha state and everything is subject to change.

Checkout the Language book: [NitroLang Book](./web/docs/src/SUMMARY.md)

### How to run the compiler

Any WASI compatible runtime should work.
In [./scripts](./scripts) you have a few utilities:

- `./run_with_wasmer.sh examples/hello_world.nitro` - Compile and run examples/hello_world.nitro using
  the [Wasmer](https://wasmer.io/) runtime
- `./run_with_deno.ts examples/hello_world.nitro` - Compile and run examples/hello_world.nitro
  using [Deno](https://deno.com/)

### Syntax overview

Basics:

```nitrolang
// The program starts at main
fun main() {
    // Semicolons are optional
    println("Hello world!")
}
```

Variables:

```nitrolang
// Global variables are constants by default
let my_var: Int = 12345

fun main() {
    // Local variables are mutable by default
    let my_var = 12345
    let another_var: Float = 3.14f
    let not_true: Boolean = false
    
    println(my_var)
    println(another_var)
    println(not_true)
}
```

Statements:

```nitrolang
fun main() {
    // If statement
    if 1 == 1 {
        println("1 is equal to 1, duh")
    } else {
        println("I broke math again")
    }

    // Do 10 times
    repeat 10 {
        println("Iteration $it")
    }

    // Clasic while loop
    let i = 0
    while i < 10 {
        println("Iteration $i")
        i = i + 1
    }
    
    // Infinite loop, with break/continue
    let j = 0
    loop {
        println("Iteration $j")
        if j >= 10 {
            break
        }
        j = j + 1
    }
    
    // For each item...
    let list = [1, 2, 3]
    for item in list {
        println("Current item: $item")
    }
    
    // Execute a lambda function on every item
    list.for_each @{ item: Int ->
        println("Current item: $item")
    }
    
    // Inline documentation
    // Usage:
    ```js
    console.log(main.find_all_users())
    ```
}
```

Default data structures:

```nitrolang
fun main() {
    // List of integers
    let my_list: List<Int> = [1, 2, 3, 4]
    
    // New lines act as commas
    let my_other_list = [
        1
        2
        3
    ]
    
    // Set of integers
    let my_set: Set<Int> = %[1, 2, 3, 4]
    
    // Map of strings to integers
    // String keys that are valid identifiers can be used without quotes
    let my_map: Map<String, Int> = #[
        first: 1
        second: 2
        third: 2 + 1
        "the last one": 4
    ]
    
    // Inline JSON support
    let my_beloved_json: Json = json! {
        "name": "John Doe",
        "age": 42,
        "is_admin": true,
        "friends": [
            "Alice",
            "Bob",
            "Charlie"
        ]
    }
}
```

Functions:

```nitrolang
// Function with 1 parameter and return type
fun convert_int_to_string(i: Int): String {
    ret i.to_string()
}

// Function with expression body
fun add_numbers(i: Int, j: Int): Int = i + j

// Function that returns nothing
fun print_number(i: Int) {
    println(i)
}

// Can also be written as
fun print_number(i: Int): Nothing {
    println(i)
}

// Function with receiver
fun Int.to_string(): String {
    ret "The number is $this"
}

// Is exactly the same as
fun to_string(i: Int): String {
    ret "The number is $i"
}

fun main() {
    // Fucntion call as method
    println(42.to_string())
    // You can call it like this too
    println(to_string(42))
}
```

Lambdas:

```nitrolang
fun main() {

    // Lambda with 1 parameter
    let my_lambda = @{ i: Int ->
        println("- $i")
    }
    
    // Lambda with 2 parameters
    let my_other_lambda = @{ i: Int, j: Int ->
        println("- $i, $j")
    }
    
    // Call the lambda functions
    my_lambda(42)
    my_other_lambda(42, 43)
    
    // Pass the lambda as the last parameter to a function
    for_each([1, 2, 3, 4]) @{ i: Int ->
        println("- $i")
    }
    
    // Type inference works for lambdas too
    for_each([1, 2, 3, 4]) @{ i ->
        println("- $i")
    }
}
```

Types:

```nitrolang
// Structs
struct User {
    name: String
    age: Int
    is_admin: Boolean
    friends: List<String>
}

// Structs have type parameters
struct Container<#Item> {
    items: List<#Item>
}

// Options, also known as Sum types or Enums on other languages, are a way to represent a value that 
// can be one of many types
option Available {
    Yes { at: String }
    No { reason: String }
    Unknown
}

// Enums, they are a list of possible values, unlike options, each item has only one instance
enum Role {
    let can_edit_posts: Boolean
    let can_delete_posts: Boolean
    
    Admin     @[can_edit_posts: true,  can_delete_posts: true]
    Moderator @[can_edit_posts: true,  can_delete_posts: false]
    Regular   @[can_edit_posts: false, can_delete_posts: false]
}

// Type alias
type_alias UserList = Container<User>

// Usage
fun main() {
    // Create struct instance
    let user = User @[
        name: "John Doe"
        age: 42
        is_admin: true
        friends: ["Alice", "Bob", "Charlie"]
    ]
    
    // Create struct instance with type parameters
    let container = Container<User> @[
        items: [user]
    ]
    
    // Same, but using the type alias
    let user_list = UserList @[
        items: [user]
    ]
    
    // Create option instances
    let is_available = Available::Yes @[at: "4:30 PM"]
    let is_not_available = Available::No @[reason: "I'm busy"]
    let maybe_available = Available::Unknown @[]
   
    // Enums already have instances created 
    let admin = Role::Admin
    let moderator = Role::Moderator
    let regular = Role::Regular
    let all_roles: List<Role> = Role::values()
}
```

Tags:

```nitrolang
// Tags are automatically added to types that have the required methods
tag ToBoolean {
    fun This.to_boolean(): Boolean
}

// Int has the required method, so it gets tagged ToBoolean
fun Int.to_boolean(): Boolean = this != 0

// Function can require a type with a tag
fun <#Any: ToBoolean> convert_to_boolean(value: #Any): Boolean {
    // Use the concrete implementation of value.to_boolean() for the given type 
    ret value.to_boolean()
}

fun main() {
    let my_bool: Boolean = convert_to_boolean(42)
}
```

Modules:

```nitrolang
mod first {
    fun foo(i: Int) {}
}

mod second {
    fun foo(i: Int) {}
}

fun main() {
    first::foo(42)
    second::foo(42)
    
    // Also valid, use in case of name conflicts
    42.first::foo()
    42.second::foo()
}
```

DSL:

```nitrolang
fun main() {
    // Example of what is posible
    html @{
        attr = @[
            lang: "en"
            classes: ["dark-mode"]
        ]
        
        head @{
            title("My page")
        }
        
        body @{
            h1("Hello world!")
            p("This is my page")
            
            a("https://example.com") @{
                classes = ["link", "link--primary"]
                target = "_blank"
                text("Visit example.com")
            }
            
            script @{
                attr = @[
                    "type": "application/json"
                ]
                
                text_content = json! {
                    settings: {
                        theme: "dark"
                        language: "en"
                        prefers_reduced_motion: true
                    }
                    session: {
                        user_id: 12345
                        role: 'admin'
                    }
                    permissions: ["read", "write", "delete"]
                }
            }
        }
    }
}
```

### Defining characteristics

- It's designed for simplicity and flexibility, making code easy to read and maintain.
- The syntax is concise and intuitive, familiar for anyone with programming experience.
- It features strong and static typing for early error detection, clearer code, and enhanced tooling and autocompletion.
- Primarily geared towards procedural programming, it also supports functional programming with first-class functions
  and lambdas
- Comes equipped with essential data structures like Lists, Maps, Sets, JSON, Optionals, Results, Pairs, etc.
- Implements a Hindley-Milner type system, with generic structs, enums, sum types/options, type aliases and modules.
- Offers function overloading and extension functions, allowing object-oriented syntax in procedural code.
- Uses tags for type extensions, enabling the addition of methods to any type without disrupting existing code.
- Features a module system for effective code organization and namespacing.
- Does not include the unnecessary complexity that comes from nulls, exceptions, inheritance, implicit type conversions or macros.
- Makes Domain-Specific Languages (DSLs) easy to build using convenient syntax for lists, maps, json, lambdas with custom receiver, etc.
- Compiles to WebAssembly, ensuring compatibility across most platforms.
- Provides a comprehensive standard library with common data structures and algorithms.
- Has a minimal runtime, suitable for embedding in other applications.
- The compiler is user-friendly, offering insightful error messages, suggestions, and code snippets. (not a greats as it use to be, since the migration to the new compiler, but it's getting there)

### Current task

- [x] [Rewrite the compiler in NitroLang itself](./src/main/nitro/compiler)
- [ ] Improve memory management
- [ ] Create a web editor that compiles and runs the code directly in the browser
