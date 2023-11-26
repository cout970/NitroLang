package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump

class LstLambdaFunction(
    val span: Span,
    val params: List<LstFunctionParam>,
    val returnTypeUsage: LstTypeUsage,
    val body: LstCode,
    val ref: FunRef,
) : Dumpable {
    var checked = false
    var codeChecked = false
    var isDeadCode = false

    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    var returnTypeBox: TypeBox? = null
    var returnType: TType
        get() = returnTypeBox!!.type
        set(v) {
            returnTypeBox!!.type = v
        }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("ref", ref.dump())
        it.add("params", params.dump())
        it.add("returnTypeUsage", returnTypeUsage.dump())
        it.add("body", body.dump())
    }

    override fun toString(): String = "Lambda-${ref.id}"
}