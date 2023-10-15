// TODO implement Set in the core library
@Extern $[lib: "core", name: "Set"]
struct Set<#Item> {}

@Extern $[lib: "core", name: "set_add"]
fun Set<#Item>.add(item: #Item) {}

@Extern $[lib: "core", name: "set_has"]
fun Set<#Item>.has(item: #Item) {}

@Extern $[lib: "core", name: "set_len"]
fun Set<#Item>.len(): Int {}
