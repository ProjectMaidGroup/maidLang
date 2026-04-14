# MaidLang 语言规范

MaidLang 是一个静态类型的脚本语言，语法类似C，运行在Kotlin虚拟机上。它包含词法分析器、解析器和解释器，支持变量、函数、控制流等基本编程结构。

## 1. 类型系统

MaidLang 是静态类型语言，所有变量和函数必须在编译时确定类型。

### 基本类型

| 类型关键字 | 描述           | 示例值        |
|------------|----------------|---------------|
| `int`      | 32位整数       | `42`, `-7`    |
| `float`    | 单精度浮点数   | `3.14`, `-2.5`|
| `char`     | 单个字符       | `'a'`, `'\n'` |
| `string`   | 字符串         | `"hello"`     |
| `bool`     | 布尔值         | `true`, `false`|
| `void`     | 无类型，用于函数返回值 | `void` |

### 类型推导

变量声明时可以省略类型，编译器会根据初始化表达式推断类型：
```c
var x = 5;          // 推断为 int
var y = 3.14;       // 推断为 float
var z = "hello";    // 推断为 string
```

### 函数类型

函数类型表示为参数类型列表和返回类型，例如 `(int, int) -> int`。

## 2. 语法

### 2.1 变量声明

```c
int age = 25;
float pi = 3.14159;
char initial = 'A';
string name = "Alice";
bool flag = true;

// 类型推导
var inferredInt = 42;
var inferredFloat = 2.718;
```

### 2.2 函数声明

```c
// 无参数无返回值
void sayHello() {
    print("Hello!");
}

// 带参数和返回值
int add(int a, int b) {
    return a + b;
}

// 类型推导返回值（暂不支持）
```

### 2.3 控制流

```c
// if 语句
if (x > 0) {
    print("positive");
} else {
    print("non-positive");
}

// while 循环
int i = 0;
while (i < 10) {
    print(i);
    i = i + 1;
}
```

### 2.4 表达式

支持常见的算术、逻辑、比较和位运算：

```c
int a = 5 + 3 * 2;      // 算术
bool b = a > 10 && a < 20; // 逻辑
int c = a & 0xFF;       // 位运算
```

## 3. 内置函数

### 3.1 打印函数

```c
print("Hello, world!"); // 输出到标准输出
```

### 3.2 导入Kotlin函数

可以使用 `import` 语句导入Kotlin标准库或其他已定义的Kotlin函数：

```c
import "kotlin.math.sin" as sin;
import "kotlin.math.cos" as cos;

float x = sin(3.14);
```

## 4. 示例程序

```c
// 计算斐波那契数列
int fib(int n) {
    if (n <= 1) {
        return n;
    }
    return fib(n - 1) + fib(n - 2);
}

void main() {
    int result = fib(10);
    print("fib(10) = " + result);
}
```

## 5. 实现细节

MaidLang 由以下组件构成：

- **lexer.kt**: 词法分析器，将源代码转换为令牌流。
- **parser.kt**: 递归下降解析器，构建抽象语法树（AST）。
- **interpreter.kt**: 解释器，执行AST并管理作用域。
- **tester.kt**: REPL交互环境，用于测试代码。

## 6. 未来扩展

- 数组和集合类型
- 结构体/类定义
- 模块系统
- 泛型
