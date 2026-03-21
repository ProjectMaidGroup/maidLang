enum class OpType { ADD, SUB, MUL, DIV, MOD, GREATER, LESS, AND, OR, BIT_AND, BIT_OR, BIT_XOR, LOAD_INT, LOAD_FLOAT, LOAD_KOTLIN_TYPE, SELF_ADD, SELF_MINUS }

sealed class AstNode {
    data class SubAST(val node: AstNode) : AstNode()
    data class IntNode(val value: Int) : AstNode()
    data class FloatNode(val value: Float) : AstNode()
    data class StringNode(val value: String) : AstNode()
    data class CharNode(val value: Char) : AstNode()
    data class Variable(val name: String) : AstNode()
    data class FuncCall(val funName: String, val args: ArrayList<AstNode>) : AstNode()
    data class BinaryOp(val op: OpType, val arg1: AstNode, val arg2: AstNode) : AstNode()
    data class Unary(val op: OpType, val arg: AstNode) : AstNode()
    data class CodeBlock(val codes: ArrayList<AstNode>) : AstNode()
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

    fun primary(): AstNode {
        val ans = when (rawToken[nowElement].type) {
            LexState.INTEGER -> AstNode.IntNode(rawToken[nowElement].value.toInt())
            LexState.FLOAT -> AstNode.FloatNode(rawToken[nowElement].value.toFloat())
            LexState.STRING -> AstNode.StringNode(rawToken[nowElement].value)
            LexState.CHAR -> AstNode.CharNode(rawToken[nowElement].value[0])
            LexState.IDENTIFIER -> when (nameTable[rawToken[nowElement].value]) {
                null -> throw IllegalStateException("Variable ${rawToken[nowElement].value} is not exist")
                VariableType.INT -> AstNode.Unary(OpType.LOAD_INT, AstNode.Variable(rawToken[nowElement].value))
                VariableType.FLOAT -> AstNode.Unary(OpType.LOAD_FLOAT, AstNode.Variable(rawToken[nowElement].value))
                VariableType.CHAR -> AstNode.Unary(OpType.LOAD_INT, AstNode.Variable(rawToken[nowElement].value))
                VariableType.KOTLIN_TYPE -> AstNode.Unary(
                    OpType.LOAD_KOTLIN_TYPE, AstNode.Variable(rawToken[nowElement].value)
                )

                VariableType.FUNC -> throw IllegalArgumentException("there shouldn't be FUNCTION in parser.primary()!")
            }

            else -> throw IllegalArgumentException("Illegal Token Type in parser.primary")
        }
        nowElement++
        return ans
    }

    fun expression(): AstNode {
        return AstNode.IntNode(1)
    }

    fun postFix(): AstNode {
        if (rawToken[nowElement + 1] == Token(
                LexState.OPERATOR, "("
            ) || rawToken[nowElement + 1] == Token(LexState.OPERATOR, "[")
        ) {
            when (rawToken[nowElement + 1]) {
                Token(LexState.OPERATOR, "(") -> {
                    if (!funcDefine.contains(rawToken[nowElement].value)) {
                        throw IllegalArgumentException("${rawToken[nowElement].value} is not a Function")
                    }

                }

                Token(LexState.OPERATOR, "[") -> {

                }
            }
        } else {
            return primary()
        }
    }
}