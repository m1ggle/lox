package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.EOF;


public class Lox {

	private static final Interpreter INTERPRETER = new Interpreter();
	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	// Lox 是一种脚本语言，这意味着它直接从源代码执行。
	// 我们的解释器支持两种运行代码的方式。
	// 如果您从命令行启动 jlox 并提供一个文件的路径，它会读取该文件并执行其中的代码。
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));

		// indicate an error in the exit code
		// if (hadError) System.exit(65);
		if (hadRuntimeError) {
			System.exit(70);
		}
	}

	// 如果您希望与解释器进行更亲密的对话，也可以以交互方式运行 jlox。
	// 在不带任何参数的情况下启动 jlox，它将让您进入一个提示符，您可以逐行输入和执行代码。
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		for (; ; ) {
			System.out.println("> ");
			String line = reader.readLine();
			if (line == null) {
				break;
			} else if (line.equalsIgnoreCase("exit;") || line.equalsIgnoreCase("quit;")) {
				System.exit(65);
			}
			run(line);
			hadError = false;
		}
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Stmt> stmts = parser.parse();
		// Expr expoession = parser.parser();
		if (hadError) {
			return;
		}
		// System.out.println(new AstPrinter().print(expoession));
		INTERPRETER.interpret(stmts);
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println(
				"[line " + line + "] Error" + where + " : " + message);
		hadError = true;
	}

	static void error(Token token, String message) {
		if (token.type == EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}


	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() +
				"\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}
}
