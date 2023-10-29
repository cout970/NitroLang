# Memory layout

### Int/Float/Ptr<\*>/RawArray<\*>

Stack:

- 4 byte: inline value as i32/f32

Heap: None

### String

Stack:

- 4 byte: Pointer to heap

Heap:

- 4 byte: String len in bytes
- 4 byte: Pointer to string characters in heap or data section

### List<#Item>

Stack:

- 4 byte: Pointer to heap

Heap:

- 4 byte: List capacity
- 4 byte: List len
- 4 byte: Pointer items in heap

## Value types

Value types have 2 encodings, when used by itself and when used as a field in a struct/option/etc.
When the value is used by itself, it is stored in the heap and referenced with a pointer in the stack.
When the value is used as a field in a struct, it is encoded as an inline value and
pointers to the value will be obtained from the struct pointer adding the field offset.

### Optional

Stack:

- 4 byte: Pointer to heap (if required)

Heap:

- 4 byte: Option variant
- 4 or more bytes: Stack representation of value or the inlined value

### Pair

Stack:

- 4 byte: Pointer to heap (if required)

Heap:

- 4 byte: Stack representation of the first value or the inlined value
- 4 or more bytes: Stack representation of the second value or the inlined value


