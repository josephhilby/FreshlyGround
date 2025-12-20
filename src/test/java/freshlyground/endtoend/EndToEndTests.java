package freshlyground.endtoend;

import freshlyground.compiler.backend.Generator;
import freshlyground.common.Token;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.Ast;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Scope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

// TODO check against slide deck
// https://mediasite.video.ufl.edu/Mediasite/Play/c6c51d03742b43578043069ce6cb934b1d

public class EndToEndTests {
    private static final Environment.Type OBJECT_TYPE =
        new Environment.Type(
            "ObjectType",
            "ObjectType",
            init(new Scope(null), scope -> {
                scope.defineVariable("field", "field", Environment.Type.INTEGER, false);
                scope.defineFunction(
                    "method",
                    "method",
                    Arrays.asList(Environment.Type.ANY),
                    Environment.Type.INTEGER);
            })
        );

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("object", "object", OBJECT_TYPE, false);
        test(source, expected, Parser::parseSource, scope);
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
            // Note: 'object' of 'ObjectType' is stubbed in test setup,
            //       language currently has no object or struct functionality.
            // DEF main(): Integer DO
            //     object.field = 1;
            //     object.method();
            //     RETURN 0;
            // END
            Arguments.of("Direct Member Access",
                String.join(System.lineSeparator(),
                    "DEF main(): Integer DO",
                    "    object.field = 1;",
                    "    object.method();",
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
                    "        object.field = 1;",
                    "        object.method();",
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
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseField, scope);
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


    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, String source, String expected) {
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseMethod, scope);
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
                    "Void func(int x, double y, String z) {",
                    "    System.out.println(x);",
                    "    System.out.println(y);",
                    "    System.out.println(z);",
                    "}"
                )
            ),
            // DEF main(): Integer DO
            //     LET i: Integer = 1;
            //     LET sum = 0;
            //     WHILE i < 50 DO
            //         sum = sum + i;
            //         i = i + 1;
            //     END
            //     print(sum);
            // END
            Arguments.of("Sum",
                String.join(System.lineSeparator(),
                    "DEF main(): Integer DO",
                    "    LET i: Integer = 1;",
                    "    LET sum = 0;",
                    "    WHILE i < 50 DO",
                    "        sum = sum + i;",
                    "        i = i + 1;",
                    "    END",
                    "    print(sum);",
                    "END"
                ),
                String.join(System.lineSeparator(),
                "int main() {",
                    "    int i = 1;",
                    "    int sum = 0;",
                    "    while (i < 50) {",
                    "        sum = sum + i;",
                    "        i = i + 1;",
                    "    }",
                    "    System.out.println(sum);",
                    "}"
                )
            ),
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
            Arguments.of("Nested Statements",
                String.join(System.lineSeparator(),
                    "DEF sumOdds(start: Integer, end: Integer): Integer DO",
                    "    LET sum: Integer = 0;",
                    "    LET num: Integer;",
                    "    FOR ( num = start; num <= end; num = num + 1 )",
                    "        IF (num / 2) * 2 != num DO",
                    "            sum = sum + num;",
                    "        END",
                    "    END",
                    "    RETURN sum;",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "int sumOdds(int start, int end) {",
                    "    int sum = 0;",
                    "    int num;",
                    "    for ( num = start; num <= end; num = num + 1 ) {",
                    "        if ((num / 2) * 2 != num) {",
                    "            sum = sum + num;",
                    "        }",
                    "    }",
                    "    return sum;",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExpression(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", "num", Environment.Type.INTEGER, false);
        test(source, expected, Parser::parseStatement, scope);
    }

    private static Stream<Arguments> testExpression() {
        return Stream.of(
            // print("Hello World");
            Arguments.of("Simple Print Expression",
                String.join(System.lineSeparator(),
                    "print(\"Hello World\");"
                ),
                String.join(System.lineSeparator(),
                    "System.out.println(\"Hello World\");"
                )
            ),
            // print(num);
            Arguments.of("Simple Variable Print",
                String.join(System.lineSeparator(),
                    "print(num);"
                ),
                String.join(System.lineSeparator(),
                    "System.out.println(num);"
                )
            ),
            // print("\n");
            Arguments.of("Simple Escape Char Print",
                String.join(System.lineSeparator(),
                    "print(\"\\\\n\");"
                ),
                String.join(System.lineSeparator(),
                    "System.out.println(\"\\n\");"
                )
            ),
            // print("Hello" + " World");
            Arguments.of("Concat Print Expression",
                String.join(System.lineSeparator(),
                    "print(\"Hello\" + \" World\");"
                ),
                String.join(System.lineSeparator(),
                    "System.out.println(\"Hello\" + \" World\");"
                )
            ),
            // print("Hello" + " " + "World" + "\n" + num);
            Arguments.of("Multiple Concat Print Expression",
                String.join(System.lineSeparator(),
                    "print(\"Hello\" + \" \" + \"World\" + \"\\\\n\" + num);"
                ),
                String.join(System.lineSeparator(),
                    "System.out.println(\"Hello\" + \" \" + \"World\" + \"\\n\" + num);"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclaration(String test, String source, String expected) {
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseStatement, scope);
    }

    private static Stream<Arguments> testDeclaration() {
        return Stream.of(
            // LET name: Integer;
            Arguments.of("Variable Declaration",
                String.join(System.lineSeparator(),
                    "LET name: Integer;"
                ),
                String.join(System.lineSeparator(),
                    "int name;"
                )
            ),
            // LET name = 1.0;
            Arguments.of("Variable Initialization",
                String.join(System.lineSeparator(),
                    "LET name = 1.0;"
                ),
                String.join(System.lineSeparator(),
                    "double name = 1.0;"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignment(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", "variable", Environment.Type.INTEGER, false);
        scope.defineVariable("object", "object", OBJECT_TYPE, false);
        test(source, expected, Parser::parseStatement, scope);
    }

    private static Stream<Arguments> testAssignment() {
        return Stream.of(
            // variable = 1;
            Arguments.of("Variable",
                String.join(System.lineSeparator(),
                    "variable = 1;"
                ),
                String.join(System.lineSeparator(),
                    "variable = 1;"
                )
            ),
            // object.field = 1;
            Arguments.of("Field",
                String.join(System.lineSeparator(),
                    "object.field = 1;"
                ),
                String.join(System.lineSeparator(),
                    "object.field = 1;"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIf(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("cond", "cond", Environment.Type.BOOLEAN, false);
        test(source, expected, Parser::parseStatement, scope);
    }

    private static Stream<Arguments> testIf() {
        return Stream.of(
            // IF cond DO
            //     print("cond is true.");
            // END
            Arguments.of("If",
                String.join(System.lineSeparator(),
                    "IF cond DO",
                    "    print(\"cond is true.\");",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "if (cond) {",
                    "    System.out.println(\"cond is true.\");",
                    "}"
                )
            ),
            // IF cond DO
            //     print("cond is true.");
            // ELSE
            //     print("cond is false.");
            // END
            Arguments.of("Else",
                String.join(System.lineSeparator(),
                    "IF cond DO",
                    "    print(\"cond is true.\");",
                    "ELSE",
                    "    print(\"cond is false.\");",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "if (cond) {",
                    "    System.out.println(\"cond is true.\");",
                    "} else {",
                    "    System.out.println(\"cond is false.\");",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFor(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", "num", Environment.Type.INTEGER, false);
        scope.defineVariable("sum", "sum", Environment.Type.INTEGER, false);
        test(source, expected, Parser::parseStatement, scope);
    }

    private static Stream<Arguments> testFor() {
        return Stream.of(
            // FOR (num = 0; num < 5; num = num + 1)
            //     sum = sum + num;
            // END
            Arguments.of("For",
                String.join(System.lineSeparator(),
                    "FOR (num = 0; num < 5; num = num + 1)",
                    "    sum = sum + num;",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "for ( num = 0; num < 5; num = num + 1 ) {",
                        "    sum = sum + num;",
                        "}"
                )
            ),
            // FOR (; num < 5;)
            //     print(num);
            //     num = num + 1;
            // END
            Arguments.of("Initialization",
                String.join(System.lineSeparator(),
                    "FOR (; num < 5;)",
                    "    print(num);",
                    "    num = num + 1;",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "for ( ; num < 5; ) {",
                        "    System.out.println(num);",
                        "    num = num + 1;",
                        "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhile(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", "num", Environment.Type.INTEGER, false);
        scope.defineVariable("cond", "cond", Environment.Type.BOOLEAN, false);
        test(source, expected, Parser::parseStatement, scope);
    }

    private static Stream<Arguments> testWhile() {
        return Stream.of(
            // WHILE cond DO END
            Arguments.of("Empty Statements",
                String.join(System.lineSeparator(),
                    "WHILE cond DO END"
                ),
                String.join(System.lineSeparator(),
                    "while (cond) {}"
                )
            ),
            // WHILE num < 10 DO
            //     print(num + "\n");
            //     num = num + 1;
            // END
            Arguments.of("Multiple Statements",
                String.join(System.lineSeparator(),
                    "WHILE num < 10 DO",
                    "    print(num + \"\\\\n\");",
                    "    num = num + 1;",
                    "END"
                ),
                String.join(System.lineSeparator(),
                    "while (num < 10) {",
                        "    System.out.println(num + \"\\n\");",
                        "    num = num + 1;",
                        "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteral(String test, String source, String expected) {
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseExpression, scope);
    }

    private static Stream<Arguments> testLiteral() {
        return Stream.of(
            // TRUE
            Arguments.of("Boolean",
                String.join(System.lineSeparator(),
                    "TRUE"
                ),
                String.join(System.lineSeparator(),
                    "true"
                )
            ),
            // 1
            Arguments.of("Integer",
                String.join(System.lineSeparator(),
                    "1"
                ),
                String.join(System.lineSeparator(),
                    "1"
                )
            ),
            // 123.456
            Arguments.of("Decimal",
                String.join(System.lineSeparator(),
                    "123.456"
                ),
                String.join(System.lineSeparator(),
                    "123.456"
                )
            ),
            // "Hello World"
            Arguments.of("String",
                String.join(System.lineSeparator(),
                    "\"Hello World\""
                ),
                String.join(System.lineSeparator(),
                    "\"Hello World\""
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroup(String test, String source, String expected) {
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseExpression, scope);
    }

    private static Stream<Arguments> testGroup() {
        return Stream.of(
            // (1 + 10)
            Arguments.of("Binary",
                String.join(System.lineSeparator(),
                    "(1 + 10)"
                ),
                String.join(System.lineSeparator(),
                    "(1 + 10)"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinary(String test, String source, String expected) {
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseExpression, scope);
    }

    private static Stream<Arguments> testBinary() {
        return Stream.of(
            // TRUE AND FALSE
            Arguments.of("And",
                String.join(System.lineSeparator(),
                    "TRUE AND FALSE"
                ),
                String.join(System.lineSeparator(),
                    "true && false"
                )
            ),
            // 1 > 10
            Arguments.of("Comparison",
                String.join(System.lineSeparator(),
                    "1 > 10"
                ),
                String.join(System.lineSeparator(),
                    "1 > 10"
                )
            ),
            // 1 + 10
            Arguments.of("Addition",
                String.join(System.lineSeparator(),
                    "1 + 10"
                ),
                String.join(System.lineSeparator(),
                    "1 + 10"
                )
            ),
            // "Ben " + 10
            Arguments.of("Concatenation",
                String.join(System.lineSeparator(),
                    "\"Ben \" + 10"
                ),
                String.join(System.lineSeparator(),
                    "\"Ben \" + 10"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccess(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", "variable", Environment.Type.INTEGER, false);
        scope.defineVariable("object", "object", OBJECT_TYPE, false);
        test(source, expected, Parser::parseExpression, scope);
    }

    private static Stream<Arguments> testAccess() {
        return Stream.of(
            // variable
            Arguments.of("Variable",
                String.join(System.lineSeparator(),
                    "variable"
                ),
                String.join(System.lineSeparator(),
                    "variable"
                )
            ),
            // object.field
            Arguments.of("Field",
                String.join(System.lineSeparator(),
                    "object.field"
                ),
                String.join(System.lineSeparator(),
                    "object.field"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunction(String test, String source, String expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", "func", Arrays.asList(), Environment.Type.INTEGER);
        test(source, expected, Parser::parseExpression, scope);
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
            // function()
            Arguments.of("Zero Arguments",
                String.join(System.lineSeparator(),
                    "function()"
                ),
                String.join(System.lineSeparator(),
                    "func()"
                )
            ),
            // print("Hello World")
            Arguments.of("Print",
                String.join(System.lineSeparator(),
                    "print(\"Hello World\")"
                ),
                String.join(System.lineSeparator(),
                    "System.out.println(\"Hello World\")"
                )
            ),
            // "string".slice(1, 5)
            Arguments.of("String Slice",
                String.join(System.lineSeparator(),
                    "\"string\".slice(1, 5)"
                ),
                String.join(System.lineSeparator(),
                    "\"string\".substring(1, 5)"
                )
            )
        );
    }

    // LEXER - PARSER - ANALYZER - GENERATOR
    private static <T extends Ast> void test(String input,
                                             String expected,
                                             Function<Parser, T> function,
                                             Scope scope) {
        // code -> LEXER -> tokens
        List<Token> tokens = new Lexer(input).lex();

        // tokens -> PARSER -> ast
        Parser parser = new Parser(tokens);
        Ast ast = function.apply(parser);

        // ast -> ANALYZER -> ast (decorated)
        Analyzer analyzer = new Analyzer(scope, true);
        analyzer.visit(ast);

        // ast (decorated) -> GENERATOR -> java
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        String result = writer.toString();

        Assertions.assertEquals(expected, result);
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }
}
