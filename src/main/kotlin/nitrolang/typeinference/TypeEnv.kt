package nitrolang.typeinference

import nitrolang.ast.*
import nitrolang.util.ErrorCollector
import nitrolang.util.ErrorInfo
import nitrolang.util.Prof
import nitrolang.util.Span

class TypeEnv(val collector: ErrorCollector) {
    private var nextId = 0
    private var nextSubId = 0
    private var nextConsId = 0

    private var allTypes = mutableMapOf<Int, TType>()
    private val allTypeBases = mutableMapOf<Int, TTypeBase>()
    private var tComposite = mutableMapOf<String, TComposite>()
    private var tGeneric = mutableMapOf<String, TGeneric>()
    private var tUnion = mutableMapOf<String, TUnion>()
    private var tStruct = mutableMapOf<String, TStruct>()
    private var tOption = mutableMapOf<String, TOption>()
    private var tOptionItem = mutableMapOf<String, TOptionItem>()
    private var tLambda = mutableMapOf<String, TLambda>()

    private val constrains = mutableListOf<TConstraint>()
    private val unresolved = mutableSetOf<TUnresolved>()
    private val boxes = mutableListOf<TypeBox>()

    private var currentConstraint: TConstraint? = null

    fun print() {
        println("allTypes:\n - ${allTypes.values.joinToString("\n - ")}")
        println("allTypeBases:\n - ${allTypeBases.values.joinToString("\n - ")}")
        println("constrains:\n - ${constrains.joinToString("\n - ")}")
        println("unresolved:\n - ${unresolved.size}")
    }

    private fun nextId() = nextId++
    private fun nextSubId() = nextSubId++

    fun getSimpleBaseTypes(): List<TTypeBase> =
        allTypeBases.values.filter { it is TStruct || it is TOption || it is TOptionItem }

    fun box(type: TType, span: Span): TypeBox {
        return TypeBox(this, type, span).also { boxes += it }
    }

    fun find(name: String): TType = allTypes.values.find {
        it is TComposite && (
                (it.base is TStruct && it.base.instance.fullName == name) ||
                        (it.base is TOption && it.base.instance.fullName == name) ||
                        (it.base is TOptionItem && it.base.instance.fullName == name)
                )
    }!!

    fun replaceAll(find: TUnresolved, replacement: TType) {
        boxes.forEach { it.replace(find, replacement) }
        val old = allTypes.toMap()
        val new = mutableMapOf<Int, TType>()

        old.forEach { (k, v) ->
            if (k == find.id) return@forEach
            val value = v.replace(find, replacement)
            new[value.id] = value
        }

        allTypes = new
        unresolved.remove(find)
    }

    fun addAssignableConstraint(expected: TType, found: TType, span: Span) {
        constrains += TUnify(nextConsId++, box(expected, span), box(found, span), span)
    }

    fun addEqualConstraint(left: TType, right: TType, span: Span) {
        constrains += TUnify(nextConsId++, box(left, span), box(right, span), span)
    }

    fun addBoundsConstraint(type: TType, bounds: List<LstTag>, span: Span) {
        constrains += TBounds(nextConsId++, box(type, span), bounds, span)
    }

    fun addFindFieldConstraint(instanceType: TType, span: Span, callback: (TType) -> Unit) {
        constrains += TFindField(nextConsId++, box(instanceType, span), callback, span)
    }

    fun addFindFunctionConstraint(arguments: List<TType>, span: Span, callback: (List<TType>) -> Unit) {
        constrains += TFindFunction(nextConsId++, arguments.map { box(it, span) }, callback, span)
    }

    fun unresolved(span: Span) = type {
        TUnresolved(it, span).also { self -> unresolved += self }
    }

    fun generic(instance: LstTypeParameter): TGeneric {
        val key = "G${instance.ref.id}"
        if (key in tGeneric) {
            return tGeneric[key]!!
        }
        return type { TGeneric(it, instance) }
    }

    fun invalid(span: Span, reason: String = "") = type { TInvalid(it, span, reason) }

    fun composite(base: TTypeBase, params: List<TType>): TComposite {
        val key = "C${base.id}<${params.joinToString(",") { it.indexKey }}>"
        if (key in tComposite) {
            return tComposite[key]!!
        }
        return type { TComposite(it, base, params) }
    }

    fun typeLambda(lambda: LstLambdaFunction): TComposite {
        val params = mutableListOf<TType>()
        lambda.params.forEach { params += it.type }
        params += lambda.returnType

        val base = typeBaseLambda(lambda)
        return composite(base, params)
    }

