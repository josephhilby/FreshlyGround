package freshlyground.compiler.integration;

import freshlyground.compiler.backend.java.Generator;

import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.semantic.Bindings;

import freshlyground.compiler.semantic.Types;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public class JavaInigrationTests {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, String input, String expected) {
        // code -> LEXER -> tokens
        List<Token> tokens = new Lexer(input).lex();

        // tokens -> PARSER -> ast
        Ast ast = new Parser(tokens).parse();

        // ast -> ANALYZER -> ast (decorated)
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineVariable("object", Types.ANY, false);
        Bindings bindings = analyzer.decorate(ast);

        test(expected, ast, bindings);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            // DEF main(): Integer DO
            //     print("Hello World");
            //     RETURN 0;
            // END
            Arguments.of("Hello World",
                String.join(System.lineSeparator(),
                    "DEF main(): Integer DO",
                    "    print(\"Hello, World!\");",
                    "    RETURN 0;",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "public class Main {",
                    "",
                    "    public static void main(String[] args) {",
                    "        System.exit(new Main().main());",
                    "    }",
                    "",
                    "    int main() {",
                    "        System.out.println(\"Hello, World!\");",
                    "        return 0;",
                    "    }",
                    "",
                    "}"
                )
            ),
            // DEF main(): Integer DO
            //     object.field = 1;
            //     object.method();
            //     RETURN 0;
            // END
            // TODO find new way to test object.field, object.method
//            Arguments.of("Direct Member Access",
//                String.join(System.lineSeparator(),
//                    "DEF main(): Integer DO",
//                    "    object.field = 1;",
//                    "    object.method();",
//                    "    RETURN 0;",
//                    "END"
//                ),
//                String.join(System.lineSeparator(),
//                    "public class Main {",
//                    "",
//                    "    public static void main(String[] args) {",
//                    "        System.exit(new Main().main());",
//                    "    }",
//                    "",
//                    "    int main() {",
//                    "        obj.fld = 1;",
//                    "        obj.method();",
//                    "        return 0;",
//                    "    }",
//                    "",
//                    "}"
//                )
//            ),
            // LET x: Integer;
            // LET y: Decimal;
            // LET z: String;
            // DEF f(): Integer DO
            //     RETURN x;
            // END
            // DEF g(): Decimal DO
            //     RETURN y;
            // END
            // DEF h(): String DO
            //     RETURN z;
            // END
            // DEF main(): Integer DO
            // END
            Arguments.of("Multiple Fields and Methods",
                String.join(System.lineSeparator(),
                    "LET x: Integer;",
                    "LET y: Decimal;",
                    "LET z: String;",
                    "DEF f(): Integer DO RETURN x; END",
                    "DEF g(): Decimal DO RETURN y; END",
                    "DEF h(): String DO RETURN z; END",
                    "DEF main(): Integer DO END"
                ),
                String.join(System.lineSeparator(),
                    "public class Main {",
                    "",
                    "    int x;",
                    "    double y;",
                    "    String z;",
                    "",
                    "    public static void main(String[] args) {",
                    "        System.exit(new Main().main());",
                    "    }",
                    "",
                    "    int f() {",
                    "        return x;",
                    "    }",
                    "",
                    "    double g() {",
                    "        return y;",
                    "    }",
                    "",
                    "    String h() {",
                    "        return z;",
                    "    }",
                    "",
                    "    int main() {}",
                    "",
                    "}"
                )
            )
        );
    }

    // LEXER - PARSER - ANALYZER - GENERATOR
    private static void test(String expected,
                             Ast ast,
                             Bindings bindings) {
        // ast (decorated) -> GENERATOR -> java
        String result = new Generator(bindings).emit(ast);

        Assertions.assertEquals(expected, result);
    }
}
