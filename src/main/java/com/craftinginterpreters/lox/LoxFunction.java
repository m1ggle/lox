package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration;
	private final Environment closure;

	public LoxFunction(Stmt.Function declaration, Environment closure) {
		this.declaration = declaration;
		this.closure = closure;
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter,
					   List<Object> arguments) {
		// Environment environment = new Environment(interpreter.globals);
		Environment environment = new Environment(closure);
		for (int i = 0; i < declaration.params.size(); i++) {
			environment.define(declaration.params.get(i).lexeme,
					arguments.get(i));
		}
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			return returnValue.value;
		}

		// 这里为啥返回null？
		// lox 语言是动态语言，所以不存在 void，编译器无法阻止你调用不包含return语句的函数的返回值
		// 所以lox中每个函数都需要返回一些内容，即使他不包含任何return语句
		// 我们用nil表示这种情况
		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
