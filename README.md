# NitroLang

Inspired by the simplicity of C with the improvements of modern languages,
NitroLang is a low level unsafe procedural language, designed to be easy to learn and easy to read.

Right now it's in alpha state and everything is subject to change.

### Syntax

Functions:

```nitrolang
fun Box.to_string(): String {
    ret this.value.to_string()
}
```

Variables:

```nitrolang
fun main() {
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
    loop {
        // Infinite loop
        // until you break
        if true {
            break
        }
    }
    
    while true {
        // same thing
    }
    
    // Do n times
    repeat 10 {
        println("Iteration $it")
    }
    
    // for each item...
    let list = #[1, 2, 3]
    for item in list {
        println("Current item: $item")
    }
    
    // Inline documentation
    // Usage:
    ```js
    console.log(main.find_all_users())
    ```
}
```

Data structures:

```nitrolang
fun main() {
    // Optional ';' to end lines
    let my_list = #[1, 2, 3, 4]
    
    // Optional ',' as separator if there is a newline
    let my_other_list = #[
        1
        2
        3
    ]
    
    let my_map: Map<String, Int> = @[
        a: 1,
        b: 2,
        c: 2 + 1,
        "this key has spaces": 4,
        ("this expression is evaluated: ${1 + 1}"): 5,
    ]
    
    let my_beloved_json = json {
        "name": "John Doe",
        "age": 42,
        "is_admin": true,
        "friends": [
            "Alice",
            "Bob",
            "Charlie"
        ]
    }
        
    // Omit parenthesis for lists, maps and lambdas if they are the last argument 
    use_my_list_function #[]

    // Lambda function as data
    let compute: () -> Int = #{ 1 + 2 }
    
    use_my_lambda_function(compute)
    
    // Inline lambda function as argument
    use_my_lambda_function #{ 2 + 2 }
}
```

Lambdas:

```nitrolang
fun main() {
    // All 4 versions are equivalent
    
    #[1, 2, 3, 4].for_each #{ i: Int ->
        println("- $i")
    }

    #[1, 2, 3, 4].for_each(#{ i: Int ->
        println("- $i")
    })

    for_each(#[1, 2, 3, 4], #{ i: Int ->
        println("- $i")
    })

    for_each(#[1, 2, 3, 4]) #{ i: Int ->
        println("- $i")
    }
}
```

Types:

```nitrolang
struct Box<#Item> {
    item: #Item
    has_been_used: Boolean
}

option Available {
    Yes {
        time: Int
    }
    No {
        reason: String
    }
}

// Not yet added..
enum Roles {
    Admin
    Moderator
    Regular
}

fun main() {
    println(Box $[item: 42, has_been_used: false])
    println(Available::No $[reason: "Not found"])
    println(Roles::Admin)
}
```

Tags:

```nitrolang
tag ToBoolean {
    fun This.to_boolean(): Boolean
}

fun Int.to_boolean(): Boolean = this != 0

fun <#Any: ToBoolean> convert(value: #Any) = value.to_boolean()

fun main() {
    // Int automatic implements ToBoolean
    println(convert(0))
    println(convert(1))
}
```

Modules:

```nitrolang
mod main_module {
    fun foo() {}
    fun Int.bar() {}
}

mod second_module {
    fun foo() {}
}

fun Int.bar() {}

fun main() {
    main_module::foo()
    second_module::foo()
    
    42.main_module::bar()
    42.bar()
    
    // Also valid
    bar(42)
}
```

### Features

- Simplicity
- Small footprint
- Mix of high level structures and lower level memory control
- WebAssembly backend
- Strong typing with good type inference
- Supports functional programing and structure programing, avoids object-oriented programing
- Supports creation of nice DSLs