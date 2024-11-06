import {editorSelectFile} from "./editor";
import {Directory} from "@wasmer/sdk";


export async function setupFiles() {
  const files = document.querySelector('#files');

  document.expandedDirs = new Set(['/src']);

  await updateFiles();

  const initGrabber = () => {
    const grab = document.querySelector('#files .files-grab');
    let holding = false;
    let initialHeight = 0;
    let initialWidth = 0;
    let startPos = {x: 0, y: 0};
    const horizontal = true;

    grab.addEventListener('mousedown', e => {
      startPos = {x: e.clientX, y: e.clientY};
      initialHeight = parseInt(files.offsetHeight, 10);
      initialHeight = isNaN(initialHeight) ? 200 : initialHeight;
      initialWidth = parseInt(files.offsetWidth, 10);
      initialWidth = isNaN(initialWidth) ? 200 : initialWidth;

      holding = true;
      e.stopPropagation();
      e.preventDefault();
    });

    document.addEventListener('mouseup', () => {
      holding = false;
    });

    document.addEventListener('mousemove', e => {
      if (!holding) return;
      const pos = {x: e.clientX, y: e.clientY};
      const diff = {x: (pos.x - startPos.x), y: -(pos.y - startPos.y)};

      if (horizontal) {
        let newWidth = initialWidth + diff.x;
        if (newWidth < 40) newWidth = 40;
        if (newWidth > window.innerWidth - 40) newWidth = window.innerWidth - 40;

        files.style.width = `${newWidth}px`;
        files.style.minWidth = `${newWidth}px`;
        files.style.height = '100%';
        files.style.minHeight = '100%';
      } else {
        let newHeight = initialHeight + diff.y;
        if (newHeight < 40) newHeight = 40;
        if (newHeight > window.innerHeight - 40) newHeight = window.innerHeight - 40;

        files.style.width = '100%';
        files.style.minWidth = '100%';
        files.style.height = `${newHeight}px`;
        files.style.minHeight = `${newHeight}px`;

        if (files.classList.contains('files-collapsed')) {
          files.classList.remove('files-collapsed');
        }
      }
    });
  }

  initGrabber();
}

/**
 * @returns {Promise<Directory>}
 */
export async function getProject() {
  if (!window.project) {
    window.project = await initFS();
  }
  return window.project;
}

