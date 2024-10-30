# Types

## Structs

Structs are a way to define a new type that is composed of other types. They are similar to classes in object-oriented
languages, but they are more limited in their capabilities. Structs are defined using the `struct` keyword, followed by
the name of the struct, and then a block of fields. Each field is defined by a name and a type.

```nitro
struct MyBook {
    title: String
    isbn: String
    pages: Int
}
```

You can create an instance of a struct using the `@[` syntax.

```nitro
let my_book = MyBook @[
    title: "The Hobbit",
    isbn: "978-0-395-07122-9",
    pages: 310,
]
```

You can access the fields of a struct using the `.` operator.

```nitro
let title = my_book.title
let isbn = my_book.isbn
let pages = my_book.pages
```

## Options

Options are a way to represent multiple variants of a type, where some variants may have additional data associated with
it.

```nitro
// Do you want an ice cream?
option Choices {
    Yes { flavor: String }
    No
}
```

You can create an instance of an option using the `@[` syntax.

```nitro
let choice1 = Choices::Yes @[flavor: "chocolate"]
let choice2 = Choices::No @[]
```

The language provides 2 useful option types: `Optional` and `Result`.

```nitro
let has_number = Optional::Some @[value: 42]
let has_no_number = Optional::None @[]

let success = Result::Ok @[value: "Success"]
let error = Result::Err @[value: "Something went wrong"]
```

Those 2 types are used so often that they have shorthands:

```nitro
let has_number = Some(42)
let has_no_number = None()

let success = Ok("Success")
let error = Err("Something went wrong")
```

You can get a value from an option using the `!!` operator.

```nitro
let number: Int = has_number!!
```

This will crash the program if the option is `None`.

## Enums

Enums are a list of static values that are grouped together under a single type.

```nitro
enum Direction {
    North
    South
    East
    West
}
```

They can hold additional static data.

```nitro
enum Direction {
    let x: Int
    let y: Int
    North @[x:  0, y:  1]
    South @[x:  0, y: -1]
    East  @[x:  1, y:  0]
    West  @[x: -1, y:  0]
}
```

You can access a value from an enum using the full path

```nitro
let north: Direction = Direction::North
let north_x: Int = Direction::North.x
let north_y: Int = Direction::North.y
```

Enums come with predefined methods:

```nitro
let directions: List<Direction> = Direction::values()
let north: Direction = Direction::from_name("North")!!
let also_north: Direction = Direction::from_variant(0)!!
let north_variant: Int = Direction::North.variant
```

## Tags

Tags are a set of function that one type must implement. They are automatically applied to all types that match them.

```nitro
tag Printable {
    fun This.print()
}

fun Int.print() {
    println("Printing an Int: $this")
}

fun String.print() {
    println("Printing an String: '$this'")
}

fun <#T: Printable> print_me(value: #T) {
    value.print()
}

fun main() {
    // Will print "Printing an Int: 42"
    print_me(42)
    // Will print "Printing an String: 'Hello, world!'"
    print_me("Hello, world!")
}
```

You can define your own tags, and any existing type with the correct methods will automatically implement them.

```nitro
fun <#T: GetOrdering> max(a: #T, b: #T): #T {
    if a > b {
        ret a
    } else {
        ret b
    }
}
```

This function will work with any type that has the `GetOrdering` tag, that is, any type with a function
`Int.get_ordering(other: Int): Ordering`, like, Int, Float, String, etc.
