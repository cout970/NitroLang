package nitrolang.ast

import nitrolang.parsing.ANNOTATION_REQUIRED
import nitrolang.parsing.MAIN_FUNCTION_NAME

class DeadCodeAnalyzer(val program: LstProgram) {
    private val visited = mutableSetOf<LstFunction>()

    companion object {
        fun markDeadCode(program: LstProgram) {
            val main = program.functions.values.find { it.fullName == MAIN_FUNCTION_NAME }
                ?: error("Missing $MAIN_FUNCTION_NAME function")

            program.functions.values.forEach {
                it.isDeadCode = it.getAnnotation(ANNOTATION_REQUIRED) == null
            }
            program.consts.values.forEach {
                it.isDeadCode = true
            }

            DeadCodeAnalyzer(program).visitFunction(program.getFunction("memory_alloc_internal"))
            DeadCodeAnalyzer(program).visitFunction(main)
        }
    }

    private fun visitFunction(func: LstFunction) {
        visited += func
        func.isDeadCode = false

        func.tag?.functionImplementations?.values?.forEach { perType ->
            perType.values.forEach { newFunc ->
                newFunc.isDeadCode = false

                if (newFunc !in visited) {
                    visitFunction(newFunc)
                }
            }
        }

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