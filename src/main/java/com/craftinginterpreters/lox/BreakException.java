package com.craftinginterpreters.lox;

/**
 * break 语句专用控制流异常。
 * 不继承 RuntimeError，避免被 interpret() 顶层 catch 误捕获。
 * super(null, null, false, false) 禁用栈追踪，抛出时无性能开销。
 */
public class BreakException extends  RuntimeException{
    BreakException() {
        super(null,null,false,false);
    }
}
