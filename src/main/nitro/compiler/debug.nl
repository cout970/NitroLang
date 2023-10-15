
fun debug() {
      let a: Optional<Optional<Int>> = Some(None<Int>())
      println(a.is_some()) // false
      println(a.is_none()) // true
}
