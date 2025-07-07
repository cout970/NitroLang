# Expressions and Operators

Expressions are the building blocks of Nitro programs - pieces of code that evaluate to a value. This chapter covers
literals, operators, and how to combine them to create meaningful computations.

## Literals

Literals are values written directly in your code. Nitro supports several types of literals:

### Numbers

```nitro
// Integers
let positive = 42
let negative = -23
let large = 1_000_000    // underscores for readability

// Floating point
let pi = 3.1416
let explicit_float = 5.0f
let scientific = 1e10    // 10,000,000,000
```

### Strings and Characters

```nitro
// Strings
let greeting = "Hello, world!"
let multiline = "
    This is a
    multi-line string
"
let raw_string = r#"This is a raw string with no escape sequences: \n"#

// Characters (Unicode)
let letter: Char = u"A"
let unicode_hex: Char = u"\x41"      // 'A' in hex
let unicode_full: Char = u"\u0041"   // 'A' in Unicode

// Characters ascii
let ascii_char: Int = a"A" // ASCII character 'A' (65 in decimal)
let ascii_hex: Int = a"\x41" // ASCII character 'A' in hex
```

### Other Literals

```nitro
// Boolean values
let is_ready = true
let is_finished = false

// Nothing (absence of value)
let empty = nothing
```

## Function Calls

Functions are called using their name followed by arguments in parentheses:

```nitro
// Built-in functions
println("Hello, world!")         // Print with newline
print("No newline")              // Print without newline

// Functions with multiple arguments
let result = add(5, 3)           // Custom function
let max_val = max(10, 20, 15)    // Variable arguments
```

> **Note:** See the [Functions](./3_functions.md) chapter for details on defining your own functions.

## Operators

Nitro provides a comprehensive set of operators for different types of operations:

### Arithmetic Operators

```nitro
let a = 10
let b = 3

let sum = a + b        // Addition: 13
let diff = a - b       // Subtraction: 7
let product = a * b    // Multiplication: 30
let quotient = a / b   // Division: 3
let remainder = a % b  // Reminder: 1
```

> **Note:** '%' is the remainder operator, not modulo. For modulo behavior you can use `a.modulo(b)` that will always
> return a non-negative result.

### Comparison Operators

```nitro
let x = 5
let y = 10

let equal = x == y          // Equality: false
let not_equal = x != y      // Inequality: true
let less = x < y            // Less than: true
let greater = x > y         // Greater than: false
let less_equal = x <= y     // Less or equal: true
let greater_equal = x >= y  // Greater or equal: false
let ordering = x <=> y      // Value comparison: Ordering::Less, Ordering::Equals or Ordering::Greater
```

### Logical Operators

```nitro
let a = true
let b = false

let and_result = a && b     // Logical AND: false
let or_result = a || b      // Logical OR: true
let xor_result = a ^^ b     // Logical XOR: true
let not_result = !a         // Logical NOT: false

// Also valid
let and_result = a and b    // Logical AND: false
let or_result = a or b      // Logical OR: true
let xor_result = a xor b    // Logical XOR: true
let not_result = not a      // Logical NOT: false
```

### Bitwise Operators

```nitro
let x = 12  // Binary: 1100
let y = 10  // Binary: 1010

let and_bits = x & y        // Bitwise AND: 8 (1000) also x.bitwise_and(y)
let or_bits = x | y         // Bitwise OR: 14 (1110) also x.bitwise_or(y)
let xor_bits = x ^ y        // Bitwise XOR: 6 (0110) also x.bitwise_xor(y)
let left_shift = x << 2     // Left shift: 48
let right_shift = x >> 2    // Right shift: 3
let unsigned = x >>> 2      // Unsigned right shift: 3 (same as >> for positive numbers)

// There is no operator for bitwise NOT, but you can use the method:
let not_bits = x.bitwise_not()           // Bitwise NOT: -13 (0b11111111111111111111111111110011)
```

### Assignment Operators

```nitro
let value = 10

value += 5    // value = value + 5 -> 15
value -= 3    // value = value - 3 -> 12
value *= 2    // value = value * 2 -> 24
value /= 4    // value = value / 4 -> 6
value %= 3    // value = value % 3 -> 0
```

### Special Operators

