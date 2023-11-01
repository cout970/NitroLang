package nitrolang.backend

import nitrolang.util.Span

// Unique identifier for a node
typealias MonoRef = Int

// Monomorphized version of LstInstruction
sealed class MonoInstruction(
    val id: MonoRef,
    val span: Span,
)

// Push Boolean to stack
class MonoBoolean(
    id: MonoRef, span: Span,
    val value: Boolean,
) : MonoInstruction(id, span)

// Push Int to stack
class MonoInt(
    id: MonoRef, span: Span,
    var value: Int,
) : MonoInstruction(id, span)

// Push Float to stack
class MonoFloat(
    id: MonoRef, span: Span,
    val value: Float,
) : MonoInstruction(id, span)

// Push address of String to stack
class MonoString(
    id: MonoRef, span: Span,
    val value: String,
    val type: MonoType,
) : MonoInstruction(id, span)

// Push 0 to stack
class MonoNothing(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// Begin an if block
class MonoIfStart(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// End the first part of the if block and start the else part
class MonoIfElse(
    id: MonoRef, span: Span
) : MonoInstruction(id, span)

// Select between two values on the stack based on a condition
// ifTrue ifFalse cond -- result
class MonoIfChoose(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// Start a regular block, where jumps go to the end of the block
class MonoStartBlock(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// Starts a loop block, where jumps go to the beginning of the block
class MonoStartLoop(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// Ends a block
class MonoEndBlock(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// Jump to the beginning or the end of block, depending on the type of the block.
// The block is selected by the depth of the jump
class MonoJump(
    id: MonoRef, span: Span,
    val depth: Int,
) : MonoInstruction(id, span)

// Return from the current function
class MonoReturn(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span)

// Prints a comment in the generated wat code
class MonoComment(
    id: MonoRef, span: Span,
    val msg: String,
) : MonoInstruction(id, span)

// Load a value from a constant on the data section
class MonoLoadConst(
    id: MonoRef, span: Span,
    val const: MonoConst,
) : MonoInstruction(id, span)

// Load a value from a local variable
class MonoLoadVar(
    id: MonoRef, span: Span,
    val variable: MonoVar,
) : MonoInstruction(id, span)

// Store a value to a local variable
class MonoStoreVar(
    id: MonoRef, span: Span,
    val variable: MonoVar,
) : MonoInstruction(id, span)

// Load a value from a field inside a struct
class MonoLoadField(
    id: MonoRef, span: Span,
    val instanceType: MonoType,
    val field: MonoStructField,
) : MonoInstruction(id, span)

// Store a value to a field inside a struct
class MonoStoreField(
    id: MonoRef, span: Span,
    val instanceType: MonoType,
    val field: MonoStructField,
) : MonoInstruction(id, span)

// Call a function
class MonoFunCall(
    id: MonoRef, span: Span,
    val function: MonoFunction,
) : MonoInstruction(id, span)

// Ejecute a wasm native opcode
class MonoOpcode(
    id: MonoRef, span: Span,
    val opcode: String,
) : MonoInstruction(id, span)

// Ejecute a wasm native opcode
class MonoIndirectCall(
    id: MonoRef, span: Span,
    val functionType: MonoType,
) : MonoInstruction(id, span)

// Duplicate the top of the stack
class MonoDup(
    id: MonoRef, span: Span,
    val auxLocal: MonoVar,
    val type: MonoType,
) : MonoInstruction(id, span)

// Swap the top two values of the stack
class MonoSwap(
    id: MonoRef, span: Span,
    val auxLocal0: MonoVar,
    val auxLocal1: MonoVar,
) : MonoInstruction(id, span)

// Synthetic node, used to mark a value that can be pushed to the stack or be ignored
class MonoProvider(
    id: MonoRef, span: Span,
    val type: MonoType
) : MonoInstruction(id, span) {
    val consumers = mutableListOf<MonoConsumer>()
}

// Synthetic node, used to mark a value that is required and must be present on the stack
class MonoConsumer(
    id: MonoRef, span: Span,
    provider: MonoProvider
) : MonoInstruction(id, span) {
    var variable: MonoVar? = null

    init {
        provider.consumers += this
    }
}

// Drop a value from the stack
class MonoDrop(
    id: MonoRef, span: Span,
    val type: MonoType
) : MonoInstruction(id, span)

// No-operation
class MonoNoop(
    id: MonoRef, span: Span
) : MonoInstruction(id, span)