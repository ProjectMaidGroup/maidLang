# MaidLang – 嵌入式轻量脚本语言

MaidLang 是一个基于 Kotlin 的嵌入式轻量脚本语言，设计用于自动化控制脚本（原为 [MobileAIDomestic](https://github.com/huy264128-netizen/MobileAIDomestic/) 项目设计）。它提供了静态类型系统、C 风格语法、解释器与编译器/虚拟机双执行引擎，适合作为嵌入式 DSL 或教学项目。

## 特性

- **静态类型系统** – 支持 `int`、`float`、`char`、`string`、`bool`、`void` 六种基本类型，支持 C 风格类型注解（如 `int x = 5;`）。
- **类型推断** – 使用 `var` 关键字自动推断变量类型。
- **丰富的语法** – 变量声明、算术/逻辑/位运算、条件语句（`if`）、循环（`while`）、函数定义与调用、代码块作用域。
- **双执行引擎** – 提供**解释器**（直接执行 AST）和**编译器+虚拟机**（将源码编译为字节码后执行）两种运行方式。
- **可扩展性** – 预留 `import` 语句接口，未来可支持反射调用 Kotlin 函数。
- **完整的测试套件** – 包含 20+ 测试用例，自动验证解释器与编译器执行结果。

## 项目结构

```
.
├── lexer.kt              # 词法分析器
├── parser.kt             # 语法分析器（含静态类型记录）
├── interpreter.kt        # 解释器（AST 直接执行）
├── bytecode.kt           # 字节码定义（Opcode、常量池、字节码块）
├── compiler.kt           # 编译器（AST → 字节码）
├── vm.kt                 # 虚拟机（字节码执行引擎）
├── tester.kt             # 测试套件 + REPL
├── docs.md               # 语言规范文档
├── plans/                # 架构设计文档
│   └── compiler_vm_plan.md
└── README.md             # 本文档
```

## 快速开始

### 环境要求
- **Kotlin 1.6+**（或任何兼容的 Kotlin 环境）
- 可选的 Kotlin 编译器（`kotlinc`）用于编译项目

### 运行测试套件
项目已集成完整的测试用例，验证所有语言特性。执行以下命令（在项目根目录）：

```bash
kotlinc -script tester.kt
```

或使用 IntelliJ IDEA 打开项目，直接运行 `tester.kt` 的 `main` 函数。

测试套件将：
1. 运行所有预设测试用例（基础类型、表达式、控制流、函数等）。
2. 先使用解释器执行，再使用编译器+虚拟机执行，并比较两者是否均通过。
3. 输出通过/失败统计。

### 使用 REPL 交互式环境
测试套件运行结束后会自动进入 REPL 模式。您可以输入 MaidLang 代码并立即看到执行结果。

示例：
```
> int a = 10;
> a + 5
=> 15
```

输入 `exit` 退出 REPL。

### 在自己的项目中使用 MaidLang

#### 1. 解释器模式
```kotlin
import lexer
import parser
import Interpreter

val code = "int x = 5; x = x * 2;"
val tokens = lexer(code)
val p = parser(tokens)
val program = p.program()

val interpreter = Interpreter()
for (node in program.codes) {
    val result = interpreter.interpret(node)
    if (result !is MaidValue.NullVal) {
        println(result)
    }
}
```

#### 2. 编译器+虚拟机模式
```kotlin
import lexer
import parser
import Compiler
import VM

val code = "int x = 5; x = x * 2;"
val tokens = lexer(code)
val p = parser(tokens)
val program = p.program()

val compiler = Compiler()
val chunk = compiler.compile(program)

val vm = VM()
// 可选的全局常量预定义
vm.defineGlobal("true", MaidValue.IntVal(1))
vm.defineGlobal("false", MaidValue.IntVal(0))
vm.defineGlobal("PI", MaidValue.FloatVal(3.141592653589793))

val result = vm.interpret(chunk)
println(result)  // 输出：10
```

#### 3. 查看生成的字节码
```kotlin
val compiler = Compiler()
val chunk = compiler.compile(program)
println(chunk.disassemble())
```

输出示例：
```
0000  LOAD_INT           0  ; 5
0002  STORE_VAR          0
0004  LOAD_VAR           0
0006  LOAD_INT           1  ; 2
0008  MUL
0009  STORE_VAR          0
```

## 语言语法概览

MaidLang 采用类 C 语法，主要结构如下：

### 变量声明
```c
int a = 5;
float b = 3.14;
char c = 'x';
string s = "hello";
bool flag = true;
var inferred = 42;          // 推断为 int
```

### 函数声明
```c
int add(int a, int b) {
    return a + b;
}

void sayHello() {
    print("Hello!");
}
```

### 控制流
```c
if (x > 0) {
    print("positive");
} else {
    print("non‑positive");
}

int i = 0;
while (i < 10) {
    print(i);
    i = i + 1;
}
```

### 运算符
- 算术：`+` `-` `*` `/` `%`
- 比较：`>` `<` `>=` `<=` `==` `!=`
- 逻辑：`&&` `||` `!`（返回 0/1）
- 位运算：`&` `|` `^`

### 代码块与作用域
```c
{
    int inner = 100;
    print(inner);
}
// inner 在此不可见
```

详细的语法规范请参阅 [docs.md](docs.md)。

## 编译器与虚拟机架构

MaidLang 的编译器将源代码经过以下流水线处理：

```
源码 → 词法分析 → Token 流 → 语法分析 → AST → 编译 → 字节码 → 虚拟机执行 → 结果
```

### 字节码指令集
设计了一套基于栈的指令集，包含 `LOAD_INT`、`LOAD_VAR`、`ADD`、`JUMP_IF_TRUE`、`CALL`、`RETURN` 等 30 余条指令。完整定义见 [bytecode.kt](bytecode.kt)。

### 虚拟机
虚拟机采用经典的操作数栈+调用栈模型，支持函数调用、局部变量、条件跳转等运行时功能。实现见 [vm.kt](vm.kt)。

详细的架构设计文档请参阅 [plans/compiler_vm_plan.md](plans/compiler_vm_plan.md)。

## API 参考

### 主要类

| 类名 | 说明 |
|------|------|
| `lexer` | 词法分析器，将字符串转换为 `Token` 列表。 |
| `parser` | 语法分析器，构建 AST 并记录类型信息。 |
| `Interpreter` | 解释器，直接执行 AST。 |
| `Compiler` | 编译器，将 AST 编译为 `BytecodeChunk`。 |
| `VM` | 虚拟机，执行字节码。 |
| `BytecodeChunk` | 字节码块，包含指令序列和常量池。 |
| `MaidValue` | 运行时值封装（`IntVal`、`FloatVal`、`StringVal` 等）。 |

### 常用方法
- `lexer(rawText: String): MutableList<Token>`
- `parser.program(): AstNode.CodeBlock`
- `Interpreter.interpret(node: AstNode): MaidValue`
- `Compiler.compile(node: AstNode): BytecodeChunk`
- `VM.interpret(chunk: BytecodeChunk): MaidValue`

## 示例程序

### 计算斐波那契数列
```c
fun int fib(int n) {
    if (n <= 1) {
        return n;
    }
    return fib(n - 1) + fib(n - 2);
}

int result = fib(10);  // 结果 55
```

### 简单的循环累加
```c
int sum = 0;
int i = 1;
while (i <= 100) {
    sum = sum + i;
    i = i + 1;
}
// sum = 5050
```

## 未来计划（待实现）

- **静态类型检查器** – 在解析阶段进行类型兼容性检查。
- **类型推导算法** – 增强 `var` 和表达式类型的推断。
- **反射调用 Kotlin 函数** – 实现 `import` 语句，直接调用外部 Kotlin 代码。
- **优化编译器** – 常量折叠、死代码消除、局部变量分配优化。
- **调试工具** – 字节码单步调试、变量查看。

## 贡献与反馈

本项目为开源教学/实验项目，欢迎提交 Issue 或 Pull Request。

- 仓库地址：[https://github.com/huy264128-netizen/MobileAIDomestic/](https://github.com/huy264128-netizen/MobileAIDomestic/)（原项目）
- 问题反馈：请在 GitHub 仓库中创建 Issue。

## 许可证

本项目基于 MIT 许可证开源。详见项目根目录的 LICENSE 文件（如有）。

---

感谢使用 MaidLang！希望这个轻量级脚本语言能为你的嵌入式控制或教学项目带来便利。
