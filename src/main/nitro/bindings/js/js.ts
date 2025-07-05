import {state} from "../../core/js_runtime/runtime";

//noinspection JSUnusedGlobalSymbols
export const bindings = {
  // Float32Array

  js_float32_array_new: (length: number) => {
    const array = new Float32Array(length);
    return state.sendObject(array);
  },

  js_float32_array_len: (array: number) => {
    const floatArray = state.getObject(array) as Float32Array;
    return floatArray.length;
  },

  js_float32_array_get: (array: number, index: number) => {
    const floatArray = state.getObject(array) as Float32Array;
    if (index < 0 || index >= floatArray.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    return floatArray[index];
  },

  js_float32_array_set: (array: number, index: number, value: number) => {
    const floatArray = state.getObject(array) as Float32Array;
    if (index < 0 || index >= floatArray.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    floatArray[index] = value;
  },

  js_float32_array_from: (values: number) => {
    const valuesArray = state.getObject(values) as Float32Array;
    const array = new Float32Array(valuesArray.length);
    for (let i = 0; i < valuesArray.length; i++) {
      array[i] = valuesArray[i];
    }
    return state.sendObject(array);
  },

  // Int32Array

  js_int32_array_new: (length: number) => {
    const array = new Int32Array(length);
    return state.sendObject(array);
  },

  js_int32_array_len: (array: number) => {
    const intArray = state.getObject(array) as Int32Array;
    return intArray.length;
  },

  js_int32_array_get: (array: number, index: number) => {
    const intArray = state.getObject(array) as Int32Array;
    if (index < 0 || index >= intArray.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    return intArray[index];
  },

  js_int32_array_set: (array: number, index: number, value: number) => {
    const intArray = state.getObject(array) as Int32Array;
    if (index < 0 || index >= intArray.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    intArray[index] = value;
  },

  js_int32_array_from: (values: number) => {
    const valuesArray = state.getObject(values) as Int32Array;
    const array = new Int32Array(valuesArray.length);
    for (let i = 0; i < valuesArray.length; i++) {
      array[i] = valuesArray[i];
    }
    return state.sendObject(array);
  },

  // Uint8Array

  js_uint8_array_new: (length: number) => {
    const array = new Uint8Array(length);
    return state.sendObject(array);
  },

  js_uint8_array_len: (array: number) => {
    const uint8Array = state.getObject(array) as Uint8Array;
    return uint8Array.length;
  },

  js_uint8_array_get: (array: number, index: number) => {
    const uint8Array = state.getObject(array) as Uint8Array;
    if (index < 0 || index >= uint8Array.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    return uint8Array[index];
  },

  js_uint8_array_set: (array: number, index: number, value: number) => {
    const uint8Array = state.getObject(array) as Uint8Array;
    if (index < 0 || index >= uint8Array.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    uint8Array[index] = value;
  },

  // Js values


  // JsObject

  js_object_new: () => {
    return state.sendObject({});
  },

  js_object_from_entries: (entriesPtr: number) => {
    const entries = state.getObject(entriesPtr) as Array<[string, any]>;
    const obj: Record<string, any> = {};
    for (const [key, value] of entries) {
      obj[key] = value;
    }
    return state.sendObject(obj);
  },

  js_object_set_property: (objPtr: number, namePtr: number, valuePtr: number) => {
    const obj = state.getObject(objPtr) as Record<string, any>;
    const name = state.getString(namePtr);
    obj[name] = state.getObject(valuePtr);
  },

  js_object_get_property: (objPtr: number, namePtr: number) => {
    const obj = state.getObject(objPtr) as Record<string, any>;
    const name = state.getString(namePtr);
    if (name in obj) {
      return state.sendObject(obj[name]);
    } else {
      throw new Error(`Property ${name} does not exist on the object`);
    }
  },

  js_object_has_property: (objPtr: number, namePtr: number) => {
    const obj = state.getObject(objPtr) as Record<string, any>;
    const name = state.getString(namePtr);
    return name in obj;
  },

  js_object_entries: (objPtr: number) => {
    const obj = state.getObject(objPtr) as Record<string, any>;
    const entries: Array<[string, any]> = [];
    // @ts-ignore
    for (const [key, value] of Object.entries(obj)) {
      entries.push([key, value]);
    }
    return state.sendObject(entries);
  },

  js_object_as_value: (objPtr: number) => {
    return objPtr;
  },

  // JsArray

  js_array_new: () => {
    return state.sendObject([]);
  },

  js_array_from_values: (valuesPtr: number) => {
    const values = state.getObject(valuesPtr) as Array<any>;
    return state.sendObject([...values]);
  },

  js_array_get_length: (arrayPtr: number) => {
    const array = state.getObject(arrayPtr) as Array<any>;
    return array.length;
  },

  js_array_get_value: (arrayPtr: number, index: number) => {
    const array = state.getObject(arrayPtr) as Array<any>;
    if (index < 0 || index >= array.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    return state.sendObject(array[index]);
  },

  js_array_set_value: (arrayPtr: number, index: number, valuePtr: number) => {
    const array = state.getObject(arrayPtr) as Array<any>;
    if (index < 0 || index >= array.length) {
      throw new Error(`Index out of bounds: ${index}`);
    }
    array[index] = state.getObject(valuePtr);
  },

  js_array_as_value: (arrayPtr: number) => {
    return arrayPtr;
  },

  // Value
/*

@Extern [lib: "bindings", name: "js_value_null_value"]
fun JsValue::null_value(): JsValue {}

@Extern [lib: "bindings", name: "js_value_undefined_value"]
fun JsValue::undefined_value(): JsValue {}

@Extern [lib: "bindings", name: "js_value_is_null"]
fun JsValue.is_null(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_undefined"]
fun JsValue.is_undefined(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_object"]
fun JsValue.is_object(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_array"]
fun JsValue.is_array(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_string"]
fun JsValue.is_string(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_number"]
fun JsValue.is_number(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_boolean"]
fun JsValue.is_boolean(): Boolean {}

@Extern [lib: "bindings", name: "js_value_is_function"]
fun JsValue.is_function(): Boolean {}

@Extern [lib: "bindings", name: "js_value_as_object"]
fun JsValue.as_object(): Optional<JsObject> {}

@Extern [lib: "bindings", name: "js_value_as_array"]
fun JsValue.as_array(): Optional<JsArray> {}

@Extern [lib: "bindings", name: "js_value_as_string"]
fun JsValue.as_string(): Optional<String> {}

@Extern [lib: "bindings", name: "js_value_as_float"]
fun JsValue.as_float(): Optional<Float> {}

@Extern [lib: "bindings", name: "js_value_as_int"]
fun JsValue.as_int(): Optional<Int> {}

@Extern [lib: "bindings", name: "js_value_as_boolean"]
fun JsValue.as_boolean(): Optional<Boolean> {}

@Extern [lib: "bindings", name: "js_value_as_function"]
fun JsValue.as_function(): Optional<JsFunction> {}

 */

  js_value_null_value: () => {
    return state.sendObject(null);
  },

  js_value_undefined_value: () => {
    return state.sendObject(undefined);
  },

  js_value_is_null: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return value === null;
  },

  js_value_is_undefined: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return value === undefined;
  },

  js_value_is_object: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return typeof value === 'object' && value !== null;
  },

  js_value_is_array: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return Array.isArray(value);
  },

  js_value_is_string: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return typeof value === 'string';
  },

  js_value_is_number: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return typeof value === 'number';
  },

  js_value_is_boolean: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return typeof value === 'boolean';
  },

  js_value_is_function: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    return typeof value === 'function';
  },

  js_value_as_object: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (typeof value === 'object' && value !== null) {
      return state.sendObject(value);
    } else {
      return state.sendNone();
    }
  },

  js_value_as_array: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (Array.isArray(value)) {
      return state.sendObject(value);
    } else {
      return state.sendNone();
    }
  },

  js_value_as_string: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (typeof value === 'string') {
      return state.sendObject(value);
    } else {
      return state.sendNone();
    }
  },

  js_value_as_float: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (typeof value === 'number') {
      return state.sendObject(value);
    } else {
      return state.sendNone();
    }
  },

  js_value_as_int: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (typeof value === 'number') {
      return state.sendObject(Math.floor(value));
    } else {
      return state.sendNone();
    }
  },

  js_value_as_boolean: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (typeof value === 'boolean') {
      return state.sendObject(value);
    } else {
      return state.sendNone();
    }
  },

  js_value_as_function: (valuePtr: number) => {
    const value = state.getObject(valuePtr);
    if (typeof value === 'function') {
      return state.sendObject(value);
    } else {
      return state.sendNone();
    }
  },

  // JsFunction

  js_function_call: (funcPtr: number, thisPtr: number, argsPtr: number) => {
    const func = state.getObject(funcPtr) as Function;
    const thisArg = state.getObject(thisPtr);
    const args = state.getObject(argsPtr) as Array<any>;
    let result = func.apply(thisArg, args);
    return state.sendObject(result);
  },
}
