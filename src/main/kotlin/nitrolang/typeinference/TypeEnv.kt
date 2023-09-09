package nitrolang.typeinference

import nitrolang.ast.LstOption
import nitrolang.ast.LstStruct
import nitrolang.ast.LstTag
import nitrolang.ast.TypeParameter
import nitrolang.util.ErrorCollector
import nitrolang.util.Span

class TypeEnv(val collector: ErrorCollector) {
    private var nextId = 0
    private var nextSubId = 0
    private var nextConsId = 0

    private var allTypes = mutableMapOf<Int, TType>()
    private val allTypeBases = mutableMapOf<Int, TTypeBase>()

    private val constrains = mutableListOf<TConstraint>()
    private val unresolved = mutableSetOf<TUnresolved>()
    private val boxes = mutableListOf<TypeBox>()

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

    fun addFindFieldConstraint(instanceType: TType, span: Span, callback: (TType) -> Unit) {
        constrains += TFindField(nextConsId++, box(instanceType, span), callback, span)
    }

    fun addFindFunctionConstraint(arguments: List<TType>, span: Span, callback: (List<TType>) -> Unit) {
        constrains += TFindFunction(nextConsId++, arguments.map { box(it, span) }, callback, span)
    }

    fun unresolved(span: Span) = type {
        TUnresolved(it, span).also { self -> unresolved += self }
    }

    fun generic(instance: TypeParameter): TGeneric {
        val existing = allTypes.values
            .find { it is TGeneric && it.instance == instance } as? TGeneric

        if (existing != null) return existing

        return type { TGeneric(it, instance) }
    }

    fun invalid(span: Span) = type { TInvalid(it, span.toString()) }

    fun composite(base: TTypeBase, params: List<TType>): TComposite {
        val existing = allTypes.values
            .find { it is TComposite && it.base == base && it.params == params } as? TComposite

        if (existing != null) return existing

        return type { TComposite(it, base, params) }
    }

    fun typeBaseStruct(instance: LstStruct): TStruct {
        val existing = allTypeBases.values
            .find { it is TStruct && it.instance == instance } as? TStruct

        if (existing != null) return existing

        val id = nextSubId()
        val ty = TStruct(id, instance)
        allTypeBases[id] = ty
        return ty
    }

    fun typeBaseOption(instance: LstOption): TOption {
        val existing = allTypeBases.values
            .find { it is TOption && it.instance == instance } as? TOption

        if (existing != null) return existing

        val id = nextSubId()
        val ty = TOption(id, instance)
        allTypeBases[id] = ty
        return ty
    }

    fun typeBaseOptionItem(instance: LstStruct, optionInstance: LstOption): TOptionItem {
        val option = typeBaseOption(optionInstance)
        val existing = allTypeBases.values
            .find { it is TOptionItem && it.instance == instance && it.option == option } as? TOptionItem

        if (existing != null) return existing

        val id = nextSubId()
        val ty = TOptionItem(id, instance, option)
        allTypeBases[id] = ty
        return ty
    }

    fun typeTag(instance: LstTag): TTag {
        val existing = allTypes.values
            .find { it is TTag && it.instance == instance } as? TTag

        if (existing != null) return existing

        val id = nextId()
        val ty = TTag(id, instance)
        allTypes[id] = ty
        return ty
    }

    fun union(options: Set<TType>): TType {
        val existing = allTypes.values
            .find { it is TUnion && it.options == options } as? TUnion

        if (existing != null) return existing

        val flatOptions = flatten(options)

        val common = commonType(flatOptions.toList())
        if (common != null) {
            return common
        }

        val id = nextId()
        val ty = TUnion(id, flatOptions)
        allTypes[id] = ty
        return ty
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
        is TUnion -> union(options.map { it.replace(find, replacement) }.toSet())
        is TComposite -> composite(base, params.map { it.replace(find, replacement) })
        else -> this
    }

    fun TType.replace(find: TGeneric, replacement: TType): TType = when (this) {
        is TGeneric -> if (find.id == this.id) replacement else this
        is TUnion -> union(options.map { it.replace(find, replacement) }.toSet())
        is TComposite -> composite(base, params.map { it.replace(find, replacement) })
        else -> this
    }

    fun TType.removeUnresolved(key: TUnresolved): TType {
        return when (this) {
            is TUnion -> union(options - key)
            is TComposite -> composite(base, params.map { it.removeUnresolved(key) })
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

    fun solveConstraints() {
        val errors = mutableListOf<TypeError>()
        val substitutions = mutableMapOf<TUnresolved, TType>()
        var madeProgress: Boolean
        do {
            madeProgress = false

            // Unify
            constrains.filterIsInstance<TUnify>().forEach { constraint ->
                unify(
                    constraint.left.type,
                    constraint.right.type,
                    substitutions,
                    errors
                )
                errors.forEach { err -> err.constraint = constraint }
            }

            if (substitutions.isNotEmpty()) {
                madeProgress = true
                val remaining = ArrayDeque(substitutions.toList())
                substitutions.clear()

                while (remaining.isNotEmpty()) {
                    val (key, value) = remaining.first()
                    val replacement = simplify(value)

                    // Replace usages of `key` in all instances of TypeBox
                    replaceAll(key, replacement)

                    // Replace `key` in errors previously detected
                    errors.forEach { err ->
                        err.left = err.left.replace(key, replacement)
                        err.right = err.right.replace(key, replacement)
                    }

                    // Remove this replacement
                    remaining.removeAt(0)

                    // Make sure all uses of `key` in remaining are updated as well
                    remaining.replaceAll { (k, v) ->
                        k to v.replace(key, replacement)
                    }
                }
            }

            errors.forEach { err ->
                collector.report("${err.msg}, expected '${err.left}', found: '${err.right}'", err.constraint.span)
            }

            errors.clear()

            // Remove useless constrains
            constrains.filterIsInstance<TUnify>().forEach { constraint ->
                if (constraint.left.type == constraint.right.type || (!constraint.left.type.hasUnresolved() && !constraint.right.type.hasUnresolved())) {
                    constrains.remove(constraint)
                }
            }

            // Find field
            constrains.filterIsInstance<TFindField>().forEach { constraint ->
                if (constraint.dependency.type !is TUnresolved) {
                    madeProgress = true
                    constraint.callback(constraint.dependency.type)
                    constrains.remove(constraint)
                }
            }

            // Find function
            constrains.filterIsInstance<TFindFunction>().forEach { constraint ->
                if (constraint.dependencies.isEmpty() || constraint.dependencies.first().type !is TUnresolved) {
                    madeProgress = true
                    constraint.callback(constraint.dependencies.map { it.type })
                    constrains.remove(constraint)
                }
            }

//            print()
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
        errors: MutableList<TypeError>
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
            errors += TypeError("Type mismatch 2", prev, next)
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
        errors: MutableList<TypeError>
    ) {
        when {
            type1 is TUnresolved -> {
                addSubstitution(type1, type2, sub, errors)
            }

            type2 is TUnresolved -> {
                addSubstitution(type2, type1, sub, errors)
            }

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
                    errors += TypeError("Type mismatch", type1, type2)
                }

                if (type1.params.size != type2.params.size) {
                    errors += TypeError("Type params count mismatch", type1, type2)
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

        // ToString and Int => ToString
        if (a is TTag && b is TComposite && b.base in a.instance.taggedBaseTypes) return a
        if (b is TTag && a is TComposite && a.base in b.instance.taggedBaseTypes) return b

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

        return null
    }
}