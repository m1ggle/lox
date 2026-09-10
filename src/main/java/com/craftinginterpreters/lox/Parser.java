package com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

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
class Parser {
    private static class ParserError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parser() {
        try {
            return expression();
        } catch (ParserError error) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
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
