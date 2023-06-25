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
  u16: new Uint16Array(),
  u8: new Uint8Array(),
}

export function alloc(amount: number): number {
  let next = getInt(ALLOC_NEXT);

  const pad = (PTR_SIZE - next) % PTR_SIZE;
  next += (pad < 0) ? pad + PTR_SIZE : pad;

  // Increment next free slot
  setInt(ALLOC_NEXT, next + amount);

  // console.log(`alloc(${amount}) => ${next} ..< ${next + amount}`);
  return next;
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

export function float_rem(a: number, b: number): number {
  return a % b;
}

export function println_int(val: number) {
  console.log(val);
}

export function println_float(val: number) {
  console.log(val);
}

export function println_string(ptr: number) {
  // console.log('println_string', ptr);
  console.log(getString(ptr));
}

export function println_string_list(ptr: number) {
  // console.log('println_string_list', ptr);
  console.log(getStringList(ptr));
}

export function list_add(ptr: number, value: number) {
  // console.log('list_add(', ptr, value, ')');
  assert(ptr);

  const type = getInt(ptr);
  let data = getInt(ptr + LIST_DATA_FIELD);
  let len = getInt(ptr + LIST_LEN_FIELD);
  let capacity = getInt(ptr + LIST_CAPACITY_FIELD);

  // console.log("Pre add", {type, data, len, capacity});

  if ((len + 1) * PTR_SIZE > capacity) {
    // Double or initialize to 16
    const newCapacity = Math.max(capacity, 8 * PTR_SIZE) * 2;
    const newData = alloc(newCapacity);

    mem.u8.copyWithin(newData, data, data + capacity);
    data = newData;
    capacity = newCapacity;

    setInt(ptr + LIST_DATA_FIELD, data);
    setInt(ptr + LIST_CAPACITY_FIELD, capacity);

    // console.log("Expanded List", {type, data, len, capacity});
  }

  const offset = len * PTR_SIZE;
  len++;

  setInt(data + offset, value);
  setInt(ptr + LIST_LEN_FIELD, len);
}

export function list_get(ptr: number, index: number): number {
  // console.log('list_get(', ptr, ')');
  const data = getInt(ptr + LIST_DATA_FIELD);
  const len = getInt(ptr + LIST_LEN_FIELD);

  if (index < 0 || index >= len) {
    throw new Error(`Index out of bounds: index=${index}, len=${len}`);
  }

  return getInt(data + index * PTR_SIZE);
}

export function list_set(ptr: number, index: number, value: number) {
  // console.log('list_get(', ptr, ')');
  const data = getInt(ptr + LIST_DATA_FIELD);
  const len = getInt(ptr + LIST_LEN_FIELD);

  if (index < 0 || index >= len) {
    throw new Error(`Index out of bounds: index=${index}, len=${len}`);
  }

  setInt(data + index * PTR_SIZE, value);
}


export function int_get_ordering(a: number, b: number): number {
  if (a == b) {
    return 0;
  } else if (a > b) {
    return 1;
  } else {
    return -1;
  }
}

export function is_less(ord: number): number {
  return (ord === -1) ? 1 : 0;
}

export function is_less_or_equals(ord: number): number {
  return (ord === -1 || ord === 0) ? 1 : 0;
}

export function is_equals(ord: number): number {
  return (ord === 0) ? 1 : 0;
}

export function is_greater_or_equals(ord: number): number {
  return (ord === 1 || ord === 0) ? 1 : 0;
}

export function is_greater(ord: number): number {
  return (ord === 1) ? 1 : 0;
}

function getString(ptr: number): string {
  assert(ptr);

  const len = getInt(ptr);
  const bytes = mem.u8.subarray(ptr + 4, ptr + 4 + len);
  const decoder = new TextDecoder('utf-8', {fatal: true});
  return decoder.decode(bytes);
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

function assert(ptr: number) {
  if (ptr === 0) {
    throw new Error("Null was here");
  }
}
