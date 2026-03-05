package freshlyground.api;

import freshlyground.compiler.backend.java.JavaGenerator;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.semantic.Bindings;

import java.util.List;
import java.util.Objects;

/**
 * Small in-process compiler API: source string in, generated code out.
 *
 * <p>This is intentionally minimal and backend-specific. When the project transitions
 * from Java emission to WAT emission, this implementation will be updated accordingly
 * while keeping the same call surface.</p>
 */
public final class CompilerService {
    private CompilerService() {}

    /** Compiles FreshlyGround source into Java source code. */
    public static String compile(String source) {
        Objects.requireNonNull(source, "source");

        // Lex
        List<Token> tokens = new Lexer(source).lex();

        // Parse
        Ast ast = new Parser(tokens).parse();

        // Analyze
        Bindings bindings = new Analyzer().decorate(ast);

        // Generate
        return new JavaGenerator(bindings).emit(ast);
    }
}
