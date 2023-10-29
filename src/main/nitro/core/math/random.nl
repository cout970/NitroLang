
@Extern $[lib: "core", name: "random_get_initial_seed"]
fun get_initial_seed(): Int {}

// Implementation of RngPcg32
struct RngPcg32 {
    seed: Int
}

// Creates a new RngPcg32 with a random seed
fun RngPcg32::new(): RngPcg32 {
    ret RngPcg32 $[seed: get_initial_seed()]
}

// Generates a random integer in the range -2^31..2^31-1
fun RngPcg32.next_int(): Int {
    let value = (seed * 1103515245 + 12345).bitwise_and(0x7FFFFFFF)
    seed = value
    ret value
}

// Generates a random boolean
fun RngPcg32.next_boolean(): Boolean {
    ret this.next_int() < 0x3FFFFFFF
}

fun RngPcg32.next_int_in(start: Int, end: Int): Int {
    let len = end - start
    if len <= 0 {
        crash("Invalid range $start..$end")
    }
    ret start + this.next_int() % len
}