    fun typeBaseStruct(instance: LstStruct): TStruct {
        val key = "S${instance.ref.id}"
        if (key in tStruct) {
            return tStruct[key]!!
        }
        return typeBase { TStruct(it, instance) }
    }

    fun typeBaseOption(instance: LstOption): TOption {
        val key = "O${instance.ref.id}"
        if (key in tOption) {
            return tOption[key]!!
        }
        return typeBase { TOption(it, instance) }
    }

    fun typeBaseOptionItem(instance: LstStruct, optionInstance: LstOption): TOptionItem {
        val key = "I${instance.ref.id}"
        if (key in tOptionItem) {
            return tOptionItem[key]!!
        }
        val option = typeBaseOption(optionInstance)
        return typeBase { TOptionItem(it, instance, option) }
    }

    fun typeBaseLambda(instance: LstLambdaFunction): TLambda {
        val key = "L${instance.ref.id}"
        if (key in tLambda) {
            return tLambda[key]!!
        }
        return typeBase { TLambda(it, instance) }
    }

    fun union(options: Set<TType>): TType {
        val key = "U<${options.sortedBy { it.indexKey }.joinToString(",") { it.indexKey }}>"
        if (key in tUnion) {
            return tUnion[key]!!
        }

        val flatOptions = flatten(options)

        val common = commonType(flatOptions.toList())
        if (common != null) {
            return common
        }
        return type { TUnion(it, flatOptions) }
    }

    private fun flatten(options: Set<TType>): Set<TType> {
        val result = mutableSetOf<TType>()
        options.forEach {
            if (it is TUnion)
                result += flatten(it.options)
            else
                result += it
        }
        return result
    }

    fun unionOf(prev: TType, next: TType): TType {
        return when {
            prev is TUnion && next is TUnion -> {
                val result = mutableSetOf<TType>()
                prev.options.forEach {
                    if (it is TUnion)
                        result += it.options
                    else
                        result += it
                }
                next.options.forEach {
                    if (it is TUnion)
                        result += it.options
                    else
                        result += it
                }
                union(result)
            }

            prev is TUnion -> union(prev.options + next)
            next is TUnion -> union(next.options + prev)
            else -> union(setOf(prev, next))
        }
    }

    private inline fun <T : TType> type(func: (Int) -> T): T {
        val id = nextId()
        val ty = func(id)
        allTypes[id] = ty
        when (ty) {
            is TComposite -> tComposite[ty.indexKey] = ty
            is TGeneric -> tGeneric[ty.indexKey] = ty
            is TUnion -> tUnion[ty.indexKey] = ty
        }
        return ty
    }

    private inline fun <T : TTypeBase> typeBase(func: (Int) -> T): T {
        val id = nextSubId()
        val ty = func(id)
        allTypeBases[id] = ty
        when (ty) {
            is TLambda -> tLambda[ty.indexKey] = ty
            is TStruct -> tStruct[ty.indexKey] = ty
            is TOption -> tOption[ty.indexKey] = ty
            is TOptionItem -> tOptionItem[ty.indexKey] = ty
        }
        return ty
    }

    // ---

    fun simplify(ty: TType): TType {
        return when (ty) {
            is TUnion -> commonType(ty.options.toList()) ?: ty
            is TComposite -> composite(ty.base, ty.params.map { simplify(it) })
            else -> ty
        }
    }

    fun TType.replace(find: TUnresolved, replacement: TType): TType = when (this) {
        is TUnresolved -> if (find.id == this.id) replacement else this
        is TUnion -> {
            val options = options.map { it.replace(find, replacement) }.toSet()
            if (this.options != options) union(options) else this
        }

        is TComposite -> {
            val params = params.map { it.replace(find, replacement) }
            if (this.params != params) composite(base, params) else this
        }

        else -> this
    }

    fun TType.replace(find: TGeneric, replacement: TType): TType = when (this) {
        is TGeneric -> if (find.id == this.id) replacement else this
        is TUnion -> {
            val options = options.map { it.replace(find, replacement) }.toSet()
            if (this.options != options) union(options) else this
        }

        is TComposite -> {
            val params = params.map { it.replace(find, replacement) }
            if (this.params != params) composite(base, params) else this
        }

        else -> this
    }

    fun TType.removeUnresolved(key: TUnresolved): TType {
        return when (this) {
            is TUnion -> {
                if (key in options) union(options - key) else this
            }

            is TComposite -> {
                if (this.hasUnresolved())
                    composite(base, params.map { it.removeUnresolved(key) })
                else this
            }

            else -> this
        }
    }