```nitro
// Type checking and casting
let value = 42
let is_int = value is Int           // Type checking: true
let not_string = value !is String   // Negative type check: true
let as_int = value as Int           // Type casting, throws error if not possible

// Membership testing
let numbers = [1, 2, 3, 4, 5]
let contains = 3 in numbers         // Contains: true
let not_contains = 6 !in numbers    // Not contains: true

// Null coalescing (default values)
let optional_value: Optional<String> = None()
let result = optional_value ?? "default"  // Uses default if None

// Safe navigation operator (avoids null dereference)
let maybe_person: Optional<Person> = None()
let name = maybe_person?.name ?? "Unknown"  // Safe access, returns "Unknown" if person is None

// Early return operator, return if None(), unwrap if Some()
let definitly_a_person = maybe_person?
```

## Precedence

Operators have a precedence that determines the order in which they are evaluated.

Operators sorted by precedence, from highest to lowest:

| Prec. | Operator                               |
|-------|----------------------------------------|
| 0     | `()`                                   |
| 1     | `[] . ?. !! ? ??`                      |
| 2     | `! - +` (unary) and `as is !is in !in` |
| 3     | `* / %`                                |
| 4     | `+ -`                                  |
| 5     | `..= ..<`                              |
| 6     | `<< >> >>>`                            |
| 7     | `&`                                    |
| 8     | `^`                                    |
| 9     | `\|`                                   |
| 10    | `< > <= >=`                            |
| 11    | `<=>`                                  |
| 12    | `== !=`                                |
| 13    | `^^`                                   |
| 14    | `&&`                                   |
| 15    | `\|\|`                                 |
| 16    | `= += -= *= /= %=`                     |

- **Precedence 0** (highest): Will evaluate first
- **Precedence 16** (lowest): Will be the last to evaluate
- Operators in the same precedence level are evaluated left-to-right (left-associative)
- Some operators cannot chain, such as `+=` and `-=`, which require a single left-hand operand

> **Note:** This precedence is subject to change in the future

## String Interpolation

Nitro supports powerful string interpolation for embedding values and expressions directly in strings:

### Basic Interpolation

```nitro
let name = "Alice"
let age = 42

// Simple variable interpolation
let greeting = "Hello, $name!"

// Expression interpolation
let message = "You are $age years old"
let summary = "In 10 years, you'll be ${age + 10}"
```

### Complex Expressions

Use `${}` for complex expressions:

```nitro
let numbers = [1, 2, 3, 4, 5]
let info = "The list has ${numbers.len} elements"

// Method calls
let text = "HELLO"
let formatted = "Lowercase: ${text.to_lowercase()}"

// Nested interpolation
let nested = "Result: ${"Value is ${42 * 2}"}"
```

### Escape Sequences

```nitro
let escaped = "Line one\nLine two\tTabbed"
let quote = "She said \"Hello!\""
let backslash = "Path: C:\\folder\\file.txt"
let dolar = "Price: \$100"
```

## Ranges

Ranges represent a sequence of contiguous values efficiently by storing only the start and end values:

### Creating Ranges

```nitro
// Inclusive range (includes both endpoints)
let inclusive = 1..=10      // 1, 2, 3, ..., 9, 10

// Exclusive range (excludes the end)
let exclusive = 1..<10      // 1, 2, 3, ..., 8, 9
```

> **Note:** The operators `..=` and `..<` can be implemented for any custom type by
> implementing [magic functions](./3_functions.md#magic-functions).

### Using Ranges

```nitro
let range = 1..=10

// Check membership
println(5 in range)         // true
println(15 in range)        // false
println(10 in 1..<10)       // false (exclusive end)

// Iterate over ranges (in loops)
for i in 1..=5 {
    println(i)  // Prints 1, 2, 3, 4, 5
}
```

## The Nothing Type

The `nothing` literal represents the absence of a value, similar to `void` or `Unit` in other languages:

```nitro
// Functions that don't return a value
fun print_message() {
    println("This function returns nothing")
}  // Implicitly returns: nothing

// Explicit nothing return
fun do_something(): Nothing {
    println("Doing something...")
    return nothing
}
```

**Important:** Unlike `null` in other languages, `nothing` is a distinct type that cannot be mixed with other types.
It is used to indicate that a function or expression intentionally does not produce a meaningful value.

## The Never Type

The `never` type represents a situation that will never occur. It is used to make functions that will never return, for
example, with an infinite loop or a critical error that will crash the program:

```nitro
// Function that never returns
fun infinite_loop(): Never {
    loop {
        // Do something forever
    }
}

// Function that always crashes
fun print_and_exit(msg: String): Never {
    println(msg)
    runtime::exit(1)
}
```
