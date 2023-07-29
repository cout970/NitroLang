const PTR_SIZE = 4;

// Bup allocator, stores a pointer to the next free byte on the addr 0
const ALLOC_NEXT = 4;

// struct List
const LIST_TYPE_FIELD = 0;
const LIST_DATA_FIELD = PTR_SIZE;
const LIST_LEN_FIELD = PTR_SIZE * 2;
const LIST_CAPACITY_FIELD = PTR_SIZE * 3;

export const mem = {
  u32: new Uint32Array(),
  f32: new Float32Array(),
  u16: new Uint16Array(),
  u8: new Uint8Array(),
}

// memory.nl

export function memory_alloc(amount: number): number {
    return alloc(amount);
}

export function memory_write_int(ptr: number, value: number) { setInt(ptr, value|0); }
export function memory_write_boolean(ptr: number, value: number) { setInt(ptr, value|0); }
export function memory_write_float(ptr: number, value: number) { setInt(ptr, value); }
export function memory_write_generic(ptr: number, raw: number, type: number) {
    setInt(ptr, raw);
    setInt(ptr + PTR_SIZE , type);
}

export function memory_read_int(ptr: number): number { setInt(ptr, value|0); }
export function memory_read_boolean(ptr: number): number { setInt(ptr, value|0); }
export function memory_read_float(ptr: number): number { setInt(ptr, value); }
export function memory_read_generic(ptr: number) {
    assert(ptr);
    return [getInt(ptr), getInt(ptr + PTR_SIZE)];
}

export function memory_copy(src: number, dst: number, len: number) {
    mem.u8.copyWithin(dst, src, src + len);
}

// string.nl

export function string_len(a: number): number {
    let str = getString(a);
    let chars = Array.from(str);
    return chars.length;
}

export function string_get_codepoint(a: number, index: number): number {
    let str = getString(a);
    let chars = Array.from(str);
    let char = chars[index];

    return char.codePointAt(0);
}

export function string_concat_string(a: number, b: number): number {
    return createString(getString(a) + getString(b))
}

export function string_replace(a: number, b: number, c: number): number {
    let base = getString(a);
    let find = getString(b);
    let replacement = getString(c);

    return createString(base.replaceAll(find, replacement))
}

export function int_to_string(int: number): number {
    return createString(String(int | 0));
}

export function float_to_string(float: number): number {
    return createString(String(float));
}

export function any_to_string(ptr: number, ty: number): number {
    return createString("#" + ptr + ":" + ty);
}

// intrinsic.nl

export function alloc(amount: number): number {
  let next = getInt(ALLOC_NEXT);

  const pad = (PTR_SIZE - next) % PTR_SIZE;
  next += (pad < 0) ? pad + PTR_SIZE : pad;

  // Increment next free slot
  setInt(ALLOC_NEXT, next + amount);

  // console.log(`alloc(${amount}) => ${next} ..< ${next + amount}`);
  return next;
}

export function is_variant(ptr: number, expected_variant: number): number {
    // console.log('is_variant', {ptr, variant: getInt(ptr), expected_variant});
    assert(ptr);
    const found: number = getInt(ptr);
    return (found === expected_variant) ? 1 : 0;
}

export function check_cast(ptr: number, expected_type: number): number {
  assert(ptr);
  const found: number = getInt(ptr);

  if (found !== expected_type) {
    throw new Error(`Cast error: expected ${expected_type}, found ${found}`);
  }

  return ptr;
}

export function choose(cond: number, a: number, b: number): number {
  return cond ? a : b;
}

// float.nl

export function float_rem(a: number, b: number): number {
  return a % b;
}

// console.nl

export function println_unit(_: number) {
  console.log('()');
}

export function println_boolean(val: number) {
  console.log(val !== 0);
}

export function println_int(val: number) {
  console.log(val);
}

export function println_float(val: number) {
  console.log(val);
}

export function println_string(ptr: number) {
  console.log(getString(ptr));
}

export function eprintln_string(ptr: number) {
  console.error(getString(ptr));
}

export function println_string_list(ptr: number) {
  // console.log('println_string_list', ptr);
  console.log(getStringList(ptr));
}

// ordering.nl

export function string_get_ordering_internal(a: number, b: number): number {
  if(a === b) return 0;

  let aStr = getString(a);
  let bStr = getString(b);

  return aStr.localeCompare(bStr);
}

// internal

function getString(ptr: number): string {
  assert(ptr);

  const len = getInt(ptr);
  const bytes = mem.u8.subarray(ptr + 4, ptr + 4 + len);

  return (new TextDecoder('utf-8', {fatal: true})).decode(bytes);
}

function createString(value: string): number {
    const uint8array = (new TextEncoder()).encode(value);

    const ptr = memory_alloc(uint8array.length + 4);
    setInt(ptr, uint8array.length);
    mem.u8.set(uint8array, ptr + 4);

    return ptr;
}

function getStringList(ptr: number) {
  assert(ptr);

  const data = getInt(ptr + LIST_DATA_FIELD);
  const len = getInt(ptr + LIST_LEN_FIELD);
  const result = [];

  for (let i = 0; i < len; i++) {
    const strPtr = getInt(data + i * PTR_SIZE);
    result.push(getString(strPtr));
  }

  return result;
}

function getInt(ptr: number): number {
  assert(ptr);

  return mem.u32[(ptr / 4) | 0];
}

function setInt(ptr: number, value: number) {
  assert(ptr);

  // console.debug(`- setInt(${ptr}, ${value})`, {prev: getInt(ptr)});
  mem.u32[(ptr / 4) | 0] = value | 0;
}

function getFloat(ptr: number): number {
  assert(ptr);

  return mem.f32[(ptr / 4) | 0];
}

function setFloat(ptr: number, value: number) {
  assert(ptr);

  // console.debug(`- setFloat(${ptr}, ${value})`, {prev: setFloat(ptr)});
  mem.f32[(ptr / 4) | 0] = value;
}

function assert(ptr: number) {
  if (ptr === 0) {
    throw new Error("Null was here");
  }
}
