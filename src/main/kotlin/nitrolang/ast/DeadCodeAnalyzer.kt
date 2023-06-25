package nitrolang.ast

import nitrolang.ANNOTATION_REQUIRED
import nitrolang.MAIN_FUNCTION_NAME

class DeadCodeAnalyzer(val program: LstProgram) {
    private val visited = mutableSetOf<LstFunction>()

    companion object {
        fun markDeadCode(program: LstProgram) {
            val main = program.functions.values.find { it.fullName == MAIN_FUNCTION_NAME }
                ?: error("Missing $MAIN_FUNCTION_NAME function")

            program.functions.values.forEach {
                if (it.getAnnotation(ANNOTATION_REQUIRED) == null) {
                    it.isDeadCode = true
                }
            }
            program.consts.values.forEach {
                it.isDeadCode = true
            }
            DeadCodeAnalyzer(program).visitFunction(main)
        }
    }

    private fun visitFunction(func: LstFunction) {
        visited += func
        func.isDeadCode = false

        func.body.nodes.filterIsInstance<LstLoadVar>().forEach {
            if (it.varRef is ConstRef) program.consts[it.varRef]!!.isDeadCode = false
        }
        func.body.nodes.filterIsInstance<LstStoreVar>().forEach {
            if (it.varRef is ConstRef) program.consts[it.varRef]!!.isDeadCode = false
        }

        func.body.nodes.filterIsInstance<LstFunCall>().forEach { node ->
            val newFunc = node.function ?: error("Function not resolved")

            if (newFunc !in visited) {
                visitFunction(newFunc)
            }
        }
    }
}