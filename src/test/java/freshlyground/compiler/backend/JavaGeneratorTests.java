package freshlyground.compiler.backend;

import freshlyground.compiler.backend.java.Generator;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.Ast;
import freshlyground.compiler.semantic.BindingMap.Bindings;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Scope;

import freshlyground.compiler.semantic.Types;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class JavaGeneratorTests {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        Analyzer analyzer = new Analyzer();

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(

            // DEF main(): Integer DO
            //   RETURN -1;
            // END
            Arguments.of("Single Method",
                new Ast.Source(
                    List.of(),
                    List.of(
                        new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.valueOf(-1))))))
                ),
                String.join(System.lineSeparator(),
                    "public class Main {",
                    "",
                    "    public static void main(String[] args) {",
                    "        System.exit(new Main().main());",
                    "    }",
                    "",
                    "    int main() {",
                    "        return -1;",
                    "    }",
                    "",
                    "}"
                )
            ),

            // LET x: Integer;
            // DEF main(): Integer DO RETURN -1; END
            Arguments.of("Single Field",
                new Ast.Source(
                    List.of(new Ast.Field("x", "Integer", false, Optional.empty())),
                    List.of(
                        new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.valueOf(-1))))))
                ),
                String.join(System.lineSeparator(),
                    "public class Main {",
                    "",
                    "    int x;",
                    "",
                    "    public static void main(String[] args) {",
                    "        System.exit(new Main().main());",
                    "    }",
                    "",
                    "    int main() {",
                    "        return -1;",
                    "    }",
                    "",
                    "}"
                )
            ),

            // DEF main(): Integer DO
            //     print("Hello, World!");
            //     RETURN 0;
            // END
            Arguments.of("Hello, World!",
                new Ast.Source(
                    List.of(),
                    List.of(
                        new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                            new Ast.Statement.Expression(
                                new Ast.Expression.Function(Optional.empty(), "print",
                                    List.of(new Ast.Expression.Literal("Hello, World!")))),
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))))
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
            // LET y: Integer = 10;
            // DEF main(): Integer DO
            //     RETURN x + y;
            // END
            Arguments.of("Multiple Fields and One Method",
                new Ast.Source(
                    List.of(
                        new Ast.Field("x", "Integer", false, Optional.empty()),
                        new Ast.Field("y", "Integer", false, Optional.of(
                            new Ast.Expression.Literal(BigInteger.TEN)))),
                    List.of(
                        new Ast.Method(
                            "main",
                            List.of(),
                            List.of(),
                            Optional.of("Integer"),
                            List.of(
                                new Ast.Statement.Return(
                                    new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(),"y"))))))
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
            ),

            // LET x: Integer;
            // DEF f(): Integer DO
            //   RETURN 1;
            // END
            // DEF g(): Decimal DO
            //   RETURN 1.0;
            // END
            // DEF h(): String DO
            //   RETURN "str";
            // END
            // DEF main(): Integer DO
            //   RETURN -1;
            // END
            Arguments.of("Multiple Methods One Field",
                new Ast.Source(
                    List.of(new Ast.Field("x", "Integer", false, Optional.empty())),
                    List.of(
                        new Ast.Method("f", List.of(), List.of(), Optional.of("Integer"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ONE)))),
                        new Ast.Method("g", List.of(), List.of(), Optional.of("Decimal"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigDecimal.ONE)))),
                        new Ast.Method("h", List.of(), List.of(), Optional.of("String"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Literal("str")))),
                        new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.valueOf(-1))))))
                ),
                String.join(System.lineSeparator(),
                    "public class Main {",
                    "",
                    "    int x;",
                    "",
                    "    public static void main(String[] args) {",
                    "        System.exit(new Main().main());",
                    "    }",
                    "",
                    "    int f() {",
                    "        return 1;",
                    "    }",
                    "",
                    "    double g() {",
                    "        return 1;",
                    "    }",
                    "",
                    "    String h() {",
                    "        return \"str\";",
                    "    }",
                    "",
                    "    int main() {",
                    "        return -1;",
                    "    }",
                    "",
                    "}"
                )
            ),

            // LET x: Integer;
            // LET y: Decimal;
            // LET z: String;
            // DEF f(): Integer DO
            //   RETURN x;
            // END
            // DEF g(): Decimal DO
            //   RETURN y;
            // END
            // DEF h(): String DO
            //   RETURN z;
            // END
            // DEF main(): Integer DO
            // END
            Arguments.of("Multiple Fields and Methods",
                new Ast.Source(
                    List.of(
                        new Ast.Field("x", "Integer", false, Optional.empty()),
                        new Ast.Field("y", "Decimal", false, Optional.empty()),
                        new Ast.Field("z", "String", false, Optional.empty())),
                    List.of(
                        new Ast.Method("f", List.of(), List.of(), Optional.of("Integer"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(),"x")))),
                        new Ast.Method("g", List.of(), List.of(), Optional.of("Decimal"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(),"y")))),
                        new Ast.Method("h", List.of(), List.of(), Optional.of("String"), List.of(
                            new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(),"z")))),
                        new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of()))
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
    void testFieldExpression(String test, Ast.Field ast, String expected) {
        Analyzer analyzer = new Analyzer();

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testFieldExpression() {
        return Stream.of(
            // LET x: String;
            Arguments.of("Declaration 1",
                new Ast.Field("x", "String", false, Optional.empty()),
                "String x;"
            ),
            // LET name: Integer;
            Arguments.of("Declaration 2",
                new Ast.Field("name", "Integer", false, Optional.empty()),
                "int name;"
            ),
            // LET dub: Decimal = 1.0;
            Arguments.of("Initialization",
                new Ast.Field("dub", "Decimal", false, Optional.of(
                        new Ast.Expression.Literal(BigDecimal.valueOf(1.1)))
                ),
                "double dub = 1.1;"
            ),
            // LET CONST y: Boolean = TRUE AND FALSE;
            Arguments.of("Initialization CONST",
                new Ast.Field("y", "Boolean", true, Optional.of(
                    new Ast.Expression.Binary("AND",
                        new Ast.Expression.Literal(true),
                        new Ast.Expression.Literal(false)))
                ),
                "final boolean y = true && false;"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethodExpression(String test, Ast.Method ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("function", List.of(Types.INTEGER), Types.INTEGER);
        analyzer.getScope().defineVariable("radius", Types.DECIMAL, false);
        analyzer.getScope().defineVariable("x", Types.INTEGER, false);
        analyzer.getScope().defineVariable("y", Types.DECIMAL, false);
        analyzer.getScope().defineVariable("z", Types.STRING, false);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testMethodExpression() {
        return Stream.of(
            // DEF func(): String DO
            // END
            Arguments.of("No Statements",
                new Ast.Method("func", List.of(), List.of(), Optional.of("String"), List.of()),
                String.join(System.lineSeparator(),
                    "String func() {}"
                )
            ),

            // DEF func(): String DO
            //   function(1);
            // END
            Arguments.of("One Statement",
                new Ast.Method("func", List.of(), List.of(), Optional.of("String"), List.of(
                    new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(),"function", List.of(
                        new Ast.Expression.Literal(BigInteger.ONE)))))
                ),
                String.join(System.lineSeparator(),
                    "String func() {",
                    "    function(1);",
                    "}"
                )
            ),

            // DEF area(radius: Decimal): Decimal DO
            //   RETURN 3.14 * radius * radius
            // END
            Arguments.of("Method",
                new Ast.Method("area", List.of("radius"), List.of("Decimal"), Optional.of("Decimal"), List.of(
                    new Ast.Statement.Return(
                        new Ast.Expression.Binary("*",
                            new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(BigDecimal.valueOf(3.14)),
                                new Ast.Expression.Access(Optional.empty(), "radius")),
                            new Ast.Expression.Access(Optional.empty(), "radius"))))
                ),
                String.join(System.lineSeparator(),
                    "double area(double radius) {",
                    "    return 3.14 * radius * radius;",
                    "}"
                )
            ),

            // DEF func(): String DO
            //   function(1);
            //   function(2);
            //   function(3);
            // END
            Arguments.of("Multiple Statement",
                new Ast.Method("func", List.of(), List.of(), Optional.of("String"), List.of(
                    new Ast.Statement.Expression(new Ast.Expression.Function(
                        Optional.empty(),
                        "function",
                        List.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                    new Ast.Statement.Expression(new Ast.Expression.Function(
                        Optional.empty(),
                        "function",
                        List.of(new Ast.Expression.Literal(BigInteger.valueOf(2))))),
                    new Ast.Statement.Expression(new Ast.Expression.Function(
                        Optional.empty(),
                        "function",
                        List.of(new Ast.Expression.Literal(BigInteger.valueOf(3))))))
                ),
                String.join(System.lineSeparator(),
                    "String func() {",
                    "    function(1);",
                    "    function(2);",
                    "    function(3);",
                    "}"
                )
            ),

            //DEF func(x: String): String DO
            // function(1);
            //END
            Arguments.of("One Parameter",
                new Ast.Method("func", List.of("x"), List.of("String"), Optional.of("String"), List.of(
                    new Ast.Statement.Expression(new Ast.Expression.Function(
                        Optional.empty(),
                        "function",
                        List.of(new Ast.Expression.Literal(BigInteger.ONE)))))
                ),
                String.join(System.lineSeparator(),
                    "String func(String x) {",
                    "    function(1);",
                    "}"
                )
            ),

            // DEF func(x: String, y: String, z: String): String DO
            //   function(1);
            // END
            Arguments.of("Multiple Parameters",
                new Ast.Method("func", List.of("x", "y", "z"), List.of("String", "String", "String"), Optional.empty(), List.of(
                    new Ast.Statement.Expression(new Ast.Expression.Function(
                        Optional.empty(),
                        "function",
                        List.of(new Ast.Expression.Literal(BigInteger.ONE)))))
                ),
                String.join(System.lineSeparator(),
                    "void func(String x, String y, String z) {",
                    "    function(1);",
                    "}"
                )
            ),

            // DEF func(x: Integer, y: Decimal, z: String) DO
            //    print(x);
            //    print(y);
            //    print(z);
            // END
            Arguments.of("Multiple Statements and Parameters",
                new Ast.Method(
                    "func",
                    List.of("x", "y", "z"),
                    List.of("Integer", "Decimal", "String"),
                    Optional.empty(),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "print",
                            List.of(new Ast.Expression.Access(Optional.empty(), "x")))),
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "print",
                            List.of(new Ast.Expression.Access(Optional.empty(), "y")))),
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "print",
                            List.of(new Ast.Expression.Access(Optional.empty(), "z")))))
                ),
                String.join(System.lineSeparator(),
                    "void func(int x, double y, String z) {",
                    "    System.out.println(x);",
                    "    System.out.println(y);",
                    "    System.out.println(z);",
                    "}"
                )
            ),

            // DEF func() DO
            //   function(1);
            // END
            Arguments.of("Empty Return Type",
                new Ast.Method("func", List.of(), List.of(), Optional.empty(), List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            List.of(new Ast.Expression.Literal(BigInteger.ONE)))))
                ),
                String.join(System.lineSeparator(),
                    "void func() {",
                    "    function(1);",
                    "}"
                )
            ),

            // DEF func(): String DO
            //   RETURN "xyz";
            // END
            Arguments.of("Return Statement",
                new Ast.Method("func", List.of(), List.of(), Optional.of("String"), List.of(
                    new Ast.Statement.Return(new Ast.Expression.Literal("xyz")))
                ),
                String.join(System.lineSeparator(),
                    "String func() {",
                    "    return \"xyz\";",
                    "}"
                )
            ),

            //DEF main(): Integer DO
            //    print("Hello World");
            //    RETURN 0;
            //END
            Arguments.of("Hello World",
                new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                    new Ast.Statement.Expression(new Ast.Expression.Function(
                        Optional.empty(),
                        "print",
                        List.of(new Ast.Expression.Literal("Hello World")))),
                    new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                ),
                String.join(System.lineSeparator(),
                    "int main() {",
                    "    System.out.println(\"Hello World\");",
                    "    return 0;",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExpressionStatement(String test, Ast.Statement.Expression ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("log", List.of(Types.STRING), Types.NIL);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
            //Function (2): function();

            // log("Hello World");
            Arguments.of("Expression",
                new Ast.Statement.Expression(
                    new Ast.Expression.Function(Optional.empty(),"log", List.of(
                        new Ast.Expression.Literal("Hello World")))
                ),
                "log(\"Hello World\");"
            )
            // 1; (MOVE TO SAD PATH)
//            Arguments.of("Initialization",
//                new Ast.Statement.Expression(init(new Ast.Expression.Literal(new BigDecimal("1")),ast -> ast.setType(Types.INTEGER))),
//                "1;"
//            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        Analyzer analyzer = new Analyzer();

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
            // LET name: Integer;
            Arguments.of("Declaration",
                new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                "int name;"
            ),
            // LET name = 1.0;
            Arguments.of("Initialization",
                new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                    new Ast.Expression.Literal(new BigDecimal("1.0")))),
                "double name = 1.0;"
            ),
            // LET str: String = "string";
            Arguments.of("Typed Initialization",
                new Ast.Statement.Declaration("str", Optional.of("String"), Optional.of(
                    new Ast.Expression.Literal("string"))),
                "String str = \"string\";"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatement(String test, Ast.Statement.Assignment ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineVariable("variableStr", Types.STRING, false);
        analyzer.getScope().defineVariable("variableFunc", Types.CHARACTER, false);
        analyzer.getScope().defineVariable("object", Types.ANY, false);
        analyzer.getScope().defineVariable("field", Types.INTEGER, false);
        analyzer.getScope().defineFunction("funcOne", List.of(), Types.CHARACTER);
        analyzer.getScope().defineVariable("variableOne", Types.CHARACTER, false);
        analyzer.getScope().defineVariable("variableTwo", Types.CHARACTER, false);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
            // variable = "Hello World";
            Arguments.of("Variable String",
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "variableStr"),
                    new Ast.Expression.Literal("Hello World")
                ),
                "variableStr = \"Hello World\";"
            ),
            // field = 1;
            Arguments.of("Variable Int",
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "field"),
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                "field = 1;"
            ),
            // TODO find new way to test object.field = 1;