    fun TType.contains(find: TUnresolved): Boolean = when (this) {
        is TUnresolved -> find.id == this.id
        is TUnion -> this.options.any { it.contains(find) }
        is TComposite -> this.params.any { it.contains(find) }
        else -> false
    }

    fun TType.hasUnresolved(): Boolean = when (this) {
        is TUnresolved -> true
        is TUnion -> this.options.any { it.hasUnresolved() }
        is TComposite -> this.params.any { it.hasUnresolved() }
        else -> false
    }
    // ---

    fun matchesTag(ty: TType, tag: LstTag): Boolean {
        if (ty is TGeneric) {
            return tag in ty.instance.requiredTags
        }

        if (ty in tag.implementers) {
            return true
        }

        return tag.implementers.any { matchesTagImplementer(ty, it) }
    }

    fun matchesTagImplementer(ty: TType, impl: TType): Boolean {
        if (ty == impl) return true

        if (ty is TComposite && impl is TComposite) {
            if (ty.base != impl.base) return false
            if (ty.params.size != impl.params.size) return false

            return ty.params.zip(impl.params).all { matchesTagImplementer(it.first, it.second) }
        }

        if (ty is TComposite && impl is TGeneric) {
            return impl.instance.requiredTags.all { tag -> matchesTag(ty, tag) }
        }

        return false
    }

    fun solveConstraints() {
        val errors = mutableListOf<ErrorInfo>()
        val substitutions = mutableMapOf<TUnresolved, TType>()
        val remaining = ArrayDeque<Pair<TUnresolved, TType>>()
        var madeProgress: Boolean
        do {
            Prof.start("loop")
            madeProgress = false

            Prof.start("unify")
            // Unify
            constrains.forEach { constraint ->
                if (constraint is TUnify) {
                    currentConstraint = constraint
                    unify(
                        constraint.left.type,
                        constraint.right.type,
                        substitutions,
                        errors
                    )
                    currentConstraint = null
                }
            }

            Prof.next("replace")
            if (substitutions.isNotEmpty()) {
                madeProgress = true
                substitutions.forEach { (a, b) -> remaining.add(a to b) }
                substitutions.clear()

                while (remaining.isNotEmpty()) {
                    val (key, value) = remaining.first()
                    val replacement = simplify(value)

                    // Replace usages of `key` in all instances of TypeBox
                    replaceAll(key, replacement)

                    // Replace `key` in errors previously detected
                    errors.forEach { err ->
                        if (err is TypeError) {
                            err.replace(this, key, replacement)
                        }
                    }

                    // Remove this replacement
                    remaining.removeAt(0)

                    // Make sure all uses of `key` in remaining are updated as well
                    remaining.replaceAll { (k, v) ->
                        k to v.replace(key, replacement)
                    }
                }
            }

            Prof.next("bounds")
            // Bound
            val iter = constrains.iterator()
            while (iter.hasNext()) {
                val constraint = iter.next()
                if (constraint !is TBounds) continue

                val ty = constraint.target.type
                if (ty.hasUnresolved()) continue

                constraint.requiredTags.forEach inner@{ tag ->
                    if (!matchesTag(ty, tag)) {
                        errors += TypeBoundsError(ty, tag, constraint)
                    }
                }

                madeProgress = true
                iter.remove()
            }

            errors.forEach { err ->
                collector.reportError(err)
            }

            errors.clear()

            Prof.next("clean")

            // Remove useless constrains
            constrains.toList().forEach { constraint ->
                if (constraint is TUnify) {
                    if (constraint.left.type == constraint.right.type || (!constraint.left.type.hasUnresolved() && !constraint.right.type.hasUnresolved())) {
                        constrains.remove(constraint)
                    }
                }

                if (constraint is TFindField) {
                    if (constraint.dependency.type !is TUnresolved) {
                        madeProgress = true
                        constraint.callback(constraint.dependency.type)
                        constrains.remove(constraint)
                    }
                }

                if (constraint is TFindFunction) {
                    if (constraint.dependencies.isEmpty() || constraint.dependencies.first().type !is TUnresolved) {
                        madeProgress = true
                        constraint.callback(constraint.dependencies.map { it.type })
                        constrains.remove(constraint)
                    }
                }
            }
            Prof.end()
            Prof.end()
        } while (madeProgress)
    }

    fun finish() {
        // Report missing types
        unresolved.forEach {
            collector.report("Not enough information to infer the type (${it.id})", it.span)
        }

        boxes.forEach { it.type = simplify(it.type) }

        val set = mutableSetOf<Span>()
        boxes.forEach {
            val ty = it.type
            if (ty is TUnion) {

                if (it.span !in set) {
                    set += it.span
                    collector.report("Multiples conflicting types ${ty.options}", it.span)
                }
            }
        }
    }

