import LexState.*
import kotlinx.coroutines.runBlocking
import Compiler
import VM

fun main() = runBlocking {
    println("MaidLang 测试套件")
    println("=================")
    
    val interpreter = Interpreter()
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
        "int bit = 5 & 3;" to "位运算",
        
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
    
    // 编译器 + 虚拟机测试
    println("\n=== 编译器/虚拟机测试 ===")
    var compilerPassed = 0
    var compilerFailed = 0
    val vm = VM()
    // 预定义全局常量 true/false/PI（与解释器一致）
    vm.defineGlobal("true", MaidValue.IntVal(1))
    vm.defineGlobal("false", MaidValue.IntVal(0))
    vm.defineGlobal("PI", MaidValue.FloatVal(3.141592653589793))
    
    for ((code, description) in testCases) {
        print("编译器测试: $description ... ")
        try {
            val tokens = lexer(code)
            val p = parser(tokens)
            val program = p.program()
            val compiler = Compiler()
            val chunk = compiler.compile(program)
            val result = vm.interpret(chunk)
            // 不检查具体结果，只要不抛出异常即认为通过
            println("✅ 通过")
            compilerPassed++
        } catch (e: Exception) {
            println("❌ 失败: ${e.message}")
            e.printStackTrace()
            compilerFailed++
        }
    }
    
    println("\n编译器测试总计: ${compilerPassed + compilerFailed} 个测试, 通过: $compilerPassed, 失败: $compilerFailed")
    
    // 进入 REPL 模式供手动测试
    println("\n进入 REPL 模式（输入 'exit' 退出）:")
    while (true) {
        print("> ")
        val stringInput = readlnOrNull()
        if (stringInput == null || stringInput == "exit") break
        if (stringInput.isBlank()) continue
        
        try {
            val tokens = lexer(stringInput)
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
}
