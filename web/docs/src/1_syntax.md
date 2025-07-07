# Getting Started

You can learn Nitro quickly, especially if you're familiar with modern programming languages like Rust,
Kotlin, or TypeScript. This chapter covers the fundamental syntax and language features you need to start writing Nitro
code.

## Variables and Constants

Variables in Nitro are declared using the `let` keyword. By default, variables are mutable:

```nitro
let name = "Alice"
let age = 25
let height = 5.6

age = 26 // Can be changed
```

Except for constants, when you use `let` on a global scope, it becomes a constant. Constants are immutable and must be
initialized at declaration:

```nitro
let counter = 0
// counter = counter + 1 // Compile time error: cannot assign to constant
```

Nitro has powerful type inference, so you often don't need to specify types explicitly. However, you can when needed:

```nitro
let score: Int = 100
let message: String = "Hello, Nitro!"
```

## Comments

Single line comments start with `//` and comment the rest of the line.

```nitro
// This is a single line comment
```

Multi-line comments start with `/*` and end with `*/`. They can span multiple lines.

```nitro
/*
    This
    is
    a
    multi-line
    comment
*/
```

Nested multi-line comments are allowed.

```nitro
/* 
 * This line opens a multi-line comment
 * Also /* nested comments are allowed */ the '* /' did not close the comment
 *
 */
```

## Keywords and Identifiers

### Reserved Keywords

These words are reserved and cannot be used as variable or function names:

```nitro
this This fun let mod struct ret size_of option internal rec tag defer type_alias
enum nothing when match alias if else for in while repeat loop is as true false 
null include break continue use mut json! test! include_as_bytes! include_as_string!
```

**Note:** Some keywords like `null` are reserved for future use even though they're not currently part of the language.

### Valid Identifiers

Identifiers name variables, functions, types, and other entities. Rules:

- Must start with a letter (a-z, A-Z)
- Can contain letters, digits, and underscores
- Cannot start or end with an underscore
- Are case-sensitive

```nitro
foo           // ✓ Valid
fooBar        // ✓ Valid (camelCase)
BarFoo        // ✓ Valid (PascalCase)
foo_bar       // ✓ Valid (snake_case)
baz42         // ✓ Valid (with numbers)
ALL_CAPS      // ✓ Valid (constants)
_invalid      // ✗ Invalid (starts with underscore)
invalid_      // ✗ Invalid (ends with underscore)
42invalid     // ✗ Invalid (starts with number)
```

The language uses PascalCase for type names, snake_case for variables, constants, and functions.

## Syntax Rules

### Semicolons and Newlines

Semicolons are optional in Nitro, newlines serve the same purpose. Use semicolons when you want multiple statements on
one line:

```nitro
// These are equivalent:
let a = 1
let b = 2
let c = 3

// Same as:
let a = 1; let b = 2; let c = 3
```

### Commas

Commas are optional in most contexts. Use them for clarity, especially in single-line collections:

```nitro
// Single line - commas recommended
let numbers = [1, 2, 3, 4, 5]

// Multi-line with commas
let fruits = [
    "apple",
    "banana",
    "cherry",    // trailing comma is fine
]

// Multi-line without commas (also valid)
let colors = [
    "red"
    "green"
    "blue"
]
```

Omitting commas is allowed but not recommended outside DSLs (Domain Specific Languages).

For example, in a DSL like JSON:

```nitro
json! {
    name: "Alice"
    age: 30
    is_student: false
}
```

That is valid Nitro code, and it will create a JSON object, isn't that neat?
