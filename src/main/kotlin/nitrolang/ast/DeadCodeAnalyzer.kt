package nitrolang.ast

import nitrolang.parsing.ANNOTATION_REQUIRED
import nitrolang.parsing.MAIN_FUNCTION_NAME

class DeadCodeAnalyzer(val program: LstProgram) {
    private val visitedFunctions = mutableSetOf<LstFunction>()
    private val visitedLambdas = mutableSetOf<LstLambdaFunction>()

    companion object {
        fun markDeadCode(program: LstProgram) {
            val main = program.functions.find { it.fullName == MAIN_FUNCTION_NAME }
                ?: error("Missing $MAIN_FUNCTION_NAME function")

            program.functions.forEach {
                it.isDeadCode = it.getAnnotation(ANNOTATION_REQUIRED) == null
            }
            program.consts.forEach {
                it.isDeadCode = true
            }

            program.functions.filter { it.isRequired }.forEach {
                DeadCodeAnalyzer(program).visitFunction(it)
            }
            DeadCodeAnalyzer(program).visitFunction(main)
        }
    }

    private fun visitFunction(func: LstFunction) {
        visitedFunctions += func
        func.isDeadCode = false

        func.tag?.functionImplementations?.values?.forEach { perType ->
            perType.values.forEach { newFunc ->
                newFunc.isDeadCode = false

                if (newFunc !in visitedFunctions) {
                    visitFunction(newFunc)
                }
            }
        }

        visitInst(func.body)
    }

    private fun visitLambda(func: LstLambdaFunction) {
        visitedLambdas += func
        func.isDeadCode = false
        visitInst(func.body)
    }

    private fun visitInst(body: LstCode) {
        body.nodes.filterIsInstance<LstLoadVar>().forEach {
            it.constant?.isDeadCode = false
        }
        body.nodes.filterIsInstance<LstStoreVar>().forEach {
            it.constant?.isDeadCode = false
        }
        body.nodes.filterIsInstance<LstLambdaInit>().forEach {
            if (it.lambda !in visitedLambdas) {
                visitLambda(it.lambda)
            }
        }
        body.nodes.filterIsInstance<LstFunCall>().forEach { node ->
            val newFunc = node.function ?: error("Function not resolved")

            if (newFunc !in visitedFunctions) {
                visitFunction(newFunc)
            }
        }
    }
}