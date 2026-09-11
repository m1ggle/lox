package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

// 将变量与值关联起来的绑定需要存住在某个地方
// 自从Lisp的发明者发明了括号
// 这种数据结构一致被称为环境，
// 你可以把她想想为一个映射，其中键是标量，值是变量的
public class Environment {

    /**
     * 其中有一个 Java Map 用于存储绑定关系。它直接使用字符串作为键，
     * 而不是 token。Token 表示源代码文本中特定位置的代码单元，
     * 但在查找变量时，相同名称的所有标识符 token 都应引用同一个变量（暂时忽略作用域）。
     * 使用原始字符串可以确保所有这些 token 都引用到同一个 map 键
     */
    private final Map<String, Object> values = new HashMap<>();

    // 支持作用域
    final Environment enclosing;

    // 无参构造函数用于全局作用域的环境，它标志着链的终点
    Environment() {
        enclosing = null;
    }

    // 新的本地作用域，并将其嵌套在给定的外层环境中
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "undefined variable '" + name.lexeme + "'");
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }
}
