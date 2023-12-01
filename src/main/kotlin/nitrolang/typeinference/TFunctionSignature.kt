package nitrolang.typeinference

import nitrolang.ast.LstFunction
import nitrolang.ast.LstLambdaFunction

typealias TFunctionSignature = String

fun TType.toSignature(): TFunctionSignature? {
    return when (this) {
        is TComposite -> {
            var signature = this.base.toSignature()
            if (this.params.isNotEmpty()) {
                val aux = mutableListOf<TFunctionSignature>()
                for (param in this.params) {
                    aux += param.toSignature() ?: return null
                }
                signature = "$signature<${aux.joinToString(",")}>"
            }
            signature
        }

        is TGeneric -> {
            val index = this.instance.definer!!.typeParameters.indexOf(this.instance)
            "#$index"
        }

        is TInvalid -> null
        is TUnion -> null
        is TUnresolved -> null
    }
}

fun TTypeBase.toSignature(): TFunctionSignature? {
    return when (this) {
        is TOption -> this.instance.fullName
        is TOptionItem -> this.instance.fullName
        is TStruct -> this.instance.fullName
        is TLambda -> this.instance.toSignature()
    }
}

fun LstLambdaFunction.toSignature(): TFunctionSignature? {
    val aux = mutableListOf<TFunctionSignature>()
    for (param in this.params) {
        aux += param.type.toSignature() ?: return null
    }
    val ret = this.returnType.toSignature() ?: return null

    return "(${aux.joinToString(", ")}): $ret"
}

fun LstFunction.toSignature(): TFunctionSignature? {
    val aux = mutableListOf<TFunctionSignature>()
    for (param in this.params) {
        aux += param.type.toSignature() ?: return null
    }
    val ret = this.returnType.toSignature() ?: return null

    return "${this.fullName}(${aux.joinToString(", ")}): $ret"
}

