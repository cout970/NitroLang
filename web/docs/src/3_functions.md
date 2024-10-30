# Functions

## Definition

Functions are defined with the keyword `fun` followed by the function name and its arguments in parentheses. The return type is specified after the arguments with a colon `:`.

```nitro
fun add(a: Int, b: Int): Int {
    return a + b
}
```

The return type can be omitted if the function returns `Nothing`.

```nitro
fun print_hello() {
    println("Hello, world!")
}
// Same as 
fun print_hello(): Nothing {
    println("Hello, world!")
}
```

## Calling functions

Functions are called with its name and arguments in parentheses.

```nitro
println("Hello, world!")
```

Functions can have a path, provided by the module where they are defined.

```nitro
// Will create a new StringBuilder
StringBuilder::new()

// Will give a list of arguments passed to the program
runtime::get_program_args()
```

## Generics

Functions can be generic, with the type parameters specified in angle brackets `<>`.
Type parameters must start with `#`.

```nitro
fun get_value_in_box<#T>(box: Box<#T>): #T {
    ret box.value
}
```

You can omit the definition of the type parameters if they are used in the parameters or return type.

```nitro
fun get_value_in_box(box: Box<#T>): #T {
    ret box.value
}
```

## Default arguments

Currently, functions do not support default arguments, that will be added in a future version.
