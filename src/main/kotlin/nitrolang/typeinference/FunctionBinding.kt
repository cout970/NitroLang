package nitrolang.typeinference

import nitrolang.ast.LstFunction
import nitrolang.parsing.ParserCtx

fun ParserCtx.findBestFunctionMatch(name: String, args: List<TType>): List<LstFunction> {
    val choices = program.functions.filter { it.fullName == name }

    if (choices.isEmpty()) {
        return emptyList()
    }

    val scored = choices.map {
        var score = if (it.tag != null) 1f else 0f
        score += functionDiffScore(it.params.map { param -> param.type }, args)
        // Prefer tags with fewer functions
        if (it.tag != null) {
            score += it.tag!!.headers.size * 0.01f
        }
        score to it
    }

    val sortedChoices = scored.sortedBy { (score, _) ->
        score
    }

    val bestScore = sortedChoices.first().first
    val conflicts = mutableListOf<LstFunction>()

    for ((score, func) in sortedChoices) {
        if (score == bestScore) {
            conflicts += func
        } else {
            break
        }
    }

    // Best match
    return conflicts
}

fun ParserCtx.functionDiffScore(params: List<TType>, args: List<TType>): Float {
    var diff = 0f

    if (params.size != args.size) {
        diff += 100000f
    }

    for ((param, arg) in params.zip(args)) {
        diff += typeDiffScore(param, arg)
    }

    return diff
}

fun ParserCtx.typeDiffScore(param: TType, arg: TType): Float {
    if (param == arg) {
        return 0f
    }

    if (param is TComposite && arg is TComposite) {
        if (param.base != arg.base) {
            return 1000f
        }

        var diff = 0f

        if (param.params.size != arg.params.size) {
            diff += 100f
        }

        param.params.zip(arg.params).forEach { (p, a) ->
            diff += typeDiffScore(p, a) * 0.1f
        }

        return diff
    }

    if (arg is TGeneric && param is TGeneric) {
        val req = param.instance.requiredTags.toMutableSet()
        req.removeAll(arg.instance.requiredTags.toSet())
        return req.size * 10f + 1f
    }

    if (arg is TComposite && param is TGeneric) {
        var missingTags = 0

        param.instance.requiredTags.forEach { tag ->
            if (arg !in tag.implementers) {
                missingTags += 1
            }
        }

        return missingTags * 10f + 1f
    }

    return 1000f
}