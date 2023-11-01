package nitrolang.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import nitrolang.typeinference.TypeBox

interface Dumpable {
    fun dump(): JsonElement
}

fun <K, V : Dumpable> Map<K, V>.dump(): JsonArray = JsonArray().also { obj ->
    this.values.forEach { t -> obj.add(t.dump()) }
}

fun <T : Dumpable> List<T>.dump(): JsonArray = JsonArray().also { obj ->
    this.forEach { t -> obj.add(t.dump()) }
}

fun <T : Dumpable> Set<T>.dump(): JsonArray = JsonArray().also { obj ->
    this.forEach { t -> obj.add(t.dump()) }
}

fun TypeBox.dump(): JsonElement = JsonPrimitive(type.toString())

fun String.dump(): JsonElement = JsonPrimitive(this)

fun Int.dump(): JsonElement = JsonPrimitive(this)

fun Float.dump(): JsonElement = JsonPrimitive(this)

fun Boolean.dump(): JsonElement = JsonPrimitive(this)
