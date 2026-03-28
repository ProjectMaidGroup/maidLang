enum class OpType { ADD, SUB, MUL, DIV, MOD, GREATER, LESS, LOGICAL_AND, LOGICAL_OR, BIT_AND, BIT_OR, BIT_XOR, SELF_ADD, SELF_MINUS }
/**
 * # 抽象语法树 (AST) 核心节点定义
 *
 * 本编译器采用递归下降法构建 AST，节点设计遵循“原子-运算-语句-结构”的层级。
 *
 * ## 1. 原子节点 (Atomic Nodes)
 * - [IntNode], [FloatNode], [StringNode], [CharNode]: 存储基础字面量原始值。
 *
 * ## 2. 标识符与变量 (Variable)
 * - [Variable]: 仅表示符号名称。设计上与 `LOAD/STORE` 动作解耦，由后端根据上下文（左值/右值）决定访问逻辑。
 *
 * ## 3. 后缀与代理访问 (Postfix & Proxy)
 * - [FuncCall]: 表示函数调用，包含函数名及 [AstNode] 参数列表。
 * - [ProxyVar]: **核心 Interop 节点**。用于处理 Kotlin 原生对象的下标访问。
 * - *对应语法*: `name\[position\]`
 * - *运行时*: 映射到 Kotlin 集合或数组的索引操作。
 *
 * ## 4. 运算节点 (Operator Nodes)
 * - [Unary]: 前缀单目运算（如取反 `!`、负号 `-`）。
 * - [BinaryOp]: 二元运算，关联 [OpType] 定义的算术与逻辑操作。
 *
 * ## 5. 结构化容器 (Structural Container)
 * - [CodeBlock]: 维护一组顺序执行的 [AstNode] 语句，对应 `{ }` 作用域及局部变量空间。
 *
 * @see OpType 支持的运算符枚举
 * @see VariableType 变量类型枚举
 */

/**
 * Kotlin 对象交互代理节点。
 *
 * 当解析器在 [postfix] 层级识别到标识符后紧跟 `[` 时触发。
 *
 * **语法示例**:
 * ```kotlin
 * // 源代码
 * list[i + 1]
 *
 * // 对应 AST 结构
 * proxyVar(
 * name = "list",
 * position = BinaryOp(OpType.ADD, Variable("i"), IntNode(1))
 * )
 * ```
 *
 * @property name Kotlin 对象的变量标识符
 * @property position 索引表达式，计算结果将作为 Kotlin 对象的访问下标
 */
data class proxyVar(val name: String, val position: AstNode) : AstNode()
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
    data class ProxyVar(val name: String, val position: AstNode) : AstNode() //Kotlin 代理对象，表示name[position]
    data class Assign(val left: AstNode, val right: AstNode) : AstNode()// 赋值语句 注意：arg1 可以是 Variable 或 ProxyVar
    data class If(val condition: AstNode, val thenBranch: AstNode, val elseBranch: AstNode?) : AstNode()// If 分支节点 elseBranch 为可选，所以用 AstNode?
    data class While(val condition: AstNode, val body: AstNode) : AstNode()// While 循环节点
    data class Return(val value: AstNode?) : AstNode()// Return 返回节点
    data class FuncDefNode(val name: String, val args: List<String>, val body: CodeBlock) : AstNode() // 函数定义节点 (如果你打算在 declaration 中解析 fun)
}

enum class VariableType { INT, FLOAT, FUNC, CHAR, KOTLIN_TYPE }
data class FuncDef(val type: VariableType, val args: List<VariableType>)
/**
 * # 递归下降解析器 (Recursive Descent Parser)
 *
 * 负责将 [Token] 流转换为 [AstNode] 构成的抽象语法树。
 *
 * ## 解析层级 (优先级由低到高):
 * 1. [program] -> [declaration] : 程序结构与变量定义。
 * 2. [statement] : 流程控制 (if, while) 与表达式语句。
 * 3. [expression] -> [assignment] : 赋值运算 (右结合)。
 * 4. [logicalOr] -> [relational] : 逻辑与比较运算。
 * 5. [additive] -> [multiplicative] : 基础算术运算。
 * 6. [unary] : 一元前缀操作。
 * 7. [postfix] : 函数调用与 ProxyVar 代理访问。
 * 8. [primary] : 原子级字面量、变量及括号表达式。
 *
 * @property rawToken 输入的词法单元序列
 * @property nameTable 符号表，记录变量名与 [VariableType] 的映射
 * @property funcDefine 函数定义表，记录 [FuncDef] 签名
 */
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
                    AstNode.ProxyVar(name, position)
                }

                else -> throw IllegalStateException("Parser reached an impossible state at token: ${nextToken.value}")
            }
        } else {
            return primary()
        }
    }
}