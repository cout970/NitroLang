
// Due to limitations of the compiler, tags cannot have type parameters.
//tag Iterator<#Item> {
//    fun This.next(): Optional<#Item>
//}

//tag ToIterator<#Iterator> {
//    fun <#Iterator: Iterator> This.to_iterator(): #Iterator
//}

struct ListIterator<#Item> {
    list: List<#Item>
    index: Int
}

fun List<#Item>.to_iterator(): ListIterator<#Item> {
    ret ListIterator<#Item> $[
        list: this
        index: 0
    ]
}

fun ListIterator<#Item>.next(): Optional<#Item> {
    if this.index >= this.list.len() {
        ret None()
    }

    let item = this.list[this.index]!!
    this.index = this.index + 1
    ret Some(item)
}