    fun addSubstitution(
        key: TUnresolved,
        value: TType,
        sub: MutableMap<TUnresolved, TType>,
        errors: MutableList<ErrorInfo>
    ) {
        if (key == value) return

        if (value.contains(key)) {
            val newValue = value.removeUnresolved(key)

            if (newValue.contains(key)) {
                error("Recursive type: $key and $value")
            }

            addSubstitution(key, newValue, sub, errors)
            return
        }

        if (key !in sub) {
            sub[key] = value
            return
        }

        val prev: TType = sub[key]!!
        val next: TType = value

        if (prev == next) return

        val common = commonType2(prev, next)
        if (common != null) {
            sub[key] = common
            return
        }

        if (!prev.hasUnresolved() && !next.hasUnresolved()) {
            errors += TypeMismatchError(prev, next, currentConstraint!!)
            sub[key] = prev
            return
        }

        // prev or next contain unresolved
        val sub2 = mutableMapOf<TUnresolved, TType>()
        unify(prev, next, sub2, errors)

        for ((subKey, subValue) in sub2) {
            if (subKey !in sub) {
                sub[subKey] = subValue
                continue
            }

            sub[subKey] = unionOf(sub[subKey]!!, subValue)
        }
    }

    fun unify(
        type1: TType,
        type2: TType,
        sub: MutableMap<TUnresolved, TType>,
        errors: MutableList<ErrorInfo>
    ) {
        when {
            type1 is TUnresolved -> {
                addSubstitution(type1, type2, sub, errors)
            }

            type2 is TUnresolved -> {
                addSubstitution(type2, type1, sub, errors)
            }

            type1 is TInvalid || type2 is TInvalid -> return

            type1 is TComposite && type2 is TComposite -> {
                type1.params.zip(type2.params)
                    .forEach { (param1, param2) ->
                        unify(
                            param1,
                            param2,
                            sub,
                            errors
                        )
                    }

                if (type1.base != type2.base &&
                    commonBaseType(type1.base, type2.base) == null
                ) {
                    errors += TypeMismatchError(type1, type2, currentConstraint!!)
                }

                if (type1.params.size != type2.params.size) {
                    errors += TypeMismatchError(type1, type2, currentConstraint!!)
                }
            }

            type1 is TGeneric -> {
                if (type2.isNever()) return

                if (type1 != type2) {
                    errors += TypeMismatchError(type1, type2, currentConstraint!!)
                }
            }

            type2 is TGeneric -> {
                if (type1.isNever()) return

                if (type1 != type2) {
                    errors += TypeMismatchError(type1, type2, currentConstraint!!)
                }
            }
        }
    }

    fun commonType(options: List<TType>): TType? {
        return when (options.size) {
            0 -> error("Empty list of options")
            1 -> options.first()
            2 -> commonType2(options[0], options[1])
            else -> (options as List<TType?>).reduceOrNull { a, b ->
                if (a != null && b != null) commonType2(a, b) else null
            }
        }
    }

    fun commonType2(a: TType, b: TType): TType? {
        if (a == b) return a

        if (a is TInvalid) return a
        if (b is TInvalid) return b

        if (a.isNever()) return b
        if (b.isNever()) return a

        if (a is TUnion && b is TUnion) return commonType((a.options + b.options).toList())
        if (a is TUnion) {
            return commonType(a.options.toList() + b)
        }
        if (b is TUnion) {
            return commonType(b.options.toList() + a)
        }

        if (a is TComposite && b is TComposite) {
            val params = a.params.zip(b.params).map { (a, b) -> commonType2(a, b) }

            val base = commonBaseType(a.base, b.base) ?: return null

            if (params.any { it == null }) {
                return null
            }

            return composite(base, params.filterNotNull())
        }

        return null
    }

    fun commonBaseType(a: TTypeBase, b: TTypeBase): TTypeBase? {
        if (a == b) return a

        if (a is TStruct && a.instance.fullName == "Never") return b
        if (b is TStruct && b.instance.fullName == "Never") return a

        // Ordering and Ordering::Equals => Ordering
        if (a is TOption && b is TOptionItem && a == b.option) return a

        // Ordering::Equals and Ordering => Ordering
        if (a is TOptionItem && b is TOption && a.option == b) return a

        // Ordering::Less and Ordering::Equals => Ordering
        if (a is TOptionItem && b is TOptionItem && a.option == b.option) return a.option

        // Function and Lambda => Function
        if (a is TStruct && a.isFunction() && b is TLambda) return a
        if (b is TStruct && b.isFunction() && a is TLambda) return b

        return null
    }
}