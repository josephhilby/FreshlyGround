package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GeneratorTests {

    private static final Environment.Type OBJECT_TYPE = new Environment.Type("ObjectType", "ObjectType", init(new Scope(null), scope -> {
        scope.defineVariable("field", "field", Environment.Type.INTEGER, false, Environment.NIL);
        scope.defineFunction("method", "method", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER, args -> Environment.NIL);
    }));

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
        Environment.Function _main = new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL);
        Environment.Variable _x = new Environment.Variable("x", "x", Environment.Type.INTEGER, false, Environment.NIL);
        Environment.Variable _y = new Environment.Variable("y", "y", Environment.Type.INTEGER, false, Environment.NIL);

        return Stream.of(
            Arguments.of("Hello, World!",
                // DEF main(): Integer DO
                //     print("Hello, World!");
                //     RETURN 0;
                // END
                new Ast.Source(
                    Arrays.asList(),

                    Arrays.asList(
                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Expression(
                                    init(new Ast.Expression.Function(
                                        Optional.empty(),
                                        "print",
                                        Arrays.asList(init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING)))),
                                        ast -> ast.setFunction(_print)
                                    )
                                ),
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER))
                                )
                            )
                        ), ast -> ast.setFunction(_main))
                    )
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
            Arguments.of("Field and Method",
                // LET x: Integer;
                // LET y: Integer = 10;
                // DEF main(): Integer DO
                //     RETURN x + y;
                // END
                new Ast.Source(
                    Arrays.asList(
                        init(new Ast.Field("x", "Integer", false, Optional.empty()), ast -> ast.setVariable(_x)),
                        init(new Ast.Field("y", "Integer", false, Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.TEN),ast -> ast.setType(Environment.Type.INTEGER)))
                            ),
                            ast -> ast.setVariable(_y)
                        )
                    ),

                    Arrays.asList(
                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Binary("+",
                                        init(new Ast.Expression.Access(Optional.empty(), "x"), ast -> ast.setVariable(_x)),
                                        init(new Ast.Expression.Access(Optional.empty(),"y"), ast -> ast.setVariable(_y))),
                                        ast -> ast.setType(Environment.Type.INTEGER)
                                    )
                                )
                            )
                        ), ast -> ast.setFunction(_main))
                    )
                ),
                String.join(System.lineSeparator(),
                    "public class Main {",
                    "",
                    "    int x;",
                    "    int y = 10;",
                    "",
                    "    public static void main(String[] args) {",
                    "        System.exit(new Main().main());",
                    "    }",
                    "",
                    "    int main() {",
                    "        return x + y;",
                    "    }",
                    "",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFieldExpression(String test, Ast.Field ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFieldExpression() {
        Environment.Variable _x = new Environment.Variable("x", "x", Environment.Type.STRING, false, Environment.NIL);
        Environment.Variable _y = new Environment.Variable("y", "y", Environment.Type.BOOLEAN, false, Environment.NIL);

        return Stream.of(
            Arguments.of("Field Non-Const",
                // LET x: String;
                init(new Ast.Field("x", "String", false, Optional.empty()),
                    ast -> ast.setVariable(_x)
                ),
                "String x;"
            ),
            Arguments.of("Field Const",
                // LET CONST y: Boolean = TRUE AND FALSE;
                init(new Ast.Field("y", "Boolean", true, Optional.of(
                    init(new Ast.Expression.Binary("AND",
                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))),
                        ast -> ast.setType(Environment.Type.BOOLEAN)
                    ))
                ),ast -> ast.setVariable(_y)),
                "final boolean y = true && false;"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethodExpression(String test, Ast.Method ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testMethodExpression() {
        Environment.Function _area = new Environment.Function("area", "area", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.DECIMAL, args -> Environment.NIL);
        Environment.Variable _radius = new Environment.Variable("radius", "radius", Environment.Type.DECIMAL, false, Environment.NIL);

        return Stream.of(
            Arguments.of("Method",
                // DEF area(radius: Decimal): Decimal DO
                //     RETURN 3.14 * radius * radius
                // END
                init(new Ast.Method(
                    "area",
                    Arrays.asList("radius"),
                    Arrays.asList("Decimal"),
                    Optional.of("Decimal"),
                    Arrays.asList(
                        new Ast.Statement.Return(
                            init(new Ast.Expression.Binary("*",
                                init(new Ast.Expression.Binary("*",
                                    init(new Ast.Expression.Literal(BigDecimal.valueOf(3.14)), ast -> ast.setType(Environment.Type.DECIMAL)),
                                    init(new Ast.Expression.Access(Optional.empty(), "radius"), ast -> ast.setVariable(_radius))
                                ), ast -> ast.setType(Environment.Type.DECIMAL)),

                                init(new Ast.Expression.Access(Optional.empty(), "radius"), ast -> ast.setVariable(_radius))
                            ), ast -> ast.setType(Environment.Type.DECIMAL))
                        )
                    )), ast -> ast.setFunction(_area)
                ),
                String.join(System.lineSeparator(),
                    "double area(double radius) {",
                    "    return 3.14 * radius * radius;",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExpressionStatement(String test, Ast.Statement.Expression ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testExpressionStatement() {
        Environment.Function _log = new Environment.Function("log", "log", Arrays.asList(Environment.Type.STRING), Environment.Type.NIL, args -> Environment.NIL);

        return Stream.of(
            Arguments.of("Expression",
                // log("Hello World");
                new Ast.Statement.Expression(
                    init(new Ast.Expression.Function(Optional.empty(),"log", Arrays.asList(
                        init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING))
                    )), ast -> ast.setFunction(_log))
                ),
                "log(\"Hello World\");"
            ),
            Arguments.of("Initialization",
                // 1;
                new Ast.Statement.Expression(init(new Ast.Expression.Literal(new BigDecimal("1")),ast -> ast.setType(Environment.Type.INTEGER))),
                "1;"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        Environment.Variable _name = new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL);
        Environment.Variable _name2 = new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL);

        return Stream.of(
            Arguments.of("Declaration",
                // LET name: Integer;
                init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()
                ), ast -> ast.setVariable(_name)),
                "int name;"
            ),
            Arguments.of("Initialization",
                // LET name = 1.0;
                init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                    init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                )), ast -> ast.setVariable(_name2)),
                "double name = 1.0;"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatement(String test, Ast.Statement.Assignment ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        Environment.Variable _variable = new Environment.Variable("variable", "variable", Environment.Type.STRING, false, Environment.NIL);

        return Stream.of(
            Arguments.of("Assignment",
                // variable = "Hello World";
                new Ast.Statement.Assignment(
                    init(new Ast.Expression.Access(Optional.empty(), "variable"),ast -> ast.setVariable(_variable)),
                    init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING))
                ),
                "variable = \"Hello World\";"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        Environment.Variable _expr = new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL);
        Environment.Variable _stmt = new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true, Environment.NIL);
        Environment.Variable _stmt2 = new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL);

        return Stream.of(
            Arguments.of("If",
                    // IF expr DO
                    //     stmt;
                    // END
                    new Ast.Statement.If(
                        init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(_expr)),
                        Arrays.asList(
                            new Ast.Statement.Expression(
                                init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(_stmt))
                            )
                        ),
                        Arrays.asList()
                    ),
                    String.join(System.lineSeparator(),
                        "if (expr) {",
                        "    stmt;",
                        "}"
                    )
            ),
            Arguments.of("Else",
                // IF expr DO
                //     stmt;
                // ELSE
                //     stmt2;
                // END
                new Ast.Statement.If(
                    init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(_expr)),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(_stmt)))
                    ),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(_stmt2)))
                    )
                ),
                String.join(System.lineSeparator(),
                    "if (expr) {",
                    "    stmt;",
                    "} else {",
                    "    stmt2;",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatement(String test, Ast.Statement.For ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testForStatement() {
        Environment.Variable _num = new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL);
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);

        return Stream.of(
            Arguments.of("For",
                // FOR (num = 0; num < 5; num = num + 1)
                //     print(num);
                // END
                new Ast.Statement.For(
                    new Ast.Statement.Assignment(
                        init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                        init(new Ast.Expression.Literal(BigInteger.valueOf(0)),ast -> ast.setType(Environment.Type.INTEGER))
                    ),

                    init(new Ast.Expression.Binary("<",
                        init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)),ast -> ast.setType(Environment.Type.INTEGER))
                    ),ast -> ast.setType(Environment.Type.BOOLEAN)),

                    new Ast.Statement.Assignment(
                        init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                        init(new Ast.Expression.Binary("+",
                            init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                            init(new Ast.Expression.Literal(BigInteger.valueOf(1)),ast -> ast.setType(Environment.Type.INTEGER))
                        ),ast -> ast.setType(Environment.Type.INTEGER))),

                    Arrays.asList(
                        new Ast.Statement.Expression(
                            init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num))
                            )), ast -> ast.setFunction(_print))
                        )
                    )
                ),
                String.join(System.lineSeparator(),
                    "for ( num = 0; num < 5; num = num + 1 ) {",
                    "    System.out.println(num);",
                    "}"
                )
            ),
            Arguments.of("Missing Signature",
                // FOR (; num < 5;)
                //     print(num);
                // END
                new Ast.Statement.For(
                    null,

                    init(new Ast.Expression.Binary("<",
                        init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)),ast -> ast.setType(Environment.Type.INTEGER))
                    ),ast -> ast.setType(Environment.Type.BOOLEAN)),

                    null,

                    Arrays.asList(
                        new Ast.Statement.Expression(
                            init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)))
                            ),ast -> ast.setFunction(_print))),

                        new Ast.Statement.Assignment(
                            init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                            init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Access(Optional.empty(), "num"),ast -> ast.setVariable(_num)),
                                init(new Ast.Expression.Literal(BigInteger.valueOf(1)),ast -> ast.setType(Environment.Type.INTEGER))
                            ),ast -> ast.setType(Environment.Type.INTEGER))
                        )
                    )
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
    void testWhileStatement(String test, Ast.Statement.While ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testWhileStatement() {
        Environment.Variable _condition = new Environment.Variable("condition", "condition", Environment.Type.BOOLEAN, false, Environment.NIL);
        Environment.Variable _stmt1 = new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true, Environment.NIL);
        Environment.Variable _stmt2 = new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL);

        return Stream.of(
            Arguments.of("While",
                // FOR (num = 0; num < 5; num = num + 1)
                //     print(num);
                // END
                new Ast.Statement.While(
                    init(new Ast.Expression.Access(Optional.empty(), "condition"), ast -> ast.setVariable(_condition)),

                    Arrays.asList(
                        new Ast.Statement.Expression(
                            init(new Ast.Expression.Access(Optional.empty(), "stmt1"),ast -> ast.setVariable(_stmt1))
                        ),

                        new Ast.Statement.Expression(
                            init(new Ast.Expression.Access(Optional.empty(), "stmt2"),ast -> ast.setVariable(_stmt2))
                        )
                    )
                ),
                String.join(System.lineSeparator(),
                    "while (condition) {",
                    "    stmt1;",
                    "    stmt2;",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testReturnExpression(String test, Ast.Statement.Return ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testReturnExpression() {
        return Stream.of(
            // RETURN 5 * 10;
            Arguments.of("Return",
                new Ast.Statement.Return(
                    init(new Ast.Expression.Binary("*",
                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)), ast -> ast.setType(Environment.Type.INTEGER)),
                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                    ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                "return 5 * 10;"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteralExpression(String test, Ast.Expression.Literal ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
            // TRUE
            Arguments.of("Boolean",
                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                "true"
            ),
            // 1
            Arguments.of("Integer",
                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                "1"
            ),
            // "Hello World"
            Arguments.of("String",
                init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING)),
                "\"Hello World\""
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpression(String test, Ast.Expression.Group ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
            // (1)
            Arguments.of("Group Literal",
                init(new Ast.Expression.Group(
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                    ), ast -> ast.setType(Environment.Type.INTEGER)),
                "(1)"
            ),
            // (1 + 10)
            Arguments.of("Group Binary",
                init(new Ast.Expression.Group(
                    init(new Ast.Expression.Binary("+",
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                    ), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.INTEGER)),
                "(1 + 10)"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
            Arguments.of("And",
                // TRUE AND FALSE
                init(new Ast.Expression.Binary("AND",
                    init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                    init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                "true && false"
            ),
            Arguments.of("Concatenation",
                // "Ben" + 10
                init(new Ast.Expression.Binary("+",
                    init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                    init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.STRING)),
                "\"Ben\" + 10"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccessExpression(String test, Ast.Expression.Access ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAccessExpression() {
        Environment.Variable _variable = new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL);
        Environment.Variable _object = new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        Environment.Variable _field = new Environment.Variable("field", "field", Environment.Type.INTEGER, false, Environment.NIL);

        return Stream.of(
            Arguments.of("Variable",
                // variable
                init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(_variable)),
                "variable"
            ),
            Arguments.of("Field",
                // object.field
                init(
                    new Ast.Expression.Access(Optional.of(
                        init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(_object))
                    ), "field"), ast -> ast.setVariable(_field)
                ),
                "object.field"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);

        return Stream.of(
            Arguments.of("Print",
                // print("Hello, World!")
                init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                    init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING)))
                ),ast -> ast.setFunction(_print)),
                "System.out.println(\"Hello, World!\")"
            )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
