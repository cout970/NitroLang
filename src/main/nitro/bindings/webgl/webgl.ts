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
    return infoLog ? state.sendOptional(state.sendString(infoLog)) : state.sendNone();
  },

  webgl_is_program: (context: number, program: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglProgram = state.getObject(program) as WebGLProgram;
    const isProgram = webglContext.isProgram(webglProgram);
    return isProgram ? 1 : 0;
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
    return infoLog ? state.sendOptional(state.sendString(infoLog)) : state.sendNone();
  },

  webgl_get_shader_source: (context: number, shader: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglShader = state.getObject(shader) as WebGLShader;
    const source = webglContext.getShaderSource(webglShader);
    return source ? state.sendOptional(state.sendString(source)) : state.sendNone();
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

  create_framebuffer: (context: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const framebuffer = webglContext.createFramebuffer();
    return state.sendOptional(framebuffer ? state.sendObject(framebuffer) : null);
  },

  delete_framebuffer: (context: number, framebuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const webglFramebuffer = state.getObject(framebuffer) as WebGLFramebuffer;
    webglContext.deleteFramebuffer(webglFramebuffer);
  },

  framebuffer_renderbuffer: (context: number, target: number, attachment: number, renderbuffer_target: number, renderbuffer: number) => {
    const webglContext = state.getObject(context) as WebGLRenderingContext;
    const renderbuffer_opt = state.getOptional(renderbuffer);
    if (renderbuffer_opt === null) {
      webglContext.framebufferRenderbuffer(target, attachment, renderbuffer_target, null);
      return;
    }
    const webglRenderbuffer = state.getObject(renderbuffer_opt) as WebGLRenderbuffer;
    webglContext.framebufferRenderbuffer(target, attachment, renderbuffer_target, webglRenderbuffer);
  },

  framebuffer_texture_2d: (context: number, target: number, attachment: number, textarget: number, texture: number, level: number) => {
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

}
