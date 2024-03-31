package nitrolang

import java.io.File

data class CompilerOptions(
    var source: String = "main.nitro",
    var output: String = "out.wat",
    var server: String? = null,
    var includes: MutableList<String> = mutableListOf(),
    var profile: Boolean = false,
    var execute: Boolean = false,
    var dumpIr: Boolean = false,
    var dumpExtern: Boolean = false,
    var dumpWasm: Boolean = false,
    var runTests: Boolean = false,
    var traceFunctions: Boolean = false,
    var listenChanges: MutableList<String> = mutableListOf(),
) {
    companion object {
        fun fromArgs(args: Array<String>): CompilerOptions? {
            if (args.isEmpty()) {
                return null
            }

            val opt = CompilerOptions()
            var sourceCount = 0
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "-o", "--output" -> {
                        i++
                        opt.output = args[i]
                        i++
                    }

                    "-i", "--include" -> {
                        i++
                        if (args.size <= i) {
                            println("Missing include file")
                            return null
                        }

                        val include = args[i]
                        if (!File(include).exists()) {
                            println("Include file not found: $include")
                            return null
                        }
                        if (File(include).isDirectory) {
                            println("This include is a directory, must be a regular file: $include")
                            return null
                        }

                        // Avoid duplicated includes
                        if (include !in opt.includes) {
                            opt.includes.add(include)
                        }
                    }

                    "-s", "--server" -> {
                        i++
                        if (args.size <= i) {
                            println("Missing server <ip>:<port>")
                            return null
                        }
                        opt.server = args[i]
                        i++
                    }

                    "-t", "--test" -> {
                        i++
                        opt.runTests = true
                    }

                    "-w", "--watch" -> {
                        i++
                        if (args.size <= i) {
                            println("Missing watch path")
                            return null
                        }

                        args[i].split(',').map { it.trim() }.forEach { path ->
                            if (File(path).exists()) {
                                opt.listenChanges.add(path)
                            } else {
                                println("Watch path does not exist: $path")
                            }
                        }
                        i++
                    }

                    "-e", "--execute" -> {
                        i++
                        opt.execute = true
                    }

                    "--profile" -> {
                        i++
                        opt.profile = true
                    }

                    "--trace" -> {
                        i++
                        opt.traceFunctions = true
                    }

                    "--dump-ir" -> {
                        i++
                        opt.dumpIr = true
                    }
                    "--dump-extern" -> {
                        i++
                        opt.dumpExtern = true
                    }

                    "--dump-wasm" -> {
                        i++
                        opt.dumpWasm = true
                    }

                    "-h", "--help" -> {
                        return null
                    }

                    else -> {
                        if (args[i].startsWith('-')) {
                            println("Unknown option: ${args[i]}")
                            return null
                        }

                        if (sourceCount > 0) {
                            println("Multiple source files specified, only one main file is allowed, please use --include for additional files")
                            return null
                        }

                        opt.source = args[i]
                        sourceCount++
                        i++
                    }
                }
            }

            // Server mode ignores the input/output files
            if (opt.server != null) {
                return opt
            }

            if (!File(opt.source).exists()) {
                println("Source file not found: ${opt.source}")
                return null
            }

            if (File(opt.output).isDirectory) {
                println("Output file already exists and is a directory: ${opt.output}")
                return null
            }

            return opt
        }

        fun showUsage() {
            println("Usage: nitro [options] main.nitro")
            println("Options:")
            println("  -o, --output <file>          Output file")
            println("  -i, --include <file>         Includes a file to the compilation unit")
            println("  -t, --test                   Run tests")
            println("  -w, --watch <dir>[,<dir2>]   Watch directories for changes")
            println("  -e, --execute                Execute the compiled program")
            println("  -s, --server                 Start a server at <ip>:<port> (or just <port>)")
            println("  --profile                    Show profiling information")
            println("  --dump-ir                    Dump IR")
            println("  --dump-wasm                  Dump WASM")
            println("  -h, --help                   Show this help")
        }
    }
}