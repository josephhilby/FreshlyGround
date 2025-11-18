package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class EndToEndGeneratorTests {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, String source, String expected) {
        test(source, expected, Parser::parseSource);
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

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testField(String test, String source, String expected) {
        test(source, expected, Parser::parseField);
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
            // LET name: Integer;
            Arguments.of("Declaration",
                String.join(System.lineSeparator(),
                    "LET name: Integer;"
                ),
                String.join(System.lineSeparator(),
                    "int name;"
                )
            ),
            // LET name: Decimal = 1.0;
            Arguments.of("Initialization",
                String.join(System.lineSeparator(),
                    "LET name: Decimal = 1.0;"
                ),
                String.join(System.lineSeparator(),
                    "double name = 1.0;"
                )
            )
        );
    }


    // Method


        // Sum:
        // DEF main(): Integer DO
        //     LET i: Integer = 1;
        //     LET sum = 0;
        //     WHILE i < 50 DO
        //         sum = sum + i;
        //         i = i + 1;
        //     END
        //     print(sum);
        // END

        // Nested Statements:
        // DEF sumOdds(start: Integer, end: Integer): Integer DO
        //     LET sum: Integer = 0;
        //     LET num: Integer;
        //     FOR ( num = start; num <= end; num = num + 1 )
        //         IF (num / 2) * 2 != num DO
        //             sum = sum + num;
        //         END
        //     END
        //     RETURN sum;
        // END
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void testMethod(String test, String source, String expected) {
            test(source, expected, Parser::parseMethod);
        }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
            // DEF square(num: Decimal): Decimal DO
            //     RETURN num * num;
            // END
            Arguments.of("Square",
                String.join(System.lineSeparator(),
                    "DEF square(num: Decimal): Decimal DO",
                    "    RETURN num * num;",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "double square(double num) {",
                    "    return num * num;",
                    "}"
                )
            ),
            // Multiple Statements:
            // DEF func(x: Integer, y: Decimal, z: String) DO
            //     print(x);
            //     print(y);
            //     print(z);
            // END
            Arguments.of("Multiple Statements",
                String.join(System.lineSeparator(),
                    "DEF func(x: Integer, y: Decimal, z: String) DO",
                    "    print(x);",
                    "    print(y);",
                    "    print(z);",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "void func(int x, double y, String z) {",
                    "    System.out.println(x);",
                    "    System.out.println(y);",
                    "    System.out.println(z);",
                    "}"
                )
            )
        );
    }

    // Statement
        // Expression (1):
            // Print Expression: print("Hello World");
        // Declaration (2):
            // Variable Declaration: LET name: Integer;
            // Variable Initialization: LET name = 1.0;
        // Assignment (2):
            // Variable: variable = 1;
            // Field: object.field = 1;
        // If (2):
            // If:
                // IF cond DO
                //     print("cond is true.");
                // END
                // Else:
                // IF cond DO
                //     print("cond is true.");
                // ELSE
                //     print("cond is false.");
                // END
        // For (2):
            // For:
                // FOR (num = 0; num < 5; num = num + 1)
                //     sum = sum + num;
                // END
            //Condition Only:
                // FOR (; num < 5;)
                //     print(num);
                //     num = num + 1;
                // END
        //While (2):
            // Empty Statements: WHILE cond DO END
            // Multiple Statements:
                // WHILE num < 10 DO
                //     print(num + "\n");
                //     num = num + 1;
                // END

    // Expression
        // Literal (4):
            // Boolean: TRUE
            // Integer: 1
            // Decimal: 123.456
            // String: "Hello World"
        // Group (1):
            // Binary: (1 + 10)
            // Binary (4):
            // And: TRUE AND FALSE
            // Comparison: 1 > 10
            // Addition: 1 + 10
            // Concatenation: "Ben " + 10
        // Access (2):
            // Variable: variable
            // Field: object.field
        // Function (3):
            // Zero Arguments: function()
            // Print: print("Hello World")
            // String Slice: "string".slice(1, 5)

    private static <T extends Ast> void test(String input, String expected, Function<Parser, T> function) {
        // LEXER - PARSER - ANALYZER - GENERATOR
        // code -> LEXER -> tokens
        List<Token> tokens = new Lexer(input).lex();

        // tokens -> PARSER -> ast
        Parser parser = new Parser(tokens);
        Ast ast = function.apply(parser);

        // ast -> ANALYZER -> ast (decorated)
        Scope scope = new Scope(null);
        Analyzer analyzer = new Analyzer(scope);
        analyzer.visit(ast);

        // decoratedAST -> GENERATOR -> java
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        String result = writer.toString();

        Assertions.assertEquals(expected, result);
    }
}
