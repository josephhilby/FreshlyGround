package freshlyground.cli;

import freshlyground.compiler.backend.java.Generator;
import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.semantic.Bindings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// To Run:
// ./build/install/PLC_Project/bin/fgc examples/src/<file>.fg examples/dist/Main.java
// javac examples/dist/Main.java
// cd examples/dist
// java Main

public final class CompilerMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: ./build/install/PLC_Project/bin/fgc <input.fg> <output.java>");
            System.exit(1);
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        String source = Files.readString(input, StandardCharsets.UTF_8);

        // Lex
        List<Token> tokens = new Lexer(source).lex();

        // Parse
        Ast ast = new Parser(tokens).parse();

        // Analyze
        Bindings bindings = new Analyzer().decorate(ast);

        // Generate
        String result = new Generator(bindings).emit(ast);

        // Write output
        Files.writeString(output, result, StandardCharsets.UTF_8);
    }
}