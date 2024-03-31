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
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoBoolean($value) // $span"
}

// Push Int to stack
class MonoInt(
    id: MonoRef, span: Span,
    var value: Int,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoInt($value) // $span"
}

// Push Long to stack
class MonoLong(
    id: MonoRef, span: Span,
    var value: Long,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLong($value) // $span"
}

// Push Float to stack
class MonoFloat(
    id: MonoRef, span: Span,
    val value: Float,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoFloat($value) // $span"
}

// Push address of String to stack
class MonoString(
    id: MonoRef, span: Span,
    val value: String,
    val type: MonoType,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoString('$value') // $span"
}

// Push 0 to stack
class MonoNothing(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoNothing() // $span"
}

// Begin an if block
class MonoIfStart(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoIfStart() // $span"
}

// End the first part of the if block and start the else part
class MonoIfElse(
    id: MonoRef, span: Span
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoIfElse() // $span"
}

// Select between two values on the stack based on a condition
// ifTrue ifFalse cond -- result
class MonoIfChoose(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoIfChoose() // $span"
}

// Start a regular block, where jumps go to the end of the block
class MonoStartBlock(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoStartBlock() // $span"
}

// Starts a loop block, where jumps go to the beginning of the block
class MonoStartLoop(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoStartLoop() // $span"
}

// Ends a block
class MonoEndBlock(
    id: MonoRef, span: Span,
    val type: String
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoEndBlock($type) // $span"
}

// Jump to the beginning or the end of block, depending on the type of the block.
// The block is selected by the depth of the jump
class MonoJump(
    id: MonoRef, span: Span,
    val depth: Int,
    val backwards: Boolean,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoJump($depth, $backwards) // $span"
}

// Return from the current function
class MonoReturn(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoReturn() // $span"
}

// Prints a comment in the generated wat code
class MonoComment(
    id: MonoRef, span: Span,
    val msg: String,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoComment('$msg') // $span"
}

// Load a value from a constant on the data section
class MonoLoadConst(
    id: MonoRef, span: Span,
    val const: MonoConst,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLoadConst('$const', ${const.type}) // $span"
}

// Load a value from a function parameter
class MonoLoadParam(
    id: MonoRef, span: Span,
    val param: MonoParam,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLoadParam('${param.name}', ${param.type}) // $span"
}

// Load a value from a local variable
class MonoLoadVar(
    id: MonoRef, span: Span,
    val variable: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLoadVar('${variable.name}', ${variable.type}) // $span"
}

// Store a value to a local variable
class MonoStoreVar(
    id: MonoRef, span: Span,
    val variable: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoStoreVar('${variable.name}', ${variable.type}) // $span"
}

// Allocate space in heap for the upValue
class MonoCreateUpValue(
    id: MonoRef, span: Span,
    val upValue: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoCreateUpValue('${upValue.name}', ${upValue.type}) // $span"
}

// Load an upValue from the lambda closure
class MonoInitUpValue(
    id: MonoRef, span: Span,
    val upValue: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoInitUpValue('${upValue.name}', ${upValue.type}) // $span"
}

// Load a value from a local variable
class MonoLoadUpValue(
    id: MonoRef, span: Span,
    val upValue: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLoadUpValue('${upValue.name}', ${upValue.type}) // $span"
}

// Store a value to a local variable
class MonoStoreUpValue(
    id: MonoRef, span: Span,
    val upValue: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoStoreUpValue('${upValue.name}', ${upValue.type}) // $span"
}

// Load a value from a field inside a struct
class MonoLoadField(
    id: MonoRef, span: Span,
    val instanceType: MonoType,
    val field: MonoStructField,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLoadField($instanceType, '${field.name}', ${field.type}) // $span"
}


// Store a value to a field inside a struct
class MonoStoreField(
    id: MonoRef, span: Span,
    val instanceType: MonoType,
    val field: MonoStructField,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoStoreField($instanceType, '${field.name}', ${field.type}) // $span"
}

// Call a function
class MonoFunCall(
    id: MonoRef, span: Span,
    val function: MonoFunction,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoFunCall('${function.signature}') // $span"
}

// Ejecute a wasm native opcode
class MonoLambdaInit(
    id: MonoRef, span: Span,
    val type: MonoType,
    val lambda: MonoFunction,
    // Lambda variable and linked function variable
    val localUpValues: List<Pair<MonoVar, MonoVar>>,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLambdaInit('${lambda.signature}', $type) // $span"
}

// Appends inline assembly code
class MonoInline(
    id: MonoRef, span: Span,
    val inline: String,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoInline('$inline') // $span"
}

// Given an address to a struct, returns the address of a field inside the struct
class MonoGetFieldAddress(
    id: MonoRef, span: Span,
    val struct: MonoStruct,
    val field: MonoStructField,
) : MonoInstruction(id, span) {
    override fun toString(): String =
        "$id: MonoGetFieldAddress('${struct.instance.name}', '${field.name}', ${field.type}) // $span"
}

// Crashes the program
class MonoUnreachable(
    id: MonoRef, span: Span,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoUnreachable() // $span"
}

// Call a lambda function
class MonoLambdaCall(
    id: MonoRef, span: Span,
    val functionType: MonoType,
    val args: Int,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoLambdaCall($functionType, $args) // $span"
}

// Load a value from memory using the pointer on the stack and an offset
class MonoMemoryLoad(
    id: MonoRef, span: Span,
    val type: MonoType,
    val offset: Int,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoMemoryLoad($type, $offset) // $span"
}

// Duplicate the top of the stack
class MonoDup(
    id: MonoRef, span: Span,
    val auxLocal: MonoVar,
    val type: MonoType,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoDup($type) // $span"
}

// Swap the top two values of the stack
class MonoSwap(
    id: MonoRef, span: Span,
    val auxLocal0: MonoVar,
    val auxLocal1: MonoVar,
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoSwap() // $span"
}

// Synthetic node, used to mark a value that can be pushed to the stack or be ignored
class MonoProvider(
    id: MonoRef, span: Span,
    val type: MonoType
) : MonoInstruction(id, span) {
    val consumers = mutableListOf<MonoConsumer>()
    override fun toString(): String = "$id: MonoProvider($type) // $span"
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

    override fun toString(): String = "$id: MonoConsumer() // $span"
}

// Drop a value from the stack
class MonoDrop(
    id: MonoRef, span: Span,
    val type: MonoType
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoDrop($type) // $span"
}

// No-operation
class MonoNoop(
    id: MonoRef, span: Span
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoNoop() // $span"
}

// Code generated by the backend
class MonoAutogenerate(
    id: MonoRef, span: Span,
    val signature: MonoFuncSignature
) : MonoInstruction(id, span) {
    override fun toString(): String = "$id: MonoAutogenerate('$signature') // $span"
}