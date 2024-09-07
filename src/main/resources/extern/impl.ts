import {
  getInt,
  setInt,
  assert,
  getFloat,
  getString,
  createString,
  setByte,
  memcopy,
  dumpMemory,
  mem,
  setFloat,
  alloc,
  PTR,
  STRUCT_HEADER,
  setLong,
  getLong,
  fs
} from './internal.ts'

import * as trace from './trace.ts'

// @formatter:off
// -------------------------------------------------------------------------
// From intrinsic.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="as_type_internal"]
// fun as_type_internal(Int, Int): Int
export function as_type_internal(ptr: number, typeId: number): number {
    assert(ptr);
    const objTypeId = getInt(ptr);

    if (objTypeId != typeId) {
        throw new Error("Attempt to cast incompatible types: expected: " + typeId + ", found: " + objTypeId);
    }

    return ptr;
}

// @Extern [lib="core", name="is_type_internal"]
// fun is_type_internal(Int, Int): Boolean
export function is_type_internal(ptr: number, typeId: number): boolean {
    assert(ptr);
    const objTypeId = getInt(ptr);
    // console.debug('is_type_internal', {ptr, typeId, objTypeId})
    return objTypeId == typeId;
}

// -------------------------------------------------------------------------
// From byte.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="byte_to_int"]
// fun to_int(Byte): Int
export function byte_to_int(self: number): number { return self & 0xFF; }

// -------------------------------------------------------------------------
// From int.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="int_to_long"]
// fun to_long(Int): Long
export function int_to_long(self: number): bigint { return BigInt(self); }

// @Extern [lib="core", name="int_to_byte"]
// fun to_byte(Int): Byte
export function int_to_byte(self: number): number { return self & 0xFF; }

// @Extern [lib="core", name="int_to_string"]
// fun to_string(Int): String
export function int_to_string(self: number): number { return createString(String(self | 0)); }

// @Extern [lib="core", name="int_to_string_in_base"]
// fun to_string_in_base(Int, Int): String
export function int_to_string_in_base(self: number, radix: number): number { return createString((self | 0).toString(radix)); }

// -------------------------------------------------------------------------
// From long.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="long_to_int"]
// fun to_int(Long): Int
export function long_to_int(self: bigint): number { return Number(self); }

// @Extern [lib="core", name="long_to_byte"]
// fun to_byte(Long): Byte
export function long_to_byte(self: bigint): number { return Number(self) & 0xFF; }

// @Extern [lib="core", name="long_to_string"]
// fun to_string(Long): String
export function long_to_string(self: bigint): number { return createString(String(self)); }

// @Extern [lib="core", name="long_to_string_in_base"]
// fun to_string_in_base(Long, Long): String
export function long_to_string_in_base(self: bigint, radix: number): number { return createString(self.toString(radix)); }

// -------------------------------------------------------------------------
// From float.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="float_rem"]
// fun rem(Float, Float): Float
export function float_rem(self: number, other: number): number { return self % other; }

// @Extern [lib="core", name="float_is_nan"]
// fun is_nan(Float): Boolean
export function float_is_nan(self: number): boolean { return Number.isNaN(self); }

// @Extern [lib="core", name="float_is_finite"]
// fun is_finite(Float): Boolean
export function float_is_finite(self: number): boolean { return Number.isFinite(self); }

// @Extern [lib="core", name="float_is_integer"]
// fun is_integer(Float): Boolean
export function float_is_integer(self: number): boolean { return Number.isInteger(self); }

// @Extern [lib="core", name="float_is_safe_integer"]
// fun is_safe_integer(Float): Boolean
export function float_is_safe_integer(self: number): boolean { return Number.isSafeInteger(self); }

// @Extern [lib="core", name="float_to_string"]
// fun to_string(Float): String
export function float_to_string(self: number): number { return createString(String(self)); }

// -------------------------------------------------------------------------
// From intrinsic_memory.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="memory_get_memory"]
// fun get_memory(): MemoryArena
export function memory_get_memory(): number { return 4; }

// @Extern [lib="core", name="memory_copy_internal"]
// fun memory_copy_internal(Int, Int, Int): Nothing
export function memory_copy_internal(target: number, source: number, len: number): void { memcopy(target, source, len); }

