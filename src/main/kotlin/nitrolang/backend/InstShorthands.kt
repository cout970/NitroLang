package nitrolang.backend

import nitrolang.ast.Ref
import nitrolang.util.Span

fun MonoBuilder.int(span: Span, value: Int) {
    val mono = current!!
    mono.instructions += MonoInt(mono.nextId(), span, value)
}

fun MonoBuilder.call(span: Span, function: String, ctx: MonoCtx) {
    val mono = current!!
    val lstFunc = program.getFunction(function)
    val monoFunction = getMonoFunction(lstFunc, ctx)
    mono.instructions += MonoFunCall(mono.nextId(), span, monoFunction)
}

fun MonoBuilder.drop(span: Span, type: MonoType) {
    val mono = current!!
    mono.instructions += MonoDrop(mono.nextId(), span, type)
}

fun MonoBuilder.dup(span: Span, type: MonoType) {
    val mono = current!!
    val dupAux = MonoVar(
        id = mono.variables.size,
        name = "dup-${mono.instructions.size}",
        type = type,
        varRef = null
    )
    mono.variables += dupAux

    mono.instructions += MonoDup(mono.nextId(), span, dupAux, type)
}

fun MonoBuilder.swap(span: Span, top: MonoType, bottom: MonoType) {
    val mono = current!!
    val swapAux0 = MonoVar(
        id = mono.variables.size,
        name = "swap-0-${mono.instructions.size}",
        type = top,
        varRef = null,
    )
    mono.variables += swapAux0
    val swapAux1 = MonoVar(
        id = mono.variables.size,
        name = "swap-1-${mono.instructions.size}",
        type = bottom,
        varRef = null
    )
    mono.variables += swapAux1

    mono.instructions += MonoSwap(mono.nextId(), span, swapAux0, swapAux1)
}

fun MonoBuilder.unreachable(span: Span) {
    val mono = current!!
    mono.instructions += MonoUnreachable(mono.nextId(), span)
}

fun MonoBuilder.comment(span: Span, msg: String) {
    val mono = current!!
    mono.instructions += MonoComment(mono.nextId(), span, msg)
}

fun MonoBuilder.typeOf(ref: Ref): MonoType {
    val mono = current!!
    val provider = mono.providers[ref] ?: error("Missing provider for ref $ref")
    return provider.type
}

fun MonoBuilder.consumer(span: Span, ref: Ref) {
    val mono = current!!
    val provider = mono.providers[ref] ?: error("Missing provider for ref $ref")
    mono.instructions += MonoConsumer(mono.nextId(), span, provider)
}

fun MonoBuilder.provider(span: Span, ref: Ref, type: MonoType) {
    val mono = current!!
    mono.instructions += MonoProvider(mono.nextId(), span, type).also { mono.providers[ref] = it }
}