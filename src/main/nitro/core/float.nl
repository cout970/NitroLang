
// This type is used to represent floating point numbers defined in the IEEE 754 standard
@Extern $[lib: "core", name: "Float"]
@ValueType
@Intrinsic
struct Float {}

// Adds two floating point numbers
@Extern $[lib: "core", name: "float_plus"]
@WasmInline $[opcode: "f32.add"]
fun Float.plus(other: Float): Float {}

// Subtracts two floating point numbers
@Extern $[lib: "core", name: "float_minus"]
@WasmInline $[opcode: "f32.sub"]
fun Float.minus(other: Float): Float {}

// Multiplies two floating point numbers
@Extern $[lib: "core", name: "float_mul"]
@WasmInline $[opcode: "f32.mul"]
fun Float.mul(other: Float): Float {}

// Divides two floating point numbers
@Extern $[lib: "core", name: "float_div"]
@WasmInline $[opcode: "f32.div"]
fun Float.div(other: Float): Float {}

// Returns the remainder of two floating point numbers
// Note: this is not the same as the modulo operator, the results are different for negative numbers
// WASM does not provide f32.rem, so its implemented in an external function
@Extern $[lib: "core", name: "float_rem"]
fun Float.rem(other: Float): Float {}

// Returns the smallest of two floating point numbers
// If any of the numbers is NaN, the result will be NaN
@Extern $[lib: "core", name: "float_min"]
@WasmInline $[opcode: "f32.min"]
fun Float.min(other: Float): Float {}

// Returns the largest of two floating point numbers
// If any of the numbers is NaN, the result will be NaN
@Extern $[lib: "core", name: "float_min"]
@WasmInline $[opcode: "f32.max"]
fun Float.max(other: Float): Float {}

// Returns this number with the sign of the provided number
@Extern $[lib: "core", name: "float_copy_sign"]
@WasmInline $[opcode: "f32.copysign"]
fun Float.copy_sign(other: Float): Float {}

// Returns the absolute value of a floating point number
@Extern $[lib: "core", name: "float_abs"]
@WasmInline $[opcode: "f32.abs"]
fun Float.abs(): Float {}

// Returns the negated value of a floating point number
// For 0, -0 is returned
// For NaN, NaN is returned
@Extern $[lib: "core", name: "float_neg"]
@WasmInline $[opcode: "f32.neg"]
fun Float.neg(): Float {}

// Returns the square root of a floating point number
@Extern $[lib: "core", name: "float_sqrt"]
@WasmInline $[opcode: "f32.sqrt"]
fun Float.sqrt(): Float {}

// Rounds up and returns the smallest integer greater than or equal to a given number
@Extern $[lib: "core", name: "float_ceil"]
@WasmInline $[opcode: "f32.ceil"]
fun Float.ceil(): Float {}

// Rounds down and returns the largest integer less than or equal to a given number
@Extern $[lib: "core", name: "float_floor"]
@WasmInline $[opcode: "f32.floor"]
fun Float.floor(): Float {}

// Returns the integer part of a number by removing any fractional digits
@Extern $[lib: "core", name: "float_truncate"]
@WasmInline $[opcode: "f32.trunc"]
fun Float.truncate(): Float {}

// Returns the nearest integer to a given number
@Extern $[lib: "core", name: "float_nearest"]
@WasmInline $[opcode: "f32.nearest"]
fun Float.nearest(): Float {}

// Converts a floating point number to an integer, by removing any fractional digits
@Extern $[lib: "core", name: "float_to_int"]
@WasmInline $[opcode: "i32.trunc_f32_s"]
fun Float.to_int(): Int {}

// Converts an integer to a floating point number
@Extern $[lib: "core", name: "float_to_int"]
@WasmInline $[opcode: "f32.convert_i32_s"]
fun Int.to_float(): Float {}

// Comparisons

// This function compares two floating point numbers for equality
@Extern $[lib: "core", name: "float_is_equal_float"]
@WasmInline $[opcode: "f32.eq"]
fun Float.is_equal(other: Float): Boolean {}

// This function compares two floating point numbers for inequality
@Extern $[lib: "core", name: "float_is_not_equal_float"]
@WasmInline $[opcode: "f32.ne"]
fun Float.is_not_equal(other: Float): Boolean {}

// Checks if this floating point number is strictly less than 'other'
@Extern $[lib: "core", name: "float_less_than"]
@WasmInline $[opcode: "f32.lt"]
fun Float.less_than(other: Float): Boolean {}

// Checks if this floating point number is less than or equal to 'other'
@Extern $[lib: "core", name: "float_less_equal"]
@WasmInline $[opcode: "f32.le"]
fun Float.less_equal(other: Float): Boolean {}

// Checks if this floating point number is greater than or equal to 'other'
@Extern $[lib: "core", name: "float_greater_equal"]
@WasmInline $[opcode: "f32.ge"]
fun Float.greater_equal(other: Float): Boolean {}

// Checks if this floating point number is strictly greater than 'other'
@Extern $[lib: "core", name: "float_greater_than"]
@WasmInline $[opcode: "f32.gt"]
fun Float.greater_than(other: Float): Boolean {}

// Checks if this floating point number is NaN
@Extern $[lib: "core", name: "float_is_nan"]
fun Float.is_nan(): Boolean {}

// Checks if this floating point number is finite, meaning it is not NaN, -Inf or +Inf
@Extern $[lib: "core", name: "float_is_finite"]
fun Float.is_finite(): Boolean {}

// Checks if this floating point number is infinite, meaning it is -Inf or +Inf
fun Float.is_infinite(): Boolean = not (this.is_finite() || this.is_nan())

// Checks if this floating point number has no fractional digits
@Extern $[lib: "core", name: "float_is_integer"]
fun Float.is_integer(): Boolean {}

// Determines whether the provided value is a number that is a safe integer
// The safe integers consist of all integers from -(2^53 - 1) to 2^53 - 1, inclusive (±9,007,199,254,740,991).
// A safe integer is an integer that:
// - can be exactly represented as an IEEE-754 double precision number, and
// - whose IEEE-754 representation cannot be the result of rounding any other integer to fit the IEEE-754 representation
@Extern $[lib: "core", name: "float_is_safe_integer"]
fun Float.is_safe_integer(): Boolean {}

// Function called when a unary minus is applied to a floating point number, e.g. -(1.0)
// Note: -1.0 is parsed as a negative number, so no operation is performed
fun Float.unary_minus(): Float = this.neg()

// Function called when a unary plus is applied to a floating point number, e.g. +(1.0)
fun Float.unary_plus(): Float = this

// Converts this floating point number into a string
@Extern $[lib: "core", name: "float_to_string"]
fun Float.to_string(): String {}

// This function compares two floating point numbers and returns an ordering value
// that represents the sorting order of the values
fun Float.get_ordering(other: Float): Ordering {
    if this == other {
        ret Ordering::Equals $[]
    } else {
        if this > other {
            ret Ordering::Greater $[]
        } else {
            ret Ordering::Less $[]
        }
    }
}

// Optimized version of max() for floating point numbers
fun max(a: Float, b: Float): Float {
    ret if a > b { a } else { b }
}

// Optimized version of min() for floating point numbers
fun min(a: Float, b: Float): Float {
    ret if a > b { b } else { a }
}
