val operators = setOf(
    '!',
    '@',
    '#',
    '$',
    '%',
    '^',
    '&',
    '*',
    '(',
    ')',
    '-',
    '+',
    '=',
    '{',
    '}',
    ':',
    ';',
    '<',
    '>',
    ',',
    '.',
    '/',
    '?',
    '|'
)

data class token(val type: Int, val value: String) //define same as state

suspend fun lexer(rawText: String) {
    var state: Int = 0  // 0 -> Null Statement 1-> Identify 2->Int 3->Float 4:Char 5:String 6:Operator
    var catchSlash: Boolean = false //  捕捉到了反斜杠时为 true
    var lexerBuffer: String = ""
    var tokenList = mutableListOf<token>()
    for (character in rawText) {
        when (state) {
            0 -> {
                when {
                    character.isDigit() -> {
                        lexerBuffer += character
                        state = 2 //switch to Int mode
                    }

                    character == '.' -> {
                        lexerBuffer = "."
                        state = 6 //switch to Float mode
                    }

                    character == '\'' -> state = 4 //switch to Char mode

                    character == '"' -> state = 5 //switch to String mode

                    setOf(' ', '\n', '\t').contains(character) -> continue
                    else -> state = 1 //switch to Identify mode
                }
            }

            1 -> {
                if (operators.contains(character) || setOf('\\', '"', '\'', ' ', '\n', '\t').contains(character)) {
                    tokenList.add(token(1, lexerBuffer))
                    lexerBuffer = ""
                    if (operators.contains(character)) {
                        lexerBuffer += character
                        state = 6
                        continue
                    }
                    state = 0
                } else lexerBuffer += character
            }

            2 -> {
                if (character == '.' || character == 'e') {
                    lexerBuffer += character
                    state = 3
                    continue
                }
                if (operators.contains(character) || setOf('\\', '"', '\'', ' ', '\n', '\t').contains(character)) {
                    tokenList.add(token(1, lexerBuffer))
                    lexerBuffer = ""
                    if (operators.contains(character)) {
                        lexerBuffer += character
                        state = 6
                        continue
                    }
                    state = 0
                    continue
                }
                if (character.isDigit())
                    lexerBuffer += character
                else
                    throw IllegalStateException("string $lexerBuffer is not a illegal value")
            }
        }
    }
}