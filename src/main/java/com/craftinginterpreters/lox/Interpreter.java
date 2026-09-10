package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {

    // The Interpreter’s public API is simply one method.
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));

        }catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator,left,right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator,left,right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator,left,right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator,left,right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator,left,right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator,left,right);
                // right can not be 0
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator,left,right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                if (left instanceof String && right instanceof Double) {
                    return (String) left + right.toString();
                }

                if (left instanceof Double && right instanceof String) {
                    return left.toString() + (String) right;
                }

                throw new RuntimeError(expr.operator,
                        "Operands must be tow number or two strings.");
        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        // 表达式树的叶子节点——所有其他表达式都由其构成的最小语法单元——是字面量。
        // 字面量几乎已经是值了，但这种区分很重要。字面量是一种能够生成值的语法片段。
        // 字面量始终会出现在用户的源代码中。许多值由计算产生，而并不直接存在于代码本身中，
        // 这些就不是字面量。字面量来自解析器的领域，而值则是解释器概念的一部分，
        // 属于运行时世界的范畴。
        // 在扫描时急切地生成了运行时值，并将其存储在标记中。
        // 解析器取这个值并将其放入字面量树节点中，因此要计算字面量，我们只需将其取出即可
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;

        }
        return null;

    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // Lox 遵循 Ruby 的简单规则：
    // `false` 和 `nil` 是假值，
    // 其他所有值都是真值。我们这样来实现
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.startsWith(".0")){
                text = text.substring(0,text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}