// @Extern [lib="core", name="memory_alloc_trace"]
// fun memory_alloc_trace(Int, Int): Nothing
export function memory_alloc_trace(amount: number, ptr: number): void {
    trace.trace_alloc(amount);
}

// @Extern [lib="core", name="memory_alloc_trace"]
// fun memory_alloc_error(amount: Int, return_ptr: Int, capacity: Int, len: Int, bytes_ptr: Int) {}
export function memory_alloc_error(amount: number, return_ptr: number, capacity: number, len: number, bytes_ptr: number): void {
    debugger;
    throw new Error(`Failed to allocate ${amount} bytes\n - return_ptr: ${return_ptr}, capacity: ${capacity}, len: ${len}, bytes_ptr: ${bytes_ptr}`);
}

// -------------------------------------------------------------------------
// From memory_arena.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="memory_copy_within"]
// fun copy_within(MemoryArena, Ptr<Byte>, Ptr<Byte>, Int): Nothing
export function memory_copy_within(_self: number, dst: number, src: number, byte_len: number): void { memcopy(dst, src, byte_len); }

// @Extern [lib="core", name="memory_read_byte"]
// fun read_byte(MemoryArena, Ptr<Byte>): Byte
export function memory_read_byte(_self: number, ptr: number): number {
    // console.log({ptr, value: getInt(ptr), byte: (getInt(ptr) & 0xFF)});
    const int = getInt(ptr);
    if (ptr % 4 == 0) return int & 0xFF;
    if (ptr % 4 == 1) return (int >> 8) & 0xFF;
    if (ptr % 4 == 2) return (int >> 16) & 0xFF;
    return (int >> 24) & 0xFF;
}

// @Extern [lib="core", name="memory_write_byte"]
// fun write_byte(MemoryArena, Ptr<Byte>, Byte): Nothing
export function memory_write_byte(_self: number, ptr: number, value: number): void { return setByte(ptr, value); }

// @Extern [lib="core", name="memory_read_int"]
// fun read_int(MemoryArena, Ptr<Int>): Int
export function memory_read_int(_self: number, ptr: number): number { return getInt(ptr); }

// @Extern [lib="core", name="memory_write_int"]
// fun write_int(MemoryArena, Ptr<Int>, Int): Nothing
export function memory_write_int(_self: number, ptr: number, value: number): void { return setInt(ptr, value); }

// @Extern [lib="core", name="memory_read_float"]
// fun read_float(MemoryArena, Ptr<Float>): Float
export function memory_read_float(_self: number, ptr: number): number { return getFloat(ptr); }

// @Extern [lib="core", name="memory_write_float"]
// fun write_float(MemoryArena, Ptr<Float>, Float): Nothing
export function memory_write_float(_self: number, ptr: number, value: number): void { return setFloat(ptr, value); }

// @Extern [lib="core", name="memory_dump"]
// fun dump(MemoryArena): Nothing
export function memory_dump(base_ptr: number): void {
    console.debug(`# Memory: len = ${getInt(base_ptr)}, bytes = ${getInt(base_ptr + 4) + 4}, capacity = ${getInt(base_ptr + 8)}, base = ${base_ptr}`)
    console.debug('Memory dump:\n' + dumpMemory(getInt(base_ptr + 4) + 4, getInt(base_ptr)));
}

// -------------------------------------------------------------------------
// From ptr.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="ptr_ptr_of"]
// fun ptr_of(#Value): Ptr<#Value>
export function ptr_ptr_of(value: number): number { return value; }

// @Extern [lib="core", name="ptr_ptr_from_address"]
// fun ptr_from_address(Int): Ptr<#Value>
export function ptr_ptr_from_address(address: number): number { return address; }

// @Extern [lib="core", name="ptr_as_ref"]
// fun unsafe_as_ref(Ptr<#Value>): #Value
export function ptr_as_ref(self: number): number { return self; }

// @Extern [lib="core", name="ptr_get_address"]
// fun get_address(Ptr<#Value>): Int
export function ptr_get_address(self: number): number { return self; }

// @Extern [lib="core", name="ptr_unsafe_cast"]
// fun unsafe_cast(Ptr<#Value>): Ptr<#Target>
export function ptr_unsafe_cast(self: number): number { return self; }

