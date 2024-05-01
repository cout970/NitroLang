import {getString, createString, fs} from './internal.ts'

interface StackFrame {
    name: string;
    file: string;
    alloc_total: number;
    start_time: number;
}

interface FlameGraphNode {
    name: string;
    value: number;
    children: FlameGraphNode[];
    parent: FlameGraphNode | null;
}

const trace_stack: StackFrame[] = [];
const root: FlameGraphNode = {name: 'root', value: 0, children: []};
const flame_graph_stack: FlameGraphNode[] = [root];
const flame_graph_enable = false;
const min_samples = 5;
const trace_type: 'cpu'|'memory' = 'cpu';

export function trace_enter(func_info: string) {
    // func_info has format "len: main/nitro/core/collections/list.nitro(list.nitro:258)"
    const name = func_info.split(":")[0];
    const file = func_info.split("(")[1].slice(0, -1);

    trace_stack.push({
        name,
        file,
        alloc_total: 0,
        start_time: performance.now()
    });

    if (!flame_graph_enable) return;

    const child = {
        name: `${name} // ${file}`,
        value: 0,
        children: [],
        enable: true,
    };
    const current = flame_graph_stack[flame_graph_stack.length - 1];
    current.children.push(child);
    flame_graph_stack.push(child);
}

export function trace_exit() {
    const frame = trace_stack.pop();

    if (!flame_graph_enable) return;

    const current = flame_graph_stack[flame_graph_stack.length - 1];
    const now = performance.now();

    if (trace_type === 'memory') {
        current.value = frame.alloc_total + current.children.reduce((acc, child) => acc + child.value, 0);
    } else {
        current.value = now - frame.start_time;
    }

    if (current.value < min_samples) {
        current.enable = false;
    }

    // Untested, may reduce memory usage
    // current.children = current.children.filter(child => child.enable);

    if (flame_graph_stack.length > 1) {
        flame_graph_stack.pop();
    }
}

export function trace_alloc(size: number) {
    if (!trace_stack.length) return;
    const frame = trace_stack[trace_stack.length - 1];
    frame.alloc_total += size;
}

export function trace_get_stacktrace(): string {
    return trace_stack.toReversed().map(frame => '  at ' + frame.name + '(' + frame.file + ')').join('\n');
}

export function trace_print_stack_trace() {
    const str = trace_get_stacktrace();

    if (str) {
        console.log(`Stack trace:  \n${trace_get_stacktrace()}`);
    } else {
        console.log('No internal stack trace available');
    }
}

export function trace_trim_flame_graph(node: FlameGraphNode): FlameGraphNode|null {
    node.total = node.value;

    const children = node.children.map(trace_trim_flame_graph).filter(Boolean);
    const total = node.total + children.reduce((acc, child) => acc + child.value, 0);

    if (total <= min_samples && node.name !== 'root') {
        return null;
    }

    return {...node, total: total, children};
}

export function trace_save_flame_graph(path) {
    if (!fs.isSupported) {
        console.error("Filesystem not supported");
        return;
    }

    try {
        const trimmed = trace_trim_flame_graph(root) as any;
        trimmed.type = trace_type;
        const json = JSON.stringify(trimmed, null, 2);
        fs.writeTextFileSync(path, json);
    } catch (e) {
        console.error("Failed to write flamegraph.json", e);
    }
}

