package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of("Zero Statements",
                Arrays.asList(),
                new Ast.Source(Arrays.asList(), Arrays.asList())
            ),
            Arguments.of("Field",
                Arrays.asList(
                    // LET name = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "=", 9),
                    new Token(Token.Type.IDENTIFIER, "expr", 11),
                    new Token(Token.Type.OPERATOR, ";", 15)
                ),
                new Ast.Source(
                    Arrays.asList(new Ast.Field("name", false, Optional.of(
                        new Ast.Expression.Access(Optional.empty(), "expr")
                    ))),
                    Arrays.asList()
                )
            ),
            Arguments.of("Method",
                Arrays.asList(
                    // DEF name() DO stmt; END
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "stmt", 14),
                    new Token(Token.Type.OPERATOR, ";", 18),
                    new Token(Token.Type.IDENTIFIER, "END", 20)
                ),
                new Ast.Source(
                    Arrays.asList(),
                    Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                    )))
                )
            ),
            Arguments.of("Field Method",
                Arrays.asList(
                    // LET name = expr;␊DEF name() DO stmt; END
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "=", 9),
                    new Token(Token.Type.IDENTIFIER, "expr", 11),
                    new Token(Token.Type.OPERATOR, ";", 15),
                    new Token(Token.Type.IDENTIFIER, "DEF", 16),
                    new Token(Token.Type.IDENTIFIER, "name", 20),
                    new Token(Token.Type.OPERATOR, "(", 24),
                    new Token(Token.Type.OPERATOR, ")", 25),
                    new Token(Token.Type.IDENTIFIER, "DO", 27),
                    new Token(Token.Type.IDENTIFIER, "stmt", 30),
                    new Token(Token.Type.OPERATOR, ";", 34),
                    new Token(Token.Type.IDENTIFIER, "END", 36)
                ),
                new Ast.Source(
                    Arrays.asList(new Ast.Field("name", false, Optional.of(
                        new Ast.Expression.Access(Optional.empty(), "expr")
                    ))),
                    Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                    )))
                )
            ),
            Arguments.of("Large Code Snippet",
                ParserTestData.input,
                ParserTestData.expected
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSourceError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseSource);
    }

    private static Stream<Arguments> testSourceError() {
        return Stream.of(
            Arguments.of("Method Field",
                Arrays.asList(
                    // DEF name() DO stmt; END␊LET name = expr;
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "stmt", 14),
                    new Token(Token.Type.OPERATOR, ";", 18),
                    new Token(Token.Type.IDENTIFIER, "END", 20),
                    new Token(Token.Type.IDENTIFIER, "LET", 23),
                    new Token(Token.Type.IDENTIFIER, "name", 27),
                    new Token(Token.Type.OPERATOR, "=", 32),
                    new Token(Token.Type.IDENTIFIER, "expr", 34),
                    new Token(Token.Type.OPERATOR, ";", 38)
                ),
                "Must have all LET statements before DEF",
                23
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
            Arguments.of("Variable",
                Arrays.asList(
                    // name;
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, ";", 4)
                ),
                new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(),"name"))
            ),
            Arguments.of("Function Expression",
                Arrays.asList(
                    // name();
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5),
                    new Token(Token.Type.OPERATOR, ";", 6)
                ),
                new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "name", Arrays.asList()))
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
            Arguments.of("Missing ;",
                Arrays.asList(
                    // name
                    new Token(Token.Type.IDENTIFIER, "name", 0)
                ),
                "Missing: ;",
                4
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
            Arguments.of("Definition",
                Arrays.asList(
                    // LET name;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ";", 8)
                ),
                new Ast.Statement.Declaration("name", Optional.empty())
            ),
            Arguments.of("Initialization",
                Arrays.asList(
                    // LET name = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "=", 9),
                    new Token(Token.Type.IDENTIFIER, "expr", 11),
                    new Token(Token.Type.OPERATOR, ";", 15)
                ),
                new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatementError() {
        return Stream.of(
            Arguments.of("Type Annotation",
                Arrays.asList(
                    // LET name: Type = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 8),
                    new Token(Token.Type.IDENTIFIER, "Type", 10),
                    new Token(Token.Type.OPERATOR, "=", 15),
                    new Token(Token.Type.IDENTIFIER, "expr", 17),
                    new Token(Token.Type.OPERATOR, ";", 21)
                ),
                "Missing: ;",
                8
            )
        );
    }

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
            Arguments.of("Missing Value",
                Arrays.asList(
                    // name = ;
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "=", 5),
                    new Token(Token.Type.OPERATOR, ";", 7)
                ),
                "Invalid Primary Expression",
                7
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
            Arguments.of("If",
                Arrays.asList(
                    // IF expr DO stmt; END
                    new Token(Token.Type.IDENTIFIER, "IF", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 3),
                    new Token(Token.Type.IDENTIFIER, "DO", 8),
                    new Token(Token.Type.IDENTIFIER, "stmt", 11),
                    new Token(Token.Type.OPERATOR, ";", 15),
                    new Token(Token.Type.IDENTIFIER, "END", 17)
                ),
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "expr"),
                    Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))),
                    Arrays.asList()
                )
            ),
            Arguments.of("Else",
                Arrays.asList(
                    // IF expr DO stmt1; ELSE stmt2; END
                    new Token(Token.Type.IDENTIFIER, "IF", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 3),
                    new Token(Token.Type.IDENTIFIER, "DO", 8),
                    new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                    new Token(Token.Type.OPERATOR, ";", 16),
                    new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                    new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                    new Token(Token.Type.OPERATOR, ";", 28),
                    new Token(Token.Type.IDENTIFIER, "END", 30)
                ),
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "expr"),
                    Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))),
                    Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatementError() {
        return Stream.of(
            Arguments.of("Missing DO",
                Arrays.asList(
                    // IF expr stmt; END
                    new Token(Token.Type.IDENTIFIER, "IF", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 3),
                    new Token(Token.Type.IDENTIFIER, "stmt", 8),
                    new Token(Token.Type.OPERATOR, ";", 12),
                    new Token(Token.Type.IDENTIFIER, "END", 14)
                ),
                "Missing: DO",
                8
            ),
            Arguments.of("Invalid DO",
                Arrays.asList(
                    // IF expr THEN
                    new Token(Token.Type.IDENTIFIER, "IF", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 3),
                    new Token(Token.Type.IDENTIFIER, "THEN", 8)
                ),
                "Missing: DO",
                8
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testForStatement(String test, List<Token> tokens, Ast.Statement.For expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
            Arguments.of("For Loop",
                Arrays.asList(
                    // FOR (id = expr1; expr2; id = expr3) stmt1; END
                    new Token(Token.Type.IDENTIFIER, "FOR", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "id", 5),
                    new Token(Token.Type.OPERATOR, "=", 8),
                    new Token(Token.Type.IDENTIFIER, "expr1", 10),
                    new Token(Token.Type.OPERATOR, ";", 15),
                    new Token(Token.Type.IDENTIFIER, "expr2", 17),
                    new Token(Token.Type.OPERATOR, ";", 22),
                    new Token(Token.Type.IDENTIFIER, "id", 24),
                    new Token(Token.Type.OPERATOR, "=", 27),
                    new Token(Token.Type.IDENTIFIER, "expr3", 29),
                    new Token(Token.Type.OPERATOR, ")", 34),
                    new Token(Token.Type.IDENTIFIER, "stmt1", 36),
                    new Token(Token.Type.OPERATOR, ";", 41),
                    new Token(Token.Type.IDENTIFIER, "END", 43)
                ),
                new Ast.Statement.For(
                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "id"),
                        new Ast.Expression.Access(Optional.empty(), "expr1")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr2"),
                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "id"),
                        new Ast.Expression.Access(Optional.empty(), "expr3")
                    ),
                    Arrays.asList(
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))
                    )
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testForStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testForStatementError() {
        return Stream.of(
            Arguments.of("Missing END",
                Arrays.asList(
                    // FOR (id = expr1; expr2; id = expr3) stmt1;
                    new Token(Token.Type.IDENTIFIER, "FOR", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "id", 5),
                    new Token(Token.Type.OPERATOR, "=", 8),
                    new Token(Token.Type.IDENTIFIER, "expr1", 10),
                    new Token(Token.Type.OPERATOR, ";", 15),
                    new Token(Token.Type.IDENTIFIER, "expr2", 17),
                    new Token(Token.Type.OPERATOR, ";", 22),
                    new Token(Token.Type.IDENTIFIER, "id", 24),
                    new Token(Token.Type.OPERATOR, "=", 27),
                    new Token(Token.Type.IDENTIFIER, "expr3", 29),
                    new Token(Token.Type.OPERATOR, ")", 34),
                    new Token(Token.Type.IDENTIFIER, "stmt1", 36),
                    new Token(Token.Type.OPERATOR, ";", 41)
                ),
                "Invalid Length. Remaining: 0 Expected: 1",
                42
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Statement.While expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
            Arguments.of("While",
                Arrays.asList(
                    // WHILE expr DO stmt; END
                    new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 6),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "stmt", 14),
                    new Token(Token.Type.OPERATOR, ";", 18),
                    new Token(Token.Type.IDENTIFIER, "END", 20)
                ),
                new Ast.Statement.While(
                    new Ast.Expression.Access(Optional.empty(), "expr"),
                    Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhileStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatementError() {
        return Stream.of(
            Arguments.of("Missing END",
                Arrays.asList(
                    // WHILE expr DO stmt;
                    new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 6),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "stmt", 14),
                    new Token(Token.Type.OPERATOR, ";", 18)
                ),
                "Invalid Length. Remaining: 0 Expected: 1",
                19
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
            Arguments.of("Return Statement",
                Arrays.asList(
                    // RETURN expr;
                    new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 7),
                    new Token(Token.Type.OPERATOR, ";", 11)
                ),
                new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testError(tokens, expectedMessage, expectedIndex, Parser::parseStatement);
    }

    private static Stream<Arguments> testReturnStatementError() {
        return Stream.of(
            Arguments.of("Missing value",
                Arrays.asList(
                    // RETURN;
                    new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                    new Token(Token.Type.OPERATOR, ";", 6)
                ),
                "Invalid Primary Expression",
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
            Arguments.of("Boolean Literal",
                Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                new Ast.Expression.Literal(Boolean.TRUE)
            ),
            Arguments.of("Nil Literal",
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
            Arguments.of("String Literal",
                Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                new Ast.Expression.Literal("string")
            ),
            Arguments.of("Escape Character",
                Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                new Ast.Expression.Literal("Hello,\nWorld!")
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
            Arguments.of("Missing )",
                Arrays.asList(
                    // (expr
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 1)
                ),
                "Missing: )",
                5
            ),
            Arguments.of("Wrong ]",
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
                    // expr1 AND expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.IDENTIFIER, "AND", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 10)
                ),
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
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
            Arguments.of("Binary Multiple +/*",
                Arrays.asList(
                    // expr1 + expr2 * expr3
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "+", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 8),
                    new Token(Token.Type.OPERATOR, "*", 14),
                    new Token(Token.Type.IDENTIFIER, "expr3", 16)
                ),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Binary("*",
                        new Ast.Expression.Access(Optional.empty(), "expr2"),
                        new Ast.Expression.Access(Optional.empty(), "expr3")
                    )
                )
            ),
            Arguments.of("Binary Multiple +/* Rev",
                Arrays.asList(
                    // expr1 * expr2 + expr3
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "*", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 8),
                    new Token(Token.Type.OPERATOR, "+", 14),
                    new Token(Token.Type.IDENTIFIER, "expr3", 16)
                ),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Binary("*",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                )
            ),
            Arguments.of("Binary Multiple AND OR",
                Arrays.asList(
                    // expr1 * expr2 + expr3
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "AND", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 8),
                    new Token(Token.Type.OPERATOR, "OR", 14),
                    new Token(Token.Type.IDENTIFIER, "expr3", 16)
                ),
                new Ast.Expression.Binary("OR",
                    new Ast.Expression.Binary("AND",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                )
            ),
            Arguments.of("Binary Multiple == !=",
                Arrays.asList(
                    // expr1 * expr2 + expr3
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "==", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 8),
                    new Token(Token.Type.OPERATOR, "!=", 14),
                    new Token(Token.Type.IDENTIFIER, "expr3", 16)
                ),
                new Ast.Expression.Binary("!=",
                    new Ast.Expression.Binary("==",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
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
            Arguments.of("Missing Operand",
                Arrays.asList(
                    // expr -
                    new Token(Token.Type.IDENTIFIER, "expr", 0),
                    new Token(Token.Type.OPERATOR, "-", 5)
                ),
                "Invalid Length. Remaining: 0 Expected: 1",
                6
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
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "field")
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
            Arguments.of("Method Call",
                Arrays.asList(
                    // obj.method()
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.IDENTIFIER, "method", 4),
                    new Token(Token.Type.OPERATOR, "(", 10),
                    new Token(Token.Type.OPERATOR, ")", 11)
                ),
                new Ast.Expression.Function(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "method", Arrays.asList())
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
            Arguments.of("Trailing Comma",
                Arrays.asList(
                    // name(expr,)
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "expr", 5),
                    new Token(Token.Type.OPERATOR, ",", 9),
                    new Token(Token.Type.IDENTIFIER, ")", 10)
                ),
                "Missing: )",
                11
            )
        );
    }

    @Test
    void testExample1() {
        List<Token> input = Arrays.asList(
            /**
             *  LET first = 1;
             *  DEF main() DO
             *      WHILE first != 10 DO
             *          print(first);
             *          first = first + 1;
             *      END
             *  END
             */
            // LET first = 1;
            new Token(Token.Type.IDENTIFIER, "LET", 0),
            new Token(Token.Type.IDENTIFIER, "first", 4),
            new Token(Token.Type.OPERATOR, "=", 10),
            new Token(Token.Type.INTEGER, "1", 12),
            new Token(Token.Type.OPERATOR, ";", 13),
            //DEF main() DO
            new Token(Token.Type.IDENTIFIER, "DEF", 15),
            new Token(Token.Type.IDENTIFIER, "main", 19),
            new Token(Token.Type.OPERATOR, "(", 23),
            new Token(Token.Type.OPERATOR, ")", 24),
            new Token(Token.Type.IDENTIFIER, "DO", 26),
            //    WHILE first != 10 DO
            new Token(Token.Type.IDENTIFIER, "WHILE", 33),
            new Token(Token.Type.IDENTIFIER, "first", 39),
            new Token(Token.Type.OPERATOR, "!=", 45),
            new Token(Token.Type.INTEGER, "10", 48),
            new Token(Token.Type.IDENTIFIER, "DO", 51),
            //        print(first);
            new Token(Token.Type.IDENTIFIER, "print", 62),
            new Token(Token.Type.OPERATOR, "(", 67),
            new Token(Token.Type.IDENTIFIER, "first", 68),
            new Token(Token.Type.OPERATOR, ")", 73),
            new Token(Token.Type.OPERATOR, ";", 74),
            //        first = first + 1;
            new Token(Token.Type.IDENTIFIER, "first", 84),
            new Token(Token.Type.OPERATOR, "=", 90),
            new Token(Token.Type.IDENTIFIER, "first", 92),
            new Token(Token.Type.OPERATOR, "+", 98),
            new Token(Token.Type.INTEGER, "1", 100),
            new Token(Token.Type.OPERATOR, ";", 101),
            //    END
            new Token(Token.Type.IDENTIFIER, "END", 107),
            //END
            new Token(Token.Type.IDENTIFIER, "END", 111)
        );
        Ast.Source expected = new Ast.Source(
            Arrays.asList(new Ast.Field("first", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
            Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                new Ast.Statement.While(
                    new Ast.Expression.Binary("!=",
                        new Ast.Expression.Access(Optional.empty(), "first"),
                        new Ast.Expression.Literal(BigInteger.TEN)
                    ),
                    Arrays.asList(
                        new Ast.Statement.Expression(
                            new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "first"))
                            )
                        ),
                        new Ast.Statement.Assignment(
                            new Ast.Expression.Access(Optional.empty(), "first"),
                            new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "first"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                            )
                        )
                    )
                )
            ))
        ));
        test(input, expected, Parser::parseSource);
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
