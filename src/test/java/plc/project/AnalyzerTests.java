package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tests have been provided for a few selective parts of the AST, and are not
 * exhaustive. You should add additional tests for the remaining parts and make
 * sure to handle all of the cases defined in the specification which have not
 * been tested here.
 */
public final class AnalyzerTests {

    private static final Environment.Type OBJECT_TYPE = new Environment.Type("ObjectType", "ObjectType", init(new Scope(null), scope -> {
        scope.defineVariable("field", "field", Environment.Type.INTEGER, false, Environment.NIL);
        scope.defineFunction("method", "method", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER, args -> Environment.NIL);
    }));

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testSource(String test, Ast.Source ast, Ast.Source expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            expected.getFields().forEach(field -> Assertions.assertEquals(field.getVariable(), analyzer.scope.lookupVariable(field.getName())));
            expected.getMethods().forEach(method -> Assertions.assertEquals(method.getFunction(), analyzer.scope.lookupFunction(method.getName(), method.getParameters().size())));
        }
    }
    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of("Valid Main",
                // DEF main(): Integer DO RETURN 0; END
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        )
                    )
                ),
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                            new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                    )
                )
            ),
            // LET value: Boolean = TRUE;
            // DEF main(): Integer
            //   DO RETURN value; END
            Arguments.of("Invalid Return",
                new Ast.Source(
                    Arrays.asList(
                        new Ast.Field("value","Boolean", false, Optional.of(new Ast.Expression.Literal(true)))
                    ),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                            new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "value")))
                        )
                    )
                ),
                null
            ),
            // DEF main()
            //   DO RETURN 0; END
            Arguments.of("Missing Return Type for Main",
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        )
                    )
                ),
                null
            ),
            // DEF main(): String
            //   DO RETURN 0; END
            Arguments.of("Invalid Return Type for Main",
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        )
                    )
                ),
                null
            ),
            // DEF main(): Str
            //   DO RETURN 0; END
            Arguments.of("Invalid Return Type",
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Str"), Arrays.asList(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        )
                    )
                ),
                null
            ),
            // LET num: Integer = 1;
            // DEF main(): Integer
            //   DO print(num + 1.0); END
            Arguments.of("Invalid Global Use",
                new Ast.Source(
                    Arrays.asList(
                        new Ast.Field("num","Integer", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))
                    ),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                            new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                new Ast.Expression.Binary("+",
                                    new Ast.Expression.Access(Optional.empty(), "num"),
                                    new Ast.Expression.Literal(BigDecimal.ONE)
                                )
                            )))
                        ))
                    )
                ),
                null
            ),
            // DEF main()
            //   DO print("Hello, World!"); END
            Arguments.of("Invalid Return Type",
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                            new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                new Ast.Expression.Literal("Hello, World!")
                            )))
                        ))
                    )
                ),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testField(String test, Ast.Field ast, Ast.Field expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
            Arguments.of("Declaration",
                // LET name: Decimal;
                new Ast.Field("name","Decimal", false, Optional.empty()),
                init(new Ast.Field("name","Decimal", false, Optional.empty()),ast ->
                    ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, false, Environment.NIL)))
            ),
            Arguments.of("Initialization",
                // LET name: Integer = 1;
                new Ast.Field("name","Integer", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                init(new Ast.Field("name","Integer", false, Optional.of(
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                    )),ast ->
                    ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL))
                )
            ),
            Arguments.of("Unknown Type",
                // LET name: Unknown;
                new Ast.Field("name","Unknown", false, Optional.empty()),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testMethod(String test, Ast.Method ast, Ast.Method expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getFunction(), analyzer.scope.lookupFunction(expected.getName(), expected.getParameters().size()));
        }
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
            Arguments.of("Main",
                // DEF main(): Integer DO RETURN 0; END
                new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                    new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                ),
                init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                    new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
            ),
            Arguments.of("Hello World",
                // DEF main(): Integer DO print("Hello, World!"); END
                new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                    new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                        new Ast.Expression.Literal("Hello, World!")
                    )))
                )),
                init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                    new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                        init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                    )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
            ),
            Arguments.of("Return Type Mismatch",
                // DEF increment(num: Integer): Decimal DO RETURN num + 1; END
                new Ast.Method("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Decimal"), Arrays.asList(
                    new Ast.Statement.Return(new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Literal(BigInteger.ONE)
                    ))
                )),
                null
            ),
            Arguments.of("No Explicit Return Type",
                // DEF empty() DO END
                new Ast.Method("empty", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList()),
                init(new Ast.Method("empty", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList()), ast ->
                    ast.setFunction(new Environment.Function("empty", "empty", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL)))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testExpressionStatement(String test, Ast.Statement.Expression ast, Ast.Statement.Expression expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
            Arguments.of("Function",
                // print(1);
                new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                    new Ast.Expression.Literal(BigInteger.ONE)
                ))),
                new Ast.Statement.Expression(
                    init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                    )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                )

            ),
            Arguments.of("Literal",
                // 1;
                new Ast.Statement.Expression(
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Ast.Statement.Declaration expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }
    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
            Arguments.of("Declaration",
                // LET name: Integer;
                new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> {
                    ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL));
                })
            ),
            Arguments.of("Initialization",
                // LET name = 1;
                new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL)))
            ),
            Arguments.of("Missing Type",
                // LET name;
                new Ast.Statement.Declaration("name", Optional.empty(), Optional.empty()),
                null
            ),
            Arguments.of("Unknown Type",
                // LET name: Unknown;
                new Ast.Statement.Declaration("name", Optional.of("Unknown"), Optional.empty()),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAssignmentStatement(String test, Ast.Statement.Assignment ast, Ast.Statement.Assignment expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL);
            scope.defineVariable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
            Arguments.of("Variable",
                // variable = 1;
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "variable"),
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                new Ast.Statement.Assignment(
                    init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL))),
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                )
            ),
            Arguments.of("Invalid Type",
                // variable = "string";
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "variable"),
                    new Ast.Expression.Literal("string")
                ),
                null
            ),
            Arguments.of("Field",
                // object.field = 1;
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                new Ast.Statement.Assignment(
                    init(new Ast.Expression.Access(Optional.of(
                        init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                    ), "field"), ast -> ast.setVariable(new Environment.Variable("field", "field", Environment.Type.INTEGER, false, Environment.NIL))),
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatement(String test, Ast.Statement.If ast, Ast.Statement.If expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
            Arguments.of("Valid Condition",
                // IF TRUE DO print(1); END
                new Ast.Statement.If(
                    new Ast.Expression.Literal(Boolean.TRUE),
                    Arrays.asList(new Ast.Statement.Expression(
                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                            new Ast.Expression.Literal(BigInteger.ONE)
                        ))
                    )),
                    Arrays.asList()
                ),
                new Ast.Statement.If(
                    init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                    Arrays.asList(new Ast.Statement.Expression(
                        init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                            init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                    ),
                    Arrays.asList()
                )
            ),
            Arguments.of("Invalid Condition",
                // IF "FALSE" DO print(1); END
                new Ast.Statement.If(
                    new Ast.Expression.Literal("FALSE"),
                    Arrays.asList(new Ast.Statement.Expression(
                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                            new Ast.Expression.Literal(BigInteger.ONE)
                        ))
                    )),
                    Arrays.asList()
                ),
                null
            ),
            Arguments.of("Invalid Statement",
                // IF TRUE DO print(9223372036854775807); END
                new Ast.Statement.If(
                    new Ast.Expression.Literal(Boolean.TRUE),
                    Arrays.asList(new Ast.Statement.Expression(
                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                            new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE))
                        ))
                    )),
                    Arrays.asList()
                ),
                null
            ),
            Arguments.of("Empty Statements",
                // IF TRUE DO END
                new Ast.Statement.If(
                    new Ast.Expression.Literal(Boolean.TRUE),
                    Arrays.asList(),
                    Arrays.asList()
                ),
                null
            )
        );
    }

    @Test
    public void testFor() {
        // FOR (num = 1; num < 5; num = num + 1) function(num); END
        Scope scope = new Scope(null);
        scope.defineFunction("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER, args -> Environment.NIL);
        scope.defineVariable("num", "num", Environment.Type.INTEGER, false, Environment.NIL);

        Ast.Statement.Assignment init = new Ast.Statement.Assignment(
            new Ast.Expression.Access(Optional.empty(), "num"),
            new Ast.Expression.Literal(BigInteger.ONE));

        Ast.Expression.Binary cond = new Ast.Expression.Binary(
            "<",
            new Ast.Expression.Access(Optional.empty(), "num"),
            new Ast.Expression.Literal(BigInteger.valueOf(5)));

        Ast.Statement.Assignment incr = new Ast.Statement.Assignment(
            new Ast.Expression.Access(Optional.empty(), "num"),
            new Ast.Expression.Binary(
                "+",
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Literal(BigInteger.ONE)));

        Ast.Statement.For astFor = new Ast.Statement.For(
            init,
            cond,
            incr,
            Arrays.asList(
                new Ast.Statement.Expression(
                    new Ast.Expression.Function(
                        Optional.empty(),
                        "function",
                        Arrays.asList(new Ast.Expression.Access(Optional.empty(), "num"))
                    )
                )
            )
        );
        Ast.Statement.For expected = new Ast.Statement.For(
            init,
            cond,
            incr,
            Arrays.asList(
                new Ast.Statement.Expression(
                    init(
                        new Ast.Expression.Function(
                            Optional.empty(),
                            "function",
                            Arrays.asList(
                                init(
                                    new Ast.Expression.Access(Optional.empty(), "num"),
                                    ast -> ast.setVariable(
                                        new Environment.Variable(
                                            "num",
                                            "num",
                                            Environment.Type.INTEGER,
                                            false,
                                            Environment.NIL
                                        )
                                    )
                                )
                            )
                        ),
                        ast -> ast.setFunction(
                            new Environment.Function(
                                "function",
                                "function",
                                Arrays.asList(Environment.Type.INTEGER),
                                Environment.Type.INTEGER,
                                args -> Environment.NIL
                            )
                        )
                    )
                )
            )
        );

        test(astFor, expected, scope);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testWhileStatement(String test, Ast.Statement.While ast, Ast.Statement.While expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
            // WHILE TRUE DO END
            Arguments.of("Valid Condition",
                new Ast.Statement.While(new Ast.Expression.Literal(Boolean.TRUE), Arrays.asList()),
                new Ast.Statement.While(
                    init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)), Arrays.asList()
                )
            ),
            // WHILE 0 DO END
            Arguments.of("Invalid Condition",
                new Ast.Statement.While(new Ast.Expression.Literal(BigInteger.ZERO), Arrays.asList()),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpression(String test, Ast.Expression.Literal ast, Ast.Expression.Literal expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
            Arguments.of("Boolean",
                // TRUE
                new Ast.Expression.Literal(true),
                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN))
            ),
            Arguments.of("Integer Valid",
                // 2147483647
                new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                init(new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)), ast -> ast.setType(Environment.Type.INTEGER))
            ),
            Arguments.of("Integer Invalid",
                // 9223372036854775807
                new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testGroupExpression(String test, Ast.Expression.Group ast, Ast.Expression.Group expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
            Arguments.of("Group Literal",
                // (1)
                new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)),
                null
            ),
            Arguments.of("Group Binary",
                // (1 + 10)
                new Ast.Expression.Group(
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigInteger.TEN)
                    )
                ),
                init(new Ast.Expression.Group(
                    init(new Ast.Expression.Binary("+",
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                    ), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.INTEGER))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpression(String test, Ast.Expression.Binary ast, Ast.Expression.Binary expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
            Arguments.of("Logical AND Valid",
                // TRUE AND FALSE
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(Boolean.TRUE),
                    new Ast.Expression.Literal(Boolean.FALSE)
                ),
                init(new Ast.Expression.Binary("AND",
                    init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                    init(new Ast.Expression.Literal(Boolean.FALSE), ast -> ast.setType(Environment.Type.BOOLEAN))
                ), ast -> ast.setType(Environment.Type.BOOLEAN))
            ),
            Arguments.of("Logical AND Invalid",
                // TRUE AND "FALSE"
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(Boolean.TRUE),
                    new Ast.Expression.Literal("FALSE")
                ),
                null
            ),
            Arguments.of("String Concatenation",
                // "Ben" + 10
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal("Ben"),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                init(new Ast.Expression.Binary("+",
                    init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                    init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.STRING))
            ),
            Arguments.of("Integer Addition",
                // 1 + 10
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                init(new Ast.Expression.Binary("+",
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                    init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.INTEGER))
            ),
            Arguments.of("Integer Decimal Addition",
                // 1 + 1.0
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigDecimal.ONE)
                ),
                null
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAccessExpression(String test, Ast.Expression.Access ast, Ast.Expression.Access expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL);
            scope.defineVariable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
            Arguments.of("Variable",
                // variable
                new Ast.Expression.Access(Optional.empty(), "variable"),
                init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL)))
            ),
            Arguments.of("Field",
                // object.field
                new Ast.Expression.Access(Optional.of(
                    new Ast.Expression.Access(Optional.empty(), "object")
                ), "field"),
                init(new Ast.Expression.Access(Optional.of(
                    init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                ), "field"), ast -> ast.setVariable(new Environment.Variable("field", "field", Environment.Type.INTEGER, false, Environment.NIL)))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunctionExpression(String test, Ast.Expression.Function ast, Ast.Expression.Function expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineFunction("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL);
            scope.defineFunction("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER, args -> Environment.NIL);
            scope.defineVariable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
            Arguments.of("Function",
                // function()
                new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()),
                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()), ast ->
                    ast.setFunction(new Environment.Function("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
            ),
            Arguments.of("Function Valid Arg",
                // function(1)
                new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE))),
                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                )), ast ->
                    ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER, args -> Environment.NIL)))
            ),
            Arguments.of("Function Invalid Arg",
                // function(1.0)
                new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(new Ast.Expression.Literal(BigDecimal.ONE))),
                null
            ),
            Arguments.of("Method",
                // object.method()
                new Ast.Expression.Function(Optional.of(
                    new Ast.Expression.Access(Optional.empty(), "object")
                ), "method", Arrays.asList()),
                init(new Ast.Expression.Function(Optional.of(
                    init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                ), "method", Arrays.asList()), ast -> ast.setFunction(new Environment.Function("method", "method", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER, args -> Environment.NIL)))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testRequireAssignable(String test, Environment.Type target, Environment.Type type, boolean success) {
        if (success) {
            Assertions.assertDoesNotThrow(() -> Analyzer.requireAssignable(target, type));
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> Analyzer.requireAssignable(target, type));
        }
    }
    private static Stream<Arguments> testRequireAssignable() {
        return Stream.of(
            Arguments.of("Integer to Integer", Environment.Type.INTEGER, Environment.Type.INTEGER, true),
            Arguments.of("Integer to Decimal", Environment.Type.DECIMAL, Environment.Type.INTEGER, false),
            Arguments.of("Integer to Comparable", Environment.Type.COMPARABLE, Environment.Type.INTEGER,  true),
            Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
            Arguments.of("Any to Integer", Environment.Type.INTEGER, Environment.Type.ANY, false)
        );
    }

    /**
     * Helper function for tests. If {@param expected} is {@code null}, analysis
     * is expected to throw a {@link RuntimeException}.
     */
    private static <T extends Ast> Analyzer test(T ast, T expected, Scope scope) {
        Analyzer analyzer = new Analyzer(scope);
        if (expected != null) {
            analyzer.visit(ast);
            Assertions.assertEquals(expected, ast);
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> analyzer.visit(ast));
        }
        return analyzer;
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
