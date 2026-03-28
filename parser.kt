enum class OpType { ADD, SUB, MUL, DIV, MOD, GREATER, LESS, LOGICAL_AND, LOGICAL_OR, BIT_AND, BIT_OR, BIT_XOR, SELF_ADD, SELF_MINUS }

sealed class AstNode {
    data class IntNode(val value: Int) : AstNode()
    data class FloatNode(val value: Float) : AstNode()
    data class StringNode(val value: String) : AstNode()
    data class CharNode(val value: Char) : AstNode()
    data class Variable(val name: String) : AstNode()
    data class FuncCall(val funName: String, val args: ArrayList<AstNode>) : AstNode()
    data class BinaryOp(val op: OpType, val arg1: AstNode, val arg2: AstNode) : AstNode()
    data class Unary(val op: OpType, val arg: AstNode) : AstNode() //前缀表达式
    data class CodeBlock(val codes: ArrayList<AstNode>) : AstNode() //代码块
    data class proxyVar(val name: String, val position: AstNode) : AstNode() //Kotlin 代理对象，表示name[position]
}

enum class VariableType { INT, FLOAT, FUNC, CHAR, KOTLIN_TYPE }
data class FuncDef(val type: VariableType, val args: List<VariableType>)
class parser(val rawToken: MutableList<Token>) {
    val nameTable = mutableMapOf<String, VariableType>()
    val funcDefine = mutableMapOf<String, FuncDef>()
    var nowElement: Int = 0
    fun consume(token: Token) {
        if (rawToken[nowElement] != token) {
            throw IllegalStateException("there should be a Token ${token.value} , but we can't find it")
        }
        nowElement++;
    }
    /** 解析整个程序：由多个声明或语句组成 */
    fun program(): AstNode.CodeBlock = TODO("循环调用 declaration() 直到文件结束")

    /** 声明层级：处理变量定义(var)、函数定义(fun)等，若不是声明则降级为 statement */
    fun declaration(): AstNode = TODO("判断是否为 var/fun，否则返回 statement()")

    /** 语句层级：处理 if, while, for, return 或 代码块 { } */
    fun statement(): AstNode = when (rawToken.getOrNull(nowElement)?.value) {
        "{" -> codeBlock()
        "if" -> ifStatement()
        "while" -> whileStatement()
        else -> expressionStatement()
    }
    /** TODO 14: If 语句 - if (expr) stmt [else stmt] */
    fun ifStatement(): AstNode = TODO("consume('if'), consume('('), condition=expression(), consume(')'), then=statement(), else=if(match('else')) statement()")

    /** TODO 15: While 语句 - while (expr) stmt */
    fun whileStatement(): AstNode = TODO("consume('while'), consume('('), condition=expression(), consume(')'), body=statement()")
    /** 代码块：解析 { 语句1; 语句2; ... } */
    fun codeBlock(): AstNode.CodeBlock = TODO("解析 { , 循环调用 statement() , 解析 }")

    /** 表达式语句：解析 expression() 并消耗末尾的分号 */
    fun expressionStatement(): AstNode = TODO("调用 expression() 并 consume(';')")

    // --- 2. 表达式优先级 (Expression Precedence - 由低到高) ---

    /** 优先级 1: 总入口 */
    fun expression(): AstNode = assignment()

    /** 优先级 2: 赋值 (右结合) -> a = b = 5 */
    fun assignment(): AstNode = TODO("left = logicalOr(), if match('=') BinaryOp(ASSIGN, left, assignment())")

    /** 优先级 3: 逻辑或 (||) */
    fun logicalOr(): AstNode = TODO("while match('||') 包装左结合 BinaryOp")

    /** 优先级 4: 逻辑与 (&&) */
    fun logicalAnd(): AstNode = TODO("while match('&&') 包装左结合 BinaryOp")

    /** 优先级 5: 相等性 (==, !=) */
    fun equality(): AstNode = TODO("while match('==' 或 '!=') 包装 BinaryOp")

    /** 优先级 6: 比较 (<, >, <=, >=) */
    fun relational(): AstNode = TODO("while match('<','>','<=','>=') 包装 BinaryOp")

