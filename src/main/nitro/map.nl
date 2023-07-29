
@Extern $[lib: "core", name: "Map"]
struct Map<#Key, #Value> {
    len: Int
    table: List<Optional<MapEntry<#Key, #Value>>>
}

struct MapEntry<#Key, #Value> {
    key: #Key
    value: #Value
}

@Extern $[lib: "core", name: "map_set"]
fun Map<#Key, #Value>.set(key: #Key, value: #Value) {}

@Extern $[lib: "core", name: "map_get"]
fun Map<#Key, #Value>.get(key: #Key): Optional<#Value> {}

fun Map<#Key, #Value>.len(): Int { ret this.len }
