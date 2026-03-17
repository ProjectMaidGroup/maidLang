val specialCharacterTable = hashSetOf(
    '+',
    '-',
    '*',
    '/',
    '=',
    '(',
    ')',
    ',',
    ';',
    ':',
    '&',
    '^',
    '%',
    '$',
    '#',
    '@',
    '!',
    '<',
    '>',
    '?',
    '\"',
    '\'',
    '.',
    '[',
    ']',
    '{',
    '}',
    '\\'
)
val ignoredCharacterTable = hashSetOf(' ', '\n', '\t')
fun splitString(rawString: String): ArrayList<String> {
    val splitStr = arrayListOf<String>()
    var strBuffer = ""
    for (character in rawString) {
        if (character == '.' && strBuffer != "" && strBuffer.toIntOrNull() != null) {
            strBuffer += '.'
        } else if (specialCharacterTable.contains(character)) {
            if (strBuffer != "" && !specialCharacterTable.contains(strBuffer[0])) {
                splitStr.add(strBuffer)
                strBuffer = ""
            }
            if (strBuffer != "") {
                if(character=='='&& setOf('+','-','*','/','^','&','|','%','=','!').contains(strBuffer[0])){
                    strBuffer+=character
                    splitStr.add(strBuffer)
                    strBuffer=""
                }else if (setOf('|','&','+','-').contains(character)&&strBuffer[0]==character){
                    strBuffer+=character
                    splitStr.add(strBuffer)
                    strBuffer=""
                }else{
                    splitStr.add(strBuffer);
                    splitStr.add(character.toString())
                }
            }

        } else if (ignoredCharacterTable.contains(character)) {
            if (strBuffer != "") {
                splitStr.add(strBuffer)
                strBuffer = ""
            }
        } else {
            if (strBuffer != "" && specialCharacterTable.contains(strBuffer[0])) {
                splitStr.add(strBuffer)
                strBuffer = ""
            }
            strBuffer += character
        }
    }
    return splitStr
}

data class token(val type: Int, val value: String?)
