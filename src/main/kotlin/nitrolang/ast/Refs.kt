package nitrolang.ast

data class Ref(val id: Int) {
    override fun toString(): String = "$$id"
}

abstract class VarRef(val id: Int) {
    override fun toString(): String = "#$id"
}

class LocalVarRef(id: Int): VarRef(id)

class ConstRef(id: Int): VarRef(id) {

    companion object {
        private var counter = 0
        fun next(): ConstRef = ConstRef(counter++)
    }
}

data class DeclRef(val id: Int) {
    companion object {
        private var counter = 0
        fun next(): DeclRef = DeclRef(counter++)
    }
}

data class FunRef(val id: Int) {
    companion object {
        private var counter = 0
        fun next(): FunRef = FunRef(counter++)
    }
}

data class TypeRef(val id: Int) {
    companion object {
        private var counter = 0
        fun next(): TypeRef = TypeRef(counter++)
    }
}

data class FieldRef(val id: Int) {
    companion object {
        private var counter = 0
        fun next(): FieldRef = FieldRef(counter++)
    }
}