import LexState.*

suspend fun main() {
    while (true) {
        val stringInput = readlnOrNull()
        if (stringInput == null) break;
        try {
            val lexerText = lexer(stringInput)
            for (i in lexerText) {
                println(
                    when (i.type) {
                        INTEGER -> "Integer"
                        IDENTIFIER -> "Identifier"
                        FLOAT -> "Float"
                        CHAR -> "Char"
                        STRING -> "String"
                        OPERATOR -> "Operator"
                        else -> "ERROR TYPE"
                    } + " " + i.value
                )
            }
        }catch (e:IllegalStateException){
            println(e.message)
        }
    }
}