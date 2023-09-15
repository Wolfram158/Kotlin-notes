abstract class Expression {
    abstract fun eval(args : LinkedHashMap<String, Float>) : Float?
    abstract fun diff(diff_var : String) : Expression
}

class Const(private val value : Float) : Expression() {
    override fun eval(args : LinkedHashMap<String, Float>) = value
    override fun toString() = value.toString()
    override fun diff(diff_var : String) = Const(0f)
}

class Variable(private val name : String) : Expression() {
    override fun eval(args : LinkedHashMap<String, Float>): Float? = args[name]
    override fun toString() = name
    override fun diff(diff_var : String) = if (diff_var == name) Const(1f) else Const(0f)
}

open class Operation(
    open val sign : String,
    open val op : (Float, Float) -> Float?,
    open val diff_rule : (String, Expression, Expression) -> Expression,
    open vararg val exprs : Expression) : Expression() {
    override fun eval(args : LinkedHashMap<String, Float>): Float? {
        var result = exprs[0].eval(args)
        for (i in 1 until exprs.size) {
            result = result?.let { op(it, exprs[i].eval(args)!!) }
        }
        return result
    }
    override fun toString() = exprs.joinToString(separator = sign, prefix = "(", postfix = ")")
    override fun diff(diff_var: String): Expression {
        return when (exprs.size) {
            2 -> diff_rule(diff_var, exprs[0], exprs[1])
            else -> diff_rule(diff_var, exprs[0],
                sign_map[sign]?.let { diff_map[sign]?.let { it1 ->
                    op_map[sign]?.let { it2 ->
                        Operation(it, it2,
                            it1, *exprs.copyOfRange(1, exprs.size))
                    }
                } }!!
            )
        }
    }
}

val add_diff : (String, Expression, Expression) -> Expression = { v, a, b -> Add(a.diff(v), b.diff(v)) }
val mul_diff : (String, Expression, Expression) -> Expression = { v, a, b -> Add(Multiply(a, b.diff(v)), Multiply(a.diff(v), b)) }
val add_op : (Float, Float) -> Float? = { x, y -> x + y }
val mul_op : (Float, Float) -> Float? = { x, y -> x * y }

val sign_map = mapOf("+" to "+", "-" to "+", "*" to "*", "/" to "*")
val diff_map = mapOf("+" to add_diff, "-" to add_diff, "*" to mul_diff, "/" to mul_diff)
val op_map = mapOf("+" to add_op, "-" to add_op, "*" to mul_op, "/" to mul_op)

class Add(override vararg val exprs : Expression) :
    Operation("+", add_op, add_diff, *exprs)
class Subtract(override vararg val exprs : Expression) :
    Operation("-", { x, y -> x - y }, { v, a, b -> Subtract(a.diff(v), b.diff(v)) }, *exprs)
class Multiply(override vararg val exprs : Expression) :
    Operation("*", mul_op, mul_diff, *exprs)
class Divide(override vararg val exprs : Expression) :
    Operation("/", { x, y -> x / y },
        { v, a, b -> Divide(Subtract(Multiply(a.diff(v), b), Multiply(a, b.diff(v))), Multiply(b, b))}, *exprs)

fun main() {
    val map = LinkedHashMap<String, Float>()
    map["x"] = 3f
    map["y"] = 7f
    map["z"] = -100f

    println(Add(Const(33f), Variable("x"), Variable("y")).eval(map))

    println(Multiply(Add(Variable("x"), Variable("y"), Variable("z")),
        Subtract(Const(17f), Variable("x")), Divide(Const(-10f),
            Multiply(Add(Variable("x"), Variable("y")), Const(3f)))).eval(map))

    println(Multiply(Add(Variable("x"), Variable("y"), Variable("z")),
        Subtract(Const(17f), Variable("x")), Divide(Const(-10f),
            Multiply(Add(Variable("x"), Variable("y")), Const(3f)))).diff("x").eval(map))

    println(Multiply(Add(Variable("x"), Variable("y"), Variable("z")),
        Subtract(Const(17f), Variable("x")), Divide(Const(-10f),
            Multiply(Add(Variable("x"), Variable("y")), Const(3f)))).diff("x").toString())

    println(Multiply(Add(Variable("x"), Variable("y"), Variable("z")),
        Subtract(Const(17f), Variable("x")), Divide(Const(-10f),
            Multiply(Add(Variable("x"), Variable("y")), Const(3f)))).toString())

    println(Multiply(Variable("x"), Variable("x"), Variable("x")).diff("x").toString())

    println(Subtract(Variable("x"), Variable("y"), Const(3f)).diff("x").toString())
}
