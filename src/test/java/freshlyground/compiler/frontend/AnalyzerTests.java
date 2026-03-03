package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Scope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class AnalyzerTests {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFieldHappyPath(String test, Ast.Field ast, Environment.Variable expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getVariable(ast));
        Assertions.assertEquals(expected, analyzer.getScope().lookupVariable(expected.getName()));
    }

    private static Stream<Arguments> testFieldHappyPath() {
        return Stream.of(
            Arguments.of("Declaration",
                // LET name: Decimal;
                new Ast.Field("name","Decimal", false, Optional.empty()),
                new Environment.Variable("name", Environment.Type.DECIMAL, false)
            ),
            Arguments.of("Initialization",
                // LET name: Integer = 1;
                new Ast.Field("name","Integer", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                new Environment.Variable("name", Environment.Type.INTEGER, false)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFieldSadPath(String test, Ast.Field ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    private static Stream<Arguments> testFieldSadPath() {
        return Stream.of(
            // TODO add test for Primitive type
            Arguments.of("Unknown Type",
                // LET name: Unknown;
                new Ast.Field("name","Unknown", false, Optional.empty()),
                "Unknown type: Unknown."
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testMethodHappyPath(String test, Ast.Method ast, Environment.Function expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getFunction(ast));
        Assertions.assertEquals(expected, analyzer.getScope().lookupFunction(expected.name(), expected.parameterTypes().size()));
    }

    private static Stream<Arguments> testMethodHappyPath() {
        return Stream.of(
            Arguments.of("Main",
                // DEF main(): Integer DO
                //   RETURN 0;
                // END
                new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                    new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                ),
                new Environment.Function("main", List.of(), Environment.Type.INTEGER)
            ),
            Arguments.of("Hello World",
                // DEF main(): Integer DO
                //   print("Hello, World!");
                // END
                new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                    new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", List.of(
                        new Ast.Expression.Literal("Hello, World!")
                    )))
                )),
                new Environment.Function("main", List.of(), Environment.Type.INTEGER)
            ),
            Arguments.of("No Explicit Return Type",
                // DEF empty() DO
                // END
                new Ast.Method("empty", List.of(), List.of(), Optional.empty(), List.of()),
                new Environment.Function("empty", List.of(), Environment.Type.NIL)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testMethodSadPath(String test, Ast.Method ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    private static Stream<Arguments> testMethodSadPath() {
        return Stream.of(
            Arguments.of("Return Type Mismatch",
                // DEF increment(num: Integer): Decimal DO RETURN num + 1; END
                new Ast.Method("increment", List.of("num"), List.of("Integer"), Optional.of("Decimal"), List.of(
                    new Ast.Statement.Return(new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Literal(BigInteger.ONE)
                    ))
                )),
                "Type unassignable: Integer -> Decimal"
            )
        );
    }

    // Literally just a wrapper for Ast.Expression.Function, no sad path needed
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testExpressionStatementHappyPath(String test, Ast.Statement.Expression ast, Environment.Function expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getFunction(ast.getExpression()));
        Assertions.assertEquals(expected, analyzer.getScope().lookupFunction(expected.name(), expected.parameterTypes().size()));
    }

    private static Stream<Arguments> testExpressionStatementHappyPath() {
        return Stream.of(
            Arguments.of("Function",
                // print(1);
                new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", List.of(
                    new Ast.Expression.Literal(BigInteger.ONE)
                ))),
                new Environment.Function("print", List.of(Environment.Type.ANY), Environment.Type.NIL)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatementHappyPath(String test, Ast.Statement.Declaration ast, Environment.Variable expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getVariable(ast));
        Assertions.assertEquals(expected, analyzer.getScope().lookupVariable(expected.getName()));
    }

    private static Stream<Arguments> testDeclarationStatementHappyPath() {
        return Stream.of(
            Arguments.of("Declaration",
                // LET name: Integer;
                new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                new Environment.Variable("name", Environment.Type.INTEGER, false)
            ),
            Arguments.of("Initialization",
                // LET name = 1;
                new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                new Environment.Variable("name", Environment.Type.INTEGER, false)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatementSadPath(String test, Ast.Statement.Declaration ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    private static Stream<Arguments> testDeclarationStatementSadPath() {
        return Stream.of(
            // MOVE TO PARSER TESTS
//            Arguments.of("Missing Type",
//                // LET name;
//                new Ast.Statement.Declaration("name", Optional.empty(), Optional.empty()),
//                "Must have declared type or value"
//            ),
            Arguments.of("Unknown Type",
                // LET name: Unknown;
                new Ast.Statement.Declaration("name", Optional.of("Unknown"), Optional.empty()),
                "Unknown type: Unknown."
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAssignmentStatementHappyPath(String test, Ast.Statement.Assignment ast, Environment.Variable expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineVariable("variable", Environment.Type.INTEGER, false);
        analyzer.getScope().defineVariable("object", Environment.Type.ANY, false);
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getVariable(ast.getReceiver()));
    }

    private static Stream<Arguments> testAssignmentStatementHappyPath() {
        return Stream.of(
            Arguments.of("Variable",
                // variable = 1;
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "variable"),
                    new Ast.Expression.Literal(BigInteger.ONE)
                ),
                new Environment.Variable("variable", Environment.Type.INTEGER, false)
            )
            // TODO find new way to do object.field = 1 test
//            Arguments.of("Field",
//                // object.field = 1;
//                new Ast.Statement.Assignment(
//                    new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
//                    new Ast.Expression.Literal(BigInteger.ONE)
//                ),
//                new Environment.Variable("field", Environment.Type.INTEGER, false)
//            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAssignmentStatementSadPath(String test, Ast.Statement.Assignment ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    private static Stream<Arguments> testAssignmentStatementSadPath() {
        return Stream.of(
            Arguments.of("Invalid Type",
                // variable = "string";
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "variable"),
                    new Ast.Expression.Literal("string")
                ),
                "The variable variable is not defined in this scope."
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatementHappyPath(String test, Ast.Statement.If ast) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertDoesNotThrow(() -> analyzer.visit(ast));
    }
    private static Stream<Arguments> testIfStatementHappyPath() {
        return Stream.of(
            Arguments.of("Valid Condition",
                // IF TRUE DO LET x = 1; END
                new Ast.Statement.If(
                    new Ast.Expression.Literal(Boolean.TRUE),
                    List.of(new Ast.Statement.Expression(
                        new Ast.Expression.Function(Optional.empty(), "print", List.of(
                            new Ast.Expression.Literal(BigInteger.ONE)
                        ))
                    )),
                    List.of(new Ast.Statement.Declaration(
                        "x",
                        Optional.empty(),
                        Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                    ))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatementSadPath(String test, Ast.Statement.If ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }
    private static Stream<Arguments> testIfStatementSadPath() {
        return Stream.of(
            Arguments.of("Invalid Condition",
                // IF "FALSE" DO LET x = 1; END
                new Ast.Statement.If(
                    new Ast.Expression.Literal("FALSE"),
                    List.of(new Ast.Statement.Expression(
                        new Ast.Expression.Function(Optional.empty(), "print", List.of(
                            new Ast.Expression.Literal(BigInteger.ONE)
                        ))
                    )),
                    List.of(new Ast.Statement.Declaration(
                        "x",
                        Optional.empty(),
                        Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                    ))
                ),
                "Type unassignable: String -> Boolean"
            ),
            Arguments.of("Empty Statements",
                // IF TRUE DO END
                new Ast.Statement.If(
                    new Ast.Expression.Literal(Boolean.TRUE),
                    List.of(),
                    List.of()
                ),
                "IF block must contain at least one then statement"
            )
        );
    }


    // Maybe consider adding these conditions
    // FOR (; num < 5; num = num + 1) sum = sum + num; END
    // FOR (num = 1; num < 5; num = num + 1) function(num); END
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testForStatementHappyPath(String test, Ast.Statement.For ast) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineVariable("i", Environment.Type.INTEGER, false);
        analyzer.visit(ast);
        Assertions.assertDoesNotThrow(() -> analyzer.visit(ast));
    }
    private static Stream<Arguments> testForStatementHappyPath() {
        return Stream.of(
            // FOR (i = 0; TRUE; i = i + 1) LET x = 1; END
            Arguments.of("Valid Condition",
                new Ast.Statement.For(
                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "i"),
                        new Ast.Expression.Literal(BigInteger.ZERO)),

                    new Ast.Expression.Literal(Boolean.TRUE),

                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "i"),
                        new Ast.Expression.Binary("+",
                            new Ast.Expression.Access(Optional.empty(), "i"),
                            new Ast.Expression.Literal(BigInteger.ONE))),

                    List.of(new Ast.Statement.Declaration(
                        "x",
                        Optional.empty(),
                        Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                    ))
                )
            ),
            // FOR (; TRUE; ) LET x = 1; END
            Arguments.of("Init and Incr Present",
                new Ast.Statement.For(
                    null,
                    new Ast.Expression.Literal(Boolean.TRUE),
                    null,
                    List.of(new Ast.Statement.Declaration(
                        "x",
                        Optional.empty(),
                        Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                    ))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testForStatementSadPath(String test, Ast.Statement.For ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    private static Stream<Arguments> testForStatementSadPath() {
        return Stream.of(
            // FOR (; 0; ) LET x = 1; END
            Arguments.of("Invalid Condition",
                new Ast.Statement.For(null, new Ast.Expression.Literal(BigInteger.ZERO), null, List.of()),
                "Type unassignable: Integer -> Boolean"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testWhileStatementHappyPath(String test, Ast.Statement.While ast) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertDoesNotThrow(() -> analyzer.visit(ast));
    }
    private static Stream<Arguments> testWhileStatementHappyPath() {
        return Stream.of(
            // WHILE TRUE DO END
            Arguments.of("Valid Condition",
                new Ast.Statement.While(new Ast.Expression.Literal(Boolean.TRUE), List.of())
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testWhileStatementSadPath(String test, Ast.Statement.While ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    private static Stream<Arguments> testWhileStatementSadPath() {
        return Stream.of(
            // WHILE 0 DO END
            Arguments.of("Invalid Condition",
                new Ast.Statement.While(new Ast.Expression.Literal(BigInteger.ZERO), List.of()),
                "Type unassignable: Integer -> Boolean"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpressionHappyPath(String test, Ast.Expression.Literal ast, Environment.Type expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getType(ast));
    }
    private static Stream<Arguments> testLiteralExpressionHappyPath() {
        return Stream.of(
            Arguments.of("Nil",
                // NIL
                new Ast.Expression.Literal(null),
                Environment.Type.NIL
            ),
            Arguments.of("Boolean",
                // TRUE
                new Ast.Expression.Literal(true),
                Environment.Type.BOOLEAN
            ),
            Arguments.of("Integer Valid",
                // MAX_INT
                new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                Environment.Type.INTEGER
            ),
            Arguments.of("Integer Valid Min",
                // MIN_INT
                new Ast.Expression.Literal(BigInteger.valueOf(Integer.MIN_VALUE)),
                Environment.Type.INTEGER
            ),
            Arguments.of("Decimal Valid",
                // MAX_DEC
                new Ast.Expression.Literal(BigDecimal.valueOf(Double.MAX_VALUE)),
                Environment.Type.DECIMAL
            ),
            Arguments.of("Decimal Valid Min",
                // MIN_DEC
                new Ast.Expression.Literal(BigDecimal.valueOf(Double.MIN_VALUE)),
                Environment.Type.DECIMAL
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpressionSadPath(String test, Ast.Expression.Literal ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    // Maybe add over and underflow
    // new Ast.Expression.Literal(BigDecimal.valueOf(Double.MIN_VALUE).subtract(BigDecimal.ONE))
    // new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE))
    private static Stream<Arguments> testLiteralExpressionSadPath() {
        return Stream.of(
            Arguments.of("Return Type Mismatch",
                // 9223372036854775807
                new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                "INT Overflow or Underflow"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testGroupExpressionHappyPath(String test, Ast.Expression.Group ast, Environment.Type expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getType(ast));
    }
    private static Stream<Arguments> testGroupExpressionHappyPath() {
        return Stream.of(
            Arguments.of("Group Binary",
                // (1 + 10)
                new Ast.Expression.Group(
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigInteger.TEN)
                    )
                ),
                Environment.Type.INTEGER
            )
        );
    }

//    @ParameterizedTest(name = "{0}")
//    @MethodSource
//    public void testGroupExpressionSadPath(String test, Ast.Expression.Group ast, String expectedMessage) {
//        Analyzer analyzer = new Analyzer();
//        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
//        Assertions.assertEquals(expectedMessage, message);
//    }
//    private static Stream<Arguments> testGroupExpressionSadPath() {
//        return Stream.of(
//            Arguments.of("Group Literal",
//                // (1)
//                new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)),
//                "Group expression must be binary"
//            )
//        );
//    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpressionHappyPath(String test, Ast.Expression.Binary ast, Environment.Type expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getType(ast));
    }
    private static Stream<Arguments> testBinaryExpressionHappyPath() {
        return Stream.of(
            Arguments.of("Logical AND Valid",
                // TRUE AND FALSE
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(Boolean.TRUE),
                    new Ast.Expression.Literal(Boolean.FALSE)
                ),
                Environment.Type.BOOLEAN
            ),
            Arguments.of("String Concatenation",
                // "Ben" + 10
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal("Ben"),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                Environment.Type.STRING
            ),
            Arguments.of("Integer Addition",
                // 1 + 10
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigInteger.TEN)
                ),
                Environment.Type.INTEGER
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpressionSadPath(String test, Ast.Expression.Binary ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }
    private static Stream<Arguments> testBinaryExpressionSadPath() {
        return Stream.of(
            Arguments.of("Integer Decimal Addition",
                // 1 + 1.0
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigDecimal.ONE)
                ),
                "Type unassignable: Decimal -> Integer"
            ),
            Arguments.of("GT Different Types",
                // 1 > 10.0
                new Ast.Expression.Binary(">",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigDecimal.TEN)
                ),
                "Types mismatch: must be type Integer but was Decimal"
            ),
            Arguments.of("Not Equal Different Types",
                // 1 != 10.0
                new Ast.Expression.Binary("!=",
                    new Ast.Expression.Literal(BigInteger.ONE),
                    new Ast.Expression.Literal(BigDecimal.TEN)
                ),
                "Types mismatch: must be type Integer but was Decimal"
            ),
            Arguments.of("Logical AND Invalid",
                // TRUE AND "FALSE"
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Literal(Boolean.TRUE),
                    new Ast.Expression.Literal("FALSE")
                ),
                "Type unassignable: String -> Boolean"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAccessExpressionHappyPath(String test, Ast.Expression.Access ast, Environment.Variable expected) {
        Analyzer analyzer = new Analyzer();
        // TODO swap these object.field tests to the builtins
        analyzer.getScope().defineVariable("variable", Environment.Type.INTEGER, false);
        analyzer.getScope().defineVariable("object", Environment.Type.ANY, false);
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getVariable(ast));
    }
    private static Stream<Arguments> testAccessExpressionHappyPath() {
        return Stream.of(
            Arguments.of("Variable",
                // variable
                new Ast.Expression.Access(Optional.empty(), "variable"),
                new Environment.Variable("variable", Environment.Type.INTEGER, false)
            )
            //
//            Arguments.of("Field",
//                // object.field
//                new Ast.Expression.Access(Optional.of(
//                    new Ast.Expression.Access(Optional.empty(), "object")
//                ), "field"),
//                new Environment.Variable("field", Environment.Type.INTEGER, false)
//            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunctionExpressionHappyPath(String test, Ast.Expression.Function ast, Environment.Function expected) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("function", List.of(), Environment.Type.INTEGER);
        analyzer.getScope().defineFunction("function", List.of(Environment.Type.INTEGER), Environment.Type.INTEGER);
        analyzer.getScope().defineVariable("object", Environment.Type.ANY, false);
        analyzer.visit(ast);
        Assertions.assertEquals(expected, analyzer.getBindings().getFunction(ast));
    }
    private static Stream<Arguments> testFunctionExpressionHappyPath() {
        return Stream.of(
            Arguments.of("Function",
                // function()
                new Ast.Expression.Function(Optional.empty(), "function", List.of()),
                new Environment.Function("function", List.of(), Environment.Type.INTEGER)
            ),
            Arguments.of("Function Valid Arg",
                // function(1)
                new Ast.Expression.Function(Optional.empty(), "function", List.of(new Ast.Expression.Literal(BigInteger.ONE))),
                new Environment.Function("function", List.of(Environment.Type.INTEGER), Environment.Type.INTEGER)
            ),
            Arguments.of("Method",
                // object.stringify()
                new Ast.Expression.Function(Optional.of(
                    new Ast.Expression.Access(Optional.empty(), "object")
                ), "stringify", List.of()),
                new Environment.Function("stringify", List.of(Environment.Type.ANY), Environment.Type.STRING)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunctionExpressionSadPath(String test, Ast.Expression.Function ast, String expectedMessage) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("function", List.of(), Environment.Type.INTEGER);
        String message = Assertions.assertThrows(CompilerException.class, () -> analyzer.visit(ast)).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }
    private static Stream<Arguments> testFunctionExpressionSadPath() {
        return Stream.of(
            Arguments.of("Function Invalid Arg",
                // function(1.0)
                new Ast.Expression.Function(Optional.empty(), "function", List.of(new Ast.Expression.Literal(BigDecimal.ONE))),
                "The function function/1 is not defined in this scope."
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testRequireAssignable(String test, Environment.Type target, Environment.Type type, boolean success) {
        if (success) {
            Assertions.assertDoesNotThrow(() -> Environment.requireAssignable(target, type));
        } else {
            Assertions.assertThrows(CompilerException.class, () -> Environment.requireAssignable(target, type));
        }
    }
    private static Stream<Arguments> testRequireAssignable() {
        return Stream.of(
            Arguments.of("Integer to Integer", Environment.Type.INTEGER, Environment.Type.INTEGER, true),
            Arguments.of("Integer to Decimal", Environment.Type.DECIMAL, Environment.Type.INTEGER, false),
            Arguments.of("Integer to Primitive", Environment.Type.PRIMITIVE, Environment.Type.INTEGER,  true),
            Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
            Arguments.of("Any to Integer", Environment.Type.INTEGER, Environment.Type.ANY, false)
        );
    }
}