    /** 优先级 7: 加减 (+, -) */
    fun additive(): AstNode = TODO("while match('+','-') 包装 BinaryOp，下一级调用 multiplicative()")

    /** 优先级 8: 乘除余 (*, /, %) */
    fun multiplicative(): AstNode = TODO("while match('*','/','%') 包装 BinaryOp，下一级调用 unary()")

    /** 优先级 9: 前缀一元运算 (!, -, ++, --) */
    fun unary(): AstNode = TODO("if match('!','-') 返回 Unary(op, unary())，否则返回 postfix()")
    fun primary(): AstNode {
        val current = rawToken.getOrNull(nowElement)
            ?: throw IllegalStateException("Unexpected end of input at position $nowElement")

        val ans = when (current.type) {
            // 1. 处理字面量
            LexState.INTEGER -> AstNode.IntNode(current.value.toInt())
            LexState.FLOAT -> AstNode.FloatNode(current.value.toFloat())
            LexState.STRING -> AstNode.StringNode(current.value)
            LexState.CHAR -> AstNode.CharNode(current.value[0])

            // 2. 处理标识符（变量名/函数名）
            LexState.IDENTIFIER -> {
                val name = current.value
                // 语义检查：确保变量已定义
                if (!nameTable.contains(name)) {
                    throw IllegalStateException("Symbol '$name' is not defined in the current scope")
                }
                // 只返回基础变量节点，不包装任何 LOAD 操作
                AstNode.Variable(name)
            }

            // 3. 处理括号表达式 (expression) -> 提高优先级
            LexState.OPERATOR -> {
                if (current.value == "(") {
                    nowElement++ // 跳过 '('
                    val node = expression() // 递归调用，解析括号内的完整逻辑
                    consume(Token(LexState.OPERATOR, ")")) // 确保有对应的 ')'
                    return node // 注意：这里直接返回，避开末尾的 nowElement++
                } else {
                    throw IllegalArgumentException("Unexpected operator '${current.value}' in primary expression")
                }
            }

            else -> throw IllegalArgumentException("Unsupported token type ${current.type} in primary")
        }

        // 只有非括号路径会走到这里，统一移动指针
        nowElement++
        return ans
    }
    fun postfix(): AstNode {
        val nextToken = rawToken.getOrNull(nowElement + 1)

        // 预判：是否存在后缀操作符
        if (nextToken?.type == LexState.OPERATOR && (nextToken.value == "(" || nextToken.value == "[")) {
            return when (nextToken.value) {
                "(" -> {
                    val func = primary() // 指针移到 "("
                    if (func !is AstNode.Variable) {
                        throw IllegalStateException("Expected a function name, but found something else.")
                    }

                    val funcName = func.name
                    val definition =
                        funcDefine[funcName] ?: throw IllegalArgumentException("Function '$funcName' is not defined.")

                    consume(Token(LexState.OPERATOR, "("))

                    val argsNode = arrayListOf<AstNode>()
                    val expectedArgs = definition.args

                    // 只有当定义中有参数时才进行循环
                    if (expectedArgs.isNotEmpty()) {
                        for (i in expectedArgs.indices) {
                            argsNode.add(expression())
                            // 如果不是最后一个参数，必须消耗逗号
                            if (i < expectedArgs.size - 1) {
                                consume(Token(LexState.OPERATOR, ","))
                            }
                        }
                    }

                    consume(Token(LexState.OPERATOR, ")"))
                    AstNode.FuncCall(funcName, argsNode)
                }

                "[" -> {
                    val kotlinObject = primary() // 指针移到 "["
                    if (kotlinObject !is AstNode.Variable) {
                        throw IllegalArgumentException("Only variables can be accessed by index.")
                    }

                    val name = kotlinObject.name
                    consume(Token(LexState.OPERATOR, "["))
                    val position = expression()
                    consume(Token(LexState.OPERATOR, "]"))
                    AstNode.proxyVar(name, position)
                }

                else -> throw IllegalStateException("Parser reached an impossible state at token: ${nextToken.value}")
            }
        } else {
            return primary()
        }
    }
}