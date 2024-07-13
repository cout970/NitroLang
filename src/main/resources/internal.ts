// deno-lint-ignore no-inferrable-types
export const PTR: number = 4;

export const mem = {
  i32: new Int32Array(),
  u32: new Uint32Array(),
  f32: new Float32Array(),
  u16: new Uint16Array(),
  u8: new Uint8Array(),
  program: null as unknown as WebAssembly.Exports,
}

export const fs = {
    isSupported: false,
    readTextFileSync: (path: string): string => {
       throw new Error('fs.readTextFileSync not implemented');
    },
    writeTextFileSync: (filename: string, data: string) => {
        throw new Error('fs.writeTextFileSync not implemented');
    },
    fileExistsSync: (path: string): boolean => {
        throw new Error('fs.fileExistsSync not implemented');
    }
}

export function getInt(ptr: number): number {
  assert(ptr);
  return mem.u32[(ptr / 4) | 0];
}

export function setInt(ptr: number, value: number) {
  assert(ptr);
  mem.u32[(ptr / 4) | 0] = value | 0;
}

export function getLong(ptr: number): bigint {
  assert(ptr);
  const b7 = mem.u8[ptr + 7];
  const b6 = mem.u8[ptr + 6];
  const b5 = mem.u8[ptr + 5];
  const b4 = mem.u8[ptr + 4];
  const b3 = mem.u8[ptr + 3];
  const b2 = mem.u8[ptr + 2];
  const b1 = mem.u8[ptr + 1];
  const b0 = mem.u8[ptr];

  return BigInt(b7) << BigInt(56) |
    BigInt(b6) << BigInt(48) |
    BigInt(b5) << BigInt(40) |
    BigInt(b4) << BigInt(32) |
    BigInt(b3) << BigInt(24) |
    BigInt(b2) << BigInt(16) |
    BigInt(b1) << BigInt(8) |
    BigInt(b0);
}

export function setLong(ptr: number, value: bigint) {
  assert(ptr);
  const b7 = Number((value >> BigInt(56)) & BigInt(0xFF));
  const b6 = Number((value >> BigInt(48)) & BigInt(0xFF));
  const b5 = Number((value >> BigInt(40)) & BigInt(0xFF));
  const b4 = Number((value >> BigInt(32)) & BigInt(0xFF));
  const b3 = Number((value >> BigInt(24)) & BigInt(0xFF));
  const b2 = Number((value >> BigInt(16)) & BigInt(0xFF));
  const b1 = Number((value >> BigInt(8)) & BigInt(0xFF));
  const b0 = Number(value & BigInt(0xFF));

  mem.u8[ptr + 7] = b7;
  mem.u8[ptr + 6] = b6;
  mem.u8[ptr + 5] = b5;
  mem.u8[ptr + 4] = b4;
  mem.u8[ptr + 3] = b3;
  mem.u8[ptr + 2] = b2;
  mem.u8[ptr + 1] = b1;
  mem.u8[ptr] = b0;
}

export function getFloat(ptr: number): number {
  assert(ptr);
  return mem.f32[(ptr / 4) | 0];
}

export function setFloat(ptr: number, value: number) {
  assert(ptr);
  mem.f32[(ptr / 4) | 0] = value;
}

export function getByte(ptr: number): number {
  assert(ptr);
  return mem.u8[ptr | 0];
}

export function setByte(ptr: number, value: number) {
  assert(ptr);
  mem.u8[ptr | 0] = value & 0xFF;
}

export function getShort(ptr: number): number {
  assert(ptr);
  return mem.u16[ptr | 0];
}

export function setShort(ptr: number, value: number) {
  assert(ptr);
  mem.u16[ptr | 0] = value & 0xFF;
}

export function assert(ptr: number) {
  if (ptr === 0) {
    throw new Error("Null pointer exception");
  }
}

