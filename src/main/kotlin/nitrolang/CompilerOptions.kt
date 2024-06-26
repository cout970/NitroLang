package nitrolang

import java.io.File

data class CompilerOptions(
    var source: String = "main.nitro",
    var output: String = "out.wat",
    var server: String? = null,
    var includes: MutableList<String> = mutableListOf(),
    var namespaces: MutableList<Pair<String, String>> = mutableListOf(),
    var profile: Boolean = false,
    var execute: Boolean = false,
    var dumpIr: Boolean = false,
    var dumpExtern: Boolean = false,
    var dumpWasm: Boolean = false,
    var runTests: Boolean = false,
    var optimize: Boolean = false,
    var traceFunctions: Boolean = false,
    var postExecScript: String? = null,
    var listenChangesToCompile: MutableList<String> = mutableListOf(),
    var listenChangesToExecute: MutableList<String> = mutableListOf(),
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
                        i++
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

                    "-O", "--optimize" -> {
                        i++
                        opt.optimize = true
                    }

                    "-n", "--namespace" -> {
                        i++
                        if (args.size <= i) {
                            println("Missing namespace")
                            return null
                        }

                        val arg = args[i]
                        i++

                        if (!arg.contains(":")) {
                            println("Required namespace and path separated by a colon")
                            return null
                        }

                        val ns = arg.substringBefore(":")
                        val include = arg.substringAfter(":")

                        if (!File(include).exists()) {
                            println("File not found: $include")
                            return null
                        }
                        if (!File(include).isDirectory) {
                            println("This is a regular file, it must be a directory: $include")
                            return null
                        }

                        // Avoid duplicated includes
                        opt.namespaces.add(ns to include)
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
                                opt.listenChangesToCompile.add(path)
                            } else {
                                println("Watch path does not exist: $path")
                            }
                        }
                        i++
                    }
                    "-x", "--exec-watch" -> {
                        i++
                        if (args.size <= i) {
                            println("Missing watch path")
                            return null
                        }

                        args[i].split(',').map { it.trim() }.forEach { path ->
                            if (File(path).exists()) {
                                opt.listenChangesToExecute.add(path)
                            } else {
                                println("Watch path does not exist: $path")
                            }
                        }
                        i++
                    }

                    "-p", "--post-exec" -> {
                        i++
                        if (args.size <= i) {
                            println("Missing post-exec path")
                            return null
                        }

                        opt.postExecScript = args[i]
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
            println("  -n, --namespace <ns>:<file>  Add an alias to a folder to use in includes")
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