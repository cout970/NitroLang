package nitrolang.util

import java.time.Instant
import java.time.temporal.ChronoUnit

object Prof {
    var enable: Boolean = false
    var maxDepth: Int = 1
    var minDuration: Int = 5
    private val stack = ArrayDeque<Pair<String, Instant>>()

    fun start(name: String) {
        if (enable) stack.add(name to Instant.now())
    }

    fun next(name: String) {
        end()
        start(name)
    }

    fun end() {
        if (!enable) return
        val (name, start) = stack.removeLastOrNull() ?: return
        val depth = stack.size
        val end = Instant.now()
        val interval = start.until(end, ChronoUnit.MILLIS)

        if (interval > minDuration && (maxDepth == -1 || depth < maxDepth)) {
            println("${"| ".repeat(depth)}$name: $interval ms")
        }
    }

    fun endAll() {
        while (stack.isNotEmpty()) end()
    }

    inline fun <T> run(name: String, op: () -> T): T {
        start(name)
        val result = op()
        end()
        return result
    }
}