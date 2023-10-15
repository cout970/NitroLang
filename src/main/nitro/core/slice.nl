
// A slice is a view into a contiguous sequence of elements in a collection.
// Anything with len() and get() is a slice.
// Due to limitations of the compiler, tags cannot have type parameters.
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
