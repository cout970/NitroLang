// vite.config.js
import { defineConfig } from 'vite'
import wasmHMRPlugin from './wasm-plugin'

export default defineConfig({
  plugins: [wasmHMRPlugin()],
  server: {
    watch: {
      // Include WASM files in the watch list
      included: ['**/*.wasm'],
      // Make sure they're not excluded
      excluded: ['**/node_modules/**', '**/dist/**']
    },
    // Force the server to restart when WASM files change
    hmr: {
      protocol: 'ws',
      host: 'localhost',
      // Optional: Add a custom handler for WASM file changes
      serverHandlers: [
        ({ file, server }) => {
          if (file.endsWith('.wasm')) {
            // Force a full reload when WASM files change
            server.ws.send({ type: 'full-reload' });
            return true; // Indicate that we've handled this file
          }
        }
      ]
    }
  },
  optimizeDeps: {
    // Exclude WASM files from optimization to avoid issues
    exclude: ['**/*.wasm']
  }
})