//            Arguments.of("Receiver",
//                new Ast.Statement.Assignment(
//                    new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
//                    new Ast.Expression.Literal(BigInteger.ONE)
//                ),
//                "object.field = 1;"
//            ),
            // variableOne = funcOne();
            Arguments.of("Function",
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "variableOne"),
                    new Ast.Expression.Function(Optional.empty(), "funcOne", List.of())
                ),
                "variableOne = funcOne();"
            )
            // variableOne = variableTwo;
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("funcOne", List.of(), Types.INTEGER);
        analyzer.getScope().defineFunction("funcOne", List.of(Types.INTEGER), Types.INTEGER);
        analyzer.getScope().defineFunction("funcTwo", List.of(), Types.INTEGER);
        analyzer.getScope().defineVariable("condition", Types.BOOLEAN, false);
        analyzer.getScope().defineVariable("conditionTwo", Types.BOOLEAN, false);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
            // IF condition DO
            //     funcOne;
            // END
            Arguments.of("If",
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of()))
                    ),
                    List.of()
                ),
                String.join(System.lineSeparator(),
                    "if (condition) {",
                    "    funcOne();",
                    "}"
                )
            ),
            // IF condition DO
            //     funcOne;
            // ELSE
            //     funcTwo;
            // END
            Arguments.of("Else",
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of()))
                    ),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcTwo", List.of()))
                    )
                ),
                String.join(System.lineSeparator(),
                    "if (condition) {",
                    "    funcOne();",
                    "} else {",
                    "    funcTwo();",
                    "}"
                )
            ),
            //If Multiple Statements:
            //IF condition DO
            //    funcOne(1);
            //    funcOne(2);
            //    funcOne(3);
            //ELSE
            //    funcOne(4);
            //END
            Arguments.of("Multiple If",
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(1))))),
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(2))))),
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(3)))))
                    ),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(4)))))
                    )
                ),
                String.join(System.lineSeparator(),
                    "if (condition) {",
                    "    funcOne(1);",
                    "    funcOne(2);",
                    "    funcOne(3);",
                    "} else {",
                    "    funcOne(4);",
                    "}"
                )
            ),
            //Else Multiple Statements:
            //IF condition DO
            //    funcOne(1);
            //ELSE
            //    funcOne(2);
            //    funcOne(3);
            //    funcOne(4);
            //END
            Arguments.of("Multiple Else",
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(1)))))
                    ),
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(2))))),
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(3))))),
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                            new Ast.Expression.Literal(BigInteger.valueOf(4)))))
                    )
                ),
                String.join(System.lineSeparator(),
                    "if (condition) {",
                    "    funcOne(1);",
                    "} else {",
                    "    funcOne(2);",
                    "    funcOne(3);",
                    "    funcOne(4);",
                    "}"
                )
            ),
            //Nested If:
            //IF condition DO
            //    IF conditionTwo DO
            //        funcOne(1);
            //    END
            //END
            Arguments.of("Nested If",
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of(
                        new Ast.Statement.If(
                            new Ast.Expression.Access(Optional.empty(), "conditionTwo"),
                            List.of(
                                new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of(
                                    new Ast.Expression.Literal(BigInteger.valueOf(1)))))
                            ),
                            List.of()
                        )
                    ),
                    List.of()
                ),
                String.join(System.lineSeparator(),
                    "if (condition) {",
                    "    if (conditionTwo) {",
                    "        funcOne(1);",
                    "    }",
                    "}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatement(String test, Ast.Statement.For ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("funcOne", List.of(), Types.INTEGER);
        analyzer.getScope().defineFunction("funcTwo", List.of(), Types.INTEGER);
        analyzer.getScope().defineVariable("num", Types.INTEGER, false);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
            //For (7)
            //For (2):
            //FOR (num = 0; num < 5; num = num + 1)
            //    sum = sum + num;
            //END
            //Multiple Statements (2):
            //FOR (num = 0; num < 5; num = num + 1)
            //    sum = sum + num;
            //    function(2);
            //    print("3");
            //END
            //Condition Only:
            //FOR ( ; num < 5; )
            //    num = num + 1;
            //END
            //No Initialization:
            //FOR ( ; sum < 5; sum = sum + 1)
            //    print(sum);
            //END
            //No Increment:
            //FOR (num = 0; num < 5; )
            //    num = num + 1;
            //END
            //Nested For:
            //FOR (sum = 0; sum < 5; sum = sum + 1)
            //    FOR (num = 0; num < 10; num = num + 1)
            //        print(sum * num);
            //    END
            //END

            // FOR (num = 0; num < 5; num = num + 1)
            //     print(num);
            // END
            Arguments.of("For",
                new Ast.Statement.For(
                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Literal(BigInteger.valueOf(0))
                    ),

                    new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                    ),

                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Binary("+",
                            new Ast.Expression.Access(Optional.empty(), "num"),
                            new Ast.Expression.Literal(BigInteger.valueOf(1))
                        )
                    ),

                    List.of(
                        new Ast.Statement.Expression(
                            new Ast.Expression.Function(Optional.empty(),"print", List.of(
                                new Ast.Expression.Access(Optional.empty(), "num")
                            ))
                        )
                    )
                ),
                String.join(System.lineSeparator(),
                    "for ( num = 0; num < 5; num = num + 1 ) {",
                    "    System.out.println(num);",
                    "}"
                )
            ),
            // FOR (; num < 5;)
            //     print(num);
            //     num = num + 1;
            // END
            Arguments.of("Missing Signature",
                new Ast.Statement.For(
                    null,

                    new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                    ),

                    null,

                    List.of(
                        new Ast.Statement.Expression(
                            new Ast.Expression.Function(Optional.empty(),"print", List.of(
                                new Ast.Expression.Access(Optional.empty(), "num")
                            ))
                        ),

                        new Ast.Statement.Assignment(
                            new Ast.Expression.Access(Optional.empty(), "num"),
                            new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "num"),
                                new Ast.Expression.Literal(BigInteger.valueOf(1))
                            )
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
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("funcOne", List.of(), Types.INTEGER);
        analyzer.getScope().defineFunction("funcTwo", List.of(), Types.INTEGER);
        analyzer.getScope().defineVariable("condition", Types.BOOLEAN, false);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
            //While (7)
            //One Statement (2):
            //WHILE cond DO
            //    function(1);
            //END
            //Multiple Statements (2):
            //WHILE cond DO
            //    function(1);
            //    function(2);
            //    function(3);
            //END
            //No Statements: WHILE cond DO END
            //Nested While:
            //WHILE cond1 DO
            //    WHILE cond2 DO
            //        function(1);
            //    END
            //END
            //Comparison Condition:
            //WHILE num < 10 DO
            //    function(num);
            //END

            // WHILE condition DO
            //   stmt1;
            //   stmt2;
            // END
            Arguments.of("While",
                new Ast.Statement.While(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of(
                        new Ast.Statement.Expression( new Ast.Expression.Function(Optional.empty(), "funcOne", List.of())),
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcTwo", List.of()))
                    )
                ),
                String.join(System.lineSeparator(),
                    "while (condition) {",
                    "    funcOne();",
                    "    funcTwo();",
                    "}"
                )
            ),
            // WHILE condition DO
            // END
            Arguments.of("While Empty Statements",
                new Ast.Statement.While(
                    new Ast.Expression.Access(Optional.empty(), "condition"),
                    List.of()
                ),
                String.join(System.lineSeparator(),
                    "while (condition) {}"
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testReturnExpression(String test, Ast.Statement.Return ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.setReturnType(Types.INTEGER);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testReturnExpression() {
        return Stream.of(
            // RETURN 5 * 10;
            Arguments.of("Return",
                new Ast.Statement.Return(
                    new Ast.Expression.Binary("*",
                        new Ast.Expression.Literal(BigInteger.valueOf(5)),
                        new Ast.Expression.Literal(BigInteger.TEN))
                ),
                "return 5 * 10;"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteralExpression(String test, Ast.Expression.Literal ast, String expected) {
        Analyzer analyzer = new Analyzer();

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
            // TRUE
            Arguments.of("Boolean",
                new Ast.Expression.Literal(true),
                "true"
            ),
            // 1
            Arguments.of("Integer",
                new Ast.Expression.Literal(BigInteger.ONE),
                "1"
            ),
            // 123.456
            Arguments.of("Double",
                new Ast.Expression.Literal(BigDecimal.valueOf(123.456)),
                "123.456"
            ),
            // "Hello World"
            Arguments.of("String",
                new Ast.Expression.Literal("Hello World"),
                "\"Hello World\""
            ),
            // 'a'
            Arguments.of("Character",
                new Ast.Expression.Literal('a'),
                "'a'"
            ),
            // "NIL"
            Arguments.of("Nil",
                new Ast.Expression.Literal(null),
                "null"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpression(String test, Ast.Expression.Group ast, String expected) {
        Analyzer analyzer = new Analyzer();

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
            //Group (4)
            //Binary (2): (1 + 10)
            //Nested (2): (1 + (2 + 3))

            // (1)
            Arguments.of("Group Literal",
                new Ast.Expression.Group(
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                "(1)"
            ),
            // (1 + 10)
            Arguments.of("Group Binary",
                new Ast.Expression.Group(
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigInteger.TEN))
                ),
                "(1 + 10)"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        Analyzer analyzer = new Analyzer();

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
            //Binary (14)
            //And Expression: TRUE AND FALSE
            //Or Expression: TRUE OR FALSE
            //Equals: 1 == 10
            //Not Equals: 1 != 10
            //Greater Than: 1 > 10
            //Less Than or Equal To: 1 <= 10
            //Addition: 1 + 10
            //Concatenation: "Ben " + 10
            //Subtraction: 10 - 1
            //Multiplication: 10 * 100
            //Division: 10 / 100
            //Chained Addition / Subtraction: 1 + 2 - 3
            //Chained Multiplication / Division: 1 * 2 / 3
            //Priority Subtraction / Multiplication: (1 - 2) * 3

            // TRUE AND FALSE
            Arguments.of("And",
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(true),
                    new Ast.Expression.Literal(false)),
                "true && false"
            ),
            // 1 > 10
            Arguments.of("Comparison",
                new Ast.Expression.Binary(">",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)),
                "1 > 10"
            ),
            // 1 + 10
            Arguments.of("Addition",
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)),
                "1 + 10"
            ),
            // "Ben" + 10
            Arguments.of("Concatenation",
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal("Ben"),
                    new Ast.Expression.Literal(BigInteger.TEN)),
                "\"Ben\" + 10"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccessExpression(String test, Ast.Expression.Access ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineVariable("variable", Types.INTEGER, false);
        analyzer.getScope().defineVariable("object", Types.ANY, false);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
            //Access (6)

            // TODO get output
            //Variable: variable
            //Variable JVM Name: name


            //Field (2): object.field
            //Field JVM Name: object.name
            //Field Chain: object.a.b.c

            // variable
            Arguments.of("Variable",
                new Ast.Expression.Access(Optional.empty(), "variable"),
                "variable"
            )
            // TODO find new way to test object.field
//            Arguments.of("Field",
//                new Ast.Expression.Access(Optional.of(
//                    new Ast.Expression.Access(Optional.empty(), "object")
//                ), "field"),
//                "object.field"
//            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("function", List.of(), Types.INTEGER);

        Bindings bindings = analyzer.decorate(ast);
        test(ast, bindings, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
            //Function (6)
            //Zero Arguments: function()
            //One Argument: function(1)
            //Multiple Arguments: function(1, 2, 3)
            //Method: object.method(1, 2, 3)
            //Function JVM Name: name()

            // func()
            Arguments.of("Zero Arguments",
                new Ast.Expression.Function(Optional.empty(),"function", List.of()),
                "function()"
            ),
            // print("Hello, World!")
            Arguments.of("Print",
                new Ast.Expression.Function(Optional.empty(),"print", List.of(
                    new Ast.Expression.Literal("Hello, World!"))),
                "System.out.println(\"Hello, World!\")"
            ),
            // "string".slice(1, 10)
            Arguments.of("String Slice",
                new Ast.Expression.Function(
                    Optional.of(
                        new Ast.Expression.Literal("string")
                    ),
                    "slice",
                    List.of(
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigInteger.TEN)
                    )
                ),
                "\"string\".substring(1, 10)"
            )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, Bindings bindings, String expected) {
        String result = new Generator(bindings).emit(ast);
        Assertions.assertEquals(expected, result);
    }
}
