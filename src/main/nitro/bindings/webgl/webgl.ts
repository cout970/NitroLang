import {state} from "../../core/js_runtime/runtime";

//noinspection JSUnusedGlobalSymbols
export const bindings = {

  // Misc

  window_request_animation_frame: (callback: number) => {
    const callback_name = state.getString(callback);
    const exp = state.exports[callback_name]
    if (typeof exp !== 'function') {
      throw new Error(`Callback ${callback_name} is not a function`);
    }
    return window.requestAnimationFrame(() => exp());
  },

  performance_now_ns: () => {
    // @ts-ignore
    return BigInt(performance.now() * 1e6); // Convert milliseconds to nanoseconds
  },

  // WebGL

  webgl_get_context_webgl: (element: number) => {
    const canvas = state.getObject(element);
    if (!(canvas instanceof HTMLCanvasElement)) {
      return state.sendNone();
    }
    let context = canvas.getContext("webgl");
    return state.sendOptional(context ? state.sendObject(context) : null);
  },

  webgl_get_canvas: (context: number) => {
    const webglContext = state.getObject(context);
    if (!(webglContext instanceof WebGLRenderingContext)) {
      return state.sendNone();
    }
    const canvas = webglContext.canvas;
    return state.sendOptional(canvas ? state.sendObject(canvas) : null);
  },

  webgl_get_drawing_buffer_width: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return webglContext.drawingBufferWidth;
  },

  webgl_get_drawing_buffer_height: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return webglContext.drawingBufferHeight;
  },

  webgl_get_context_attributes: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const attributes = webglContext.getContextAttributes();
    if (!attributes) {
      return state.sendNone();
    }
    return state.sendOptional(state.sendObject(attributes));
  },

  webgl_is_context_lost: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return webglContext.isContextLost();
  },

  webgl_scissor: (context: number, x: number, y: number, width: number, height: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.scissor(x, y, width, height);
  },

  webgl_viewport: (context: number, x: number, y: number, width: number, height: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.viewport(x, y, width, height);
  },

  webgl_clear_color: (context: number, red: number, green: number, blue: number, alpha: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.clearColor(red, green, blue, alpha);
  },

  webgl_clear: (context: number, mask: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.clear(mask);
  },

  webgl_get_parameter_as_int: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_get_parameter_as_float: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_get_parameter_as_boolean: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    return typeof value === 'boolean' ? state.sendOptional(value ? 1 : 0) : state.sendNone();
  },

  webgl_get_parameter_as_string: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    return typeof value === 'string' ? state.sendOptional(state.sendString(value)) : state.sendNone();
  },

  webgl_get_parameter_as_float_array: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof Float32Array) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_get_parameter_as_int_array: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof Int32Array) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_get_parameter_as_program: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof WebGLProgram) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_get_parameter_as_buffer: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof WebGLBuffer) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_get_parameter_as_framebuffer: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof WebGLFramebuffer) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_get_parameter_as_renderbuffer: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof WebGLRenderbuffer) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_get_parameter_as_texture: (context: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getParameter(pname);
    if (value instanceof WebGLTexture) {
      return state.sendOptional(state.sendObject(value));
    }
    return state.sendNone();
  },

  webgl_is_enabled: (context: number, cap: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const isEnabled = webglContext.isEnabled(cap);
    return isEnabled ? 1 : 0;
  },

  webgl_enable: (context: number, cap: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.enable(cap);
  },

  webgl_disable: (context: number, cap: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.disable(cap);
  },

  webgl_get_error: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return webglContext.getError();
  },

  webgl_hint: (context: number, target: number, mode: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.hint(target, mode);
  },

  webgl_flush: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.flush();
  },

  webgl_front_face: (context: number, mode: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.frontFace(mode);
  },

  webgl_line_width: (context: number, width: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.lineWidth(width);
  },

  webgl_active_texture: (context: number, texture: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.activeTexture(texture);
  },

  webgl_blend_color: (context: number, red: number, green: number, blue: number, alpha: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.blendColor(red, green, blue, alpha);
  },

  webgl_blend_equation: (context: number, mode: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.blendEquation(mode);
  },

  webgl_blend_equation_separate: (context: number, mode_rgb: number, mode_alpha: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.blendEquationSeparate(mode_rgb, mode_alpha);
  },

  webgl_blend_func: (context: number, sfactor: number, dfactor: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.blendFunc(sfactor, dfactor);
  },

  webgl_blend_func_separate: (context: number, src_rgb: number, dst_rgb: number, src_alpha: number, dst_alpha: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.blendFuncSeparate(src_rgb, dst_rgb, src_alpha, dst_alpha);
  },

  webgl_clear_depth: (context: number, depth: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.clearDepth(depth);
  },

  webgl_clear_stencil: (context: number, s: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.clearStencil(s);
  },

  webgl_color_mask: (context: number, red: number, green: number, blue: number, alpha: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.colorMask(red !== 0, green !== 0, blue !== 0, alpha !== 0);
  },

  webgl_cull_face: (context: number, mode: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.cullFace(mode);
  },

  webgl_depth_func: (context: number, func: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.depthFunc(func);
  },

  webgl_depth_mask: (context: number, flag: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.depthMask(flag !== 0);
  },

  webgl_depth_range: (context: number, z_near: number, z_far: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.depthRange(z_near, z_far);
  },

  webgl_pixel_storei: (context: number, pname: number, param: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.pixelStorei(pname, param);
  },

  webgl_polygon_offset: (context: number, factor: number, units: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.polygonOffset(factor, units);
  },

  webgl_sample_coverage: (context: number, value: number, invert: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.sampleCoverage(value, invert !== 0);
  },

  webgl_stencil_func: (context: number, func: number, ref_value: number, mask: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.stencilFunc(func, ref_value, mask);
  },

  webgl_stencil_func_separate: (context: number, face: number, func: number, ref_value: number, mask: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.stencilFuncSeparate(face, func, ref_value, mask);
  },

  webgl_stencil_mask: (context: number, mask: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.stencilMask(mask);
  },

  webgl_stencil_mask_separate: (context: number, face: number, mask: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.stencilMaskSeparate(face, mask);
  },

  webgl_stencil_op: (context: number, fail: number, zfail: number, zpass: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.stencilOp(fail, zfail, zpass);
  },

  webgl_stencil_op_separate: (context: number, face: number, fail: number, zfail: number, zpass: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.stencilOpSeparate(face, fail, zfail, zpass);
  },

  webgl_create_buffer: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const buffer = webglContext.createBuffer();
    return state.sendOptional(buffer ? state.sendObject(buffer) : null);
  },

  webgl_delete_buffer: (context: number, buffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglBuffer = state.getObject(buffer) as WebGLBuffer;
    webglContext.deleteBuffer(webglBuffer);
  },

  webgl_bind_buffer: (context: number, target: number, buffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const buffer_opt = state.getOptional(buffer);
    if (buffer_opt === null) {
      webglContext.bindBuffer(target, null);
      return;
    }
    const webglBuffer = state.getObject(buffer_opt) as WebGLBuffer;
    webglContext.bindBuffer(target, webglBuffer);
  },

  webgl_buffer_data_float: (context: number, target: number, data: number, usage: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const floatArray = state.getObject(data) as Float32Array;
    webglContext.bufferData(target, floatArray, usage);
  },

  webgl_buffer_data_int: (context: number, target: number, data: number, usage: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const intArray = state.getObject(data) as Int32Array;
    webglContext.bufferData(target, intArray, usage);
  },

  webgl_buffer_sub_data_float: (context: number, target: number, offset: number, data: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const floatArray = state.getObject(data) as Float32Array;
    webglContext.bufferSubData(target, offset, floatArray);
  },

  webgl_buffer_sub_data_int: (context: number, target: number, offset: number, data: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const intArray = state.getObject(data) as Int32Array;
    webglContext.bufferSubData(target, offset, intArray);
  },

  webgl_get_buffer_parameter_as_int: (context: number, target: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getBufferParameter(target, pname);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_is_buffer: (context: number, buffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglBuffer = state.getObject(buffer) as WebGLBuffer;
    const isBuffer = webglContext.isBuffer(webglBuffer);
    return isBuffer ? 1 : 0;
  },

  // Shaders

  webgl_create_program: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const program = webglContext.createProgram();
    return state.sendOptional(program ? state.sendObject(program) : null);
  },

  webgl_delete_program: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    webglContext.deleteProgram(webglProgram);
  },

  webgl_use_program: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const program_opt = state.getOptional(program);
    if (program_opt) {
      const webglProgram = state.getObject(program_opt) as WebGLProgram;
      webglContext.useProgram(webglProgram);
    } else {
      webglContext.useProgram(null);
    }
  },

  webgl_link_program: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    webglContext.linkProgram(webglProgram);
  },

  webgl_get_program_parameter_as_int: (context: number, program: number, parameter_constant: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const value = webglContext.getProgramParameter(webglProgram, parameter_constant);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_get_program_parameter_as_boolean: (context: number, program: number, parameter_constant: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const value = webglContext.getProgramParameter(webglProgram, parameter_constant);
    return typeof value === 'boolean' ? state.sendOptional(value ? 1 : 0) : state.sendNone();
  },

  webgl_get_program_info_log: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const infoLog = webglContext.getProgramInfoLog(webglProgram);
    return state.sendString(infoLog ?? '');
  },

  webgl_is_program: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const isProgram = webglContext.isProgram(webglProgram);
    return isProgram ? 1 : 0;
  },

  webgl_validate_program: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    webglContext.validateProgram(webglProgram);
  },

  webgl_bind_attrib_location: (context: number, program: number, index: number, name: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const attribName = state.getString(name);
    webglContext.bindAttribLocation(webglProgram, index, attribName);
  },

  webgl_create_shader: (context: number, shader_type: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const shader = webglContext.createShader(shader_type);
    return state.sendOptional(shader ? state.sendObject(shader) : null);
  },

  webgl_delete_shader: (context: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    webglContext.deleteShader(webglShader);
  },

  webgl_attach_shader: (context: number, program: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const webglShader = state.getObject(shader) as WebGLShader;
    webglContext.attachShader(webglProgram, webglShader);
  },

  webgl_detach_shader: (context: number, program: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const webglShader = state.getObject(shader) as WebGLShader;
    webglContext.detachShader(webglProgram, webglShader);
  },

  webgl_compile_shader: (context: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    webglContext.compileShader(webglShader);
  },

  webgl_get_attached_shaders: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const shaders = webglContext.getAttachedShaders(webglProgram);
    return state.sendObjectArray(shaders ? shaders.map(shader => state.sendObject(shader)) : []);
  },

  webgl_get_shader_parameter_as_int: (context: number, shader: number, parameter_constant: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const value = webglContext.getShaderParameter(webglShader, parameter_constant);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_get_shader_parameter_as_boolean: (context: number, shader: number, parameter_constant: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const value = webglContext.getShaderParameter(webglShader, parameter_constant);
    return typeof value === 'boolean' ? state.sendOptional(value ? 1 : 0) : state.sendNone();
  },

  webgl_get_shader_info_log: (context: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const infoLog = webglContext.getShaderInfoLog(webglShader);
    return state.sendString(infoLog ?? '');
  },

  webgl_get_shader_source: (context: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const source = webglContext.getShaderSource(webglShader);
    return state.sendString(source ?? '');
  },

  webgl_shader_source: (context: number, shader: number, source: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const shaderSource = state.getString(source);
    webglContext.shaderSource(webglShader, shaderSource);
  },

  webgl_is_shader: (context: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const isShader = webglContext.isShader(webglShader);
    return isShader ? 1 : 0;
  },

  webgl_get_shader_precision_format: (context: number, shader_type: number, precision_type: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const precisionFormat = webglContext.getShaderPrecisionFormat(shader_type, precision_type);
    return state.sendOptional(precisionFormat ? state.sendObject(precisionFormat) : null);
  },

  webgl_get_shader_precision_format_range_min: (precisionFormat: number) => {
    const format = state.getObject(precisionFormat) as WebGLShaderPrecisionFormat;
    return format.rangeMin;
  },

  webgl_get_shader_precision_format_range_max: (precisionFormat: number) => {
    const format = state.getObject(precisionFormat) as WebGLShaderPrecisionFormat;
    return format.rangeMax;
  },

  webgl_get_shader_precision_format_precision: (precisionFormat: number) => {
    const format = state.getObject(precisionFormat) as WebGLShaderPrecisionFormat;
    return format.precision;
  },

  webgl_enable_vertex_attrib_array: (context: number, index: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.enableVertexAttribArray(index);
  },

  webgl_disable_vertex_attrib_array: (context: number, index: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.disableVertexAttribArray(index);
  },

  webgl_vertex_attrib_pointer: (context: number, index: number, size: number, type: number, normalized: number, stride: number, offset: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.vertexAttribPointer(index, size, type, normalized !== 0, stride, offset);
  },

  webgl_draw_arrays: (context: number, mode: number, first: number, count: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.drawArrays(mode, first, count);
  },

  webgl_bind_framebuffer: (context: number, target: number, framebuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const framebuffer_opt = state.getOptional(framebuffer);
    if (framebuffer_opt === null) {
      webglContext.bindFramebuffer(target, null);
      return;
    }
    const webglFramebuffer = state.getObject(framebuffer_opt) as WebGLFramebuffer;
    webglContext.bindFramebuffer(target, webglFramebuffer);
  },

  webgl_create_framebuffer: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const framebuffer = webglContext.createFramebuffer();
    return state.sendOptional(framebuffer ? state.sendObject(framebuffer) : null);
  },

  webgl_delete_framebuffer: (context: number, framebuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglFramebuffer = state.getObject(framebuffer) as WebGLFramebuffer;
    webglContext.deleteFramebuffer(webglFramebuffer);
  },

  webgl_framebuffer_renderbuffer: (context: number, target: number, attachment: number, renderbuffer_target: number, renderbuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const renderbuffer_opt = state.getOptional(renderbuffer);
    if (renderbuffer_opt === null) {
      webglContext.framebufferRenderbuffer(target, attachment, renderbuffer_target, null);
      return;
    }
    const webglRenderbuffer = state.getObject(renderbuffer_opt) as WebGLRenderbuffer;
    webglContext.framebufferRenderbuffer(target, attachment, renderbuffer_target, webglRenderbuffer);
  },

  webgl_framebuffer_texture_2d: (context: number, target: number, attachment: number, textarget: number, texture: number, level: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const texture_opt = state.getOptional(texture);
    if (texture_opt === null) {
      webglContext.framebufferTexture2D(target, attachment, textarget, null, level);
      return;
    }
    const webglTexture = state.getObject(texture_opt) as WebGLTexture;
    webglContext.framebufferTexture2D(target, attachment, textarget, webglTexture, level);
  },

  webgl_get_framebuffer_attachment_parameter: (context: number, target: number, attachment: number, parameter_constant: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getFramebufferAttachmentParameter(target, attachment, parameter_constant);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_is_framebuffer: (context: number, framebuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglFramebuffer = state.getObject(framebuffer) as WebGLFramebuffer;
    const isFramebuffer = webglContext.isFramebuffer(webglFramebuffer);
    return isFramebuffer ? 1 : 0;
  },

  webgl_read_pixels: (context: number, x: number, y: number, width: number, height: number, format: number, type_constant: number, pixels: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const pixels_opt = state.getOptional(pixels);
    if (pixels_opt === null) {
      webglContext.readPixels(x, y, width, height, format, type_constant, null);
      return;
    }
    const pixelArray = state.getObject(pixels_opt) as Uint8Array;
    webglContext.readPixels(x, y, width, height, format, type_constant, pixelArray);
  },

  webgl_bind_renderbuffer: (context: number, target: number, renderbuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const renderbuffer_opt = state.getOptional(renderbuffer);
    if (renderbuffer_opt === null) {
      webglContext.bindRenderbuffer(target, null);
      return;
    }
    const webglRenderbuffer = state.getObject(renderbuffer_opt) as WebGLRenderbuffer;
    webglContext.bindRenderbuffer(target, webglRenderbuffer);
  },

  webgl_create_renderbuffer: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const renderbuffer = webglContext.createRenderbuffer();
    return state.sendOptional(renderbuffer ? state.sendObject(renderbuffer) : null);
  },

  webgl_delete_renderbuffer: (context: number, renderbuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglRenderbuffer = state.getObject(renderbuffer) as WebGLRenderbuffer;
    webglContext.deleteRenderbuffer(webglRenderbuffer);
  },

  webgl_get_renderbuffer_parameter_as_int: (context: number, target: number, parameter_constant: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getRenderbufferParameter(target, parameter_constant);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_is_renderbuffer: (context: number, renderbuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglRenderbuffer = state.getObject(renderbuffer) as WebGLRenderbuffer;
    const isRenderbuffer = webglContext.isRenderbuffer(webglRenderbuffer);
    return isRenderbuffer ? 1 : 0;
  },

  webgl_renderbuffer_storage: (context: number, target: number, internal_format: number, width: number, height: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.renderbufferStorage(target, internal_format, width, height);
  },

  webgl_bind_texture: (context: number, target: number, texture: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const texture_opt = state.getOptional(texture);
    if (texture_opt === null) {
      webglContext.bindTexture(target, null);
      return;
    }
    const webglTexture = state.getObject(texture_opt) as WebGLTexture;
    webglContext.bindTexture(target, webglTexture);
  },

  webgl_create_texture: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const texture = webglContext.createTexture();
    return state.sendOptional(texture ? state.sendObject(texture) : null);
  },

  webgl_delete_texture: (context: number, texture: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglTexture = state.getObject(texture) as WebGLTexture;
    webglContext.deleteTexture(webglTexture);
  },

  webgl_tex_image_2d: (context: number, target: number, level: number, internal_format: number, width: number, height: number, border: number, format: number, type_constant: number, pixels: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const pixels_opt = state.getOptional(pixels);
    if (pixels_opt === null) {
      webglContext.texImage2D(target, level, internal_format, width, height, border, format, type_constant, null);
      return;
    }
    const pixelArray = state.getObject(pixels_opt) as Uint8Array;
    webglContext.texImage2D(target, level, internal_format, width, height, border, format, type_constant, pixelArray);
  },

  webgl_tex_sub_image_2d: (context: number, target: number, level: number, x_offset: number, y_offset: number, width: number, height: number, format: number, type_constant: number, pixels: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const pixels_opt = state.getOptional(pixels);
    if (pixels_opt === null) {
      webglContext.texSubImage2D(target, level, x_offset, y_offset, width, height, format, type_constant, null);
      return;
    }
    const pixelArray = state.getObject(pixels_opt) as Uint8Array;
    webglContext.texSubImage2D(target, level, x_offset, y_offset, width, height, format, type_constant, pixelArray);
  },

  webgl_tex_parameterf: (context: number, target: number, pname: number, param: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.texParameterf(target, pname, param);
  },

  webgl_tex_parameteri: (context: number, target: number, pname: number, param: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.texParameteri(target, pname, param);
  },

  webgl_compressed_tex_image_2d: (context: number, target: number, level: number, internal_format: number, width: number, height: number, border: number, data: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const pixelArray = state.getObject(data) as Uint8Array;
    webglContext.compressedTexImage2D(target, level, internal_format, width, height, border, pixelArray);
  },

  webgl_compressed_tex_sub_image_2d: (context: number, target: number, level: number, x_offset: number, y_offset: number, width: number, height: number, format: number, data: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const pixelArray = state.getObject(data) as Uint8Array;
    webglContext.compressedTexSubImage2D(target, level, x_offset, y_offset, width, height, format, pixelArray);
  },

  webgl_copy_tex_image_2d: (context: number, target: number, level: number, internal_format: number, x: number, y: number, width: number, height: number, border: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.copyTexImage2D(target, level, internal_format, x, y, width, height, border);
  },

  webgl_copy_tex_sub_image_2d: (context: number, target: number, level: number, x_offset: number, y_offset: number, x: number, y: number, width: number, height: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.copyTexSubImage2D(target, level, x_offset, y_offset, x, y, width, height);
  },

  webgl_generate_mipmap: (context: number, target: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.generateMipmap(target);
  },

  webgl_get_tex_parameter_as_int: (context: number, target: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getTexParameter(target, pname);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_get_tex_parameter_as_float: (context: number, target: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getTexParameter(target, pname);
    return typeof value === 'number' ? state.sendOptional(value) : state.sendNone();
  },

  webgl_is_texture: (context: number, texture: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglTexture = state.getObject(texture) as WebGLTexture;
    const isTexture = webglContext.isTexture(webglTexture);
    return isTexture ? 1 : 0;
  },

  webgl_get_active_attrib: (context: number, program: number, index: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const activeInfo = webglContext.getActiveAttrib(webglProgram, index);
    return state.sendOptional(activeInfo ? state.sendObject(activeInfo) : null);
  },

  webgl_get_active_uniform: (context: number, program: number, index: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const activeInfo = webglContext.getActiveUniform(webglProgram, index);
    return state.sendOptional(activeInfo ? state.sendObject(activeInfo) : null);
  },

  webgl_active_info_get_name: (activeInfo: number) => {
    const info = state.getObject(activeInfo) as WebGLActiveInfo;
    return state.sendString(info.name);
  },

  webgl_active_info_get_size: (activeInfo: number) => {
    const info = state.getObject(activeInfo) as WebGLActiveInfo;
    return info.size;
  },

  webgl_active_info_get_type: (activeInfo: number) => {
    const info = state.getObject(activeInfo) as WebGLActiveInfo;
    return info.type;
  },

  webgl_get_attrib_location: (context: number, program: number, name: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const attribName = state.getString(name);
    return webglContext.getAttribLocation(webglProgram, attribName);
  },

  webgl_get_uniform: (context: number, program: number, location: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return state.sendNone();
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const value = webglContext.getUniform(webglProgram, uniformLocation);
    return state.sendOptional(value ? state.sendObject(value) : null);
  },

  webgl_get_uniform_location: (context: number, program: number, name: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const uniformName = state.getString(name);
    const location = webglContext.getUniformLocation(webglProgram, uniformName);
    return state.sendOptional(location ? state.sendObject(location) : null);
  },

  webgl_get_vertex_attrib: (context: number, index: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const value = webglContext.getVertexAttrib(index, pname);
    return state.sendOptional(value ? state.sendObject(value) : null);
  },

  webgl_get_vertex_attrib_offset: (context: number, index: number, pname: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return webglContext.getVertexAttribOffset(index, pname);
  },

  webgl_uniform1f: (context: number, location: number, x: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform1f(uniformLocation, x);
  },

  webgl_uniform1fv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniform1fv(uniformLocation, floatArray);
  },

  webgl_uniform1i: (context: number, location: number, x: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform1i(uniformLocation, x);
  },

  webgl_uniform1iv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const intArray = state.getObject(value) as Int32Array;
    webglContext.uniform1iv(uniformLocation, intArray);
  },

  webgl_uniform2f: (context: number, location: number, x: number, y: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform2f(uniformLocation, x, y);
  },

  webgl_uniform2fv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniform2fv(uniformLocation, floatArray);
  },

  webgl_uniform2i: (context: number, location: number, x: number, y: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform2i(uniformLocation, x, y);
  },

  webgl_uniform2iv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const intArray = state.getObject(value) as Int32Array;
    webglContext.uniform2iv(uniformLocation, intArray);
  },

  webgl_uniform3f: (context: number, location: number, x: number, y: number, z: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform3f(uniformLocation, x, y, z);
  },

  webgl_uniform3fv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniform3fv(uniformLocation, floatArray);
  },

  webgl_uniform3i: (context: number, location: number, x: number, y: number, z: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform3i(uniformLocation, x, y, z);
  },

  webgl_uniform3iv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const intArray = state.getObject(value) as Int32Array;
    webglContext.uniform3iv(uniformLocation, intArray);
  },

  webgl_uniform4f: (context: number, location: number, x: number, y: number, z: number, w: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform4f(uniformLocation, x, y, z, w);
  },

  webgl_uniform4fv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniform4fv(uniformLocation, floatArray);
  },

  webgl_uniform4i: (context: number, location: number, x: number, y: number, z: number, w: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    webglContext.uniform4i(uniformLocation, x, y, z, w);
  },

  webgl_uniform4iv: (context: number, location: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const intArray = state.getObject(value) as Int32Array;
    webglContext.uniform4iv(uniformLocation, intArray);
  },

  webgl_uniform_matrix2fv: (context: number, location: number, transpose: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniformMatrix2fv(uniformLocation, transpose !== 0, floatArray);
  },

  webgl_uniform_matrix3fv: (context: number, location: number, transpose: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniformMatrix3fv(uniformLocation, transpose !== 0, floatArray);
  },

  webgl_uniform_matrix4fv: (context: number, location: number, transpose: number, value: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const location_opt = state.getOptional(location);
    if (location_opt === null) {
      return;
    }
    const uniformLocation = state.getObject(location_opt) as WebGLUniformLocation;
    const floatArray = state.getObject(value) as Float32Array;
    webglContext.uniformMatrix4fv(uniformLocation, transpose !== 0, floatArray);
  },

  webgl_vertex_attrib_1f: (context: number, index: number, x: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.vertexAttrib1f(index, x);
  },

  webgl_vertex_attrib_1fv: (context: number, index: number, values: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const floatArray = state.getObject(values) as Float32Array;
    webglContext.vertexAttrib1fv(index, floatArray);
  },

  webgl_vertex_attrib_2f: (context: number, index: number, x: number, y: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.vertexAttrib2f(index, x, y);
  },

  webgl_vertex_attrib_2fv: (context: number, index: number, values: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const floatArray = state.getObject(values) as Float32Array;
    webglContext.vertexAttrib2fv(index, floatArray);
  },

  webgl_vertex_attrib_3f: (context: number, index: number, x: number, y: number, z: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.vertexAttrib3f(index, x, y, z);
  },

  webgl_vertex_attrib_3fv: (context: number, index: number, values: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const floatArray = state.getObject(values) as Float32Array;
    webglContext.vertexAttrib3fv(index, floatArray);
  },

  webgl_vertex_attrib_4f: (context: number, index: number, x: number, y: number, z: number, w: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.vertexAttrib4f(index, x, y, z, w);
  },

  webgl_vertex_attrib_4fv: (context: number, index: number, values: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const floatArray = state.getObject(values) as Float32Array;
    webglContext.vertexAttrib4fv(index, floatArray);
  },

  webgl_draw_elements: (context: number, mode: number, count: number, type_constant: number, offset: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.drawElements(mode, count, type_constant, offset);
  },

  webgl_finish: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.finish();
  },

  webgl_check_framebuffer_status: (context: number, target: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return webglContext.checkFramebufferStatus(target);
  },

  webgl_get_drawing_buffer_color_space: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return state.sendString(webglContext.drawingBufferColorSpace || 'srgb');
  },

  webgl_set_drawing_buffer_color_space: (context: number, color_space: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    webglContext.drawingBufferColorSpace = state.getString(color_space) as PredefinedColorSpace;
  },

  webgl_get_supported_extensions: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const extensions = webglContext.getSupportedExtensions();
    return state.sendObjectArray(extensions ? extensions.map(ext => state.sendString(ext)) : []);
  },

  webgl_get_extension: (context: number, extension_name: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const extName = state.getString(extension_name);
    const extension = webglContext.getExtension(extName);
    return state.sendOptional(extension ? state.sendObject(extension) : null);
  },

  webgl_get_unpack_color_space: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    return state.sendString((webglContext as any).unpackColorSpace || 'srgb');
  },

  webgl_set_unpack_color_space: (context: number, color_space: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    (webglContext as any).unpackColorSpace = state.getString(color_space) as PredefinedColorSpace;
  },

}
