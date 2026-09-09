package com.craftinginterpreters.lox;

public class Token {
    final String lexeme;
    final Object literal;
    final int line;
    final TokenType type;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
