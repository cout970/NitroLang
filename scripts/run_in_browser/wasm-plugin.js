// wasm-plugin.js
export default function wasmHMRPlugin() {
  return {
    name: 'wasm-hmr',
    // Add WASM file type to the list of handled assets
    enforce: 'pre',

    // Handle WASM file imports
    resolveId(source) {
      if (source.endsWith('.wasm')) {
        return source; // Keep the source path as is
      }
      return null;
    },

    // Track WASM file changes
    handleHotUpdate({ file, server }) {
      if (file.endsWith('.wasm')) {
        console.log('WASM file changed:', file);
        // Force reload the page when a WASM file changes
        server.ws.send({ type: 'full-reload' });
        return []; // Don't let Vite handle this update
      }
    }
  };
}
