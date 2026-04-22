# MaidLang – 嵌入式轻量脚本语言

MaidLang 是一个基于 Kotlin 的嵌入式轻量脚本语言，设计用于自动化控制脚本（原为 [MobileAIDomestic](https://github.com/huy264128-netizen/MobileAIDomestic/) 项目设计）。它提供了静态类型系统、C 风格语法、解释器与编译器/虚拟机双执行引擎，适合作为嵌入式 DSL 或教学项目。

## 特性

- **静态类型系统** – 支持 `int`、`float`、`char`、`string`、`bool`、`void` 六种基本类型，支持 C 风格类型注解（如 `int x = 5;`）。
- **类型推断** – 使用 `var` 关键字自动推断变量类型。
- **丰富的语法** – 变量声明、算术/逻辑/位运算、条件语句（`if`）、循环（`while` / `for`）、函数定义与调用、代码块作用域。
- **双执行引擎** – 提供**解释器**（直接执行 AST）和**编译器+虚拟机**（将源码编译为字节码后执行）两种运行方式。
- **可扩展性** – 支持 `import` 语句反射调用 Kotlin/Java 静态方法，以及 `external fun` 免反射原生函数注册。
- **完整的测试套件** – 包含 26 个测试用例，自动验证解释器与编译器执行结果。

## 项目结构

```
.
├── src/main/kotlin/
│   ├── lexer.kt              # 词法分析器
│   ├── parser.kt             # 语法分析器（含静态类型记录）
│   ├── interpreter.kt        # 解释器（AST 直接执行）
│   ├── bytecode.kt           # 字节码定义（Opcode、常量池、字节码块）
│   ├── compiler.kt           # 编译器（AST → 字节码）
│   ├── vm.kt                 # 虚拟机（字节码执行引擎）
│   └── tester.kt             # 测试套件 + REPL + 文件执行入口
├── docs.md                   # 语言规范文档
├── build.gradle.kts          # Gradle 构建配置
├── gradlew / gradlew.bat     # Gradle Wrapper
├── *.ml                      # MaidLang 示例源文件
├── plans/
│   └── compiler_vm_plan.md   # 架构设计文档
└── README.md                 # 本文档
```

## 快速开始

### 环境要求

- **JDK 21+**（推荐 JDK 21）
- **Gradle**（可使用项目自带的 `gradlew.bat` / `gradlew`）

### 运行测试套件

项目已集成完整的测试用例，验证所有语言特性：

```bash
# Windows
gradlew.bat run --args="--test"

# Linux/macOS
./gradlew run --args="--test"
```

测试套件将输出：

```
MaidLang 测试套件
=================
测试: int 变量声明 ... ✅ 通过
测试: for 循环（类型声明） ... ✅ 通过
...
总计: 26 个测试, 通过: 26, 失败: 0
所有测试通过！
```

### 运行 MaidLang 源文件

创建 `.ml` 源文件（如 [`test_for.ml`](test_for.ml)），然后直接运行：

```bash
gradlew.bat run --args="test_for.ml"
```

示例输出：

```
=> IntVal(value=10)
sum = 10
5! = 120
count = 0
count = 1
count = 2
total = 9
innerVar = 0
innerVar = 10
outside = 100
```

### 使用 REPL 交互式环境

不带参数运行即可进入 REPL 模式：

```bash
gradlew.bat run
```

```
MaidLang REPL（输入 'exit' 退出）:
> int a = 10;
> int b = 20;
> a + b
=> 30
> for (int i = 0; i < 5; i = i + 1) { print(i); }
01234
> exit
```

### 在自己的项目中使用 MaidLang

#### 1. 解释器模式

```kotlin
import lexer
import parser
import Interpreter

val code = """
    int sum = 0;
    for (int i = 1; i <= 100; i = i + 1) {
        sum = sum + i;
    }
"""
val tokens = lexer(code)
val p = parser(tokens)
val program = p.program()

val interpreter = Interpreter()
for (node in program.codes) {
    val result = interpreter.interpret(node)
    if (result !is MaidValue.NullVal) {
        println(result)  // 输出：5050
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
vm.defineGlobal("true", MaidValue.IntVal(1))
vm.defineGlobal("false", MaidValue.IntVal(0))

val result = vm.interpret(chunk)
println(result)  // 输出：10
```

#### 3. 查看生成的字节码

```kotlin
val compiler = Compiler()
val chunk = compiler.compile(program)
println(chunk.disassemble())
```

for 循环编译后的字节码示例：

```
0000  LOAD_INT           0  ; 0
0002  STORE_VAR          0   ; sum = 0
0004  LOAD_INT           1  ; 1
0006  STORE_VAR          1   ; i = 1
0008  LOAD_VAR           1   ; i
0010  LOAD_INT           2  ; 100
0012  LESS_EQUAL              ; i <= 100
0013  JUMP_IF_FALSE     27   ; 跳出循环
0016  LOAD_VAR           0   ; sum
0018  LOAD_VAR           1   ; i
0020  ADD                     ; sum + i
0021  STORE_VAR          0   ; sum = sum + i
0023  LOAD_VAR           1   ; i
0025  LOAD_INT           3  ; 1
0027  ADD                     ; i + 1
0028  STORE_VAR          1   ; i = i + 1
0030  JUMP               8   ; 跳回条件检查
0033  ...                     ; 循环结束
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
// if 语句
if (x > 0) {
    print("positive");
} else {
    print("non‑positive");
}

// while 循环
int i = 0;
while (i < 10) {
    print(i);
    i = i + 1;
}

// for 循环（C 风格）
for (int i = 0; i < 10; i = i + 1) {
    print(i);
}

// for 循环 - 省略初始化
int j = 0;
for (; j < 5; j = j + 1) {
    print(j);
}

// for 循环 - 无限循环
for (;;) {
    // 无限循环体
}
```

### 运算符

- 算术：`+` `-` `*` `/` `%`
- 比较：`>` `<` `>=` `<=` `==` `!=`
- 逻辑：`&&` `||` `!`（返回 0/1）
- 位运算：`&` `|` `^`
- 自增/自减：`++` `--`（前缀）

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

设计了一套基于栈的指令集，包含 `LOAD_INT`、`LOAD_VAR`、`ADD`、`JUMP_IF_TRUE`、`CALL`、`RETURN` 等 30 余条指令。完整定义见 [`bytecode.kt`](src/main/kotlin/bytecode.kt)。

### 虚拟机

虚拟机采用经典的操作数栈+调用栈模型，支持函数调用、局部变量、条件跳转等运行时功能。实现见 [`vm.kt`](src/main/kotlin/vm.kt)。

详细的架构设计文档请参阅 [plans/compiler_vm_plan.md](plans/compiler_vm_plan.md)。

## API 参考

### 主要类

| 类名 | 说明 |
|------|------|
| `lexer()` | 词法分析器，将字符串转换为 `Token` 列表。 |
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

### 使用 for 循环累加求和

```c
// 计算 1 到 100 的和
int sum = 0;
for (int i = 1; i <= 100; i = i + 1) {
    sum = sum + i;
}
// sum = 5050
```

### 嵌套 for 循环

```c
// 打印九九乘法表
for (int i = 1; i <= 9; i = i + 1) {
    for (int j = 1; j <= i; j = j + 1) {
        print(i * j);
        print(" ");
    }
    println("");
}
```

### 使用 external fun 调用原生函数

```c
// 声明外部函数
external fun abs(int) -> int;
external fun sin(float) -> float;
external fun sqrt(float) -> float;

int x = abs(-5);       // 5
float y = sin(1.57);   // ~1.0
float z = sqrt(9.0);   // 3.0
```

需要在 Kotlin 宿主代码中注册：

```kotlin
interpreter.registerNative("abs") { args ->
    MaidValue.IntVal(kotlin.math.abs(args[0].asInt()))
}
```

### 使用 import 调用 Kotlin/Java 方法

```c
import "java.lang.Math.abs" as abs;
import "java.lang.Math.sin" as sin;

int x = abs(-5);     // 5
float y = sin(1.57); // ~1.0
```

## 调用方式总结

| 方式 | 命令 | 说明 |
|------|------|------|
| 运行测试套件 | `gradlew.bat run --args="--test"` | 验证所有语言特性 |
| 运行 `.ml` 文件 | `gradlew.bat run --args="文件名.ml"` | 执行 MaidLang 源文件 |
| REPL 交互模式 | `gradlew.bat run` | 逐行输入并执行代码 |
| Kotlin 嵌入 | 调用 `Interpreter` / `Compiler` + `VM` API | 在 Kotlin 项目中嵌入使用 |

## 未来计划（待实现）

- **静态类型检查器** – 在解析阶段进行类型兼容性检查。
- **类型推导算法** – 增强 `var` 和表达式类型的推断。
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
