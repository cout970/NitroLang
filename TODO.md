# TODO

Pending list of things to do, not complete by any means.

Intended for the developers

### Compiler

- [x] Grammar
- [x] Parser
- [x] Resolve and check symbols
- [x] Resolve and check types
- [x] Link types and symbols
- [x] Type inference
- [x] Compile to WASM
- [x] StdLib
- [x] Self-hosted compiler
- [ ] Remove old kotlin compiler
- [ ] Better memory management
- [ ] Intellij Language Plugin
- [ ] Package manager for dependencies
- [ ] Marketing
- [ ] Community feedback and fine-tuning
- [ ] Sell the project to a big company and retire to a tropical paradise
- [ ] Profit?

### Other features to implement

- Fix missing field in struct creation does not produce an error
- `it?.func()` to call a function on an optional value
- Optional arguments 
- Improve core library
- Smart casts
- Place core in a module for namespacing
- Assert type has tag `assert tag ToString for List<#Item>`
- Tags with parametric types
- Implement interface for runtime function call indirection
- Fix external functions with Floats compressed as pointers
- Add more tests to the code library
- Get annotations of type: type_annotation<Box>("Extern")
- Runtime reflection of types
- Arena allocators
- Optional function arguments
- Variadic function arguments
- Allow fields at the option level, to be added to all the option variants
- Add formatter to convert values to string without allocating intermediary strings and allow special formats (hex,
  binary, fixed number of decimales, custom separators, etc.)
- Add inverse tags, if Int and Float have the tag Numeric, any function shared between Int and Float, becomes a function
  of Numeric
- Add suspend functions (async/await)
- Support WASI runtime
- Inline struct in another struct to share a set of fields between structs
- Add bench! to create benchmarks
- Add dbg! to debug expression and block of statements
- Add reference-count garbage collector
- Add option to disable GC on specific instances
- Implement merge sort
- Prefix 'B'/'b' for byte literals
- Add Short and prefix 'S'/'s' for short literals
- Change Some/None to Ok/Err to lowercase
- Check nested ret if ... { ret if ... { ret if ... {} } }

### Pending renames

- `bytes_len` to `len` in strings
- `a"-"` to `u"-"` ascii to unicode (Int)
- `a"-"` to `b"-"` ascii to byte (Byte)
- `a"-"` to `c"-"` ascii to char (Char)
- `get_or_else`, `get_or_default` to `or_else`, `or_default`
- `or_fail_with` to `expect` 

### Known Bugs

- Function with return type by no return at the end, does not cause an error, crashes at runtime
- 'break' in when expression emits error 'Int' vs 'Nothing'