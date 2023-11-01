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
- Vec<*> implementation
- Get annotations of type: type_annotation<Box>("Extern")
- Arena allocators
- Smart casts
- Question mark operator `?` at the end of an expression
- implicit this instead of writing `this.something()` everywhere
- `for` statement with iterators
- Runtime reflection of types
- Enums, not like rust, but like java, a list of constants that can be iterated
- Consts only accept primitive values, it should allow lists, maps and sets
- Fix external functions with Floats compressed as pointers
- Lambdas as closures, implement upvalues
- Call lambdas without the `func.invoke(args)` syntax
- Assert type has tag `assert tag ToString for List<#Item>`
- Tags with parametric types
- Improve core library
- Self-hosted compiler
- Check for read of uninitialized variables
- Implement interface for runtime function call indirection

### Bugs
- `ret when { ... }` Missing provider for ref
- 0 compressed as pointer causes null pointer exceptions
- `Some(None<Int>)` returns `true` when calling `is_none()`