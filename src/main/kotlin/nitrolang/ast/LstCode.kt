package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.dump
import nitrolang.util.formatItem
import java.util.*
import kotlin.collections.ArrayDeque

class LstCode : Dumpable {
    // Ref id
    private var counter = 0
    private var lastBlock = 0

    val nodes: MutableList<LstInstruction> = ArrayDeque()

    var returnTypeBox: TypeBox? = null
    var returnType: TType
        get() = returnTypeBox!!.type
        set(v) {
            returnTypeBox!!.type = v
        }

    var lastExpression: Ref? = null

    // Block nesting
    val rootBlock: LstBlock = LstBlock(null, lastBlock++, true)
    var currentBlock: LstBlock = rootBlock

    // Let declarations
    val variables: MutableMap<VarRef, LstVar> = mutableMapOf()

    // Linking loops and break/continue
    var breaks: MutableList<LstLoopJump> = mutableListOf()
    var continues: MutableList<LstLoopJump> = mutableListOf()

    val blockStack: Deque<LstBlock> = java.util.ArrayDeque()
    var jumpedOutOfBlock = false

    fun createBlock(isJumpTarget: Boolean): LstBlock {
        return LstBlock(currentBlock, lastBlock++, isJumpTarget)
    }

    fun currentRef() = Ref(counter)

    fun nextRef() = Ref(counter++)

    fun nextVarRef() = LocalVarRef(counter++)

    fun getInst(ref: Ref): LstInstruction = nodes.find { it.ref == ref } ?: error("Ref not found!")

    override fun dump(): JsonElement = JsonObject().also {
        it.add("variables", variables.dump())
        it.add("nodes", nodes.dump())
    }

    fun toPrettyString(): String {
        return "LstCode {\n" +
                "  variables=${formatItem(variables.values)}\n" +
                "  nodes=${formatItem(nodes)}\n" +
                "}"
    }

    override fun toString(): String {
        // Printing all the variables and nodes slows down the debugger
        return "LstCode ()"
    }

    fun enterBlock(isJumpTarget: Boolean) {
        blockStack.addLast(currentBlock)
        currentBlock = createBlock(isJumpTarget)
    }

    fun exitBlock() {
        if (!jumpedOutOfBlock && currentBlock.deferredActions.isNotEmpty()) {
            currentBlock.deferredActions.reversed().forEach { it() }
        }
        jumpedOutOfBlock = false
        currentBlock = blockStack.removeLast()
    }

    fun exitDeferBlock() {
        currentBlock = blockStack.removeLast()
    }

    fun executeDeferredActions(block: LstBlock = currentBlock) {
        if (block.deferredActions.isNotEmpty()) {
            block.deferredActions.reversed().forEach { it() }
        }

        if (block.parent != null && !block.isJumpTarget) {
            executeDeferredActions(block.parent)
        }
    }
}