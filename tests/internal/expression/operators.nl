// Result of comparisons, a.get_ordering(b) => Ordering
option Ordering {
    Less    {}
    Equals  {}
    Greater {}
}

// Range of values 1..<2
struct Range {
    start_inclusive: Int
    end_exclusive: Int
}

// Operator overloads
@Extern fun Int.plus(other: Int): Int {}
@Extern fun Int.minus(other: Int): Int {}
@Extern fun Int.mul(other: Int): Int {}
@Extern fun Int.div(other: Int): Int {}
@Extern fun Int.rem(other: Int): Int {}
@Extern fun Boolean.logical_not(): Boolean {}
@Extern fun Boolean.logical_or(other: Boolean): Boolean {}
@Extern fun Boolean.logical_and(other: Boolean): Boolean {}
@Extern fun Boolean.logical_xor(other: Boolean): Boolean {}
@Extern fun Int.bitwise_or(other: Int): Int {}
@Extern fun Int.bitwise_and(other: Int): Int {}
@Extern fun Int.bitwise_xor(other: Int): Int {}
@Extern fun Int.bitwise_shift_left(other: Int): Int {}
@Extern fun Int.bitwise_shift_right(other: Int): Int {}
@Extern fun Int.bitwise_shift_right_unsigned(other: Int): Int {}
@Extern fun Int.get_ordering(other: Int): Ordering {}
@Extern fun Ordering.is_less(): Boolean {}
@Extern fun Ordering.is_less_or_equals(): Boolean {}
@Extern fun Ordering.is_equals(): Boolean {}
@Extern fun Ordering.is_not_equals(): Boolean {}
@Extern fun Ordering.is_greater_or_equals(): Boolean {}
@Extern fun Ordering.is_greater(): Boolean {}
@Extern fun Int.range_in(other: Int): Range {}
@Extern fun Int.range_ex(other: Int): Range {}

fun main(): Unit {
    let a: Int = 1 + 2 // 1.plus(2)
    let b: Int = 1 - 2 // 1.minus(2)
    let c: Int = 1 * 2 // 1.mul(2)
    let d: Int = 1 / 2 // 1.div(2)
    let e: Int = 1 % 2 // 1.rem(2)

    let pemdas: Int = 1 + 2 * 3 // => 7

    let l1: Boolean = !true         // not
    let l2: Boolean = true || false // or  (short-circuit)
    let l3: Boolean = true && true  // and (short-circuit)
    let l4: Boolean = true ^^ true  // xor
    let l5: Int = 0xFF | 0xAA   // or  (bitwise)
    let l6: Int = 0xFF & 0xAA   // and (bitwise)
    let l7: Int = 0xFF ^ 0xAA   // xor (bitwise)

    let c1  = 1 ==  2 // equals
    let c2  = 1 !=  2 // not equals
    let c3  = 1 >   2 // greater
    let c4  = 1 >=  2 // greater or equals
    let c5  = 1 <   2 // less
    let c6  = 1 <=  2 // less or equals
    let c7  = 1 <=> 2 // compare
    let c8  = 1 >>  2 // shift right
    let c9  = 1 >>> 2 // shift right unsigned
    let c10 = 1 <<  2 // shift left
    let c11 = 1 ..= 2 // range inclusive
    let c12 = 1 ..< 2 // range exclusive
}
