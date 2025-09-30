package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;



/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    // expression ";"
    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
            Arguments.of("Function Expression",
                Arrays.asList(
                    // name();
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5),
                    new Token(Token.Type.OPERATOR, ";", 6)
                ),
                new Ast.Statement.Expression(new Ast.Expression.Function(
                    Optional.empty(),
                    "name",
                    Arrays.asList()))
            ),
            Arguments.of("Variable Expression",
                Arrays.asList(
                    // name;
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, ";", 4)
                ),
                new Ast.Statement.Expression(new Ast.Expression.Access(
                    Optional.empty(),
                    "name")
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatementError() {
        return Stream.of(
            Arguments.of( "Missing ;",
                Arrays.asList(
                    // name()
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5)
                ),
                "Missing: ;",
                6
            )
        );
    }

    // expression "=" expression ";"
    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
            Arguments.of("Assignment",
                Arrays.asList(
                    // name = value;
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "=", 5),
                    new Token(Token.Type.IDENTIFIER, "value", 7),
                    new Token(Token.Type.OPERATOR, ";", 12)
                ),
                new Ast.Statement.Assignment(
                    new Ast.Expression.Access(Optional.empty(), "name"),
                    new Ast.Expression.Access(Optional.empty(), "value")
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatementError() {
        return Stream.of(
            Arguments.of( "Missing ;",
                Arrays.asList(
                    // name = value;
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "=", 5),
                    new Token(Token.Type.IDENTIFIER, "value", 7)
                ),
                "Missing: ;",
                12
            ),
            Arguments.of( "Missing value",
                Arrays.asList(
                    // name =
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "=", 5)
                ),
                "Invalid Length. Remaining: 0 Expected: 1",
                6
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
            Arguments.of("Boolean Literal True",
                Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                new Ast.Expression.Literal(Boolean.TRUE)
            ),
            Arguments.of("Boolean Literal False",
                Arrays.asList(new Token(Token.Type.IDENTIFIER, "FALSE", 0)),
                new Ast.Expression.Literal(Boolean.FALSE)
            ),
            Arguments.of("Boolean Literal Nil",
                Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                new Ast.Expression.Literal(null)
            ),
            Arguments.of("Integer Literal",
                Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                new Ast.Expression.Literal(new BigInteger("1"))
            ),
            Arguments.of("Decimal Literal",
                Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                new Ast.Expression.Literal(new BigDecimal("2.0"))
            ),
            Arguments.of("Character Literal",
                Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                new Ast.Expression.Literal('c')
            ),
            Arguments.of("Character Escape",
                Arrays.asList(new Token(Token.Type.CHARACTER, "'\b'", 0)),
                new Ast.Expression.Literal('\b')
            ),
            Arguments.of("Character Escape",
                Arrays.asList(new Token(Token.Type.CHARACTER, "'\\b'", 0)),
                new Ast.Expression.Literal('\b')
            ),
            Arguments.of("String Literal",
                Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                new Ast.Expression.Literal("string")
            ),
            Arguments.of("Escape Character",
                Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                new Ast.Expression.Literal("Hello,\nWorld!")
            ),
            Arguments.of("String Escape Single Char",
                Arrays.asList(new Token(Token.Type.STRING, "\"\b\"", 0)),
                new Ast.Expression.Literal("\b")
            ),
            Arguments.of("String Escape Single Char",
                Arrays.asList(new Token(Token.Type.STRING, "\"\\b\"", 0)),
                new Ast.Expression.Literal("\b")
            ),
            Arguments.of("Multiple Escape Character",
                Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld\\n!\"", 0)),
                new Ast.Expression.Literal("Hello,\nWorld\n!")
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
            Arguments.of("Grouped Variable",
                Arrays.asList(
                    // (expr)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 1),
                    new Token(Token.Type.OPERATOR, ")", 5)
                ),
                new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
            ),
            Arguments.of("Grouped Binary",
                Arrays.asList(
                    // (expr1 + expr2)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.OPERATOR, "+", 7),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.OPERATOR, ")", 14)
                ),
                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                ))
            ),
            Arguments.of("Grouped Multiple Binary",
                Arrays.asList(
                    // (expr1 + expr2 + expr3)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.OPERATOR, "+", 7),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.OPERATOR, "+", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 17),
                    new Token(Token.Type.OPERATOR, ")", 22)
                ),
                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                ))
            ),
            Arguments.of("Grouped More Multiple Binary",
                Arrays.asList(
                    // (expr1 + expr2 + expr3 + expr4)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.OPERATOR, "-", 7),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.OPERATOR, "+", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 17),
                    new Token(Token.Type.OPERATOR, "+", 23),
                    new Token(Token.Type.IDENTIFIER, "expr4", 25),
                    new Token(Token.Type.OPERATOR, ")", 30)
                ),
                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Binary("-",
                            new Ast.Expression.Access(Optional.empty(), "expr1"),
                            new Ast.Expression.Access(Optional.empty(), "expr2")
                        ),
                        new Ast.Expression.Access(Optional.empty(), "expr3")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr4")
                ))
            ),
            Arguments.of("Grouped Multiple Binary PEMDS",
                Arrays.asList(
                    // (expr1 + expr2 + expr3)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.OPERATOR, "+", 7),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.OPERATOR, "*", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 17),
                    new Token(Token.Type.OPERATOR, ")", 22)
                ),
                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Binary("*",
                        new Ast.Expression.Access(Optional.empty(), "expr2"),
                        new Ast.Expression.Access(Optional.empty(), "expr3")
                    )
                ))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpressionError() {
        return Stream.of(
            Arguments.of("Missing Closing )",
                Arrays.asList(
                    // (expr1
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1)
                ),
                "Missing: )",
                6
            ),
            Arguments.of("Missing Closing ) Binary",
                Arrays.asList(
                    // (expr1 + expr2
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.OPERATOR, "+", 7),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9)
                ),
                "Missing: )",
                14
            ),
            Arguments.of("Missing Operator Binary",
                Arrays.asList(
                    // (expr1 expr2)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.IDENTIFIER, "expr2", 7),
                    new Token(Token.Type.OPERATOR, ")", 11)
                ),
                "Missing: )",
                7
            ),
            Arguments.of("Wrong Closing Binary",
                Arrays.asList(
                    // (expr]
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 1),
                    new Token(Token.Type.OPERATOR, "]", 5)
                ),
                "Missing: )",
                5
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
            Arguments.of("Binary And",
                Arrays.asList(
                    // expr1 && expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "&&", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 10)
                ),
                new Ast.Expression.Binary("&&",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Binary Or",
                Arrays.asList(
                    // expr1 || expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "||", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 10)
                ),
                new Ast.Expression.Binary("||",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Multiple Binary And",
                Arrays.asList(
                    // expr1 && expr2 AND expr3
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "&&", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.OPERATOR, "AND", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 18)
                ),
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Binary("&&",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                )
            ),
            Arguments.of("Binary Equality",
                Arrays.asList(
                    // expr1 == expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "==", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9)
                ),
                new Ast.Expression.Binary("==",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Binary Addition",
                Arrays.asList(
                    // expr1 + expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "+", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 8)
                ),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Binary Multiplication",
                Arrays.asList(
                    // expr1 * expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "*", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 8)
                ),
                new Ast.Expression.Binary("*",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Multiple Binary Logic Comparison",
                Arrays.asList(
                    // expr1 == expr2 && expr3 == expr4
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "==", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.OPERATOR, "&&", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 18),
                    new Token(Token.Type.OPERATOR, "==", 24),
                    new Token(Token.Type.IDENTIFIER, "expr4", 27)
                ),
                new Ast.Expression.Binary("&&",
                    new Ast.Expression.Binary("==",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Binary("==",
                        new Ast.Expression.Access(Optional.empty(), "expr3"),
                        new Ast.Expression.Access(Optional.empty(), "expr4")
                    )
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpressionError() {
        return Stream.of(
            Arguments.of("Missing Operand Add",
                Arrays.asList(
                    // expr1 +
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "+", 6)
                ),
                "Invalid Length. Remaining: 0 Expected: 1",
                7
            ),
            Arguments.of("Missing Operand Mult",
                Arrays.asList(
                    // expr1 *
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "*", 6)
                ),
                "Invalid Length. Remaining: 0 Expected: 1",
                7
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
            Arguments.of("Variable",
                // name
                Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                new Ast.Expression.Access(Optional.empty(), "name")
            ),
            Arguments.of("Field Access",
                Arrays.asList(
                    // obj.field
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.IDENTIFIER, "field", 4)
                ),
                new Ast.Expression.Access(Optional.of(new
                    Ast.Expression.Access(Optional.empty(), "obj")), "field")
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpressionError() {
        return Stream.of(
            Arguments.of("Missing Operand",
                Arrays.asList(
                    // obj.5
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.INTEGER, "5", 4)
                ),
                "Type Error. Expected: IDENTIFIER, Got: INTEGER",
                4
            ),
            Arguments.of("Invalid Expression",
                Arrays.asList(
                    // ?
                    new Token(Token.Type.OPERATOR, "?", 0)
                ),
                "Invalid Primary Expression",
                0
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
            Arguments.of("Zero Arguments",
                Arrays.asList(
                    // name()
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5)
                ),
                new Ast.Expression.Function(Optional.empty(), "name", Arrays.asList())
            ),
            Arguments.of("Multiple Arguments",
                Arrays.asList(
                    // name(expr1, expr2, expr3)
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "expr1", 5),
                    new Token(Token.Type.OPERATOR, ",", 10),
                    new Token(Token.Type.IDENTIFIER, "expr2", 12),
                    new Token(Token.Type.OPERATOR, ",", 17),
                    new Token(Token.Type.IDENTIFIER, "expr3", 19),
                    new Token(Token.Type.OPERATOR, ")", 24)
                ),
                new Ast.Expression.Function(Optional.empty(), "name", Arrays.asList(
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2"),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                ))
            ),
            Arguments.of("Function Field Access",
                Arrays.asList(
                    // obj.field(arg1, arg2)
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.IDENTIFIER, "field", 4),
                    new Token(Token.Type.OPERATOR, "(", 9),
                    new Token(Token.Type.IDENTIFIER, "arg1", 10),
                    new Token(Token.Type.OPERATOR, ",", 14),
                    new Token(Token.Type.IDENTIFIER, "arg2", 16),
                    new Token(Token.Type.OPERATOR, ")", 20)
                ),
                new Ast.Expression.Function(Optional.of(new
                    Ast.Expression.Access(Optional.empty(), "obj")),
                    "field",
                    List.of(
                        new Ast.Expression.Access(Optional.empty(), "arg1"),
                        new Ast.Expression.Access(Optional.empty(), "arg2")
                    )
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpressionError() {
        return Stream.of(
            Arguments.of("Missing Parameters",
                Arrays.asList(
                    // name(expr1, expr2, )
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "expr1", 5),
                    new Token(Token.Type.OPERATOR, ",", 10),
                    new Token(Token.Type.IDENTIFIER, "expr2", 12),
                    new Token(Token.Type.OPERATOR, ",", 17),
                    new Token(Token.Type.OPERATOR, ")", 19)
                ),
                "Invalid Primary Expression",
                19
            )
        );
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

    private static <T extends Ast> void testError(List<Token> tokens, String expectedMsg, int expectedIndex, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        ParseException ex = Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        Assertions.assertEquals(expectedMsg, ex.getMessage());
        Assertions.assertEquals(expectedIndex, ex.getIndex());
    }
}
