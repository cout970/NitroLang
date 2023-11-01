package nitrolang.typeinference

import nitrolang.ast.LstLambdaFunction
import nitrolang.ast.LstOption
import nitrolang.ast.LstStruct

sealed interface TTypeBase {
    val id: Int
    val indexKey: String
}

// Lambda function
data class TLambda(override val id: Int, val instance: LstLambdaFunction) : TTypeBase {
    override val indexKey: String = "L${instance.ref.id}"

    override fun toString(): String = instance.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TLambda) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

data class TOption(override val id: Int, val instance: LstOption) : TTypeBase {
    override val indexKey: String = "O${instance.ref.id}"

    override fun toString(): String = instance.fullName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TOption) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

data class TOptionItem(override val id: Int, val instance: LstStruct, val option: TOption) : TTypeBase {
    override val indexKey: String = "I${instance.ref.id}"

    override fun toString(): String = instance.fullName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TOptionItem) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

data class TStruct(override val id: Int, val instance: LstStruct) : TTypeBase {
    override val indexKey: String = "S${instance.ref.id}"

    fun isFunction(): Boolean = instance.isIntrinsic && instance.fullName == "Function"

    override fun toString(): String = instance.fullName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TStruct) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}