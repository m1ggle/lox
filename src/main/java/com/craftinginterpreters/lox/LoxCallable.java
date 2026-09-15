package com.craftinginterpreters.lox;

import java.util.List;

public interface LoxCallable {
    // Arity（参数数量/元数）是一个来自计算机科学和数学（逻辑学）的术语，
    // 指的是一个函数、操作符或方法所接受的参数（operands/arguments）的个数
    // 在编程语言理论中，函数根据 arity 可分为：
    //
    // Nullary（零元）​：不接受参数，如 Math.random()
    // Unary（一元）​：接受 1 个参数，如 Math.abs(-5)
    // Binary（二元）​：接受 2 个参数，如 a + b
    // Ternary（三元）​：接受 3 个参数，如 condition ? a : b
    // Variadic（可变参数）​：参数数量不固定，如 Math.max(1, 2, 3)
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
    String toString();
}
