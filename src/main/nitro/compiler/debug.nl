
struct B<#P1, #P2> {}

type_alias Vec = List<#Item>

fun debug() {
    let a: Vec<Int> = #[1, 2, 3]
}
