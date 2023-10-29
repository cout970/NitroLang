
// Useful mathematical constants

// Tau is the ratio of a circle's circumference to its radius
let TAU: Float = 6.283185307179586
// Pi is the ratio of a circle's circumference to its diameter
let PI: Float = 3.141592653589793
// E is Euler's number, the base of natural logarithms
let E: Float = 2.718281828459045
// ln(10) is the natural logarithm of 10
let LN10: Float = 2.302585092994046
// ln(2) is the natural logarithm of 2
let LN2: Float = 0.6931471805599453
// log10(e) is the base 10 logarithm of e
let LOG10E: Float = 0.4342944819032518
// log2(e) is the base 2 logarithm of e
let LOG2E: Float = 1.4426950408889634
// sqrt(1/2) is the square root of 1/2
let SQRT1_2: Float = 0.7071067811865476
// sqrt(2) is the square root of 2
let SQRT2: Float = 1.4142135623730951

// Math.acos(x): Returns the arccosine of x in radians.
@Extern $[lib: "core", name: "math_acos"]
fun Math::acos(x: Float): Float {}

// Math.acosh(x): Returns the hyperbolic arccosine of x.
@Extern $[lib: "core", name: "math_acosh"]
fun Math::acosh(x: Float): Float {}

// Math.asin(x): Returns the arcsine of x in radians.
@Extern $[lib: "core", name: "math_asin"]
fun Math::asin(x: Float): Float {}

// Math.asinh(x): Returns the hyperbolic arcsine of x.
@Extern $[lib: "core", name: "math_asinh"]
fun Math::asinh(x: Float): Float {}

// Math.atan(x): Returns the arctangent of x in radians.
@Extern $[lib: "core", name: "math_atan"]
fun Math::atan(x: Float): Float {}

// Math.atanh(x): Returns the hyperbolic arctangent of x.
@Extern $[lib: "core", name: "math_atanh"]
fun Math::atanh(x: Float): Float {}

// Math.atan2(y, x): Returns the arctangent of the quotient of its arguments.
@Extern $[lib: "core", name: "math_atan2"]
fun Math::atan2(y: Float, x: Float): Float {}

// Math.sqrt(x): Returns the square root of x
@Extern $[lib: "core", name: "math_sqrt"]
fun Math::sqrt(x: Float): Float {}

// Math.cbrt(x): Returns the cube root of x.
@Extern $[lib: "core", name: "math_cbrt"]
fun Math::cbrt(x: Float): Float {}

// Math.cos(x): Returns the cosine of x (where x is in radians).
@Extern $[lib: "core", name: "math_cos"]
fun Math::cos(x: Float): Float {}

// Math.cosh(x): Returns the hyperbolic cosine of x.
@Extern $[lib: "core", name: "math_cosh"]
fun Math::cosh(x: Float): Float {}

// Math.exp(x): Returns e raised to the power x.
@Extern $[lib: "core", name: "math_exp"]
fun Math::exp(x: Float): Float {}

// Math.expm1(x): Returns e raised to the power x minus 1.
@Extern $[lib: "core", name: "math_expm1"]
fun Math::expm1(x: Float): Float {}

// Math.log(x): Returns the natural logarithm (base e) of x.
@Extern $[lib: "core", name: "math_log"]
fun Math::log(x: Float): Float {}

// Math.log1p(x): Returns the natural logarithm (base e) of 1 + x for a number x.
@Extern $[lib: "core", name: "math_log1p"]
fun Math::log1p(x: Float): Float {}

// Math.log10(x): Returns the base 10 logarithm of x.
@Extern $[lib: "core", name: "math_log10"]
fun Math::log10(x: Float): Float {}

// Math.log2(x): Returns the base 2 logarithm of x
@Extern $[lib: "core", name: "math_log2"]
fun Math::log2(x: Float): Float {}

// Math.pow(x, y): Returns the base x raised to the power y.
@Extern $[lib: "core", name: "math_pow"]
fun Math::pow(x: Float, y: Float): Float {}

// Math.sin(x): Returns the sine of x (where x is in radians).
@Extern $[lib: "core", name: "math_sin"]
fun Math::sin(x: Float): Float {}

// Math.sinh(x): Returns the hyperbolic sine of x.
@Extern $[lib: "core", name: "math_sinh"]
fun Math::sinh(x: Float): Float {}

// Math.tan(x): Returns the tangent of x (where x is in radians).
@Extern $[lib: "core", name: "math_tan"]
fun Math::tan(x: Float): Float {}

// Math.tanh(x): Returns the hyperbolic tangent of x.
@Extern $[lib: "core", name: "math_tanh"]
fun Math::tanh(x: Float): Float {}
