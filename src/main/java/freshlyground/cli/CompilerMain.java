package freshlyground.cli;

import freshlyground.compiler.backend.Generator;
import freshlyground.common.Token;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.Ast;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.semantic.Bindings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;


public class CompilerMain {
    public static void main(String[] args) throws IOException {
        Options opt = Options.parse(args);
        String src = Files.readString(opt.input, StandardCharsets.UTF_8);

        Lexer lexer = new Lexer(src);
        List<Token> tokens = lexer.lex();

        if (opt.dumpTokens) {
            System.out.println("Tokens:");
            tokens.forEach(System.out::println);
            System.out.println();
        }

        Ast ast = new Parser(tokens).parse();

        if (opt.dumpAst) {
            System.out.println("Undecorated AST:");
            System.out.println(ast);
            System.out.println();
        }

        Bindings bindings = new Analyzer().decorate(ast);

        if (opt.dumpDecorated) {
            System.out.println("Decorated AST:");
            System.out.println(ast);
            System.out.println();
        }

        String result = new Generator(bindings).emit(ast);

        Files.writeString(opt.output, result, StandardCharsets.UTF_8);
    }

    private static final class Options {
        final Path input;
        final Path output;
        final boolean dumpTokens;
        final boolean dumpAst;
        final boolean dumpDecorated;

        private Options(Path input,
                        Path output,
                        boolean dumpTokens,
                        boolean dumpAst,
                        boolean dumpDecorated) {
            this.input = input;
            this.output = output;
            this.dumpTokens = dumpTokens;
            this.dumpAst = dumpAst;
            this.dumpDecorated = dumpDecorated;
        }

        static Options parse(String[] args) {
            Path input = null;
            Path output = null;
            boolean dumpTokens = false;
            boolean dumpAst = false;
            boolean dumpDecorated = false;

            Deque<String> queue = new ArrayDeque<>(Arrays.asList(args));
            while (!queue.isEmpty()) {
                String arg = queue.peek();

                switch (arg) {
                    case "-i":
                        input = Path.of(queue.removeFirst());
                        break;
                    case "-o":
                        output = Path.of(queue.removeFirst());
                        break;
                    case "--dump-tokens":
                        queue.removeFirst();
                        dumpTokens = true;
                        break;
                    case "--dump-ast":
                        queue.removeFirst();
                        dumpAst = true;
                        break;
                    case "--dump-decorated":
                        queue.removeFirst();
                        dumpDecorated = true;
                        break;
                    default:
                        usage();
                        throw new IllegalArgumentException("Unknown option: " + arg);
                }
            }

            if (input == null) {
                throw new IllegalArgumentException("Input path is required");
            }

            if (!Files.exists(input)) {
                throw new IllegalArgumentException("Input path does not exist: " + input);
            }

            if (output == null) {
                throw new IllegalArgumentException("Output path is required");
            }

            return new Options(input, output, dumpTokens, dumpAst, dumpDecorated);
        }

        static String usage() {
            return String.join("\n",
                "Usage:",
                "  fgcc -i <input> -o <output> [options]",
                "",
                "Required:",
                "  -i <file>                 Input source file",
                "  -o <file>                 Output Java source file",
                "",
                "Options:",
                "  --dump-tokens             Print token stream",
                "  --dump-ast                Print AST before analysis",
                "  --dump-decorated          Print AST after analysis"
            );
        }
    }
}