// @Extern [lib="core", name="ptr_write"]
// fun write(Ptr<Ptr<#Value>>, Ptr<#Value>): Nothing
export function ptr_write(self: number, value: number): void { setInt(self, value); }

// @Extern [lib="core", name="ptr_read"]
// fun read(Ptr<Ptr<#Value>>): Ptr<#Value>
export function ptr_read(self: number): number { return getInt(self); }

// @Extern [lib="core", name="ptr_to_raw_array"]
// fun to_raw_array(Ptr<#Value>): RawArray<#Value>
export function ptr_to_raw_array(self: number): number { return self; }

// -------------------------------------------------------------------------
// From exponential_utils.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="math_pow"]
// fun Math::pow(Float, Float): Float
export function math_pow(x: number, y: number): number { return Math.pow(x, y); }

// @Extern [lib="core", name="math_sqrt"]
// fun Math::sqrt(Float): Float
export function math_sqrt(x: number): number { return Math.sqrt(x); }

// @Extern [lib="core", name="math_cbrt"]
// fun Math::cbrt(Float): Float
export function math_cbrt(x: number): number { return Math.cbrt(x); }

// @Extern [lib="core", name="math_exp"]
// fun Math::exp(Float): Float
export function math_exp(x: number): number { return Math.exp(x); }

// @Extern [lib="core", name="math_expm1"]
// fun Math::expm1(Float): Float
export function math_expm1(x: number): number { return Math.expm1(x); }

// @Extern [lib="core", name="math_log"]
// fun Math::log(Float): Float
export function math_log(x: number): number { return Math.log(x); }

// @Extern [lib="core", name="math_log1p"]
// fun Math::log1p(Float): Float
export function math_log1p(x: number): number { return Math.log1p(x); }

// @Extern [lib="core", name="math_log10"]
// fun Math::log10(Float): Float
export function math_log10(x: number): number { return Math.log10(x); }

// @Extern [lib="core", name="math_log2"]
// fun Math::log2(Float): Float
export function math_log2(x: number): number { return Math.log2(x); }

// -------------------------------------------------------------------------
// From trigonometry_utils.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="math_sin"]
// fun Math::sin(Float): Float
export function math_sin(x: number): number { return Math.sin(x); }

// @Extern [lib="core", name="math_sinh"]
// fun Math::sinh(Float): Float
export function math_sinh(x: number): number { return Math.sinh(x); }

// @Extern [lib="core", name="math_asin"]
// fun Math::asin(Float): Float
export function math_asin(x: number): number { return Math.asin(x); }

// @Extern [lib="core", name="math_asinh"]
// fun Math::asinh(Float): Float
export function math_asinh(x: number): number { return Math.asinh(x); }

// @Extern [lib="core", name="math_cos"]
// fun Math::cos(Float): Float
export function math_cos(x: number): number { return Math.cos(x); }

// @Extern [lib="core", name="math_cosh"]
// fun Math::cosh(Float): Float
export function math_cosh(x: number): number { return Math.cosh(x); }

// @Extern [lib="core", name="math_acos"]
// fun Math::acos(Float): Float
export function math_acos(x: number): number { return Math.acos(x); }

// @Extern [lib="core", name="math_acosh"]
// fun Math::acosh(Float): Float
export function math_acosh(x: number): number { return Math.acosh(x); }

// @Extern [lib="core", name="math_tan"]
// fun Math::tan(Float): Float
export function math_tan(x: number): number { return Math.tan(x); }

// @Extern [lib="core", name="math_tanh"]
// fun Math::tanh(Float): Float
export function math_tanh(x: number): number { return Math.tanh(x); }

// @Extern [lib="core", name="math_atan"]
// fun Math::atan(Float): Float
export function math_atan(x: number): number { return Math.atan(x); }

// @Extern [lib="core", name="math_atanh"]
// fun Math::atanh(Float): Float
export function math_atanh(x: number): number { return Math.atanh(x); }

// @Extern [lib="core", name="math_atan2"]
// fun Math::atan2(Float, Float): Float
export function math_atan2(y: number, x: number): number { return Math.atan2(y, x); }

