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

- Get annotations of type: type_annotation<Box>("Extern")
- Arena allocators
- Smart casts
- Question mark operator `?` at the end of an expression
- Optional function arguments
- Variadic function arguments
- Implicit this instead of writing `this.something()` everywhere
- Runtime reflection of types
- Fix external functions with Floats compressed as pointers
- Lambdas as closures, implement upvalues
- Call lambdas without the `func.invoke(args)` syntax
- Assert type has tag `assert tag ToString for List<#Item>`
- Tags with parametric types
- Improve core library
- Self-hosted compiler
- Check for read of uninitialized variables
- Implement interface for runtime function call indirection
- Place core in a module for namespacing

### Bugs

- `ret when { ... }` Missing provider for ref
- 0 compressed as pointer causes null pointer exceptions

### New syntax

Old:

```
let list = #[1, 2, 3]
let map = @[x: 0, y: 1, z: 0]
let set = %[0, 1, 0]
let struct = Vec3 $[x: 0, y: 1, z: 0]
list[0]
if cond {} else {}
for i in 0..10 {}
func #[1, 2, 3]
func #{ i -> i + 1 }
```

New:

```
let list = [1, 2, 3]
let map = #[x: 0, y: 1, z: 0]
let set = %[0, 1, 0]
let struct = Vec3 @[x: 0, y: 1, z: 0]
list[0]
list[] = 0
if cond {} else {}
for i in 0..10 {}
func([1, 2, 3])
func @{ it }
```