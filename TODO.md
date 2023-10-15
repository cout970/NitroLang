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
- Vec, Set implementations
- Get annotations of type: type_annotation<Box>("Extern")
- Arena allocators
- Smart casts
- `for` statement with iterators
- Runtime reflection of types
- Enums, not like rust, like java, a list of constants that can be iterated
- Consts only accept primitive values, it should allow lists, maps and sets
- && and || should short-circuit
- Fix external functions with Floats compressed as pointers
- Lambdas as closures
- Call lambdas without the `func.invoke(args)` syntax
- Assert type has tag `assert tag ToString for List<#Item>`
- Tags with parametric types
- Finish stdlib
- Self-hosted compiler