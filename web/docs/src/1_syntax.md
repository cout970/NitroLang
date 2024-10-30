# Basic syntax

Nitro is designed to be easy to learn, most of it syntax is inspired by other languages like Rust, Kotlin, and
JavaScript.

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
 * /* Nested comments are allowed */ 
 */
```

## Reserved keywords

Some words are reserved and cannot be used as identifiers. These are:

```nitro
this This fun let mod struct ret size_of option internal rec tag defer type_alias
enum nothing when match alias if else for in while repeat loop is as true false 
null include break continue use mut json! test! include_as_bytes! include_as_string!
```

Some of these are not used, but they are reserved for future use.

For example, `null` is not used in Nitro, but attempting to use it as an identifier will result in a syntax error.

## Identifiers

Identifiers are used to name variables, functions, types, and other entities. They must start with a letter followed by
letters, digits, or underscores. They cannot
start with an underscore. Identifiers are case-sensitive.

```nitro
foo
fooBar
BarFoo
foo_bar
baz42
ALL_CAPS
```

## Newlines and semicolons

Semicolons are optional in Nitro. They are equivalent to newlines, you can use them to separate multiple statements on
the same line.

```nitro
a = 1; b = 2; c = 3
```

## Commas

In most places, commas are optional. They are recommended, but if you prefer not to use them, you can omit them.

All those are valid:

```nitro
// Single line
a = [1, 2, 3]

// Multi-line with trailing comma
a = [
    1,
    2,
    3,   
]

// No commas at all
a = [
    1
    2
    3
]
```

