package nitrolang

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import nitrolang.util.ErrorCollector
import java.net.InetSocketAddress
import java.time.Instant
import java.util.Base64
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

fun listen(opts: CompilerOptions) {
    val setting = opts.server!!

    val (bindAddress, port) = when {
        setting.contains(":") -> setting.split(":").let {
            it[0] to it[1]
        }

        else -> "0.0.0.0" to setting
    }

    val address = InetSocketAddress(bindAddress, port.toIntOrNull() ?: error("Invalid port"))

    val server = HttpServer.create(address, 0)
    server.createContext("/", MyHandler(opts))
    server.executor = null // creates a default executor
    server.start()
    println("Server started at $address")
}

class MyHandler(val opts: CompilerOptions) : HttpHandler {
    private val gson = GsonBuilder().create()
    private val base64 = Base64.getEncoder()
    private val startTime = ThreadLocal.withInitial { Instant.now() }

    override fun handle(t: HttpExchange) {
        // CORS
        t.responseHeaders.add("Access-Control-Allow-Origin", "*")
        t.responseHeaders.add("Access-Control-Allow-Methods", "POST, OPTIONS")
        t.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
        t.responseHeaders.add("Access-Control-Max-Age", "86400")

        if (t.requestMethod == "OPTIONS") {
            t.sendResponseHeaders(200, 0)
            t.responseBody.close()
            return
        }

        val start = Instant.now()
        startTime.set(start)
        println("[$start] Request: ${t.requestMethod} ${t.requestURI} (${t.requestBody.available()} bytes)")

        if (t.requestMethod != "POST") {
            sendError(t, 400, "Invalid method")
        }
        if (t.requestURI.path != "/compile") {
            sendError(t, 404, "Not found")
        }

        val obj: JsonObject = try {
            gson.fromJson(t.requestBody.reader(), JsonObject::class.java)
        } catch (e: Exception) {
            sendError(t, 400, "Invalid JSON")
            return
        }

        val format = when (t.requestHeaders["Accept"]?.first()) {
            "application/json" -> "application/json"
            "application/wat" -> "application/wat"
            "application/wasm" -> "application/wasm"
            else -> "application/json"
        }

        val sourcePrim = obj.getAsJsonPrimitive("source") ?: return sendError(t, 400, "Missing source")
        val source = sourcePrim.asString ?: return sendError(t, 400, "Invalid source")

        val inputFile = kotlin.io.path.createTempFile(prefix = "in_", suffix = ".nitro")
        val watFile = kotlin.io.path.createTempFile(prefix = "out_", suffix = ".wat")
        val wasmFile = kotlin.io.path.createTempFile(prefix = "out_", suffix = ".wasm")

        try {
            inputFile.writeText(source)

            val copy = opts.copy(
                source = inputFile.absolutePathString(),
                output = watFile.absolutePathString()
            )

            val program = compileToWat(copy)

            if (program.collector.isNotEmpty()) {
                sendCompilationError(t, program.collector)
                return
            }

            when (format) {
                "application/wat" -> sendOk(t, format, watFile.readBytes())
                "application/wasm" -> {
                    if (!compileToWasm(copy, watFile.toFile(), wasmFile.toFile())) {
                        sendError(t, 500, "Internal compilation error. This is a bug on the compiler, please report.")
                        return
                    }
                    sendOk(t, format, wasmFile.readBytes())
                }

                "application/json" -> {
                    if (!compileToWasm(copy, watFile.toFile(), wasmFile.toFile())) {
                        sendError(t, 500, "Internal compilation error. This is a bug on the compiler, please report.")
                        return
                    }

                    val end = Instant.now()
                    val elapsed = end.toEpochMilli() - start.toEpochMilli()

                    sendOk(
                        t, format, gson.toJson(
                            mapOf(
                                "compilation_time_ms" to elapsed,
                                "wat" to base64.encodeToString(watFile.readBytes()),
                                "wasm" to base64.encodeToString(wasmFile.readBytes()),
                            )
                        ).toByteArray()
                    )
                }
            }
        } catch (e: Throwable) {
            sendError(t, 500, e.message ?: "Unknown error")
            e.printStackTrace()
            return
        } finally {
            inputFile.deleteIfExists()
            watFile.deleteIfExists()
            wasmFile.deleteIfExists()
        }
    }

    private fun sendOk(t: HttpExchange, type: String, response: ByteArray) {
        t.responseHeaders.add("Content-Type", type)
        t.sendResponseHeaders(200, response.size.toLong())
        t.responseBody.write(response)
        t.responseBody.close()

        val end = Instant.now()
        val start = startTime.get()
        val elapsed = end.toEpochMilli() - start.toEpochMilli()
        println("[$end] Response: ${t.requestMethod} ${t.requestURI} HTTP 200 OK (${response.size} bytes, $elapsed ms)")
    }

    private fun sendCompilationError(t: HttpExchange, collector: ErrorCollector) {
        val message = "Compilation error"
        val response = gson.toJson(
            mapOf(
                "error" to message,
                "info" to collector.toString(),
            )
        ).toByteArray()
        t.responseHeaders.add("Content-Type", "application/json")
        t.sendResponseHeaders(400, response.size.toLong())
        t.responseBody.write(response)
        t.responseBody.close()

        val end = Instant.now()
        val start = startTime.get()
        val elapsed = end.toEpochMilli() - start.toEpochMilli()
        println("[$end] Response: ${t.requestMethod} ${t.requestURI} HTTP 400 $message (${response.size} bytes, $elapsed ms)")
    }

    private fun sendError(t: HttpExchange, code: Int, message: String) {
        val response = gson.toJson(
            mapOf(
                "error" to message
            )
        ).toByteArray()
        t.responseHeaders.add("Content-Type", "application/json")
        t.sendResponseHeaders(code, response.size.toLong())
        t.responseBody.write(response)
        t.responseBody.close()

        val end = Instant.now()
        val start = startTime.get()
        val elapsed = end.toEpochMilli() - start.toEpochMilli()
        println("[$end] Response: ${t.requestMethod} ${t.requestURI} HTTP $code $message (${response.size} bytes, $elapsed ms)")
    }
}