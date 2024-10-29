import {defineConfig} from "vite";
import {NodeGlobalsPolyfillPlugin} from '@esbuild-plugins/node-globals-polyfill'

const wasmContentType = () => ({
  name: 'configure-server',
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      if (req.originalUrl.endsWith('.wasm')) {
        const prev = res.writeHead;

        // Content type is overridden at the end of the response process, so we need to override it in writeHead()
        res.writeHead = ((writeHead) => (...args) => {
          res.setHeader('Content-Type', 'application/wasm');
          writeHead.apply(res, args);
        })(prev);
      }
      next()
    })
  },
})

export default defineConfig({
  server: {
    headers: {
      "Cross-Origin-Opener-Policy": "same-origin",
      "Cross-Origin-Embedder-Policy": "require-corp",
    },
  },
  plugins: [wasmContentType()],
  resolve: {
    extensions: ['.js', '.wasm']
  },
  build: {
    modulePreload: {
      polyfill: false,
    },
    rollupOptions: {
      input: {
        'main': 'index.html'
      },
      output: {
        manualChunks: {
          'wasmer': ['@wasmer/sdk']
        }
      }
    }
  },
  optimizeDeps: {
    exclude: ['*.wasm'],
    esbuildOptions: {
      // Node.js global to browser globalThis
      define: {
        global: 'globalThis',
      },
      // Enable esbuild polyfill plugins
      plugins: [
        NodeGlobalsPolyfillPlugin({
          buffer: true
        })
      ]
    }
  }
});
