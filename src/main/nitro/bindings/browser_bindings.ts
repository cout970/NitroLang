import {bindings as js} from './js/js';
import {bindings as dom} from './dom/dom';
import {bindings as canvas} from './canvas/canvas';
import {bindings as webgl} from './webgl/webgl';

export default {
  ...js,
  ...dom,
  ...canvas,
  ...webgl,
}
