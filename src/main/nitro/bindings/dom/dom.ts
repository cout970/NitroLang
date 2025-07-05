import {state} from "../../core/js_runtime/runtime";

//noinspection JSUnusedGlobalSymbols
export const bindings = {
  dom_get_document: () => {
    return state.sendObject(window.document);
  },

  document_get_element_by_id: (document: number, id: number) => {
    const doc = state.getObject(document) as Document;
    const element = doc.getElementById(state.getString(id));
    return state.sendOptional(state.sendObject(element));
  },

  document_query_selector: (document: number, selector: number) => {
    const doc = state.getObject(document) as Document;
    const element = doc.querySelector(state.getString(selector));
    return state.sendOptional(state.sendObject(element));
  },

  element_cast_to_canvas_element: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    if (!(element instanceof HTMLCanvasElement)) {
      return state.sendOptional(null);
    }
    const ptr = state.sendObject(element);
    return state.sendOptional(ptr);
  },

  element_get_id: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    return state.sendString(element.id);
  },

  element_get_tag_name: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    return state.sendString(element.tagName.toLowerCase());
  },

  element_get_class_name: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    return state.sendString(element.className);
  },

  element_get_inner_html: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    return state.sendString(element.innerHTML);
  },

  element_get_outer_html: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    return state.sendString(element.outerHTML);
  },

  element_get_local_name: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    return state.sendString(element.localName);
  },

  element_get_namespace_uri: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    let value = element.namespaceURI;
    return state.sendOptional(value === null ? null : state.sendString(value));
  },

  element_get_prefix: (elementPtr: number) => {
    const element = state.getObject(elementPtr) as HTMLElement;
    let value = element.prefix;
    return state.sendOptional(value === null ? null : state.sendString(value));
  }
}
