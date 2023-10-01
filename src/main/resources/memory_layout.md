# Memory layout

### Int/Float/Ptr<#Value>

Stack:
- 4 byte: inline value in stack

Heap: None

### String

Stack:
- 4 byte: Pointer to heap

Heap:
- 4 byte: Struct type
- 4 byte: String len in bytes
- 4 byte: Pointer to string characters in heap or data section

### List<#Item>

Stack:
- 4 byte: Pointer to heap

Heap:
- 4 byte: Struct type
- 4 byte: List len
- 4 byte: List capacity
- 4 byte: Pointer items in heap

### Optional

Stack:
- 4 byte: Pointer to heap

Heap:
- 4 byte: Struct type
- 4 byte: Option variant
- 4 byte: Pointer to value
