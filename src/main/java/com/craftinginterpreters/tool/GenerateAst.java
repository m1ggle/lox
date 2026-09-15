package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * 帮助生成每个类的定义，字段声明，构造函数和初始化函数
 */
public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        // 要生成类，它需要每种类型及其字段的某种描述
        // 运算
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign     : Token name, Expr value",                // 赋值
                "Binary     : Expr left, Token operator, Expr right", // 二元运算
                "Grouping   : Expr expression",                       // 括号
                "Call       : Expr callee, Token paren, List<Expr> arguments",
                "Literal    : Object value",                          // 字面量
                "Logical    : Expr left, Token operator, Expr right",
                "Unary      : Token operator, Expr right",            // 一元运算
                "Variable   : Token name"                           // 可声明
        ));

        // 声明和状态
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Break      : Token keyword",
                "Block      : List<Stmt> statements",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Expression : Expr expression",                     // 表达式
                "Function   : Token name, List<Token> params, List<Stmt> body",
                "Print      : Expr expression",                     // print
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",        // 变量声明
                "While      : Expr condition, Stmt body"            // while循环
        ));
    }

    // 首先要做的事情是输出 Expr 基类。
    // 当我们调用这个方法时，baseName 是“Expr”，这既是类的名称，
    // 也是它输出的文件名。我们将其作为参数传递，而不是硬编码名称，
    // 因为稍后我们将为语句添加单独的类族。
    private static void defineAst(String outputDir,
                                  String baseName,
                                  List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path,"UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);
        // 在基类中，我们定义每一个子类
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer,baseName, className, fields);
        }
        // the base accept() method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        writer.println("}");
        writer.println();
        writer.close();
    }

    private static void defineVisitor(
            PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("    }");
    }

    private static void defineType(PrintWriter writer,
                                   String baseName,
                                   String className,
                                   String fieldList) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        // 构造函数
        writer.println("    " + className + "(" + fieldList + ") {");

        // Store parameters in fields
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("    this." + name + " = " + name + ";");
        }
        writer.println("    }");

        // visitor pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("        return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        // fields
        writer.println();
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("    }");
    }
}
