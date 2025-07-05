export const state = {
  // Program arguments
  args: ['program.wasm'],
  // WASM Memory views
  memory_8: new Uint8Array(0),
  memory_16: new Uint16Array(0),
  memory_32: new Uint32Array(0),
  // Exported functions
  exports: {},
  // JS objects references
  refs: [],
  // Internals
  stdoutBuffer: '',

  // Align a value to the next multiple of 4
  align(value: number): number {
    const alignment = 4;
    return Math.ceil(value / alignment) * alignment;
  },
  // Allocate memory for a value
  alloc(bytes: number) {
    const base = state.memory_32[1]; // memory.base
    const heapStart = state.memory_32[2]; // memory.heap_start
    const offset = state.align(heapStart + base);
    state.memory_32[1] += (offset + bytes) - (heapStart + base);
    return offset;
  },
  sendByteArray(bytes: Uint8Array): number {
    const offset = state.alloc(4 + bytes.length);

    state.memory_32[offset / 4] = bytes.length;
    state.memory_8.set(bytes, offset + 4);
    return offset;
  },
  // Sends a string to the wasm memory and returns a pointer to it
  sendString(string: string) {
    const encoder = new TextEncoder();
    const bytes = encoder.encode(string);
    const utf8_data = state.sendByteArray(bytes);

    const offset = state.alloc(12);

    state.memory_32[offset / 4] = bytes.length;
    state.memory_32[offset / 4 + 1] = utf8_data;
    state.memory_32[offset / 4 + 2] = 0xFFFFFFFF;
    return offset;
  },
  // Gets a string from the wasm memory
  getString(ptr: number): string {
    if (ptr === 0) {
      throw new Error('Null pointer found at getString()');
    }

    // String layout in memory:
    // 0: len
    // 4: utf8_data -> Array<Byte> is a ptr
    // 8: hash_cache
    // Array<Byte> layout:
    // 0: len
    // 4: contents inline up to len bytes
    const len = state.memory_32[ptr / 4];
    const utf8_data = state.memory_32[ptr / 4 + 1];
    const offset = utf8_data + 4; // skip len

    const bytes = state.memory_8.slice(offset, offset + len);
    const decoder = new TextDecoder();
    return decoder.decode(bytes);
  },
  // Creates an ID in wasm to reference a JS object
  sendObject(value: any): number {
    let index = state.refs.indexOf(value);
    if (index === -1) {
      index = state.refs.length;
      state.refs.push(value);
    }
    return index + 1;
  },
  // Given an ID from wasm, returns the JS object linked to it
  getObject(num: number): any {
    let index = num - 1;
    if (index >= 0 && index < state.refs.length) {
      return state.refs[index];
    }
    throw new Error('Index out of bounds, refs count is ' + state.refs.length + ', index: ' + index);
  },
  // Creates a new Optional<> instance in wasm with the value provided
  sendOptional(value: number | null): number {
    const offset = state.alloc(8);

    // Optional layout in memory:
    // 0: variant: 0 None, 1 Some
    // 4: value: ptr or 0
    state.memory_32[offset / 4] = value === null ? 0 : 1;
    state.memory_32[offset / 4 + 1] = value === null ? 0 : value;
    return offset;
  },
  // Shorthand for sending an Optional::None()
  sendNone(): number {
    return state.sendOptional(null);
  },
  // Given a pointer to an Optional<> instance, returns the value inside it or null
  getOptional(ptr: number): number | null {
    if (ptr === 0) {
      return null;
    }

    // Optional layout in memory:
    // 0: variant: 0 None, 1 Some
    // 4: value: ptr or 0
    const variant = state.memory_32[ptr / 4];
    if (variant === 0) {
      return null;
    } else if (variant === 1) {
      return state.memory_32[ptr / 4 + 1];
    }
    throw new Error('Invalid optional variant ' + variant);
  },
  // Sends an array of Object pointers to the wasm memory and returns a pointer to it
  sendObjectArray: (array: number[]): number => {
    const offset = state.alloc(4 + array.length * 4);

    // Array layout in memory:
    // 0: len
    // 4: contents inline up to len * 4 bytes
    state.memory_32[offset / 4] = array.length;
    for (let i = 0; i < array.length; i++) {
      state.memory_32[offset / 4 + 1 + i] = array[i];
    }
    return offset;
  },
  // Gets an array of Object pointers from the wasm memory
  getObjectArray: (ptr: number): number[] => {
    if (ptr === 0) {
      throw new Error('Null pointer found at getObjectArray()');
    }

    // Array layout in memory:
    // 0: len
    // 4: contents inline up to len * 4 bytes
    const len = state.memory_32[ptr / 4];
    const array: number[] = [];
    for (let i = 0; i < len; i++) {
      array.push(state.memory_32[ptr / 4 + 1 + i]);
    }
    return array;
  },
};

export const core = {
  runtime_exit() {
    throw new Error('Runtime exit');
  },
  runtime_get_program_args_count: () => {
    return state.args.length;
  },
  runtime_get_program_arg: (index: number) => {
    if (index < state.args.length) {
      let string = state.args[index];
      return state.sendString(string);
    }
    throw new Error('Index out of bounds, program args count is ' + state.args.length + ', index: ' + index);
  },
  runtime_get_environment_count: () => {
    return 0;
  },
  runtime_get_environment_key: (index: number) => {
    throw new Error('Index out of bounds, environment count is 0, index: ' + index);
  },
  runtime_get_environment_value: (index: number) => {
    throw new Error('Index out of bounds, environment count is 0, index: ' + index);
  },
  runtime_get_realtime_clock_ms: () => {
    return Date.now();
  },
  runtime_get_monotonic_clock_ns: () => {
    return performance.now();
  },
  runtime_random: () => {
    return Math.random();
  },
  runtime_stdout_write_char: (unicode_codepoint: number) => {
    state.stdoutBuffer += String.fromCharCode(unicode_codepoint);
    if (unicode_codepoint === 10 /* '\n' */) {
      console.log(state.stdoutBuffer);
      state.stdoutBuffer = '';
    }
  },
  runtime_stdout_write_int: (integer: number) => {
    state.stdoutBuffer += String(integer);
  },
};
