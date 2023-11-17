import {
    getInt,
    setInt,
    assert,
    getFloat,
    getString,
    createString,
    alloc,
    setByte,
    memcopy,
    dumpMemory
} from './internal.ts'

// ptr.nitro

export function ptr_ptr_of(ptr: number): number {
    return ptr;
}

export function ptr_ptr_from_address(int: number): number {
    return int;
}

export function ptr_as_ref(ptr: number): number {
    // console.debug('ptr_get_value', {ptr});
    // assert(ptr); // this fails for integer 0 compressed as a pointer
    return ptr;
}

export function ptr_get_address(ptr: number): number {
    return ptr;
}

export function ptr_to_raw_array(ptr: number): number {
    return ptr;
}

export function raw_array_to_ptr(ptr: number): number {
    // console.debug('raw_array_to_ptr', {ptr});
    return ptr;
}

export function ptr_write(value_ptr: number, value: number) {
    setInt(value_ptr, value);
}

export function ptr_read(value_ptr: number): number {
    // console.debug('ptr_read', {value_ptr, result: getInt(value_ptr)});
    return getInt(value_ptr);
}

export function ptr_unsafe_cast(ptr: number): number {
    return ptr;
}

export function ptr_copy_into(self: number, other: number, len: number) {
    // console.debug('ptr_copy_into', {self, other, len});
    memcopy(other, self, len);
}

// raw_array.nitro

export function raw_array_copy_into(self: number, other: number, len: number) {
    // console.debug('raw_array_copy_into', {self, other, len});
    memcopy(other, self, len);
}

// intrinsic.nitro

// Check the type id of a struct instance
export function is_type_internal(ptr: number, typeId: number): boolean {
    assert(ptr);
    const objTypeId = getInt(ptr);
    // console.debug('is_type_internal', {ptr, typeId, objTypeId})
    return objTypeId == typeId;
}

// Cast to another type
export function as_type_internal(ptr: number, typeId: number): number {
    assert(ptr);
    const objTypeId = getInt(ptr);

    if (objTypeId != typeId) {
        throw new Error("Attempt to cast incompatible types: expected: " + typeId + ", found: " + objTypeId);
    }

    return ptr;
}

// memory.nitro

export function memory_get_memory(): number {
    return 4;
}

export function memory_alloc_trace(amount: number, ptr: number) {
    // console.debug(`# Memory: capacity = ${getInt(4)}, len = ${getInt(8)}, bytes = ${getInt(12)}`)
    console.debug(`# Alloc ${amount.toString().padStart(10, ' ')} at ${ptr.toString().padStart(10, ' ')} (0x${ptr.toString(16).padStart(8, '0')})`)
}

export function memory_alloc(amount: number): number {
    return alloc(amount);
}

export function memory_write_byte(ptr: number, value: number) {
    setByte(ptr, value);
}

export function memory_write_int(ptr: number, value: number) {
    setInt(ptr, value | 0);
}

export function memory_write_boolean(ptr: number, value: number) {
    setInt(ptr, value | 0);
}

export function memory_write_float(ptr: number, value: number) {
    setInt(ptr, value);
}

export function memory_write_internal(ptr: number, value: number, size: number): number {
    assert(ptr);
    // console.debug({ptr, value, size});
    memory_copy_within(value, ptr, size);
    return 0;
}

export function memory_read_byte(ptr: number): number {
    // console.log({ptr, value: getInt(ptr), byte: (getInt(ptr) & 0xFF)});
    const int = getInt(ptr);
    if (ptr % 4 == 0) return int & 0xFF;
    if (ptr % 4 == 1) return (int >> 8) & 0xFF;
    if (ptr % 4 == 2) return (int >> 16) & 0xFF;
    return (int >> 24) & 0xFF;
}

export function memory_read_int(ptr: number): number {
    return getInt(ptr);
}

export function memory_read_boolean(ptr: number): number {
    return getInt(ptr);
}

export function memory_read_float(ptr: number): number {
    return getFloat(ptr);
}

export function memory_read_internal(ptr: number, size: number): number {
    assert(ptr);
    const value = memory_alloc(size);
    memory_copy_within(ptr, value, size);
    return value;
}

export function memory_copy_within(src: number, dst: number, len: number) {
    // console.debug('memory_copy_within', {src, dst, len});
    memcopy(dst, src, len);
}

export function memory_copy_internal(dst: number, src: number, len: number) {
    console.debug('memory_copy_internal', {src, dst, len});
    memcopy(dst, src, len);
}

export function memory_dump(ptr: number) {
    console.debug(`# Memory: capacity = ${getInt(4)}, len = ${getInt(8)}, bytes = ${getInt(12)}, base = ${ptr}`)
    console.debug('Memory dump:\n' + dumpMemory(getInt(12), getInt(8)));
}

// string.nitro

export function string_codepoint_len(a: number): number {
    const str = getString(a);
    const chars = Array.from(str);
    return chars.length;
}

