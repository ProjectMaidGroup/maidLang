// C 风格 for 循环测试

// 测试1: 基本的 for 循环（带类型声明）
int sum = 0;
for (int i = 0; i < 5; i = i + 1) {
    sum = sum + i;
}
println("sum = " + sum);  // 预期: 0+1+2+3+4 = 10

// 测试2: for 循环（var 类型推导）
int factorial = 1;
for (var j = 1; j <= 5; j = j + 1) {
    factorial = factorial * j;
}
println("5! = " + factorial);  // 预期: 120

// 测试3: for 循环（表达式初始化）
int count = 0;
for (count = 0; count < 3; count = count + 1) {
    println("count = " + count);
}
// 预期输出: count = 0, count = 1, count = 2

// 测试4: for 循环（嵌套）
int total = 0;
for (int x = 0; x < 3; x = x + 1) {
    for (int y = 0; y < 3; y = y + 1) {
        total = total + 1;
    }
}
println("total = " + total);  // 预期: 9 (3x3)

// 测试5: for 循环中变量作用域
int outside = 100;
for (int k = 0; k < 2; k = k + 1) {
    int innerVar = k * 10;
    println("innerVar = " + innerVar);
}
// 注意: for 循环内声明的变量在循环外不可访问
// 这行应该正常执行，因为 outside 是在外部声明的
println("outside = " + outside);
