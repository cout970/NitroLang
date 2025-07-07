# Functions

Functions are the primary way to organize and reuse code in Nitro. They encapsulate behavior and can accept parameters,
perform computations, and return values.

## Definition and Calling

### Basic Function Syntax

Functions are defined using the `fun` keyword:

```nitro
// Function with parameters and return type
fun add(a: Int, b: Int): Int {
    return a + b
}

// Function without return value
fun greet(name: String) {
    println("Hello, $name!")
}

// Explicit Nothing return type (optional)
fun print_hello(): Nothing {
    println("Hello, world!")
}
```

### Expression Functions

For simple functions, you can use the special expression syntax:

```nitro
// Single expression function
fun square(x: Int): Int = x * x

// Important: if the type is not specified, it will be `Nothing`, causing unexpected behavior
fun double(x: Int) = x * 2  // Compile error
```

> Note: This behavior is a limitation of the type inference. The return type must be known before inspecting the
> function body.

### Calling Functions

Call functions by name with arguments in parentheses:

```nitro
// Basic function calls
let result = add(5, 3)           // result = 8
greet("Alice")                   // prints "Hello, Alice!"
let squared = square(4)          // squared = 16
```

### Module Functions

Functions defined in modules are called using the module path:

```nitro
// Calling functions from specific modules
let builder = StringBuilder::new()              // Create new StringBuilder
let args = runtime::get_program_args()          // Get command line arguments
let file = runtime::fs::read_text("config.txt")  // Read file from filesystem module
```

The same applies to constants and types defined in modules.

## Generics

Generic functions work with multiple types using type parameters. Type parameters are prefixed with `#`:

### Explicit Generic Declaration

```nitro
// Explicit type parameter declaration
fun get_value_in_box<#T>(box: Box<#T>): #T {
    return box.value
}

// Generic function with multiple type parameters
fun convert<#From, #To>(value: #From, converter: (#From) -> #To): #To {
    return converter(value)
}
```

### Inferred Generic Parameters

Type parameters can be inferred when used in parameters or return types:

```nitro
// Type parameter #T is inferred from usage
fun get_value_in_box(box: Box<#T>): #T {
    return box.value
}

// Multiple inferred type parameters
fun create_pair(first: #A, second: #B): Pair<#A, #B> {
    return Pair::of(first, second)
}
```

### Using Generic Functions

```nitro
let int_box = Box::new(42)
let string_box = Box::new("hello")

// Type inference at call site
let int_value = get_value_in_box(int_box)        // Returns Int
let string_value = get_value_in_box(string_box)  // Returns String

// Explicit type specification (when needed)
let pair = create_pair<String, Int>("age", 25)
```

## Parameters

### Function Parameters

Functions can accept multiple parameters with different types:

```nitro
// Multiple parameters
fun calculate_area(width: Float, height: Float): Float {
    return width * height
}

// Parameters with different types
fun format_message(template: String, count: Int, is_urgent: Boolean): String {
    let urgency = if is_urgent { "URGENT: " } else { "" }
    return "$urgency$template (count: $count)"
}
```

### Return Values

Functions can return values using the `return` keyword, or implicitly return the last expression:

```nitro
// Explicit return
fun add_explicit(a: Int, b: Int): Int {
    return a + b
}

// Implicit return (last expression)
fun add_implicit(a: Int, b: Int): Int {
    a + b  // This value is returned
}

// Early return
fun safe_divide(a: Float, b: Float): Optional<Float> {
    if b == 0.0 {
        return None()  // Early return
    }
    Some(a / b)
}
```

## Magic Functions

The language will search the project for functions with specific names to handle certain operations:

### Operator Overloading

You can define functions to overload operators like `+`, `-`, `*`, etc.

| Operator | Function Name                  | Parameters              | Return Type         |
|----------|--------------------------------|-------------------------|---------------------|
| `+`      | `plus`                         | `AnyType, AnyType`      | `AnyType`           |
| `-`      | `minus`                        | `AnyType, AnyType`      | `AnyType`           |
| `*`      | `mul`                          | `AnyType, AnyType`      | `AnyType`           |
| `/`      | `div`                          | `AnyType, AnyType`      | `AnyType`           |
| `%`      | `rem`                          | `AnyType, AnyType`      | `AnyType`           |
| `<=>`    | `get_ordering`                 | `AnyType, AnyType`      | `Ordering`          |
| `==`     | `is_equals`                    | `AnyType, AnyType`      | `Boolean`           |
| `^^`     | `logical_xor`                  | `AnyType, AnyType`      | `Boolean`           |
| `&&`     | `logical_and`                  | `AnyType, AnyType`      | `Boolean`           |
| `\|\|`   | `logical_or`                   | `AnyType, AnyType`      | `Boolean`           |
| `!`      | `logical_not`                  | `AnyType`               | `Boolean`           |
| `[]`     | `get`                          | `AnyType, Int`          | `AnyType`           |
| `[]!!`   | `unsafe_get`                   | `AnyType, Int`          | `AnyType`           |
| `a[i] =` | `set`                          | `AnyType, Int, AnyType` | `Nothing`           |
| `a[] =`  | `add`                          | `AnyType, AnyType`      | `Nothing`           |
| `..<`    | `range_up_to`                  | `AnyType, AnyType`      | `AnyType`           |
| `..=`    | `range_to`                     | `AnyType, AnyType`      | `AnyType`           |
| `<<`     | `bitwise_shift_left`           | `AnyType, Int`          | `AnyType`           |
| `>>`     | `bitwise_shift_right`          | `AnyType, Int`          | `AnyType`           |
| `>>>`    | `bitwise_shift_right_unsigned` | `AnyType, Int`          | `AnyType`           |
| `&`      | `bitwise_and`                  | `AnyType, AnyType`      | `AnyType`           |
| `^`      | `bitwise_xor`                  | `AnyType, AnyType`      | `AnyType`           |
| `\| `    | `bitwise_or`                   | `AnyType, AnyType`      | `AnyType`           |
| `?`      | `is_returnable_error`          | `AnyType`               | `Boolean`           |
| `!!`     | `get_or_crash`                 | `AnyType`               | `AnyType`           |
| for loop | `to_iterator`                  | `AnyType`               | `AnyType`           |
| for loop | `next_item`                    | `AnyType`               | `Optional<AnyType>` |

#### Example of Operator Overloading

```nitro
// Overloading the + operator for a custom type
fun Vec2.add(other: Vec2): Vec2 = Vec2::new(this.x + other.x, this.y + other.y)
```

The only requirement is that the function must be named according to the operator it overloads, and it should accept the
appropriate parameters.

### Getters and Setters

You can call any getter or setter function using the property syntax:

```nitro
struct Person { ... }

fun Person.get_full_name(): String = "$first_name $last_name"

fun Person.set_full_name(name: String) {
    let parts = name.split(" ")
    first_name = parts[0]
    last_name = parts[1]
}

person.full_name  // Calls get_full_name()
person.full_name = "John Doe"  // Calls set_full_name("John Doe")
```

## Future Features

**Default Arguments:** Currently not supported but planned for a future version.

```nitro
// This will be supported in the future:
// fun greet(name: String, greeting: String = "Hello") { ... }

greet("Alice")  // Prints "Hello, Alice!"
```
