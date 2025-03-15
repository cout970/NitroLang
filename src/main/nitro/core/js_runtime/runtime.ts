const state = {
  stdoutBuffer: '',
  args: ['program.wasm'],
  memory: new Uint8Array(0),
};

const sendString = (string: string) => {
  const encoder = new TextEncoder();
  const bytes = encoder.encode(string);
  const offset = 40000;
  state.memory[offset + 3] = (bytes.length >> 0) & 0xFF
  state.memory[offset + 2] = (bytes.length >> 8) & 0xFF
  state.memory[offset + 1] = (bytes.length >> 16) & 0xFF
  state.memory[offset + 0] = (bytes.length >> 24) & 0xFF
  state.memory.set(encoder.encode(string), offset + 4);
  return offset;
}

const core = {
  runtime_exit() {
    throw new Error('Runtime exit');
  },
  runtime_get_program_args_count: () => {
    return state.args.length;
  },
  runtime_get_program_arg: (index: number) => {
    if (index < state.args.length) {
      let string = state.args[index];
      return sendString(string);
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

export default {core, state};
