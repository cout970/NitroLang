
// TODO support type params in tags
//tag Slice<#Item> {
//    fun This.len(): Int
//    fun This.get(index: Int): #Item
//}
//
//fun <#Slice: Slice<#Item>> #Slice.to_list(): List<#Item> {
//    let list = List::new<#Item>()
//
//    repeat this.len() {
//        list[] = this[it]
//    }
//
//    ret list
//}
