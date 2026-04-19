import kotlin.math.*

sealed class MaidValue {
    data class IntVal(var value: Int) : MaidValue()
    data class FloatVal(var value: Float) : MaidValue()
    data class StringVal(var value: String) : MaidValue()
    data class CharVal(var value: Char) : MaidValue()
    data class FunctionVal(val node: AstNode.FuncDefNode, val closure: Scope) : MaidValue()
    object NullVal : MaidValue()

    fun asInt(): Int = when (this) {
        is IntVal -> value
        is FloatVal -> value.toInt()
        is StringVal -> value.toIntOrNull() ?: 0
        is CharVal -> value.code
        else -> 0
    }

    fun asFloat(): Float = when (this) {
        is IntVal -> value.toFloat()
        is FloatVal -> value
        is StringVal -> value.toFloatOrNull() ?: 0f
        is CharVal -> value.code.toFloat()
        else -> 0f
    }

    fun asBool(): Boolean = asInt() != 0

    override fun toString(): String = when (this) {
        is IntVal -> value.toString()
        is FloatVal -> value.toString()
        is StringVal -> value
        is CharVal -> value.toString()
        is FunctionVal -> "<function ${node.name}>"
        NullVal -> "null"
    }
}

class Scope(val parent: Scope? = null) {
    private val variables = mutableMapOf<String, MaidValue>()

    fun define(name: String, value: MaidValue) {
        variables[name] = value
    }

    fun assign(name: String, value: MaidValue) {
        if (variables.containsKey(name)) {
            variables[name] = value
        } else if (parent != null) {
            parent.assign(name, value)
        } else {
            throw RuntimeException("Undefined variable: $name")
        }
    }

    fun get(name: String): MaidValue {
        return variables[name] ?: parent?.get(name) ?: throw RuntimeException("Undefined variable: $name")
    }
}

class ReturnException(val value: MaidValue) : RuntimeException()

class Interpreter {
    val globalScope = Scope()

    init {
        // 预定义布尔常量 true 和 false
        globalScope.define("true", MaidValue.IntVal(1))
        globalScope.define("false", MaidValue.IntVal(0))
        // 可选：预定义一些数学常量
        globalScope.define("PI", MaidValue.FloatVal(3.141592653589793))
    }

    fun interpret(node: AstNode): MaidValue {
        return try {
            eval(node, globalScope)
        } catch (e: ReturnException) {
            e.value
        } catch (e: Exception) {
            println("Runtime Error: ${e.message}")
            MaidValue.NullVal
        }
    }

    private fun eval(node: AstNode, scope: Scope): MaidValue {
        return when (node) {
            is AstNode.IntNode -> MaidValue.IntVal(node.value)
            is AstNode.FloatNode -> MaidValue.FloatVal(node.value)
            is AstNode.StringNode -> MaidValue.StringVal(node.value)
            is AstNode.CharNode -> MaidValue.CharVal(node.value)
            is AstNode.Variable -> scope.get(node.name)
            
            is AstNode.CodeBlock -> {
                var lastValue: MaidValue = MaidValue.NullVal
                val innerScope = Scope(scope)
                for (code in node.codes) {
                    lastValue = eval(code, innerScope)
                }
                lastValue
            }

            is AstNode.BinaryOp -> {
                val left = eval(node.arg1, scope)
                val right = eval(node.arg2, scope)
                applyBinaryOp(node.op, left, right)
            }

            is AstNode.Unary -> {
                val value = eval(node.arg, scope)
                applyUnaryOp(node.op, value, node.arg, scope)
            }

            is AstNode.Assign -> {
                val value = eval(node.right, scope)
                when (val left = node.left) {
                    is AstNode.Variable -> scope.assign(left.name, value)
                    is AstNode.ProxyVar -> {
                        // TODO: Implement proxy variable assignment if needed
                        println("Warning: ProxyVar assignment not fully implemented")
                    }
                    else -> throw RuntimeException("Invalid assignment target")
                }
                value
            }

            is AstNode.If -> {
                val condition = eval(node.condition, scope)
                if (condition.asBool()) {
                    eval(node.thenBranch, scope)
                } else if (node.elseBranch != null) {
                    eval(node.elseBranch, scope)
                } else {
                    MaidValue.NullVal
                }
            }

            is AstNode.While -> {
                var lastValue: MaidValue = MaidValue.NullVal
                while (eval(node.condition, scope).asBool()) {
                    lastValue = eval(node.body, scope)
                }
                lastValue
            }

            is AstNode.Return -> {
                val value = node.value?.let { eval(it, scope) } ?: MaidValue.NullVal
                throw ReturnException(value)
            }

            is AstNode.FuncDefNode -> {
                val func = MaidValue.FunctionVal(node, scope)
                scope.define(node.name, func)
                func
            }

            is AstNode.FuncCall -> {
                val funcValue = scope.get(node.funName)
                if (funcValue is MaidValue.FunctionVal) {
                    val funcDef = funcValue.node
                    val callScope = Scope(funcValue.closure)
                    
                    if (node.args.size != funcDef.args.size) {
                        throw RuntimeException("Function ${node.funName} expects ${funcDef.args.size} arguments, but got ${node.args.size}")
                    }

                    for (i in node.args.indices) {
                        callScope.define(funcDef.args[i], eval(node.args[i], scope))
                    }

                    try {
                        eval(funcDef.body, callScope)
                    } catch (e: ReturnException) {
                        e.value
                    }
                } else {
                    // Built-in functions or errors
                    when (node.funName) {
                        "print" -> {
                            val args = node.args.map { eval(it, scope) }
                            println(args.joinToString(" "))
                            MaidValue.NullVal
                        }
                        else -> throw RuntimeException("${node.funName} is not a function")
                    }
                }
            }
            
            is AstNode.ProxyVar -> {
                // For now, let's treat it as a mock or simple array access if we had arrays
                // The user mentioned it's a "Kotlin 代理对象"
                val base = scope.get(node.name)
                val index = eval(node.position, scope)
                println("Accessing proxy ${node.name} at index $index")
                MaidValue.NullVal
            }
        }
    }