async function initFS() {
  const fs = new Directory();
  await fs.createDir('src');
  await fs.createDir('src/core');
  await fs.createDir('src/examples');
  await fs.createDir('src/compiler');
  await fs.createDir("out");
  await fs.createDir("out/cache");

  // Load from localstorage
  console.debug('Loading files from local storage');
  const cacheEntries = Object.keys(localStorage).filter(k => k.startsWith("fs/"));
  for (const cacheEntry of cacheEntries) {
    const name = cacheEntry.substring(3);
    const hex = localStorage.getItem(cacheEntry);
    const bytes = new Uint8Array(hex.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
    await createParentDirs(fs, name);
    await fs.writeFile(name, bytes);
  }

  // Copy core
  const coreFiles = [
    'collections/array_deque.nitro',
    'collections/array_deque_iterator.nitro',
    'collections/iterator.nitro',
    'collections/json.nitro',
    'collections/list.nitro',
    'collections/list_iterator.nitro',
    'collections/map.nitro',
    'collections/map_iterator.nitro',
    'collections/multimap.nitro',
    'collections/optional.nitro',
    'collections/pair.nitro',
    'collections/range_iterator.nitro',
    'collections/range_to.nitro',
    'collections/range_up_to.nitro',
    'collections/result.nitro',
    'collections/set.nitro',
    'collections/slice.nitro',
    'debug/check.nitro',
    'debug/crash.nitro',
    'debug/debug.nitro',
    'debug/log.nitro',
    'debug/logger.nitro',
    'debug/trace.nitro',
    'fs/file_metadata.nitro',
    'fs/file_path.nitro',
    'lang/annotations.nitro',
    'lang/intrinsic.nitro',
    'math/hash/crc32.nitro',
    'math/hash/md5.nitro',
    'math/random/random.nitro',
    'math/exponentiation.nitro',
    'math/logarithms.nitro',
    'math/math.nitro',
    'math/trigonometry.nitro',
    'memory/array.nitro',
    'memory/bytes.nitro',
    'memory/encoding.nitro',
    'memory/ffi.nitro',
    'memory/intrinsic_memory.nitro',
    'memory/memory_allocator.nitro',
    'memory/memory_arena.nitro',
    'memory/memory_slice.nitro',
    'memory/memory_stack.nitro',
    'memory/ptr.nitro',
    'memory/rc.nitro',
    'primitives/boolean.nitro',
    'primitives/byte.nitro',
    'primitives/float.nitro',
    'primitives/function.nitro',
    'primitives/int.nitro',
    'primitives/long.nitro',
    'primitives/never.nitro',
    'primitives/nothing.nitro',
    'sorting/ordering.nitro',
    'sorting/quicksort.nitro',
    'string/char.nitro',
    'string/string.nitro',
    'string/string_builder.nitro',
    'time/duration.nitro',
    'time/instant.nitro',
    'wasi/runtime.nitro',
    'wasi/wasi.nitro',
    'core.nitro',
  ];
  for (let file of coreFiles) {
    const path = 'src/core/' + file;
    await createParentDirs(fs, path);
    const url = new URL(`../../src/main/nitro/core/${file}`, import.meta.url).href;
    await fs.writeFile(path, new Uint8Array(await (await fetch(url)).arrayBuffer()));
  }

  const compilerFiles = [
    'main.nitro',
    'wasm/final_type/final_type.nitro',
    'wasm/final_type/final_type_container.nitro',
    'wasm/final_type/type_mapping_ctx.nitro',
    'wasm/final_type/final_type_pattern.nitro',
    'wasm/wasm_visitor.nitro',
    'wasm/wasm_compilation.nitro',
    'wasm/wasm_emitter.nitro',
    'wasm/monomorphization_key.nitro',
    'wasm/wasm_backend.nitro',
    'wasm/wasm_autogenerated.nitro',
    'wasm/wasm_program.nitro',
    'wasm/wasm_instructions.nitro',
    'parser/expression/map.nitro',
    'parser/expression/with_suffix.nitro',
    'parser/expression/lambda.nitro',
    'parser/expression/break.nitro',
    'parser/expression/set.nitro',
    'parser/expression/size_of.nitro',
    'parser/expression/return.nitro',
    'parser/expression/include_as_string.nitro',
    'parser/expression/if.nitro',
    'parser/expression/include_as_bytes.nitro',
    'parser/expression/list.nitro',
    'parser/expression/literal.nitro',
    'parser/expression/binary_operation.nitro',
    'parser/expression/json.nitro',
    'parser/expression/string.nitro',
    'parser/expression/simple.nitro',
    'parser/expression/nvalue.nitro',
    'parser/expression/when.nitro',
    'parser/expression/continue.nitro',
    'parser/expression/base.nitro',
    'parser/statement.nitro',
    'parser/parse_error.nitro',
    'parser/statement/foreign_block.nitro',
    'parser/statement/repeat.nitro',
    'parser/statement/while.nitro',
    'parser/statement/defer.nitro',
    'parser/statement/let.nitro',
    'parser/statement/if.nitro',
    'parser/statement/for.nitro',
    'parser/statement/when.nitro',
    'parser/statement/loop.nitro',
    'parser/expression.nitro',
    'parser/parser.nitro',
    'parser/definition/module.nitro',
    'parser/definition/function.nitro',
    'parser/definition/option.nitro',
    'parser/definition/test.nitro',
    'parser/definition/struct.nitro',
    'parser/definition/type_alias.nitro',
    'parser/definition/const.nitro',
    'parser/definition/type_usage.nitro',
    'parser/definition/tag.nitro',
    'parser/definition/enum.nitro',
    'parser/definition/include.nitro',
    'parser/definition/type_pattern.nitro',
    'parser/definition/type_parameter.nitro',
    'parser/definition/annotation.nitro',
    'parser/definition.nitro',
    'parser/parser_utils.nitro',
    'program/program.nitro',
    'program/nitro_const.nitro',
    'program/nitro_generic.nitro',
    'program/var.nitro',
    'program/nitro_lambda.nitro',
    'program/code_merge.nitro',
    'program/nitro_tag.nitro',
    'program/nitro_type_usage.nitro',
    'program/code.nitro',
    'program/code_block.nitro',
    'program/nitro_function_header.nitro',
    'program/nitro_include.nitro',
    'program/nitro_function.nitro',
    'program/nitro_option.nitro',
    'program/code_container.nitro',
    'program/nitro_type_alias.nitro',
    'program/nitro_struct.nitro',
    'program/nitro_annotation.nitro',
    'program/code_inst.nitro',
    'program/nitro_type_pattern.nitro',
    'util/id_provider.nitro',
    'util/reporter.nitro',
    'util/change_collector.nitro',
    'util/byte_buffer.nitro',
    'util/included_file.nitro',
    'lexer/span.nitro',
    'lexer/lexer.nitro',
    'lexer/token.nitro',
    'includes.nitro',
    'compilation_unit.nitro',
    'tests.nitro',
    'type_checker/type/type_kind.nitro',
    'type_checker/type/create.nitro',
    'type_checker/type/type_token.nitro',
    'type_checker/type/utils.nitro',
    'type_checker/type/type.nitro',
    'type_checker/type/replace.nitro',
    'type_checker/type/generics.nitro',
    'type_checker/type/type_container.nitro',
    'type_checker/check/accessor.nitro',
    'type_checker/check/find.nitro',
    'type_checker/check/fun_call.nitro',
    'type_checker/check/code.nitro',
    'type_checker/check/upvalue.nitro',
    'type_checker/check/unify.nitro',
    'type_checker/check/type_usage.nitro',
    'type_checker/check/lambda.nitro',
    'type_checker/check/tag.nitro',
    'type_checker/check/load_field.nitro',
    'type_checker/check/type_pattern.nitro',
    'type_checker/check/fun_call_target.nitro',
    'type_checker/check/alloc.nitro',
    'type_checker/check/load_var.nitro',
    'type_checker/codegen.nitro',
    'type_checker/type_checker.nitro',
    'type_checker/type_constraints.nitro',
  ];
  for (let file of compilerFiles) {
    const path = 'src/compiler/' + file;
    await createParentDirs(fs, path);
    const url = new URL(`../../src/main/nitro/compiler/${file}`, import.meta.url).href;
    await fs.writeFile(path, new Uint8Array(await (await fetch(url)).arrayBuffer()));
  }

  // Copy examples
  const files = ['binary_tree.nitro', 'fibonacci.nitro', 'hello_world.nitro', 'linked_list.nitro', 'mandelbrot_fractal.nitro', 'advent_of_code_2023_3.nitro'];
  for (let file of files) {
    const path = 'src/examples/' + file;
    await createParentDirs(fs, path);
    const url = new URL(`../../examples/${file}`, import.meta.url).href;
    await fs.writeFile(path, new Uint8Array(await (await fetch(url)).arrayBuffer()));
  }

  return fs;
}

async function createParentDirs(fs, path) {
  const parts = path.split('/');
  parts.pop();
  let subPath = '';
  for (let part of parts) {
    try {
      await fs.createDir(subPath + '/' + part);
    } catch (_) {
      // ignore
    }
    subPath = subPath + '/' + part;
  }
}

export async function saveFS(fs) {
  const saveDir = async (path) => {
    const entries = await fs.readDir(path);

    for (const entry of entries) {
      const entryPath = path + entry.name;

      if (entryPath.startsWith('/src/core/') || entryPath.startsWith('/src/compiler/') || entryPath.startsWith('/src/examples/')) {
        continue;
      }

      if (entry.type === "file") {
        const fileContents = await fs.readFile(entryPath);
        const hex = [...fileContents].map(b => b.toString(16).padStart(2, '0')).join('');
        localStorage.setItem(`fs${entryPath}`, hex);
      }

      if (entry.type === "dir") {
        await saveDir(entryPath + '/');
      }
    }
  }

  await saveDir('/');
}

export async function updateFiles() {
  const fileTree = document.querySelector('#file-tree');
  const fileTemplate = document.querySelector('#files-file-template');
  const project = await getProject();

  const root = await project.readDir('/');
  fileTree.innerHTML = '';

  /** @type {Set<string>} */
  const expandedDirs = document.expandedDirs;

  const iter = async (entry, path, level) => {
    const currentPath = path + '/' + entry.name;

    const elem = fileTemplate.content.cloneNode(true);
    const rootElem = elem.querySelector('.file');
    entry.type === 'dir' && rootElem.classList.add('file-directory');
    entry.type === 'file' && rootElem.classList.add('file-regular');
    entry.type === 'unknown' && rootElem.classList.add('file-unknown');
    rootElem.style.paddingLeft = `${level * 29}px`;

    const nameElem = elem.querySelector('.name');
    nameElem.textContent = entry.name;
    fileTree.appendChild(elem);

    if (entry.type === 'file') {
      rootElem.addEventListener('click', () => {
        editorSelectFile(currentPath)
      });
    }

    if (entry.type === 'dir') {
      rootElem.addEventListener('click', () => {
        if (expandedDirs.has(currentPath)) {
          expandedDirs.delete(currentPath);
        } else {
          expandedDirs.add(currentPath);
        }
        updateFiles().catch(console.error);
      });
    }

    if (entry.type === 'dir') {
      const expanded = expandedDirs.has(currentPath);

      if (expanded) {
        rootElem.classList.add('expanded');

        const children = await project.readDir(currentPath);

        children.sort((a, b) => {
          if (a.type === 'dir' && b.type !== 'dir') return -1;
          if (a.type !== 'dir' && b.type === 'dir') return 1;
          return a.name.localeCompare(b.name);
        });

        for (const child of children) {
          await iter(child, currentPath, level + 1);
        }
      }
    }
  }

  for (const entry of root) {
    await iter(entry, '', 0);
  }
}
