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

export function getInt(ptr: number): number {
    assert(ptr);
    return mem.u32[(ptr / 4) | 0];
}

export function setInt(ptr: number, value: number) {
    assert(ptr);
    mem.u32[(ptr / 4) | 0] = value | 0;
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
    const func = mem.program.memory_alloc_internal as CallableFunction;
    const res = func(bytes);
    // console.debug(`alloc(${bytes}) => 0x${res.toString(16).padStart(4, '0')}`);
    return res
}

export function createString(value: string): number {
    // Heap:
    // - 4 byte: String len in bytes
    // - 4 byte: Pointer to string characters in heap or data section
    const ptr = alloc(PTR * 2);
    const len_ptr = ptr;
    const bytes_ptr = ptr + PTR;

    //console.debug(`createString(${value}) => 0x${ptr.toString(16).padStart(4, '0')}`);

    const uint8array = (new TextEncoder()).encode(value);
    const data_ptr = alloc(uint8array.length);
    mem.u8.set(uint8array, data_ptr);

    setInt(len_ptr, uint8array.length);
    setInt(bytes_ptr, data_ptr);
    return ptr;
}

export function getString(ptr: number): string {
    assert(ptr);
    // Heap:
    // - 4 byte: String len in bytes
    // - 4 byte: Pointer to string characters in heap or data section
    const len = getInt(ptr);
    const bytes = getInt(ptr + PTR);

    // console.log('getString', {ptr, len, bytes})
    // dumpMemory(bytes, len);

    const contents = mem.u8.subarray(bytes, bytes + len);

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