    private fun applyBinaryOp(op: OpType, left: MaidValue, right: MaidValue): MaidValue {
        return when (op) {
            OpType.ADD -> {
                if (left is MaidValue.StringVal || right is MaidValue.StringVal) {
                    MaidValue.StringVal(left.toString() + right.toString())
                } else if (left is MaidValue.FloatVal || right is MaidValue.FloatVal) {
                    MaidValue.FloatVal(left.asFloat() + right.asFloat())
                } else {
                    MaidValue.IntVal(left.asInt() + right.asInt())
                }
            }
            OpType.SUB -> if (left is MaidValue.FloatVal || right is MaidValue.FloatVal) MaidValue.FloatVal(left.asFloat() - right.asFloat()) else MaidValue.IntVal(left.asInt() - right.asInt())
            OpType.MUL -> if (left is MaidValue.FloatVal || right is MaidValue.FloatVal) MaidValue.FloatVal(left.asFloat() * right.asFloat()) else MaidValue.IntVal(left.asInt() * right.asInt())
            OpType.DIV -> if (left is MaidValue.FloatVal || right is MaidValue.FloatVal) MaidValue.FloatVal(left.asFloat() / right.asFloat()) else MaidValue.IntVal(left.asInt() / right.asInt())
            OpType.MOD -> MaidValue.IntVal(left.asInt() % right.asInt())
            
            OpType.EQUAL -> MaidValue.IntVal(if (compareValues(left, right) == 0) 1 else 0)
            OpType.NOT_EQUAL -> MaidValue.IntVal(if (compareValues(left, right) != 0) 1 else 0)
            OpType.GREATER -> MaidValue.IntVal(if (compareValues(left, right) > 0) 1 else 0)
            OpType.LESS -> MaidValue.IntVal(if (compareValues(left, right) < 0) 1 else 0)
            OpType.GREATER_EQUAL -> MaidValue.IntVal(if (compareValues(left, right) >= 0) 1 else 0)
            OpType.LESS_EQUAL -> MaidValue.IntVal(if (compareValues(left, right) <= 0) 1 else 0)
            
            OpType.LOGICAL_AND -> MaidValue.IntVal(if (left.asBool() && right.asBool()) 1 else 0)
            OpType.LOGICAL_OR -> MaidValue.IntVal(if (left.asBool() || right.asBool()) 1 else 0)
            
            OpType.BIT_AND -> MaidValue.IntVal(left.asInt() and right.asInt())
            OpType.BIT_OR -> MaidValue.IntVal(left.asInt() or right.asInt())
            OpType.BIT_XOR -> MaidValue.IntVal(left.asInt() xor right.asInt())
            
            else -> throw RuntimeException("Unsupported binary operator: $op")
        }
    }

    private fun compareValues(left: MaidValue, right: MaidValue): Int {
        return when {
            left is MaidValue.IntVal && right is MaidValue.IntVal -> left.value.compareTo(right.value)
            left is MaidValue.FloatVal || right is MaidValue.FloatVal -> left.asFloat().compareTo(right.asFloat())
            left is MaidValue.StringVal && right is MaidValue.StringVal -> left.value.compareTo(right.value)
            left is MaidValue.CharVal && right is MaidValue.CharVal -> left.value.compareTo(right.value)
            else -> left.toString().compareTo(right.toString())
        }
    }

    private fun applyUnaryOp(op: OpType, value: MaidValue, argNode: AstNode, scope: Scope): MaidValue {
        return when (op) {
            OpType.NOT -> MaidValue.IntVal(if (value.asBool()) 0 else 1)
            OpType.SUB -> {
                if (value is MaidValue.FloatVal) MaidValue.FloatVal(-value.value)
                else MaidValue.IntVal(-value.asInt())
            }
            OpType.SELF_ADD -> {
                if (argNode is AstNode.Variable) {
                    val newValue = if (value is MaidValue.FloatVal) MaidValue.FloatVal(value.value + 1) else MaidValue.IntVal(value.asInt() + 1)
                    scope.assign(argNode.name, newValue)
                    newValue
                } else throw RuntimeException("++ requires a variable")
            }
            OpType.SELF_MINUS -> {
                if (argNode is AstNode.Variable) {
                    val newValue = if (value is MaidValue.FloatVal) MaidValue.FloatVal(value.value - 1) else MaidValue.IntVal(value.asInt() - 1)
                    scope.assign(argNode.name, newValue)
                    newValue
                } else throw RuntimeException("-- requires a variable")
            }
            else -> throw RuntimeException("Unsupported unary operator: $op")
        }
    }
}