// -------------------------------------------------------------------------
// From random.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="random_get_initial_seed"]
// fun random_get_initial_seed(): Int
export function random_get_initial_seed(): number { return (Math.random() * 0x7FFFFFFF) | 0; }

// -------------------------------------------------------------------------
// From ordering.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="string_get_ordering_internal"]
// fun get_ordering_internal(String, String): Int
export function string_get_ordering_internal(self: number, other: number): number {
    if (self === other) return 0;
    const aStr = getString(self);
    const bStr = getString(other);
    return aStr.localeCompare(bStr);
}

// -------------------------------------------------------------------------
// From string.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="string_codepoint_len"]
// fun codepoint_len(String): Int
export function string_codepoint_len(self: number): number {
    const str = getString(self);
    const chars = Array.from(str);
    return chars.length;
}

// @Extern [lib="core", name="string_get_codepoint"]
// fun get_codepoint(String, Int): Int
export function string_get_codepoint(self: number, index: number): number {
    const str = getString(self);
    const chars = Array.from(str);
    if (index < 0 || index >= chars.length) {
        throw new Error(`Index out of bounds, index: ${index}, length: ${chars.length}`);
    }
    const char = chars[index];
    return char.codePointAt(0) || 0;
}

// @Extern [lib="core", name="string_concat_string"]
// fun concat(String, String): String
export function string_concat_string(self: number, other: number): number {
    const s1 = getString(self);
    const s2 = getString(other);
    return createString(s1 + s2);
}

// @Extern [lib="core", name="string_concat_char"]
// fun concat(String, Char): String
export function string_concat_char(self: number, char: number): number {
    return createString(getString(self) + String.fromCodePoint(getInt(char)));
}

// @Extern [lib="core", name="string_is_equal"]
// fun is_equal(String, String): Boolean
export function string_is_equal(self: number, other: number): boolean { return getString(self) === getString(other); }

// @Extern [lib="core", name="string_is_not_equal"]
// fun is_not_equal(String, String): Boolean
export function string_is_not_equal(self: number, other: number): boolean { return getString(self) !== getString(other); }

// @Extern [lib="core", name="string_replace"]
// fun replace(String, String, String): String
export function string_replace(self: number, find: number, replacement: number): number {
    const base = getString(self);
    const findStr = getString(find);
    const replacementStr = getString(replacement);
    return createString(base.replaceAll(findStr, replacementStr));
}

// @Extern [lib="core", name="string_sub_string"]
// fun sub_string(String, Int, Int): String
export function string_sub_string(self: number, start: number, end: number): number {
    return createString(getString(self).substring(start, end));
}

// @Extern [lib="core", name="string_to_ascii_lowercase"]
// fun to_ascii_lowercase(String): String
export function string_to_ascii_lowercase(self: number): number {
    return createString(getString(self).toLowerCase());
}

// @Extern [lib="core", name="string_to_ascii_uppercase"]
// fun to_ascii_uppercase(String): String
export function string_to_ascii_uppercase(self: number): number {
    return createString(getString(self).toUpperCase());
}

//
// -------------------------------------------------------------------------
// From instant.nitro
// -------------------------------------------------------------------------
//
// At src/main/nitro/core/time/instant.nitro(instant.nitro:143)
// @Extern [lib="core", name="instant_now"]
// fun Instant::now(): Instant
export function instant_now(): number {
    let preciseClockMs = performance.now() + performance.timeOrigin;
    let decimals = preciseClockMs % 1000;
    let seconds = BigInt((preciseClockMs / 1000)|0);
    let nanos = decimals * 1_000_000;

    let ptr = alloc(PTR * 3);
    setLong(STRUCT_HEADER + ptr, seconds);
    setInt(STRUCT_HEADER + ptr + 2 * PTR, nanos);
    return ptr;
}
//
// At src/main/nitro/core/time/instant.nitro(instant.nitro:147)
// @Extern [lib="core", name="instant_format_to_iso8601"]
// fun format_to_iso8601(Instant): String
export function instant_format_to_iso8601(self: number): number {
    let seconds = Number(getLong(self))
    let nanos = getInt(self + 2)
    return createString(new Date(seconds + nanos / 1_000_000_000).toISOString());
}

