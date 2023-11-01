package nitrolang.util

import java.time.Instant
import java.time.temporal.ChronoUnit

object Prof {
    private const val ENABLE: Boolean = false
    private val stack = ArrayDeque<Pair<String, Instant>>()

    fun start(name: String) {
        if (ENABLE) stack.add(name to Instant.now())
    }

    fun next(name: String) {
        end()
        start(name)
    }

    fun end() {
        if (!ENABLE) return
        val (name, start) = stack.removeLastOrNull() ?: return
        val depth = stack.size
        val end = Instant.now()
        val interval = start.until(end, ChronoUnit.MILLIS)

        if (interval > 0) {
            println("${"| ".repeat(depth)}$name: $interval ms")
        }
    }
}