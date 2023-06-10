// Prerequisites of 'for' needed to work without the stdlib
@Extern fun List<#T>.get(index: Int): #T {}
@Extern fun List<#T>.len(): Int {}
@Extern fun Int.get_ordering(other: Int): Int {}
@Extern fun Int.is_less(): Boolean {}

option Optional<#T> { Some { value: #T }; None {} }

fun Optional<#T>.is_some(): Boolean = this is Optional::Some<#T>

struct Iterator<#T> {
    index: Int
    list: List<#T>
}

fun Iterator<#T>.next(): Optional<#T> {
    if this.index < this.list.len() {
        let next = Optional::Some $[
            value: this.list[this.index]
        ]
        this.index = this.index + 1
        ret next
    }

    ret Optional::None $[]
}

fun List<Int>.to_iterator(): Iterator<Int> {
    ret Iterator $[index: 0, list: this]
}

// Actual test
fun main(): Unit {
    for i in #[1, 2, 3] {
        42
    }
}