// -------------------------------------------------------------------------
// From file_path.nitro
// -------------------------------------------------------------------------

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:16)
// @Extern [lib="core", name="file_path_join"]
// fun FilePath::join(String, String): String
export function file_path_join(dir: number, sub: number): number {
    return createString(fs.join(getString(dir), getString(sub)));
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:19)
// @Extern [lib="core", name="file_path_dirname"]
// fun FilePath::directory_name(String): String
export function file_path_dirname(path: number): number {
    return createString(fs.dirname(getString(path)));
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:22)
// @Extern [lib="core", name="file_path_basename"]
// fun FilePath::base_name(String): String
export function file_path_basename(path: number): number {
    return createString(fs.basename(getString(path)));
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:16)
// @Extern [lib="core", name="file_path_read_text"]
// fun read_text(FilePath): Result<String, IoError>
export function file_path_read_text(self: number): number {
    try {
        const path = getInt(self)
        const data = fs.readTextFileSync(getString(path))
        return mem.program.io_error_result_ok(createString(data));
    } catch(e) {
        console.error(e);
        return mem.program.io_error_result_error(
            createString(e.message),
            createString("Exception while reading file")
        );
    }
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:19)
// @Extern [lib="core", name="file_path_write_text"]
// fun write_text(FilePath, String): Result<Nothing, IoError>
export function file_path_write_text(self: number, text: number): number {
    try {
        const path = getInt(self)
        fs.writeTextFileSync(getString(path), getString(text))
        return mem.program.io_error_result_ok(0);
    } catch(e) {
        console.error(e);
        return mem.program.io_error_result_error(createString(e.message), createString("Exception while writing file"));
    }
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:22)
// @Extern [lib="core", name="file_path_exists"]
// fun exists(FilePath): Result<Boolean, IoError>
export function file_path_exists(self: number): boolean {
    try {
        const path = getString(getInt(self));
        const exits = fs.fileExistsSync(path);
        return mem.program.io_error_result_ok(exits ? 1 : 0);
    } catch(e) {
        console.error(e);
        return mem.program.io_error_result_error(createString(e.message), createString("Exception while writing file"));
    }
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:33)
// @Extern [lib="core", name="file_path_last_modified"]
// fun FilePath.last_modified(): Result<Instant, IoError> {}
export function file_path_last_modified(self: number): boolean {
    try {
        const path = getString(getInt(self));
        // Float representing the last modified time in seconds
        const unix_time = fs.getLastModifiedTime(path);

        const seconds = BigInt(unix_time - (unix_time % 1.0));
        const nanos = (unix_time % 1.0) * 1000000000;

        let instant = alloc(PTR * 3);
        setLong(STRUCT_HEADER + instant, seconds);
        setInt(STRUCT_HEADER + instant + 2 * PTR, nanos);

        return mem.program.io_error_result_ok(instant);
    } catch(e) {
        console.error(e);
        return mem.program.io_error_result_error(createString(e.message), createString("Exception while writing file"));
    }
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:33)
// @Extern [lib="core", name="file_path_unsafe_debug_last_modified"]
// fun FilePath.unsafe_debug_last_modified(): Int {}
export function file_path_unsafe_debug_last_modified(self: number): number {
    try {
        const path = getString(getInt(self));
        // Float representing the last modified time in seconds
        const unix_time = fs.getLastModifiedTime(path);
        return unix_time | 0;
    } catch(e) {
        console.error(e);
        return 0;
    }
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:36)
// @Extern [lib: "core", name: "file_path_unsafe_debug_read_bytes"]
// fun FilePath.unsafe_debug_read_bytes(): Array<Byte> {}
export function file_path_unsafe_debug_read_bytes(self: number): boolean {
    try {
        const path = getString(getInt(self));
        const data = fs.readFileSync(path);
        const wasm_array = alloc(PTR + data.length);
        setInt(wasm_array, data.length);
        mem.u8.set(data, PTR + wasm_array);

        return wasm_array;
    } catch(e) {
        console.error(e);
        return false;
    }
}

