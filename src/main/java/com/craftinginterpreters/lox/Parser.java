package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LoggingPermission;

import static com.craftinginterpreters.lox.TokenType.*;


class Parser {
    private static class ParserError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;
    //  添加循环深度字段，静态检查 break 必须在循环内
    private int loopDepth = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * expression     → equality ;
     * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
     * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     * term           → factor ( ( "-" | "+" ) factor )* ;
     * factor         → unary ( ( "/" | "*" ) unary )* ;
     * unary          → ( "!" | "-" ) unary
     *                | primary ;
     * primary        → NUMBER | STRING | "true" | "false" | "nil"
     *                | "(" expression ")" ;
     */
    Expr parser() {
        try {
            return expression();
        } catch (ParserError error) {
            return null;
        }
    }

    /**
     * program        → statement* EOF ;
     *
     * statement      → exprStmt
     *                | printStmt ;
     *
     * exprStmt       → expression ";" ;
     * printStmt      → "print" expression ";" ;
     */
    List<Stmt> parse() {
        List<Stmt> stmts = new ArrayList<>();
        while (!isAtEnd()) {
            stmts.add(declaration());
        }
        return stmts;

    }

    /**
     * 在解析块或脚本中的一系列语句时反复调用的，
     * 因此在解析器进入恐慌模式时同步是正确的地方。
     * 整个方法的代码被包裹在一个 try 块中，
     * 以捕获解析器开始错误恢复时抛出的异常。
     * 这样就能返回到尝试解析下一条语句或声明
     */
    private Stmt declaration() {
        try {
            // 首先，它检查我们是否处于变量声明中，方法是查找开头的 var 关键字。
            // 如果不是，它会继续到现有的 statement() 方法，该方法用于解析打印和表达式语句。
            if(match(VAR)) return varDeclaration();
            return statement();
        } catch (ParserError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement(){
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BREAK)) return breakStatement();
        if (match(LEFT_BRACE))  return  new Stmt.Block(block());
        return expressionStatement();
    }

    /**
     * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
     *                  expression? ";"
     *                  expression? ")" statement ;
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expr '(' after 'for'.");

        Stmt initializer; // ( varDecl | exprStmt | ";" )
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // expression? ";"
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' from loop condition.");

        // expression? ")" statement ;
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        loopDepth++;
        Stmt body = statement();
        loopDepth--;

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)
                    )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer,body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expr '(' after 'if'.");
        Expr condition = expression();

        consume(RIGHT_PAREN, "Expr ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);

    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * 我们创建一个空列表，然后解析语句并将其添加到列表中，
     * 直到遇到块的结束标记 `}`。注意循环还显式检查了 `isAtEnd()`。
     * 我们必须小心避免陷入无限循环，即使是在解析无效代码时。
     * 如果用户忘记闭合 `}`，解析器不应该卡住
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON,"Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        loopDepth++;
        Stmt body = statement();
        loopDepth--;
        return new Stmt.While(condition, body);
    }

    private Stmt breakStatement() {
        Token keywrod = previous();
        if (loopDepth == 0) {
            throw error(keywrod, "Break must be inside a loop.");
        }
        consume(SEMICOLON, "Expect ';' after break.");
        return new Stmt.Break(keywrod);

    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Expr expression() {
        //return equality();
        return assignment();
    }

    // 创建赋值表达式节点之前检查左侧表达式的类型，
    // 并确定它是一个什么样的赋值目标。
    // 我们将 r-值表达式节点转换为 l-值表示形式
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr roght = equality();
            expr = new Expr.Logical(expr, operator, roght);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL,EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator,right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER,GREATER_EQUAL,LESS,LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS,PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH,STAR)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message){
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParserError error(Token token, String message){
        Lox.error(token, message);
        return new ParserError();
    }

    // 在使用递归下降时，解析器的状态---他正在识别的规则---并未显示存储在字段
    // 只是利用了Java自身的调用堆栈来跟踪解析器正在做什么，
    // 每个正在解析的规则都是堆栈上的一个调用帧，为了重置该状态，我们需要清楚这个调用帧

    // 在 Java 中自然实现这一点的方式是异常。当我们想要同步时，
    // 抛出 ParseError 对象。在我们正同步的语法规则的方法上层，
    // 我们将捕获它。由于我们在语句边界处同步，我们将在那里捕获异常。
    // 捕获异常后，解析器处于正确的状态。剩下的只是同步令牌。

    // 我们希望丢弃令牌，直到正好到达下一条语句的开头。
    // 这个边界很容易识别——这就是我们选择它的主要原因之一。
    // 分号之后，很可能已完成一个语句。大多数语句以关键字开头——for、if、return、var 等。
    // 当下一个令牌是这些关键字之一时，我们可能即将开始一个新语句。
    private  void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type){
        if (isAtEnd()) return false;
        return peek().type == type;

    }

    // consume the current token and return it
    private Token advance() {
        if(!isAtEnd()) current++;
        return previous();
    }

    // check if run out of tokens to parser
    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    // return the current token have yet to consume
    private Token peek(){
        return tokens.get(current);
    }

    // return the most recently consumed token
    private Token previous() {
        return tokens.get(current - 1);
    }
}
