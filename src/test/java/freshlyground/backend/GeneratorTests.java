package freshlyground.backend;

import freshlyground.frontend.Ast;
import freshlyground.semantic.Environment;
import freshlyground.semantic.Scope;
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
        scope.defineVariable("field", "field", Environment.Type.INTEGER, false);
        scope.defineFunction("method", "method", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER);
    }));

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL);
        Environment.Function _main = new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER);
        Environment.Variable _x = new Environment.Variable("x", "x", Environment.Type.INTEGER, false);
        Environment.Variable _y = new Environment.Variable("y", "y", Environment.Type.INTEGER, false);
        Environment.Variable _num = new Environment.Variable("num", "num", Environment.Type.INTEGER, false);
        Environment.Variable _sum = new Environment.Variable("sum", "sum", Environment.Type.INTEGER, false);

        return Stream.of(

            // DEF main(): Integer DO
            //   RETURN -1;
            // END
            Arguments.of("Single Method",
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigInteger.valueOf(-1)), ast -> ast.setType(Environment.Type.INTEGER))
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
                    "        return -1;",
                    "    }",
                    "",
                    "}"
                )
            ),

            // LET x: Integer;
            Arguments.of("Single Field",
                new Ast.Source(
                    Arrays.asList(
                        init(new Ast.Field("x", "Integer", false, Optional.empty()), ast -> ast.setVariable(_x))
                    ),
                    Arrays.asList(
                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigInteger.valueOf(-1)), ast -> ast.setType(Environment.Type.INTEGER))
                                )
                            )
                        ), ast -> ast.setFunction(_main))
                    )
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

            // LET x: Integer;
            // DEF main(): Integer DO RETURN -1; END
            Arguments.of("Single Field and Method",
                new Ast.Source(
                    Arrays.asList(
                        init(new Ast.Field("x", "Integer", false, Optional.empty()), ast -> ast.setVariable(_x))
                    ),
                    Arrays.asList(
                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigInteger.valueOf(-1)), ast -> ast.setType(Environment.Type.INTEGER))
                                )
                            )
                        ), ast -> ast.setFunction(_main))
                    )
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

            // LET x: Integer;
            // LET y: Integer = 10;
            // DEF main(): Integer DO
            //     RETURN x + y;
            // END
            Arguments.of("Multiple Fields and One Method",
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
                    Arrays.asList(
                        init(new Ast.Field("x", "Integer", false, Optional.empty()), ast -> ast.setVariable(_x))
                    ),

                    Arrays.asList(
                        init(new Ast.Method(
                            "f",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigInteger.ONE),ast -> ast.setType(Environment.Type.INTEGER))
                                )
                            )
                        ), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER))),

                        init(new Ast.Method(
                            "g",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Decimal"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigDecimal.ONE),ast -> ast.setType(Environment.Type.DECIMAL))
                                )
                            )
                        ), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL))),

                        init(new Ast.Method(
                            "h",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("String"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal("str"),ast -> ast.setType(Environment.Type.STRING))
                                )
                            )
                        ), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING))),

                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Literal(BigInteger.valueOf(-1)),ast -> ast.setType(Environment.Type.INTEGER))
                                )
                            )
                        ), ast -> ast.setFunction(_main))
                    )
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
                    Arrays.asList(
                        init(new Ast.Field("x", "Integer", false, Optional.empty()), ast -> ast.setVariable(_x)),
                        init(new Ast.Field("y", "Decimal", false, Optional.empty()), ast -> ast.setVariable(
                                new Environment.Variable("y", "y", Environment.Type.DECIMAL, false)
                            )),
                        init(new Ast.Field("z", "String", false, Optional.empty()), ast -> ast.setVariable(
                                new Environment.Variable("z", "z", Environment.Type.STRING, false)
                            ))
                    ),

                    Arrays.asList(
                        init(new Ast.Method(
                            "f",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Access(Optional.empty(),"x"), ast -> ast.setVariable(
                                        new Environment.Variable("x", "x", Environment.Type.INTEGER, false)
                                    ))
                                )
                            )
                        ), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER))),

                        init(new Ast.Method(
                            "g",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Decimal"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Access(Optional.empty(),"y"), ast -> ast.setVariable(
                                        new Environment.Variable("y", "y", Environment.Type.DECIMAL, false)
                                    ))
                                )
                            )
                        ), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL))),

                        init(new Ast.Method(
                            "h",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("String"),
                            Arrays.asList(
                                new Ast.Statement.Return(
                                    init(new Ast.Expression.Access(Optional.empty(),"z"), ast -> ast.setVariable(
                                        new Environment.Variable("z", "z", Environment.Type.STRING, false)
                                    ))
                                )
                            )
                        ), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING))),

                        init(new Ast.Method(
                            "main",
                            Arrays.asList(),
                            Arrays.asList(),
                            Optional.of("Integer"),
                            Arrays.asList()
                        ), ast -> ast.setFunction(_main))
                    )
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
        test(ast, expected);
    }

    private static Stream<Arguments> testFieldExpression() {
        Environment.Variable _x = new Environment.Variable("x", "x", Environment.Type.STRING, false);
        Environment.Variable _y = new Environment.Variable("y", "y", Environment.Type.BOOLEAN, false);
        Environment.Variable _name = new Environment.Variable("name", "name", Environment.Type.INTEGER, false);
        Environment.Variable _dub = new Environment.Variable("dub", "dub", Environment.Type.DECIMAL, false);

        return Stream.of(
            // LET x: String;
            Arguments.of("Declaration 1",
                init(new Ast.Field("x", "String", false, Optional.empty()),
                    ast -> ast.setVariable(_x)
                ),
                "String x;"
            ),
            // LET name: Integer;
            Arguments.of("Declaration 2",
                init(new Ast.Field("name", "Integer", false, Optional.empty()),
                    ast -> ast.setVariable(_name)
                ),
                "int name;"
            ),
            // LET dub: Decimal = 1.0;
            Arguments.of("Initialization",
                init(new Ast.Field("dub", "Decimal", false, Optional.of(
                        init(new Ast.Expression.Literal(BigDecimal.valueOf(1.1)), ast -> ast.setType(Environment.Type.DECIMAL))
                    )),
                    ast -> ast.setVariable(_dub)
                ),
                "double dub = 1.1;"
            ),
            // LET CONST y: Boolean = TRUE AND FALSE;
            Arguments.of("Initialization CONST",
                init(new Ast.Field("y", "Boolean", true, Optional.of(
                    init(new Ast.Expression.Binary("AND",
                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))),
                        ast -> ast.setType(Environment.Type.BOOLEAN)
                    ))
                ),ast -> ast.setVariable(_y)),
                "final boolean y = true && false;"
            ),
            // LET str: Comparable = string;
            Arguments.of("Supertype Supertype",
                init(new Ast.Field("str", "Comparable", false, Optional.of(
                        init(new Ast.Expression.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
                    )),
                    ast -> ast.setVariable(_dub)
                ),
                "Comparable str = \"string\";"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethodExpression(String test, Ast.Method ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testMethodExpression() {
        Environment.Function _area = new Environment.Function("area", "area", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.DECIMAL);
        Environment.Function _func = new Environment.Function("func", "func", Arrays.asList(Environment.Type.INTEGER, Environment.Type.DECIMAL, Environment.Type.STRING), Environment.Type.DECIMAL);
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL);
        Environment.Function _function = new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER);
        Environment.Function _empty = new Environment.Function("func", "func", Arrays.asList(), Environment.Type.STRING);

        Environment.Variable _radius = new Environment.Variable("radius", "radius", Environment.Type.DECIMAL, false);
        Environment.Variable _x = new Environment.Variable("x", "x", Environment.Type.INTEGER, false);
        Environment.Variable _y = new Environment.Variable("y", "y", Environment.Type.DECIMAL, false);
        Environment.Variable _z = new Environment.Variable("z", "z", Environment.Type.STRING, false);

        return Stream.of(
            // DEF func(): String DO
            // END
            Arguments.of("No Statements",
                init(new Ast.Method(
                    "func",
                    Arrays.asList(),
                    Arrays.asList(),
                    Optional.of("String"),
                    Arrays.asList()), ast -> ast.setFunction(_empty)
                ),
                String.join(System.lineSeparator(),
                    "String func() {}"
                )
            ),

            // DEF func(): String DO
            //   function(1);
            // END
            Arguments.of("One Statement",
                init(new Ast.Method(
                    "func",
                    Arrays.asList(),
                    Arrays.asList(),
                    Optional.of("String"),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function)))
                    )), ast -> ast.setFunction(_empty)
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
            ),

            // DEF func(): String DO
            //   function(1);
            //   function(2);
            //   function(3);
            // END
            Arguments.of("Multiple Statement",
                init(new Ast.Method(
                    "func",
                    Arrays.asList(),
                    Arrays.asList(),
                    Optional.of("String"),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function)))
                    )), ast -> ast.setFunction(_empty)
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
                init(new Ast.Method(
                    "func",
                    Arrays.asList("x"),
                    Arrays.asList("String"),
                    Optional.of("String"),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function)))
                    )), ast -> ast.setFunction(_empty)
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
                init(new Ast.Method(
                    "func",
                    Arrays.asList("x", "y", "z"),
                    Arrays.asList("String", "String", "String"),
                    Optional.empty(),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function)))
                    )), ast -> ast.setFunction(
                        new Environment.Function("func",
                            "func",
                            Arrays.asList(Environment.Type.STRING, Environment.Type.STRING, Environment.Type.STRING),
                            Environment.Type.NIL)
                    )
                ),
                String.join(System.lineSeparator(),
                    "Void func(String x, String y, String z) {",
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
                init(new Ast.Method(
                    "func",
                    Arrays.asList("x", "y", "z"),
                    Arrays.asList("Integer", "Decimal", "String"),
                    Optional.empty(),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "x",
                            Arrays.asList(init(new Ast.Expression.Access(Optional.empty(), "x"), ast -> ast.setVariable(_x)))
                        ), ast -> ast.setFunction(_print))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "y",
                            Arrays.asList(init(new Ast.Expression.Access(Optional.empty(), "y"), ast -> ast.setVariable(_y)))
                        ), ast -> ast.setFunction(_print))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "z",
                            Arrays.asList(init(new Ast.Expression.Access(Optional.empty(), "z"), ast -> ast.setVariable(_z)))
                        ), ast -> ast.setFunction(_print)))
                    )), ast -> ast.setFunction(
                        new Environment.Function("func",
                            "func",
                            Arrays.asList(),
                            Environment.Type.NIL))
                ),
                String.join(System.lineSeparator(),
                    "Void func(int x, double y, String z) {",
                    "    System.out.println(x);",
                    "    System.out.println(y);",
                    "    System.out.println(z);",
                    "}"
                )
            ),

            // TODO fix
            // DEF func() DO
            //   function(1);
            // END
            Arguments.of("Empty Return Type",
                init(new Ast.Method(
                    "func",
                    Arrays.asList(),
                    Arrays.asList(),
                    Optional.empty(),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_function)))
                    )), ast -> ast.setFunction(
                    new Environment.Function("func", "func", Arrays.asList(), Environment.Type.NIL)
                    )
                ),
                String.join(System.lineSeparator(),
                    "Void func() {",
                    "    function(1);",
                    "}"
                )
            ),

            // DEF func(): String DO
            //   RETURN "xyz";
            // END
            Arguments.of("Return Statement",
                init(new Ast.Method(
                    "func",
                    Arrays.asList(),
                    Arrays.asList(),
                    Optional.of("String"),
                    Arrays.asList(
                        new Ast.Statement.Return(
                            init(new Ast.Expression.Literal("xyz"), ast -> ast.setType(Environment.Type.STRING))
                        )
                    )), ast -> ast.setFunction(
                    new Environment.Function("func",
                        "func",
                        Arrays.asList(),
                        Environment.Type.STRING)
                    )
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
                init(new Ast.Method(
                    "main",
                    Arrays.asList(),
                    Arrays.asList(),
                    Optional.of("Integer"),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "print",
                            Arrays.asList(
                                init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING))
                            )), ast -> ast.setFunction(_print))
                        ),
                        new Ast.Statement.Return(
                            init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER))
                        )
                    )), ast -> ast.setFunction(
                    new Environment.Function("main",
                        "main",
                        Arrays.asList(),
                        Environment.Type.INTEGER)
                    )
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
        test(ast, expected);
    }

    private static Stream<Arguments> testExpressionStatement() {
        Environment.Function _log = new Environment.Function("log", "log", Arrays.asList(Environment.Type.STRING), Environment.Type.NIL);

        return Stream.of(
            //Function (2): function();

            // log("Hello World");
            Arguments.of("Expression",
                new Ast.Statement.Expression(
                    init(new Ast.Expression.Function(Optional.empty(),"log", Arrays.asList(
                        init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING))
                    )), ast -> ast.setFunction(_log))
                ),
                "log(\"Hello World\");"
            ),
            // 1;
            Arguments.of("Initialization",
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
        Environment.Variable _name = new Environment.Variable("name", "name", Environment.Type.INTEGER, true);
        Environment.Variable _name2 = new Environment.Variable("name", "name", Environment.Type.DECIMAL, true);

        return Stream.of(
            // LET name: Integer;
            Arguments.of("Declaration",
                init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()
                ), ast -> ast.setVariable(_name)),
                "int name;"
            ),
            // LET name = 1.0;
            Arguments.of("Initialization",
                init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                    init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                )), ast -> ast.setVariable(_name2)),
                "double name = 1.0;"
            ),
            // LET str: String = string;
            Arguments.of("Typed Initialization",
                init(new Ast.Statement.Declaration("str", Optional.of("String"), Optional.of(
                    init(new Ast.Expression.Literal("string"),ast -> ast.setType(Environment.Type.STRING))
                )), ast -> ast.setVariable(
                    new Environment.Variable("str", "str", Environment.Type.STRING, true))),
                "String str = \"string\";"
            ),
            // LET str: Comparable = string;
            Arguments.of("Supertype",
                init(new Ast.Statement.Declaration("str", Optional.of("Comparable"), Optional.of(
                    init(new Ast.Expression.Literal("string"),ast -> ast.setType(Environment.Type.STRING))
                )), ast -> ast.setVariable(
                    new Environment.Variable("str", "str", Environment.Type.COMPARABLE, true))),
                "Comparable str = \"string\";"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatement(String test, Ast.Statement.Assignment ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        Environment.Variable _variable = new Environment.Variable("variable", "variable", Environment.Type.STRING, false);
        Environment.Variable _object = new Environment.Variable("object", "object", OBJECT_TYPE, false);
        Environment.Variable _field = new Environment.Variable("field", "field", Environment.Type.INTEGER, false);


        return Stream.of(
            // variable = "Hello World";
            Arguments.of("Variable",
                new Ast.Statement.Assignment(
                    init(new Ast.Expression.Access(Optional.empty(), "variable"),ast -> ast.setVariable(_variable)),
                    init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING))
                ),
                "variable = \"Hello World\";"
            ),
            // field = 1;
            Arguments.of("Variable JVM field",
                new Ast.Statement.Assignment(
                    init(new Ast.Expression.Access(Optional.empty(), "field"),ast -> ast.setVariable(_field)),
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                "field = 1;"
            ),
            // object.field = 1;
            Arguments.of("Field JVM field",
                new Ast.Statement.Assignment(
                    init(
                        new Ast.Expression.Access(Optional.of(
                            init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(_object))
                            ), "field"), ast -> ast.setVariable(_field)
                    ),
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                "object.field = 1;"
            ),
            // variable = function();
            Arguments.of("Function",
                new Ast.Statement.Assignment(
                    init(new Ast.Expression.Access(Optional.empty(), "variable"),ast -> ast.setVariable(
                            new Environment.Variable("variable", "variable", Environment.Type.COMPARABLE, false)
                    )),
                    init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()), ast -> ast.setFunction(
                        new Environment.Function("function", "function", Arrays.asList(), Environment.Type.NIL)
                    ))
                ),
                "variable = function();"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        Environment.Variable _expr = new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true);
        Environment.Variable _stmt = new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true);
        Environment.Variable _stmt2 = new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true);

        Environment.Function _func = new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER);

        return Stream.of(
            // IF expr DO
            //     stmt;
            // END
            Arguments.of("If",
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
            // IF expr DO
            //     stmt;
            // ELSE
            //     stmt2;
            // END
            Arguments.of("Else",
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
            ),
            //If Multiple Statements:
            //IF expr DO
            //    function(1);
            //    function(2);
            //    function(3);
            //ELSE
            //    function(4);
            //END
            Arguments.of("Multiple If",
                new Ast.Statement.If(
                    init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(_expr)),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func)))
                    ),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(4)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func)))
                    )
                ),
                String.join(System.lineSeparator(),
                    "if (expr) {",
                    "    function(1);",
                    "    function(2);",
                    "    function(3);",
                    "} else {",
                    "    function(4);",
                    "}"
                )
            ),
            //Else Multiple Statements:
            //IF expr DO
            //    function(1);
            //ELSE
            //    function(2);
            //    function(3);
            //    function(4);
            //END
            Arguments.of("Multiple Else",
                new Ast.Statement.If(
                    init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(_expr)),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func)))
                    ),
                    Arrays.asList(
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(new Ast.Expression.Literal(BigInteger.valueOf(4)), ast -> ast.setType(Environment.Type.INTEGER))
                            )
                        ), ast -> ast.setFunction(_func)))
                    )
                ),
                String.join(System.lineSeparator(),
                    "if (expr) {",
                    "    function(1);",
                    "} else {",
                    "    function(2);",
                    "    function(3);",
                    "    function(4);",
                    "}"
                )
            ),
            //Nested If:
            //IF cond1 DO
            //    IF cond2 DO
            //        function(1);
            //    END
            //END
            Arguments.of("Nested If",
                new Ast.Statement.If(
                    init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(_expr)),
                    Arrays.asList(
                        new Ast.Statement.If(
                            init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(_expr)),
                            Arrays.asList(
                                new Ast.Statement.Expression(init(new Ast.Expression.Function(
                                    Optional.empty(),
                                    "function",
                                    Arrays.asList(
                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                    )
                                ), ast -> ast.setFunction(_func)))
                            ),
                            Arrays.asList()
                        )
                    ),
                    Arrays.asList()
                ),
                String.join(System.lineSeparator(),
                    "if (expr) {",
                    "    if (expr) {",
                    "        function(1);",
                    "    }",
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
        Environment.Variable _num = new Environment.Variable("num", "num", Environment.Type.INTEGER, false);
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL);

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
            // FOR (; num < 5;)
            //     print(num);
            // END
            Arguments.of("Missing Signature",
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
        Environment.Variable _condition = new Environment.Variable("condition", "condition", Environment.Type.BOOLEAN, false);
        Environment.Variable _stmt1 = new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true);
        Environment.Variable _stmt2 = new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true);

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
            // END
            Arguments.of("While",
                new Ast.Statement.While(
                    init(new Ast.Expression.Access(Optional.empty(), "condition"), ast -> ast.setVariable(_condition)),
                    Arrays.asList()
                ),
                String.join(System.lineSeparator(),
                    "while (condition) {}"
                )
            ),
            // WHILE condition DO
            //   stmt1;
            //   stmt2;
            // END
            Arguments.of("While",
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
            // 123.456
            Arguments.of("Double",
                init(new Ast.Expression.Literal(BigDecimal.valueOf(123.456)), ast -> ast.setType(Environment.Type.DECIMAL)),
                "123.456"
            ),
            // "Hello World"
            Arguments.of("String",
                init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING)),
                "\"Hello World\""
            ),
            // 'a'
            Arguments.of("Character",
                init(new Ast.Expression.Literal('a'), ast -> ast.setType(Environment.Type.CHARACTER)),
                "\'a\'"
            ),
            // "NIL"
            Arguments.of("Nil",
                init(new Ast.Expression.Literal(null), ast -> ast.setType(Environment.Type.NIL)),
                "null"
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
            //Group (4)
            //Binary (2): (1 + 10)
            //Nested (2): (1 + (2 + 3))

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
                init(new Ast.Expression.Binary("AND",
                    init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                    init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                "true && false"
            ),
            // 1 > 10
            Arguments.of("Comparison",
                init(new Ast.Expression.Binary(">",
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                    init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                "1 > 10"
            ),
            // 1 + 10
            Arguments.of("Addition",
                init(new Ast.Expression.Binary("+",
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                    init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.INTEGER)),
                "1 + 10"
            ),
            // "Ben" + 10
            Arguments.of("Concatenation",
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
        Environment.Variable _variable = new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false);
        Environment.Variable _object = new Environment.Variable("object", "object", OBJECT_TYPE, false);
        Environment.Variable _field = new Environment.Variable("field", "field", Environment.Type.INTEGER, false);

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
                init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(_variable)),
                "variable"
            ),
            // object.field
            Arguments.of("Field",
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
        Environment.Function _print = new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL);
        Environment.Function _func = new Environment.Function("func", "func", Arrays.asList(), Environment.Type.INTEGER);
        Environment.Function _slice = Environment.Type.STRING.getFunction("slice", 2);

        return Stream.of(
            //Function (6)
            //Zero Arguments: function()
            //One Argument: function(1)
            //Multiple Arguments: function(1, 2, 3)
            //Method: object.method(1, 2, 3)
            //Function JVM Name: name()
            //String Slice: "string".slice(1, 5)

            // func()
            Arguments.of("Zero Arguments",
                init(new Ast.Expression.Function(Optional.empty(),"func", Arrays.asList()), ast -> ast.setFunction(_func)),
                "func()"
            ),
            // print("Hello, World!")
            Arguments.of("Print",
                init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                    init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING)))
                ),ast -> ast.setFunction(_print)),
                "System.out.println(\"Hello, World!\")"
            ),
            // "string".slice(1, 10)
            Arguments.of("String Slice",
                init(new Ast.Expression.Function(
                    Optional.of(
                        init(new Ast.Expression.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
                    ),
                    "slice",
                    Arrays.asList(
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                    )
                ), ast -> ast.setFunction(_slice)),
                "\"string\".substring(1, 10)"
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