export function alloc(bytes: number): number {
  if (mem.program.memory_alloc_internal) {
    return mem.program.memory_alloc_internal(bytes);
  }

  // Port of: `fun MemoryArena.alloc_bytes(bytes: Int): Ptr<Byte> {}`
  // from memory_arena.nitro
  // Memory: len = getInt(mem_base), bytes = getInt(mem_base + 4), capacity = getInt(mem_base + 8)
  //
  const ptr_size = PTR;
  const mem_base = 4;
  let next = getInt(mem_base);

  const pad = (ptr_size - next) % ptr_size;
  next = next + (pad < 0 ? pad + ptr_size : pad);

  let inUse = next + bytes;
  setInt(mem_base, inUse);
  const start = getInt(mem_base + 4) + 4;
  const result = start + next;

  const end = result + bytes;
  const capacity = getInt(mem_base + 8);

  if (end > start + capacity) {
    throw new Error(`Out of memory while trying to allocate ${bytes} bytes, current usage is ${inUse} bytes`);
  }

  // console.debug(`alloc(${bytes}) => 0x${result.toString(16).padStart(4, '0')} (${result})`);
  return result;
}

export function createString(value: string): number {
  // Heap:
  // - 4 byte: String len in bytes
  // - 4 byte: Pointer to Array<Byte> in heap or data section
  // - 4 byte: String hash
  const ptr = alloc(PTR * 3);
  const len_ptr = ptr;
  const bytes_ptr = ptr + PTR;
  const hash_ptr = ptr + PTR + PTR;

  //console.debug(`createString(${value}) => 0x${ptr.toString(16).padStart(4, '0')}`);

  const uint8array = (new TextEncoder()).encode(value);
  const data_ptr = alloc(PTR + uint8array.length);
  mem.u8.set(uint8array, data_ptr + PTR);
  setInt(data_ptr, uint8array.length);

  setInt(len_ptr, uint8array.length);
  setInt(bytes_ptr, data_ptr);
  setInt(hash_ptr, -1);
  return ptr;
}

export function getString(ptr: number): string {
  assert(ptr);
  // Heap:
  // - 4 byte: String len in bytes
  // - 4 byte: Pointer to Array<Byte> in heap or data section
  // - 4 byte: String hash
  const len = getInt(ptr);
  const bytes = getInt(ptr + PTR);

  // console.log('getString', {ptr, len, bytes})
  // dumpMemory(bytes, len);

  const contents = mem.u8.subarray(bytes + PTR, bytes + PTR + len);

  const textDecoder = new TextDecoder('utf-8', {fatal: true});
  return textDecoder.decode(contents);
}

export function memcopy(dst: number, src: number, len: number) {
  assert(dst);
  assert(src);
  mem.u8.copyWithin(dst, src, src + len);
}

function byteToString(byte: number): string {
  return byte < 32 ? '.' : String.fromCharCode(byte);
}

export function dumpMemory(ptr: number, len: number): string {
  let str = '';

  let pad = (PTR - ptr) % PTR;
  pad = (pad < 0) ? pad + PTR : pad;
  ptr -= pad
  len += pad + PTR

  // Do not exceed the memory size
  len = Math.min(len, mem.u8.length - ptr);

  for (let i = ptr; i < ptr + len; i += 4) {
    str += `${i.toString().padStart(8, '0')} `;
    str += `0x${i.toString(16).padStart(8, '0')} `;
    str += `- ${mem.u8[i].toString(16).padStart(2, '0')} `;
    str += `${mem.u8[i + 1].toString(16).padStart(2, '0')} `;
    str += `${mem.u8[i + 2].toString(16).padStart(2, '0')} `;
    str += `${mem.u8[i + 3].toString(16).padStart(2, '0')} `;
    str += byteToString(mem.u8[i]);
    str += byteToString(mem.u8[i + 1]);
    str += byteToString(mem.u8[i + 2]);
    str += byteToString(mem.u8[i + 3]);
    // str += ` 0x${mem.u32[(i / 4) | 0].toString(16).padStart(8, '0')}, `;
    str += ` i32: ${mem.i32[(i / 4) | 0].toString().padStart(10, ' ')},`;
    str += ` f32: ${mem.f32[(i / 4) | 0]}`;
    str += '\n';
  }
  return str;
}
