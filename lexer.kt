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

data class Token(val type: Int, val value: String) //define same as state

suspend fun lexer(rawText: String) {
    var state: Int = 0  // 0 -> Null Statement 1-> Identify 2->Int 3->Float 4:Char 5:String 6:Operator
    var catchSlash: Boolean = false //  捕捉到了反斜杠时为 true
    var lexerBuffer: String = ""
    var tokenList = mutableListOf<Token>()
    for (character: Char in rawText) {

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
                    tokenList.add(Token(1, lexerBuffer))
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
                    tokenList.add(Token(1, lexerBuffer))
                    lexerBuffer = ""
                    if (operators.contains(character)) {
                        lexerBuffer += character
                        state = 6
                        continue
                    }
                    state = 0
                    continue
                }
                if (character.isDigit()) lexerBuffer += character
                else throw IllegalStateException("string $lexerBuffer is not a illegal value")
            }

            3 -> {
                if (operators.contains(character) || setOf('\\', '"', '\'', ' ', '\n', '\t').contains(character)) {
                    tokenList.add(Token(1, lexerBuffer))
                    lexerBuffer = ""
                    if (operators.contains(character)) {
                        lexerBuffer += character
                        state = 6
                        continue
                    }
                    state = 0
                    continue
                }
                if (character.isDigit()) lexerBuffer += character
                else throw IllegalStateException("string $lexerBuffer is not a illegal value")
            }

            4 -> {
                if (character == '\\' && !catchSlash) {
                    catchSlash = true
                    continue
                }
                val currentCharacter: Char = when (catchSlash) {
                    true -> run {
                        val mappedChar = mapOf('n' to '\n', 't' to '\t', '\'' to '\'')[character]
                        if (mappedChar != null) return@run mappedChar
                        return@run character
                    }

                    false -> character
                }
                catchSlash = false
                if (character == '\'') {
                    tokenList.add(Token(4, lexerBuffer))
                    lexerBuffer = ""
                    continue
                }
                if (lexerBuffer != "") {
                    throw IllegalStateException("Type Char must have only one Character \u270B\uD83D\uDE2D\uD83E\uDD1A")
                } else {
                    lexerBuffer += currentCharacter
                }
            }

            5 -> {
                if (character == '\\' && !catchSlash) {
                    catchSlash = true
                    continue
                }
                if (catchSlash) {
                    val currentChar = mapOf('n' to '\n', 't' to '\t', '\'' to '\'')[character];
                    if (currentChar != null) lexerBuffer += currentChar
                    else lexerBuffer += character
                    continue
                }
                if (character == '"') {
                    tokenList.add(Token(4, lexerBuffer))
                    lexerBuffer = ""
                    continue
                }
                lexerBuffer += character
            }
        }
    }
}