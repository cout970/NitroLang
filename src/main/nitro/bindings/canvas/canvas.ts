import {state} from "../../core/js_runtime/runtime";

//noinspection JSUnusedGlobalSymbols
export const bindings = {
  canvas_get_canvas_element_by_id: (id: number) => {
    const idStr = state.getString(id);
    const element = document.getElementById(idStr);
    if (element === null || !(element instanceof HTMLCanvasElement)) {
      return state.sendOptional(null);
    }
    let ptr = state.sendObject(element);
    return state.sendOptional(ptr);
  },

  // Context Management
  canvas_get_context_2d: (htmlCanvasPtr: number) => {
    const canvas = state.getObject(htmlCanvasPtr) as HTMLCanvasElement;
    const context = canvas.getContext("2d");
    if (context === null) {
      return state.sendOptional(null);
    }
    return state.sendOptional(state.sendObject(context));
  },

  // Basic Properties
  canvas_set_width: (htmlCanvasPtr: number, width: number) => {
    const canvas = state.getObject(htmlCanvasPtr) as HTMLCanvasElement;
    canvas.width = width;
  },

  canvas_set_height: (htmlCanvasPtr: number, height: number) => {
    const canvas = state.getObject(htmlCanvasPtr) as HTMLCanvasElement;
    canvas.height = height;
  },

  canvas_get_width: (htmlCanvasPtr: number) => {
    const canvas = state.getObject(htmlCanvasPtr) as HTMLCanvasElement;
    return canvas.width;
  },

  canvas_get_height: (htmlCanvasPtr: number) => {
    const canvas = state.getObject(htmlCanvasPtr) as HTMLCanvasElement;
    return canvas.height;
  },

  // Drawing Rectangles
  canvas_clear_rect: (contextPtr: number, x: number, y: number, width: number, height: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.clearRect(x, y, width, height);
  },

  canvas_fill_rect: (contextPtr: number, x: number, y: number, width: number, height: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.fillRect(x, y, width, height);
  },

  canvas_stroke_rect: (contextPtr: number, x: number, y: number, width: number, height: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.strokeRect(x, y, width, height);
  },

  // Colors, Styles and Shadows
  canvas_set_fill_style: (contextPtr: number, stylePtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.fillStyle = state.getString(stylePtr);
  },

  canvas_set_fill_style_canvas_gradient: (contextPtr: number, gradientPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.fillStyle = state.getObject(gradientPtr) as CanvasGradient;
  },

  canvas_set_stroke_style: (contextPtr: number, stylePtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.strokeStyle = state.getString(stylePtr);
  },

  canvas_set_stroke_style_canvas_gradient: (contextPtr: number, gradientPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.strokeStyle = state.getObject(gradientPtr) as CanvasGradient;
  },

  canvas_set_line_width: (contextPtr: number, width: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.lineWidth = width;
  },

  canvas_set_shadow_color: (contextPtr: number, colorPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.shadowColor = state.getString(colorPtr);
  },

  canvas_set_shadow_blur: (contextPtr: number, blur: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.shadowBlur = blur;
  },

  canvas_set_shadow_offset_x: (contextPtr: number, offset: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.shadowOffsetX = offset;
  },

  canvas_set_shadow_offset_y: (contextPtr: number, offset: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.shadowOffsetY = offset;
  },

  // Line Styles
  canvas_set_line_cap: (contextPtr: number, capPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.lineCap = state.getString(capPtr) as CanvasLineCap;
  },

  canvas_set_line_join: (contextPtr: number, joinPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.lineJoin = state.getString(joinPtr) as CanvasLineJoin;
  },

  canvas_set_miter_limit: (contextPtr: number, limit: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.miterLimit = limit;
  },

  // Paths
  canvas_begin_path: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.beginPath();
  },

  canvas_close_path: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.closePath();
  },

  canvas_move_to: (contextPtr: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.moveTo(x, y);
  },

  canvas_line_to: (contextPtr: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.lineTo(x, y);
  },

  canvas_bezier_curve_to: (contextPtr: number, cp1x: number, cp1y: number, cp2x: number, cp2y: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
  },

  canvas_quadratic_curve_to: (contextPtr: number, cpx: number, cpy: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.quadraticCurveTo(cpx, cpy, x, y);
  },

  canvas_arc: (contextPtr: number, x: number, y: number, radius: number, startAngle: number, endAngle: number, anticlockwise: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    // Convert the anticlockwise byte to boolean
    context.arc(x, y, radius, startAngle, endAngle, anticlockwise !== 0);
  },

  canvas_arc_to: (contextPtr: number, x1: number, y1: number, x2: number, y2: number, radius: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.arcTo(x1, y1, x2, y2, radius);
  },

  canvas_rect: (contextPtr: number, x: number, y: number, width: number, height: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.rect(x, y, width, height);
  },

  canvas_ellipse: (contextPtr: number, x: number, y: number, radiusX: number, radiusY: number, rotation: number, startAngle: number, endAngle: number, anticlockwise: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle, anticlockwise !== 0);
  },

  canvas_fill: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.fill();
  },

  canvas_stroke: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.stroke();
  },

  canvas_clip: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.clip();
  },

  // Text
  canvas_fill_text: (contextPtr: number, textPtr: number, x: number, y: number, maxWidthPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const text = state.getString(textPtr);
    const maxWidth = state.getOptional(maxWidthPtr);

    if (maxWidth === null) {
      context.fillText(text, x, y);
    } else {
      context.fillText(text, x, y, maxWidth);
    }
  },

  canvas_stroke_text: (contextPtr: number, textPtr: number, x: number, y: number, maxWidthPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const text = state.getString(textPtr);
    const maxWidth = state.getOptional(maxWidthPtr);

    if (maxWidth === null) {
      context.strokeText(text, x, y);
    } else {
      context.strokeText(text, x, y, maxWidth);
    }
  },

  canvas_measure_text: (contextPtr: number, textPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const text = state.getString(textPtr);
    const metrics = context.measureText(text);
    return state.sendObject(metrics);
  },

  canvas_text_metrics_width: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.width;
  },

  canvas_text_metrics_actual_bounding_box_left: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.actualBoundingBoxLeft;
  },

  canvas_text_metrics_actual_bounding_box_right: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.actualBoundingBoxRight;
  },

  canvas_text_metrics_font_bounding_box_ascent: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.fontBoundingBoxAscent;
  },

  canvas_text_metrics_font_bounding_box_descent: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.fontBoundingBoxDescent;
  },

  canvas_text_metrics_actual_bounding_box_ascent: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.actualBoundingBoxAscent;
  },

  canvas_text_metrics_actual_bounding_box_descent: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.actualBoundingBoxDescent;
  },

  canvas_text_metrics_em_height_ascent: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.emHeightAscent;
  },

  canvas_text_metrics_em_height_descent: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.emHeightDescent;
  },

  canvas_text_metrics_hanging_baseline: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.hangingBaseline;
  },

  canvas_text_metrics_alphabetic_baseline: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.alphabeticBaseline;
  },

  canvas_text_metrics_ideographic_baseline: (contextPtr: number) => {
    const metrics = state.getObject(contextPtr) as TextMetrics;
    return metrics.ideographicBaseline;
  },

  canvas_set_font: (contextPtr: number, fontPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.font = state.getString(fontPtr);
  },

  canvas_set_text_align: (contextPtr: number, alignPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.textAlign = state.getString(alignPtr) as CanvasTextAlign;
  },

  canvas_set_text_baseline: (contextPtr: number, baselinePtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.textBaseline = state.getString(baselinePtr) as CanvasTextBaseline;
  },

  // Images
  canvas_draw_image: (contextPtr: number, imagePtr: number, dx: number, dy: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const image = state.getObject(imagePtr) as CanvasImageSource;
    context.drawImage(image, dx, dy);
  },

  canvas_draw_image_with_size: (contextPtr: number, imagePtr: number, dx: number, dy: number, dw: number, dh: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const image = state.getObject(imagePtr) as CanvasImageSource;
    context.drawImage(image, dx, dy, dw, dh);
  },

  canvas_draw_image_with_source_and_size: (contextPtr: number, imagePtr: number, sx: number, sy: number, sw: number, sh: number, dx: number, dy: number, dw: number, dh: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const image = state.getObject(imagePtr) as CanvasImageSource;
    context.drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh);
  },

  // Canvas State
  canvas_save: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.save();
  },

  canvas_restore: (contextPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.restore();
  },

  // Transformations
  canvas_scale: (contextPtr: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.scale(x, y);
  },

  canvas_rotate: (contextPtr: number, angle: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.rotate(angle);
  },

  canvas_translate: (contextPtr: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.translate(x, y);
  },

  canvas_transform: (contextPtr: number, a: number, b: number, c: number, d: number, e: number, f: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.transform(a, b, c, d, e, f);
  },

  canvas_set_transform: (contextPtr: number, a: number, b: number, c: number, d: number, e: number, f: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.setTransform(a, b, c, d, e, f);
  },

  // Pixel Manipulation
  canvas_create_image_data: (contextPtr: number, width: number, height: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const imageData = context.createImageData(width, height);
    return state.sendObject(imageData);
  },

  canvas_create_linear_gradient: (contextPtr: number, x0: number, y0: number, x1: number, y1: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const gradient = context.createLinearGradient(x0, y0, x1, y1);
    return state.sendObject(gradient);
  },

  canvas_create_radial_gradient: (contextPtr: number, x0: number, y0: number, r0: number, x1: number, y1: number, r1: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const gradient = context.createRadialGradient(x0, y0, r0, x1, y1, r1);
    return state.sendObject(gradient);
  },

  canvas_add_color_stop: (gradientPtr: number, offset: number, colorPtr: number) => {
    const gradient = state.getObject(gradientPtr) as CanvasGradient;
    const color = state.getString(colorPtr);
    gradient.addColorStop(offset, color);
  },

  canvas_get_image_data: (contextPtr: number, x: number, y: number, width: number, height: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const imageData = context.getImageData(x, y, width, height);
    return state.sendObject(imageData);
  },

  canvas_put_image_data: (contextPtr: number, imageDataPtr: number, x: number, y: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const imageData = state.getObject(imageDataPtr) as ImageData;
    context.putImageData(imageData, x, y);
  },

  canvas_put_image_data_with_dirty: (contextPtr: number, imageDataPtr: number, x: number, y: number, dirtyX: number, dirtyY: number, dirtyWidth: number, dirtyHeight: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    const imageData = state.getObject(imageDataPtr) as ImageData;
    context.putImageData(imageData, x, y, dirtyX, dirtyY, dirtyWidth, dirtyHeight);
  },

  // Compositing
  canvas_set_global_alpha: (contextPtr: number, alpha: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.globalAlpha = alpha;
  },

  canvas_set_global_composite_operation: (contextPtr: number, operationPtr: number) => {
    const context = state.getObject(contextPtr) as CanvasRenderingContext2D;
    context.globalCompositeOperation = state.getString(operationPtr) as GlobalCompositeOperation;
  }
}
