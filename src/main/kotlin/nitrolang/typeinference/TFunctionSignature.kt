package nitrolang.typeinference

import nitrolang.ast.LstFunction
import nitrolang.ast.LstLambdaFunction

typealias TFunctionSignature = String

fun TType.toSignature(): TFunctionSignature {
    return when (this) {
        is TComposite -> {
            var signature = this.base.toSignature()
            if (this.params.isNotEmpty()) {
                signature = "$signature<${this.params.joinToString(",") { it.toSignature() }}>"
            }
            signature
        }

        is TGeneric -> {
            val index = this.instance.definer!!.typeParameters.indexOf(this.instance)
            "#$index"
        }

        is TInvalid -> error("Invalid type")
        is TUnion -> error("Union type")
        is TUnresolved -> error("Unresolved type")
    }
}

fun TTypeBase.toSignature(): TFunctionSignature {
    return when (this) {
        is TOption -> this.instance.fullName
        is TOptionItem -> this.instance.fullName
        is TStruct -> this.instance.fullName
        is TLambda -> this.instance.toSignature()
    }
}

fun LstLambdaFunction.toSignature(): TFunctionSignature {
    return "(${this.params.joinToString(", ") { it.type.toSignature() }}): ${this.returnType.toSignature()}"
}

fun LstFunction.toSignature(): TFunctionSignature {
    return "${this.fullName}(${this.params.joinToString(", ") { it.type.toSignature() }}): ${this.returnType.toSignature()}"
}

