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
- [ ] StdLib
- [ ] Intellij Language Plugin
- [ ] Package manager for dependencies
- [ ] Marketing
- [ ] Community feedback and fine-tuning
- [ ] Sell the project to a big company and retire to a tropical paradise
- [ ] Profit?

### Other features to implement

- String scape sequences `\n`, `\"`, etc.
- Add automatic properties by detecting functions with the pattern get_<property> and set_property
- Improve core library
- Check for read of uninitialized variables
- Call lambdas without the `func.invoke(args)` syntax
- Implicit this instead of writing `this.something()` everywhere
- Smart casts
- Place core in a module for namespacing
- Assert type has tag `assert tag ToString for List<#Item>`
- Tags with parametric types
- Implement interface for runtime function call indirection
- Fix external functions with Floats compressed as pointers
- Add more tests to the code library
- Self-hosted compiler
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
- Add reference-count garbage collector
- Add option to disable GC on specific instances
- Implement merge sort

### Known Bugs

- 0 compressed as pointer causes null pointer exceptions
