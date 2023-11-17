
enum Color {
    RED, ORANGE, YELLOW, GREEN, BLUE, INDIGO, VIOLET
}

fun main() {
    let red = Color::RED

    println(red)
    println(Color::from_variant(0))
    println(Color::from_variant(1))
    println(Color::from_variant(2))
    println(Color::VIOLET.variant)
    println(Color::values().len())
    println(Color::from_variant(7))
    println(Color::values())
}
