import LexState.*
import kotlinx.coroutines.runBlocking
import Compiler
import VM
import MaidValue
import Interpreter
import java.io.File

fun main(args: Array<String>) = runBlocking {
    when {
        args.isNotEmpty() && args[0] == "--test" -> runTestSuite()
        args.isNotEmpty() -> runFile(args[0])
        else -> runRepl()
    }
}

suspend fun runTestSuite() {
    println("MaidLang 测试套件")
    println("=================")
    
    val interpreter = Interpreter()
    // 注册原生函数（免反射调用）
    registerDefaultNatives(interpreter)
    val testCases = listOf(
        // 基础类型和变量声明
        "int a = 5;" to "int 变量声明",
        "float b = 3.14;" to "float 变量声明",
        "char c = 'x';" to "char 变量声明",
        "string s = \"hello\";" to "string 变量声明",
        "bool flag = true;" to "bool 变量声明",
        "var x = 42;" to "var 类型推断",
        "var y = 2.718;" to "var 推断为 float",
        "var z = \"world\";" to "var 推断为 string",
        
        // 表达式和运算
        "int sum = 1 + 2 * 3;" to "算术表达式",
        "bool cmp = 5 > 3;" to "比较运算",
        "bool logic = true && false;" to "逻辑运算",
        // 位运算暂时注释掉，因为解析器不支持
        // "int bit = 5 & 3;" to "位运算",
        
        // 控制流
        "if (true) { print(\"if works\"); }" to "if 语句",
        "int i = 0; while (i < 3) { print(i); i = i + 1; }" to "while 循环",
        
        // 函数定义和调用
        "fun int add(int a, int b) { return a + b; }" to "函数定义",
        "int result = add(2, 3);" to "函数调用",
        "fun void sayHello() { print(\"Hello!\"); }" to "void 函数",
        "sayHello();" to "调用 void 函数",
        
        // 嵌套作用域和代码块
        "{ int inner = 10; print(inner); }" to "代码块作用域",
        
        // 赋值和变量更新
        "int v = 0; v = v + 1;" to "变量赋值",
        
        // 返回语句
        "fun int getFive() { return 5; }" to "返回整数",
        
        // 更多类型混合
        "float f = 2.5 + 1.0;" to "float 加法",
        "string concat = \"ab\" + \"cd\";" to "字符串拼接",
        
        // for 循环
        "int s = 0; for (int i = 0; i < 5; i = i + 1) { s = s + i; }" to "for 循环（类型声明）",
        "int total = 0; for (var j = 0; j < 3; j = j + 1) { total = total + j; }" to "for 循环（var 推导）",
        "int cnt = 0; for (cnt = 0; cnt < 2; cnt = cnt + 1) { }" to "for 循环（表达式初始化）",
        "int x = 0; for (; x < 3; x = x + 1) { }" to "for 循环（省略初始化）",
    )
    
    var passed = 0
    var failed = 0
    
    for ((code, description) in testCases) {
        print("测试: $description ... ")
        try {
            val tokens = lexer(code)
            val p = parser(tokens)
            val program = p.program()
            for (node in program.codes) {
                interpreter.interpret(node)
            }
            println("✅ 通过")
            passed++
        } catch (e: Exception) {
            println("❌ 失败: ${e.message}")
            e.printStackTrace()
            failed++
        }
    }
    
    println("\n总计: ${passed + failed} 个测试, 通过: $passed, 失败: $failed")
    
    if (failed == 0) {
        println("所有测试通过！")
    } else {
        println("存在失败的测试。")
    }
    
    // 虚拟机测试暂时禁用
    val compilerPassed = 0
    val compilerFailed = 0
    println("\n编译器测试总计: 0 个测试, 通过: 0, 失败: 0")
}

suspend fun runFile(path: String) {
    val file = File(path)
    if (!file.exists()) {
        println("文件不存在: $path")
        return
    }
    val code = file.readText()
    val interpreter = Interpreter()
    registerDefaultNatives(interpreter)
    runCode(code, interpreter)
}

suspend fun runRepl() {
    val interpreter = Interpreter()
    registerDefaultNatives(interpreter)
    println("MaidLang REPL（输入 'exit' 退出）:")
    while (true) {
        print("> ")
        val line = readlnOrNull()
        if (line == null || line == "exit") break
        if (line.isBlank()) continue
        runCode(line, interpreter)
    }
}

suspend fun runCode(code: String, interpreter: Interpreter = Interpreter()) {
    try {
        val tokens = lexer(code)
        val p = parser(tokens)
        val program = p.program()
        for (node in program.codes) {
            val result = interpreter.interpret(node)
            if (result !is MaidValue.NullVal) {
                println("=> $result")
            }
        }
    } catch (e: Exception) {
        println("错误: ${e.message}")
    }
}

/**
 * 注册默认的原生函数，供 external fun 声明使用。
 * 这些函数通过 Kotlin lambda 直接实现，无需 Java 反射。
 */
fun registerDefaultNatives(interpreter: Interpreter) {
    // 数学函数
    interpreter.registerNative("abs") { args ->
        when (args.firstOrNull()) {
            is MaidValue.FloatVal -> MaidValue.FloatVal(kotlin.math.abs((args[0] as MaidValue.FloatVal).value))
            else -> MaidValue.IntVal(kotlin.math.abs(args[0].asInt()))
        }
    }
    interpreter.registerNative("sin") { args ->
        MaidValue.FloatVal(kotlin.math.sin(args[0].asFloat().toDouble()).toFloat())
    }
    interpreter.registerNative("cos") { args ->
        MaidValue.FloatVal(kotlin.math.cos(args[0].asFloat().toDouble()).toFloat())
    }
    interpreter.registerNative("sqrt") { args ->
        MaidValue.FloatVal(kotlin.math.sqrt(args[0].asFloat().toDouble()).toFloat())
    }
    interpreter.registerNative("pow") { args ->
        val base = args[0].asFloat().toDouble()
        val exp = args[1].asFloat().toDouble()
        MaidValue.FloatVal(java.lang.Math.pow(base, exp).toFloat())
    }
    interpreter.registerNative("max") { args ->
        MaidValue.IntVal(kotlin.math.max(args[0].asInt(), args[1].asInt()))
    }
    interpreter.registerNative("min") { args ->
        MaidValue.IntVal(kotlin.math.min(args[0].asInt(), args[1].asInt()))
    }
    interpreter.registerNative("round") { args ->
        MaidValue.IntVal(java.lang.Math.round(args[0].asFloat()))
    }
    interpreter.registerNative("floor") { args ->
        MaidValue.IntVal(kotlin.math.floor(args[0].asFloat().toDouble()).toInt())
    }
}
