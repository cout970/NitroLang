package nitrolang.ast

import nitrolang.parsing.MAIN_FUNCTION_NAME

class DeadCodeAnalyzer(val program: LstProgram) {
    private val visitedFunctions = mutableSetOf<LstFunction>()
    private val visitedLambdas = mutableSetOf<LstLambdaFunction>()
    private var usesAlloc: Boolean = false

    companion object {
        fun markDeadCode(program: LstProgram) {
            program.functions.forEach {
                it.isDeadCode = true
            }
            program.consts.forEach {
                it.isDeadCode = true
            }

            val deadCodeAnalyzer = DeadCodeAnalyzer(program)

            program.functions.filter { it.isRequired }.forEach {
                deadCodeAnalyzer.visitFunction(it)
            }

            if (program.compilerOptions.runTests) {
                deadCodeAnalyzer.visitFunction(program.getFunction("run_test"))
                program.functions.filter { it.isTest }.forEach {
                    deadCodeAnalyzer.visitFunction(it)
                }
            } else {
                val main = program.functions.find { it.fullName == MAIN_FUNCTION_NAME }
                    ?: error("Missing $MAIN_FUNCTION_NAME function")

                deadCodeAnalyzer.visitFunction(main)
            }

            program.usesAlloc = deadCodeAnalyzer.usesAlloc
            if (deadCodeAnalyzer.usesAlloc) {
                val func = program.getFunction("memory_alloc_internal")
                deadCodeAnalyzer.visitFunction(func)
            }
        }
    }

    private fun visitFunction(func: LstFunction) {
        visitedFunctions += func
        func.isDeadCode = false

        // Required for Array::new()
        if (func.autogenerate) {
            usesAlloc = true
        }

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
        if (body.nodes.any { it is LstAlloc || it is LstLambdaInit }) {
            usesAlloc = true
        }
        if (body.variables.any { it.isUpValue }) {
            usesAlloc = true
        }
        body.nodes.filterIsInstance<LstFunCall>().forEach { node ->
            val newFunc = node.function ?: error("Function not resolved")

            if (newFunc !in visitedFunctions) {
                visitFunction(newFunc)
            }
        }
    }
}