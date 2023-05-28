package nitrolang.ast

data class Ref(val id: Int) {
    override fun toString(): String = "$$id"
}

data class VarRef(val id: Int) {
    override fun toString(): String = "#$id"
}

data class FunRef(val id: Int) {
    companion object {
        private var counter = 0
        fun next(): FunRef = FunRef(counter++)
    }
}

data class ConstRef(val id: Int) {
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