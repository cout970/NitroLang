package nitrolang.ast

import nitrolang.util.Span

fun LstCode.int(span: Span, value: Int): Ref {
    val ref = nextRef()
    val node = LstInt(
        ref = ref,
        span = span,
        block = currentBlock,
        value = value,
    )
    nodes += node
    return ref
}

fun LstCode.float(span: Span, value: Float): Ref {
    val ref = nextRef()
    val node = LstFloat(
        ref = ref,
        span = span,
        block = currentBlock,
        value = value,
    )
    nodes += node
    return ref
}

fun LstCode.boolean(span: Span, value: Boolean): Ref {
    val ref = nextRef()
    val node = LstBoolean(
        ref = ref,
        span = span,
        block = currentBlock,
        value = value,
    )
    nodes += node
    return ref
}

fun LstCode.string(span: Span, value: String): Ref {
    val ref = nextRef()
    val node = LstString(
        ref = ref,
        span = span,
        block = currentBlock,
        value = value,
    )
    nodes += node
    return ref
}

fun LstCode.letVar(span: Span, name: String, typeUsage: LstTypeUsage?): LstVar {
    val ref = nextRef()
    val variable = LstVar(
        span = span,
        name = name,
        block = currentBlock,
        typeUsage = typeUsage,
        definedBy = ref,
        ref = nextVarRef(),
        definedIn = this,
    )
    val node = LstLetVar(
        ref = ref,
        span = span,
        block = currentBlock,
        variable = variable,
    )
    variables += variable
    nodes += node
    return variable
}


fun LstCode.returnExpr(span: Span, value: Ref): Ref {
    val ref = nextRef()
    val node = LstReturn(
        ref = ref,
        span = span,
        block = currentBlock,
        expr = value,
    )
    nodes += node
    return ref
}

fun LstCode.ifStart(span: Span, cond: Ref): Ref {
    val ref = nextRef()
    val node = LstIfStart(
        ref = ref,
        span = span,
        block = currentBlock,
        cond = cond,
    )
    nodes += node
    return ref
}

fun LstCode.ifEnd(span: Span): Ref {
    val ref = nextRef()
    val node = LstIfEnd(
        ref = ref,
        span = span,
        block = currentBlock,
    )
    nodes += node
    return ref
}

fun LstCode.call(
    span: Span,
    path: String,
    name: String,
    arguments: List<Ref> = emptyList(),
    explicitTypeParams: List<LstTypeUsage> = emptyList(),
    config: (LstFunCall.() -> Unit)? = null,
): Ref {
    val ref = nextRef()
    val node = LstFunCall(
        ref = ref,
        span = span,
        block = currentBlock,
        path = path,
        name = name,
        arguments = arguments,
        explicitTypeParams = explicitTypeParams,
    )
    config?.invoke(node)
    nodes += node
    return ref
}

fun LstCode.loadVar(span: Span, path: String, name: String, config: (LstLoadVar.() -> Unit)? = null): Ref {
    val ref = nextRef()
    val node = LstLoadVar(
        ref = ref,
        span = span,
        block = currentBlock,
        path = path,
        name = name,
    )
    config?.invoke(node)
    nodes += node
    return ref
}

fun LstCode.storeVar(span: Span, path: String, name: String, expr: Ref, config: (LstStoreVar.() -> Unit)? = null): Ref {
    val ref = nextRef()
    val node = LstStoreVar(
        ref = ref,
        span = span,
        block = currentBlock,
        path = path,
        name = name,
        expr = expr,
    )
    config?.invoke(node)
    nodes += node
    return ref
}

fun LstCode.loadConst(
    span: Span,
    path: String,
    name: String,
    const: LstConst,
    config: (LstLoadVar.() -> Unit)? = null
): Ref {
    val ref = nextRef()
    val node = LstLoadVar(
        ref = ref,
        span = span,
        block = currentBlock,
        path = path,
        name = name,
    )
    node.constant = const
    config?.invoke(node)
    nodes += node
    return ref
}

fun LstCode.loadField(span: Span, instance: Ref, name: String, config: (LstLoadField.() -> Unit)? = null): Ref {
    val ref = nextRef()
    val node = LstLoadField(
        ref = ref,
        span = span,
        block = currentBlock,
        instance = instance,
        name = name,
    )
    config?.invoke(node)
    nodes += node
    return ref
}

fun LstCode.storeField(
    span: Span,
    instance: Ref,
    name: String,
    expr: Ref,
    struct: LstStruct? = null,
    field: LstStructField? = null,
    config: (LstStoreField.() -> Unit)? = null
): Ref {
    val ref = nextRef()
    val node = LstStoreField(
        ref = ref,
        span = span,
        block = currentBlock,
        instance = instance,
        name = name,
        expr = expr,
    )
    node.struct = struct
    node.field = field
    config?.invoke(node)
    nodes += node
    return ref
}

fun LstCode.alloc(
    span: Span,
    typeUsage: LstTypeUsage,
    struct: LstStruct? = null,
    config: (LstAlloc.() -> Unit)? = null
): Ref {
    val ref = nextRef()
    val node = LstAlloc(
        ref = ref,
        span = span,
        block = currentBlock,
        typeUsage = typeUsage,
    )
    node.struct = struct
    config?.invoke(node)
    nodes += node
    return ref
}