// At src/main/nitro/core/fs/file_path.nitro(file_path.nitro:40)
// @Extern [lib="core", name="file_path_unsafe_debug_write_bytes"]
// fun FilePath.unsafe_debug_write_bytes(bytes: Array<Byte>, len: Int): Boolean {}
export function file_path_unsafe_debug_write_bytes(self: number, bytes: number, len: number): boolean {
    try {
        const path = getString(getInt(self))
        const offset = bytes + 4
        const array = mem.u8.subarray(offset, offset + len)

        fs.writeFileSync(path, array)
        return true;
    } catch(e) {
        console.error(e);
        return false;
    }
}


// -------------------------------------------------------------------------
// From debug.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="println_string"]
// fun println(String): String
export function println_string(i: number): number { console.debug(getString(i)); return i; }

// @Extern [lib="core", name="println_int"]
// fun println(Int): Int
export function println_int(i: number): number { console.debug(i|0); return i; }

// @Extern [lib="core", name="println_long"]
// fun println(Long): Long
export function println_long(i: bigint): bigint { console.debug(i); return i; }

// @Extern [lib="core", name="println_float"]
// fun println(Float): Float
export function println_float(i: number): number { console.debug(i); return i; }

// @Extern [lib="core", name="println_boolean"]
// fun println(Boolean): Boolean
export function println_boolean(i: boolean): boolean { console.debug(!!i); return i; }

// -------------------------------------------------------------------------
// From logger.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="logger_get_logger"]
// fun get_logger(): Logger
export function logger_get_logger(): number { return 0; }

// @Extern [lib="core", name="logger_log"]
// fun log(Logger, String, String): Nothing
export function logger_log(_self: number, level: number, message: number): void {
    const levelStr = getString(level);
    const msgStr = getString(message);

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

// -------------------------------------------------------------------------
// From check.nitro
// -------------------------------------------------------------------------

// @Extern [lib="core", name="run_test"]
// fun run_test(String, String): Nothing
export function run_test(internal_function_name: number, test_name: number): boolean {
    const internal_name = getString(internal_function_name);
    const msg = getString(test_name);

    console.debug(`--------------------------------------------------------------------------------`);
    console.debug(`# Running test (${internal_name}): ${msg}`);
    try {
        const start = performance.now();
        // @ts-ignore
        mem.program[internal_name]();
        const end = performance.now();
        console.debug(`%c# Test passed (${end - start} ms)`, 'color: green;');
        return true;
    } catch (e) {
        console.error(`%c# Test failed: ${e}`, 'color: red;');
        return false;
    }
}

// -------------------------------------------------------------------------
// From trace.nitro
// -------------------------------------------------------------------------


// At src/main/nitro/core/debug/trace.nitro(trace.nitro:4)
// @Extern [lib="core", name="trace_enter"]
// fun Trace::trace_enter(String): Nothing
export function trace_enter(func_info: number): void {
    assert(func_info);
    trace.trace_enter(getString(func_info));
}

// At src/main/nitro/core/debug/trace.nitro(trace.nitro:7)
// @Extern [lib="core", name="trace_exit"]
// fun Trace::trace_exit(String): Nothing
export function trace_exit(func_info: number): void {
    trace.trace_exit();
}

// At src/main/nitro/core/debug/trace.nitro(trace.nitro:10)
// @Extern [lib="core", name="trace_get_stacktrace"]
// fun Trace::trace_get_stacktrace(): number
export function trace_get_stacktrace(): String { return createString(trace.trace_get_stacktrace()); }

// At src/main/nitro/core/debug/trace.nitro(trace.nitro:13)
// @Extern [lib="core", name="trace_print_stack_trace"]
// fun Trace::print_stack_trace(): Nothing
export function trace_print_stack_trace(): void { trace.trace_print_stack_trace(); }

// -------------------------------------------------------------------------
// From runtime.nitro
// -------------------------------------------------------------------------

// At src/main/nitro/core/debug/runtime.nitro(runtime.nitro:19)
// @Extern [lib: "core", name: "runtime_program_arg_count"]
// fun Runtime::program_arg_count(): Int
export function runtime_program_arg_count(): number { return mem.program_args.length; }

// At src/main/nitro/core/debug/runtime.nitro(runtime.nitro:22)
// @Extern [lib: "core", name: "runtime_program_arg_get"]
// fun Runtime::program_arg_get(Int): String
export function runtime_program_arg_get(index: number): number { return createString(mem.program_args[index] ?? ''); }

// @formatter:on
