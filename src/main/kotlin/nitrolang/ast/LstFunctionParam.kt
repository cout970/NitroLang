package nitrolang.ast

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nitrolang.typeinference.TType
import nitrolang.typeinference.TypeBox
import nitrolang.util.Dumpable
import nitrolang.util.Span
import nitrolang.util.dump

class LstFunctionParam(
    val span: Span,
    val name: String,
    val index: Int,
    val typeUsage: LstTypeUsage,
) : Dumpable {
    var typeBox: TypeBox? = null
    var type: TType
        get() = typeBox!!.type
        set(v) {
            typeBox!!.type = v
        }

    var variable: LstVar? = null

    fun createVariable(body: LstCode) {
        val variable = LstVar(
            span = span,
            name = name,
            block = body.currentBlock,
            typeUsage = typeUsage,
            validAfter = body.currentRef(),
            ref = body.nextVarRef()
        )
        variable.isParam = true
        body.variables[variable.ref] = variable
        this.variable = variable
    }

    override fun dump(): JsonElement = JsonObject().also {
        it.add("name", name.dump())
        it.add("index", index.dump())
        it.add("type", typeBox?.dump())
    }
}