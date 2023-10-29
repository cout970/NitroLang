
// @Extern $[lib, name]
// Marks a function which implementation resides outside the WASM execution
// When a struct is marked with this annotation, the details of the struct are filled by the compiled,
// for example: Int, Float, Bytes, etc.

// @ValueType
// Marks a struct that is stored inline in another struct/option/array

// @WasmName $[name]
// Marks a function that can be called from outside of WASM withing the defined name

// @WasmInline $[opcode]
// Marks a function that represents a WASM native instruction,
// the compiler will replace the function call with the instruction

// @Required
// Marks a function to be excluded from the dead code elimination process

// @Intrinsic
// Marks a type or function that has special handling in the compiler
