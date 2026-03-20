enum class OpType { ADD, SUB, MUL, DIV }
sealed class AstNode {
    data class SubAST(val node: AstNode) : AstNode()
    data class IntNode(val value: Int) : AstNode()
    data class FloatNode(val value: Float) : AstNode()
    data class FuncCall(val funName: String, val args: ArrayList<AstNode>) : AstNode()
    data class BinaryOp(val op: OpType, val arg1: AstNode, val arg2: AstNode) : AstNode()
    data class CodeBlock(val codes:ArrayList<AstNode>):AstNode()
}