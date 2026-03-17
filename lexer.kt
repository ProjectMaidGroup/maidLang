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
        if (character == '.' && strBuffer != "" && strBuffer.toIntOrNull()!=null) {
            strBuffer+='.'
        } else if (specialCharacterTable.contains(character)) {
            if (strBuffer != "") {
                splitStr.add(strBuffer)
                strBuffer = ""
            }
            splitStr.add(character.toString())
        } else if (ignoredCharacterTable.contains(character)) {
            if (strBuffer != "") {
                splitStr.add(strBuffer)
                strBuffer = ""
            }
        } else {
            strBuffer += character
        }
    }
    return splitStr
}
data class token(val type:Int,val value:String)
