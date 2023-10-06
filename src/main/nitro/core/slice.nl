
struct Slice<#Item> {
    len: Int
    ptr: RawArray<#Item>
}

fun create_slice(len: Int, ptr: RawArray<#Item>): Slice<#Item> {
    return Slice<#Item> $[len, ptr]
}

fun Slice<#Item>.len(): Int = this.len

fun Slice<#Item>.get(index: Int): #Item {
    if index < 0 || index >= this.len {
        crash("Invalid slice index: $index, len: ${this.len}")
    }
    ret this.ptr.get(index)
}

fun Slice<#Item>.to_list(): List<#Item> {
    let list = create_list<#Item>()

    repeat this.len {
        list[] = this[it]
    }

    ret list
}
