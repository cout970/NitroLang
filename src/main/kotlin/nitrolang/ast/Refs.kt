package nitrolang.ast

import com.google.gson.JsonElement
import nitrolang.util.Dumpable
import nitrolang.util.dump

data class Ref(val id: Int) : Dumpable {
    override fun toString(): String = "$$id"

    override fun dump(): JsonElement = toString().dump()
}

abstract class VarRef(val id: Int) : Dumpable {

    override fun dump(): JsonElement = toString().dump()
}

class LocalVarRef(id: Int) : VarRef(id) {
    override fun toString(): String = "var#$id"
}

class ConstRef(id: Int) : VarRef(id) {

    override fun toString(): String = "const#$id"

}

data class StructRef(val id: Int) : Dumpable {

    override fun toString(): String = "struct#$id"

    override fun dump(): JsonElement = toString().dump()
}

data class OptionRef(val id: Int) : Dumpable {

    override fun toString(): String = "option#$id"

    override fun dump(): JsonElement = toString().dump()
}

data class FunRef(val id: Int) : Dumpable {

    override fun toString(): String = "fun#$id"

    override fun dump(): JsonElement = toString().dump()
}

data class TagRef(val id: Int) : Dumpable {

    override fun toString(): String = "tag#$id"

    override fun dump(): JsonElement = toString().dump()
}

data class TypeRef(val id: Int) : Dumpable {

    override fun toString(): String = "type#$id"

    override fun dump(): JsonElement = toString().dump()
}

data class TypeParamRef(val id: Int) : Dumpable {
    override fun toString(): String = "type_param#$id"

    override fun dump(): JsonElement = toString().dump()
}

data class FieldRef(val id: Int) : Dumpable {

    override fun toString(): String = "field#$id"

    override fun dump(): JsonElement = toString().dump()

}