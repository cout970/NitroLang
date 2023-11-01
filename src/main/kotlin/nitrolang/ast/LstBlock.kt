package nitrolang.ast

import com.google.gson.JsonElement
import nitrolang.util.Dumpable
import nitrolang.util.dump

data class LstBlock(
    val parent: LstBlock? = null,
    val id: Int,
    val isJumpTarget: Boolean,
) : Dumpable {
    val depth: Int get() = if (parent == null) 0 else parent.depth + 1
    val deferredActions = mutableListOf<() -> Unit>()

    override fun toString(): String = "block#$id/$depth"

    override fun dump(): JsonElement = toString().dump()
}