# Expressions

Expressions are pieces of code that can be evaluated to a value.

## Literals

Literals are values that are directly written in the code. For example, `42` is a literal for the number 42.

```nitro
// Int literal
42
-23

// Float literal
3.1416
5f
1e10

// String literal
"Hello, world!"

// Char literals for 'A'
u"A"
u"\x41"
u"\u0041"

// Boolean literals
true
false

// Nothing literal
nothing
```

## Functions

Functions are the essence of programming, they can be called with its name and arguments in parentheses.

```nitro
// Print a string
println("Hello, world!")
```

There is more info available in the [functions](./3_functions.md) section.

## Operators

Nitro supports most of the common operators found in other programming languages.

Arithmetic operators: `+ - * / %`
Bitwise operators: `& | ^ ~ << >> >>>`
Logical operators: `&& || ^^ !`
Comparison operators: `== != < > <= >= <=>`
Assignment operators: `= += -= *= /= %=`
Range operators: `..= ..<`
Type operators: `is !is as`
Contains operators: `in !in`
Null coalescing: `??`

The `<=>` operator is used to compare two values and returns `Ordering::Less`, `Ordering::Equals` or
`Ordering::Greater`.

The operators `..=` and `..<` are used to create ranges. The `..=` operator includes the last value, while the `..<`
operator excludes it.

The `??` operator is used to provide a default value if the left-hand side is an error type (Optional::None, Result::
Error, etc.)

```nitro
let value = optional_value ?? "default_value"
```

## Precedence

Operators have a precedence that determines the order in which they are evaluated.

Operators sorted by precedence, from highest to lowest:

| Prec. | Operator              |
|-------|-----------------------|
| 1     | `() [] .`             |
| 2     | `is !is in !in as ??` |
| 3     | `* / %`               |
| 4     | `+ -`                 |
| 5     | `..= ..<`             |
| 6     | `<< >> >>>`           |
| 7     | `&`                   |
| 8     | `^`                   |
| 9     | `\|`                  |
| 10    | `< > <= >=`           |
| 11    | `== != <=>`           |
| 12    | `^^`                  |
| 13    | `\|\|`                |
| 14    | `&&`                  |
| 15    | `= += -= *= /= %=`    |

## String

Strings are a sequence of characters. They support string interpolation and escape sequences.

```nitro
let name = "Alice"
let age = 42
let message = "Hello, $name! You are $age years old"
let summary = "Message is ${message.len} bytes long"
```

Any expression can be put inside `${}` to be interpolated in a string.

```nitro
"You can put strings inside strings ${"and even put other expressions inside ${42 * 2}"}!"
```

## Ranges

Ranges are a sequence of contiguous values that stored as the beginning and end values.

```nitro
let range = 1..=10
let exclusive_range = 1..<10
```

You can check if a value is inside a range using the `in` operator.

```nitro
// Prints true
println(10 in range)
// Prints false
println(10 in exclusive_range)
```

There is also the inverse operator `!in` that checks if a value is not inside a range.

## Nothing

The `nothing` literal is a special value that represents the absence of a value. It is similar to `null` in other
languages, but cannot be mixed with other types.

Functions that return `Nothing` have no return value.

