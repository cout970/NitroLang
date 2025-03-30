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
- [x] Remove old kotlin compiler
- [ ] Better memory management
- [ ] Intellij Language Plugin
- [ ] Package manager for dependencies
- [ ] Marketing
- [ ] Community feedback and fine-tuning
- [ ] Sell the project to a big company and retire to a tropical paradise
- [ ] Profit?

### Other features to implement

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
  binary, fixed number of decimals, custom separators, etc.)
- Add inverse tags, if Int and Float have the tag Numeric, any function shared between Int and Float, becomes a function
  of Numeric
- Add suspend functions (async/await)
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
- Refactor and rename of fields/functions/types that where temporary and end up permanent
- Add better data structure for NCode inst_list, with inserts/removes while iterating and faster index_of_inst(id)
- Not discard comments at lexing to keep them in the AST as Comment nodes
- Fix imports and module resolution
- Add `include "file" as prefix_name` like `import "std.nitro" as std`
- Add path prefix to fields, like `internal::field: Int`
- Add default values for fields in structs
- Add default values for parameters in functions
- Add commas to separate multiple values in `when` expressions

### Known Bugs

- Function with return type by no return at the end, does not cause an error, crashes at runtime
- 'break' in when expression emits error 'Int' vs 'Nothing'
- Crash with options if they only have one variant
- Crash with "Test $"

### Action plan

- Improve compiler memory usage
- Add implicit context parameter to all functions
- Add interfaces
- Add heap allocated stack frames
- Add coroutines
- Add mark and sweep garbage collector using the heap stack frames