export function string_get_codepoint(a: number, index: number): number {
    const str = getString(a);
    const chars = Array.from(str);
    const char = chars[index];

    return char.codePointAt(0) || 0;
}

export function string_concat_string(a: number, b: number): number {
    const s1 = getString(a);
    const s2 = getString(b);
    // console.debug('string_concat_string', {s1, s2, a, b});
    return createString(s1 + s2)
}

export function string_concat_char(a: number, b: number): number {
    return createString(getString(a) + String.fromCodePoint(b))
}

export function string_is_equal(a: number, b: number): boolean {
    assert(a);
    assert(b);
    return getString(a) == getString(b)
}

export function string_is_not_equal(a: number, b: number): boolean {
    assert(a);
    assert(b);
    return getString(a) != getString(b)
}

export function string_replace(a: number, b: number, c: number): number {
    const base = getString(a);
    const find = getString(b);
    const replacement = getString(c);

    return createString(base.replaceAll(find, replacement))
}

export function int_to_string(int: number): number {
    return createString(String(int | 0));
}

export function int_to_string_in_base(int: number, radix: number): number {
    return createString((int | 0).toString(radix));
}

// float.nitro

export function float_to_string(float: number): number {
    return createString(String(float));
}

export function float_is_nan(float: number): boolean {
    return Number.isNaN(float);
}

export function float_is_finite(float: number): boolean {
    return Number.isFinite(float);
}

export function float_is_integer(float: number): boolean {
    return Number.isInteger(float);
}

export function float_is_safe_integer(float: number): boolean {
    return Number.isSafeInteger(float);
}

// byte.nitro

export function byte_to_int(byte: number): number {
    return byte & 0xFF;
}

// int.nitro

export function int_to_byte(int: number): number {
   return int & 0xFF;
}

// intrinsic.nitro

export function choose(cond: number, a: number, b: number): number {
    return cond ? a : b;
}

// float.nitro

export function float_rem(a: number, b: number): number {
    return a % b;
}

// console.nitro

export function println_boolean(val: number): number {
    console.log(val !== 0);
    return val;
}

export function println_int(val: number): number {
    console.log(val);
    return val;
}

export function println_float(val: number): number {
    console.log(val);
    return val;
}

export function println_string(ptr: number): number {
    console.log(getString(ptr));
    return ptr;
}

// ordering.nitro
export function string_get_ordering_internal(a: number, b: number): number {
    if (a === b) return 0;

    const aStr = getString(a);
    const bStr = getString(b);

    return aStr.localeCompare(bStr);
}

// math.nitro

export function math_acos(x: number): number             { return Math.acos(x);     }
export function math_acosh(x: number): number            { return Math.acosh(x);    }
export function math_asin(x: number): number             { return Math.asin(x);     }
export function math_asinh(x: number): number            { return Math.asinh(x);    }
export function math_atan(x: number): number             { return Math.atan(x);     }
export function math_atanh(x: number): number            { return Math.atanh(x);    }
export function math_atan2(x: number, y: number): number { return Math.atan2(x, y); }
export function math_sqrt(x: number): number             { return Math.sqrt(x);     }
export function math_cbrt(x: number): number             { return Math.cbrt(x);     }
export function math_cos(x: number): number              { return Math.cos(x);      }
export function math_cosh(x: number): number             { return Math.cosh(x);     }
export function math_exp(x: number): number              { return Math.exp(x);      }
export function math_expm1(x: number): number            { return Math.expm1(x);    }
export function math_log(x: number): number              { return Math.log(x);      }
export function math_log1p(x: number): number            { return Math.log1p(x);    }
export function math_log10(x: number): number            { return Math.log10(x);    }
export function math_log2(x: number): number             { return Math.log2(x);     }
export function math_pow(x: number, y: number): number   { return Math.pow(x, y);   }
export function math_sin(x: number): number              { return Math.sin(x);      }
export function math_sinh(x: number): number             { return Math.sinh(x);     }
export function math_tan(x: number): number              { return Math.tan(x);      }
export function math_tanh(x: number): number             { return Math.tanh(x);     }

// random.nitro

export function random_get_initial_seed(): number { return (Math.random() * 0x7FFFFFFF) | 0; }

// logger.nitro

export function logger_get_logger(): number { return 0; }

export function logger_log(logger: number, level: number, msg: number) {
    const levelStr = getString(level);
    const msgStr = getString(msg);

    if (levelStr == "trace") {
        console.debug(msgStr);
    } else if (levelStr == "debug") {
        console.debug(msgStr);
    } else if (levelStr == "info") {
        console.log(msgStr);
    } else if (levelStr == "warn") {
        console.warn(msgStr);
    } else if (levelStr == "error") {
        console.error(msgStr);
    } else if (levelStr == "fatal") {
        console.error(msgStr);
    } else {
        console.log(msgStr);
    }
}