package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        Scope scope = new Scope(null);
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            // DEF main() DO
            //   RETURN 0;
            // END
            Arguments.of("Main",
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(new Ast.Method(
                        "main",
                        Arrays.asList(),
                        Arrays.asList(
                            new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                        ))
                    )
                ),
                BigInteger.ZERO
            ),

            // LET x = 1;
            // LET y = 10;
            // DEF main() DO
            //   x + y;
            // END
            // Note: x + y is evaluated but not returned (no RETURN statement)
            Arguments.of("Fields & No Return",
                new Ast.Source(
                    Arrays.asList(
                        new Ast.Field("x", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        new Ast.Field("y", false, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                    ),
                    Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Binary("+",
                            new Ast.Expression.Access(Optional.empty(), "x"),
                            new Ast.Expression.Access(Optional.empty(), "y"))
                        )
                    )))
                ), Environment.NIL.getValue()
            ),

            Arguments.of("Foo", FooTestData.tree_main, Environment.NIL.getValue())
        );
    }

    @Test
    void testMethodCallsStatement() {
        // DEF f(x) DO
        //   log(x);
        // END
        // DEF g(y) DO
        //   log(y);
        //   f(y + 1);
        // END
        // DEF h(z) DO
        //   log(z);
        //   g(z + 1);
        // END
        // DEF main() DO
        //   f(0);
        //   g(1);
        //   h(2);
        // END
        Scope scope = new Scope(null);
        StringWriter writer = new StringWriter();
        scope.defineFunction(
            "log",
            1,
            args -> {
                writer.write(String.valueOf(args.get(0).getValue()));
                return args.get(0);
            }
        );

        Ast ast = new Ast.Source(
            Arrays.asList(),
            Arrays.asList(

                // DEF f(x) DO
                //   log(x);
                // END
                new Ast.Method(
                    "f",
                    Arrays.asList("x"),
                    Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "log",
                            Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x"))
                        ))
                    )
                ),

                // DEF g(y) DO
                //   log(y);
                //   f(y + 1);
                // END
                new Ast.Method(
                    "g",
                    Arrays.asList("y"),
                    Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "log",
                            Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y"))
                        )),

                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "f",
                            Arrays.asList(new Ast.Expression.Binary( "+",
                                new Ast.Expression.Access(Optional.empty(), "y"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                            ))
                        ))
                    )
                ),

                // DEF h(z) DO
                //   log(z);
                //   g(z + 1);
                // END
                new Ast.Method(
                    "h",
                    Arrays.asList("z"),
                    Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "log",
                            Arrays.asList(new Ast.Expression.Access(Optional.empty(), "z"))
                        )),

                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "g",
                            Arrays.asList(new Ast.Expression.Binary( "+",
                                new Ast.Expression.Access(Optional.empty(), "z"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                            ))
                        ))
                    )
                ),

                // DEF main() DO
                //   f(0);
                //   g(1);
                //   h(2);
                // END
                new Ast.Method(
                    "main",
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "f",
                            Arrays.asList(new Ast.Expression.Literal(BigInteger.ZERO))
                        )),
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "g",
                            Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE))
                        )),
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "h",
                            Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(2)))
                        ))
                    )
                )
            )
        );

        test(
            ast,
            Environment.NIL.getValue(),
            scope);
        String log = writer.toString();
        Assertions.assertEquals("012234", log);
    }

    @ParameterizedTest
    @MethodSource
    void testField(String test, Ast.Field ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
            // TODO: add test with CONST
            // LET name;
            Arguments.of("Declaration",
                new Ast.Field("name", false, Optional.empty()),
                Environment.NIL.getValue()
            ),

            // LET name = 1;
            Arguments.of("Initialization",
                new Ast.Field("name", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                BigInteger.ONE
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethod(String test, Ast.Method ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
            // DEF main() DO
            //   RETURN 0;
            // END
            Arguments.of("Main",
                new Ast.Method("main", Arrays.asList(), Arrays.asList(
                    new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                )),
                Arrays.asList(),
                BigInteger.ZERO
            ),

            // DEF square(x) DO
            //   RETURN x * x;
            // END
            Arguments.of("Arguments",
                new Ast.Method("square", Arrays.asList("x"), Arrays.asList(
                    new Ast.Statement.Return(new Ast.Expression.Binary("*",
                        new Ast.Expression.Access(Optional.empty(), "x"),
                        new Ast.Expression.Access(Optional.empty(), "x")
                    ))
                )),
                Arrays.asList(Environment.create(BigInteger.TEN)),
                BigInteger.valueOf(100)
            )
        );
    }

    @Test
    void testExpressionPrintStatement() {
        // print("Hello, World!");
        // -> NIL,
        //    %System.out.println("Hello, World!");%
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(
                new Ast.Statement.Expression(
                    new Ast.Expression.Function(
                        Optional.empty(),
                        "print",
                        Arrays.asList(new Ast.Expression.Literal("Hello, World!")))
                ),
                Environment.NIL.getValue(),
                new Scope(null)
            );
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
            // LET name;
            Arguments.of("Declaration",
                new Ast.Statement.Declaration("name", Optional.empty()),
                Environment.NIL.getValue()
            ),

            // LET name = 1;
            Arguments.of("Initialization",
                new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                BigInteger.ONE
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testVariableAssignmentStatement(String test, Ast.Statement.Assignment ast, String name, Scope scope, Object expected) {
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable(name).getValue().getValue());
    }

    private static Stream<Arguments> testVariableAssignmentStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", false, Environment.create("variable"));

        return Stream.of(
            // variable = 1;
            Arguments.of("Assignment",
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(),"variable"),
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                "variable",
                scope,
                BigInteger.ONE
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFieldAssignmentStatement(String test, Ast.Statement.Assignment ast, String name, Scope object, Scope root, Object expected) {
        test(ast, Environment.NIL.getValue(), root);
        Assertions.assertEquals(expected, object.lookupVariable(name).getValue().getValue());
    }

    private static Stream<Arguments> testFieldAssignmentStatement() {
        Scope root = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create("object.field"));
        root.defineVariable("object", false, new Environment.PlcObject(object, "object"));

        return Stream.of(
            // object.field = 10;
            Arguments.of("Field Assignment",
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.of(
                        new Ast.Expression.Access(Optional.empty(), "object")),
                        "field"
                    ),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                "field",
                object,
                root,
                BigInteger.TEN
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", false, Environment.NIL);

        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
            // IF TRUE DO
            //   num = 1;
            // END
            Arguments.of("True Condition",
                new Ast.Statement.If(
                    new Ast.Expression.Literal(true),
                    Arrays.asList(
                        new Ast.Statement.Assignment(
                            new Ast.Expression.Access(Optional.empty(),"num"),
                            new Ast.Expression.Literal(BigInteger.ONE)
                        )
                    ),

                    Arrays.asList()
                ),
                BigInteger.ONE
            ),
            // IF FALSE DO
            //   ELSE
            //     num = 10;
            // END
            Arguments.of("False Condition",
                new Ast.Statement.If(
                    new Ast.Expression.Literal(false),
                    Arrays.asList(),

                    Arrays.asList(
                        new Ast.Statement.Assignment(
                            new Ast.Expression.Access(Optional.empty(),"num"),
                            new Ast.Expression.Literal(BigInteger.TEN)
                        )
                    )
                ),
                BigInteger.TEN
            )
        );
    }


    @Test
    void testForStatement() {
        // FOR (num = 0; num < 5; num = num + 1)
        //   sum = sum + num;
        // END
        Scope scope = new Scope(null);
        scope.defineVariable("sum", false, Environment.create(BigInteger.ZERO));
        scope.defineVariable("num", false, Environment.NIL);

        test(new Ast.Statement.For(
            new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Literal(BigInteger.ZERO)),

            new Ast.Expression.Binary("<",
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Literal(BigInteger.valueOf(5))),

            new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(), "num"),
                    new Ast.Expression.Literal(BigInteger.ONE))),

            Arrays.asList(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"sum"),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(),"sum"),
                    new Ast.Expression.Access(Optional.empty(),"num")
                )
            ))
        ), Environment.NIL.getValue(), scope);

        // you can evaluate the state of each variable in scope one at a time, here is an example:
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("sum").getValue().getValue());
        Assertions.assertEquals(BigInteger.valueOf(5), scope.lookupVariable("num").getValue().getValue());

        // you can also build a list of the expected results, comparing all as a group
        // expected is what the test case expects to be produced by your solution
        ArrayList<BigInteger> expected = new ArrayList<BigInteger>(2);
        expected.add(BigInteger.TEN);
        expected.add(BigInteger.valueOf(5));

        // actual is the result actually produced by your solution
        ArrayList<Object> actual = new ArrayList<Object>(2);
        actual.add(scope.lookupVariable("sum").getValue().getValue());
        actual.add(scope.lookupVariable("num").getValue().getValue());

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testForStatement2() {
        // FOR (num = 0; num < 5; )
        //   sum = sum + num;
        //   num = num + 1;
        // END

        // scope = {num = NIL, sum = 0}
        Scope scope = new Scope(null);
        scope.defineVariable("sum", false, Environment.create(BigInteger.ZERO));
        scope.defineVariable("num", false, Environment.NIL);

        // FOR (num = 0; num < 5; )
        test(new Ast.Statement.For(
            new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Literal(BigInteger.ZERO)),

            new Ast.Expression.Binary("<",
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Literal(BigInteger.valueOf(5))),

            null,

            Arrays.asList(
                //   sum = sum + num;
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(),"sum"),
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(),"sum"),
                        new Ast.Expression.Access(Optional.empty(),"num"))),

                //   num = num + 1;
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(),"num"),
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Literal(BigInteger.ONE)))
            )
        ), Environment.NIL.getValue(), scope);

        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("sum").getValue().getValue());
        Assertions.assertEquals(BigInteger.valueOf(5), scope.lookupVariable("num").getValue().getValue());
    }

    @Test
    void testForStatement3() {
        // FOR (; num < 5; num = num + 1)
        //   sum = sum + num;
        // END

        // scope = {num = 0, sum = 0}
        Scope scope = new Scope(null);
        scope.defineVariable("num", false, Environment.create(BigInteger.ZERO));
        scope.defineVariable("sum", false, Environment.create(BigInteger.ZERO));

        // FOR (; num < 5; num = num + 1)
        test(new Ast.Statement.For(
            null,

            new Ast.Expression.Binary("<",
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Literal(BigInteger.valueOf(5))),

            new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(), "num"),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(),"num"),
                    new Ast.Expression.Literal(BigInteger.ONE))),


            Arrays.asList(
                //   sum = sum + num;
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(),"sum"),
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(),"sum"),
                        new Ast.Expression.Access(Optional.empty(),"num")))
            )
        ), Environment.NIL.getValue(), scope);

        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("sum").getValue().getValue());
        Assertions.assertEquals(BigInteger.valueOf(5), scope.lookupVariable("num").getValue().getValue());
    }

    @Test
    void testWhileStatement() {
        // WHILE num < 10 DO
        //   num = num + 1;
        // END
        Scope scope = new Scope(null);
        scope.defineVariable("num", false, Environment.create(BigInteger.ZERO));

        test(new Ast.Statement.While(
            new Ast.Expression.Binary("<",
                new Ast.Expression.Access(Optional.empty(),"num"),
                new Ast.Expression.Literal(BigInteger.TEN)
            ),

            Arrays.asList(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"num"),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(),"num"),
                    new Ast.Expression.Literal(BigInteger.ONE)
                )
            ))
        ),Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
            Arguments.of("Nil", new Ast.Expression.Literal(null), Environment.NIL.getValue()), //remember, special case
            Arguments.of("Boolean", new Ast.Expression.Literal(true), true),
            Arguments.of("Integer", new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE),
            Arguments.of("Decimal", new Ast.Expression.Literal(BigDecimal.ONE), BigDecimal.ONE),
            Arguments.of("Character", new Ast.Expression.Literal('c'), 'c'),
            Arguments.of("String", new Ast.Expression.Literal("string"), "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
            Arguments.of("Literal", new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)), BigInteger.ONE),
            Arguments.of("Binary",
                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                )),
                BigInteger.valueOf(11)
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
            Arguments.of("And",
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(true),
                    new Ast.Expression.Literal(false)
                ),
                false
            ),
            Arguments.of("Or (Short Circuit)",
                new Ast.Expression.Binary("OR",
                    new Ast.Expression.Literal(true),
                    new Ast.Expression.Access(Optional.empty(), "undefined")
                ),
                true
            ),
            Arguments.of("And (Short Circuit)",
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(false),
                    new Ast.Expression.Access(Optional.empty(), "undefined")
                ),
                false
            ),
            Arguments.of("Less Than",
                new Ast.Expression.Binary("<",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                true
            ),
            Arguments.of("Greater Than or Equal",
                new Ast.Expression.Binary(">=",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                false
            ),
            Arguments.of("Equal",
                new Ast.Expression.Binary("==",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                false
            ),
            Arguments.of("Concatenation",
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal("a"),
                    new Ast.Expression.Literal("b")
                ),
                "ab"
            ),
            Arguments.of("RHS String",
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal("10")
                ),
                "110"
            ),
            Arguments.of("String Compare True",
                new Ast.Expression.Binary("<",
                    new Ast.Expression.Literal("abc"),
                    new Ast.Expression.Literal("abd")
                ),
                true
            ),
            Arguments.of("String Compare False",
                new Ast.Expression.Binary(">",
                    new Ast.Expression.Literal("abc"),
                    new Ast.Expression.Literal("abd")
                ),
                false
            ),
            Arguments.of("Addition",
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                BigInteger.valueOf(11)
            ),
            Arguments.of("Division",
                new Ast.Expression.Binary("/",
                    new Ast.Expression.Literal(new BigDecimal("1.2")),
                    new Ast.Expression.Literal(new BigDecimal("3.4"))
                ),
                new BigDecimal("0.4")
            ),
            Arguments.of("Division by Zero",
                new Ast.Expression.Binary("/",
                    new Ast.Expression.Literal(new BigInteger("1")),
                    new Ast.Expression.Literal(new BigInteger("0"))
                ),
                null
            ),
            Arguments.of("Nil Equals",
                new Ast.Expression.Binary("==",
                    new Ast.Expression.Literal(null),
                    new Ast.Expression.Literal(null)
                ),
                true
            ),
            Arguments.of("Distinct Types",
                new Ast.Expression.Binary("!=",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal("1")
                ),
                true
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, Ast ast, Object expected) {
        // TODO: Canvas test page says this should be (Variable: variable, scope = {variable = 1}), (Field: object.field, scope = {object = PlcObject{field = 1}})
        // variable
        // -> Scope{ parent=null,
        //           variables={"variable" Scope{}},
        //           functions={}
        //    }
        Scope scope = new Scope(null);
        scope.defineVariable("variable", false, Environment.create("variable"));

        // object.field
        // -> Scope{ parent=null,
        //           variables={ "variable": {"variable Scope{}},
        //                       "object":  Scope{ parent=null,
        //                                         variables={ "field": {"object.field Scope{}} },
        //                                         functions={}
        //                      }
        //           },
        //           functions={}
        //    }
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create("object.field"));
        scope.defineVariable("object", false, new Environment.PlcObject(object, "object"));

        test(ast, expected, scope);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
            Arguments.of("Variable",
                new Ast.Expression.Access(Optional.empty(), "variable"),
                "variable"
            ),
            Arguments.of("Field",
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
                "object.field"
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, Ast ast, Object expected) {
        // function()
        // -> 1,
        //    Scope{ parent=null,
        //           variables={},
        //           functions={ "function/0" }
        //    }
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));

        // log(2)
        // -> 2,
        //    Scope{ parent=null,
        //           variables={},
        //           functions={ "log/1" }
        //    }
        // Stub: log(1), scope = {log/1 = ...} (returns the argument, 1), changed arg to 2 for clarity
        scope.defineFunction("log", 1, args -> {
            BigInteger arg1 = (BigInteger) args.get(0).getValue();
            return Environment.create(arg1);
        });

        // object.method()
        // -> Scope{ parent=null,
        //           variables={ "object": Scope{ parent=null,
        //                                        variables={},
        //                                        functions={ "method/1" }
        //                                 }
        //           },
        //           functions={ "function/0" }
        //    }
        Scope object = new Scope(null);
        object.defineFunction("method", 1, args -> Environment.create("object.method"));
        scope.defineVariable("object", false, new Environment.PlcObject(object, "object"));

        test(ast, expected, scope);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
            Arguments.of("Function",
                new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()),
                "function"
            ),
            Arguments.of("Log",
                new Ast.Expression.Function(Optional.empty(), "log", Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(2)))),
                BigInteger.valueOf(2)
            ),
            Arguments.of("Method",
                new Ast.Expression.Function(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "method", Arrays.asList()),
                "object.method"
            ),
            Arguments.of("Print",
                new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(new Ast.Expression.Literal("Hello, World!"))),
                Environment.NIL.getValue()
            )
        );
    }

    @Test
    void testFunctionScope() {
        // LET x = 1;
        // LET y = 2;
        // LET z = 3;
        // DEF f(z) DO
        //   RETURN x + y + z;
        // END
        // DEF main() DO
        //   LET y = 4;
        //   RETURN f(5);
        // END

        Scope scope = new Scope(null);
        StringWriter writer = new StringWriter();
        scope.defineFunction(
            "log",
            1,
            args -> {
                writer.write(String.valueOf(args.get(0).getValue()));
                return args.get(0);
            }
        );

        Ast ast = new Ast.Source(
            // LET x = 1;
            // LET y = 2;
            // LET z = 3;
            Arrays.asList(
                new Ast.Field("x", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                new Ast.Field("y", false, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2)))),
                new Ast.Field("z", false, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(3))))
            ),
            Arrays.asList(

                // DEF f(z) DO
                //   log(x + y + z);
                //   RETURN x + y + z;
                // END
                new Ast.Method(
                    "f",
                    Arrays.asList("z"),
                    Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "log",
                            Arrays.asList(
                                new Ast.Expression.Binary("+",
                                    new Ast.Expression.Access(Optional.empty(), "x"),
                                    new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "y"),
                                        new Ast.Expression.Access(Optional.empty(), "z")
                                    )
                                )
                            )
                        )),
                        new Ast.Statement.Return(
                            new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "x"),
                                new Ast.Expression.Binary("+",
                                    new Ast.Expression.Access(Optional.empty(), "y"),
                                    new Ast.Expression.Access(Optional.empty(), "z")
                                )
                            )
                        )
                    )
                ),

                // DEF main() DO
                //   LET y = 40;
                //   RETURN f(10);
                // END
                new Ast.Method(
                    "main",
                    Arrays.asList(),
                    Arrays.asList(
                        new Ast.Statement.Declaration(
                            "y",
                            Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(40)))
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function(
                            Optional.empty(),
                            "f",
                            Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(10)))
                        ))
                    )
                )
            )
        );

        test(
            ast,
            Environment.NIL.getValue(),
            scope);
        String log = writer.toString();
        Assertions.assertEquals("13", log);
    }

    @Test
    void testLogarithmExpression() {
        Scope scope = new Scope(null);
        test(new Ast.Expression.Function(Optional.empty(),
            "logarithm",
            Arrays.asList(new Ast.Expression.Literal(BigDecimal.valueOf(Math.E)))),
            BigDecimal.valueOf(1.0),
            scope
        );
    }

    @Test
    void testLogarithmExpressionError() {
        Scope scope = new Scope(null);
        test(new Ast.Expression.Function(Optional.empty(),
                "logarithm",
                Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(3)))),
            null,
            scope
        );
    }

    @Test
    void testConversionExpression() {
        Scope scope = new Scope(null);
        test(new Ast.Expression.Function(
            Optional.empty(),
            "converter",
            Arrays.asList(
                new Ast.Expression.Literal(BigInteger.valueOf(13)),
                new Ast.Expression.Literal(BigInteger.valueOf(2)))
            ),
            "1101",
            scope
        );
    }

    @Test
    void testConversionExpressionError() {
        Scope scope = new Scope(null);
        test(new Ast.Expression.Function(
            Optional.empty(),
            "logarithm",
            Arrays.asList(
                new Ast.Expression.Literal(13),
                new Ast.Expression.Literal(2))
            ),
            null,
            scope
        );
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }
}
