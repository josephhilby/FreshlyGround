package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.frontend.artifacts.Ast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class ParserTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        testParse(tokens, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of("Zero Statements",
                List.of(),
                new Ast.Source(List.of(), List.of())
            ),
            Arguments.of("Field",
                List.of(
                    //LET name: Type = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 9),
                    new Token(Token.Type.IDENTIFIER, "Type", 11),
                    new Token(Token.Type.OPERATOR, "=", 15),
                    new Token(Token.Type.IDENTIFIER, "expr", 17),
                    new Token(Token.Type.OPERATOR, ";", 21)
                ),
                new Ast.Source(
                    List.of(new Ast.Field("name", "Type", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                    List.of()
                )
            ),
            Arguments.of("Method",
                 List.of(
                    //DEF name(): Type DO func(); END
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.OPERATOR, ":", 10),
                    new Token(Token.Type.IDENTIFIER, "Type", 12),
                    new Token(Token.Type.IDENTIFIER, "DO", 17),
                    new Token(Token.Type.IDENTIFIER, "func", 20),
                    new Token(Token.Type.OPERATOR, "(", 24),
                    new Token(Token.Type.OPERATOR, ")", 25),
                    new Token(Token.Type.OPERATOR, ";", 26),
                    new Token(Token.Type.IDENTIFIER, "END", 28)
                ),
                new Ast.Source(
                    List.of(),
                    List.of(new Ast.Method("name", List.of(), List.of(), Optional.of("Type"), List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of()))
                    )))
                )
            ),
            Arguments.of("Method w/o Type",
                List.of(
                    // DEF name() DO func(); END
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "func", 14),
                    new Token(Token.Type.OPERATOR, "(", 18),
                    new Token(Token.Type.OPERATOR, ")", 19),
                    new Token(Token.Type.OPERATOR, ";", 20),
                    new Token(Token.Type.IDENTIFIER, "END", 22)
                ),
                new Ast.Source(
                    List.of(),
                    List.of(new Ast.Method("name", List.of(), List.of(), Optional.empty(), List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of()))
                    )))
                )
            ),
            Arguments.of("Method with Params",
                List.of(
                    // DEF name(x: Type, y: Type, z: Type): Type DO func(); END
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.IDENTIFIER, "x", 9),
                    new Token(Token.Type.OPERATOR, ":", 15),
                    new Token(Token.Type.IDENTIFIER, "Type1", 17),
                    new Token(Token.Type.OPERATOR, ",", 10),
                    new Token(Token.Type.IDENTIFIER, "y", 11),
                    new Token(Token.Type.OPERATOR, ":", 15),
                    new Token(Token.Type.IDENTIFIER, "Type2", 17),
                    new Token(Token.Type.OPERATOR, ",", 12),
                    new Token(Token.Type.IDENTIFIER, "z", 13),
                    new Token(Token.Type.OPERATOR, ":", 15),
                    new Token(Token.Type.IDENTIFIER, "Type3", 17),
                    new Token(Token.Type.OPERATOR, ")", 14),
                    new Token(Token.Type.OPERATOR, ":", 15),
                    new Token(Token.Type.IDENTIFIER, "Type4", 17),
                    new Token(Token.Type.IDENTIFIER, "DO", 22),
                    new Token(Token.Type.IDENTIFIER, "func", 25),
                    new Token(Token.Type.OPERATOR, "(", 29),
                    new Token(Token.Type.OPERATOR, ")", 30),
                    new Token(Token.Type.OPERATOR, ";", 31),
                    new Token(Token.Type.IDENTIFIER, "END", 33)
                ),
                new Ast.Source(
                    List.of(),
                    List.of(new Ast.Method("name", List.of("x", "y", "z"), List.of("Type1", "Type2", "Type3"), Optional.of("Type4"), List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of()))
                    )))
                )
            ),
            Arguments.of("Field Method",
                List.of(
                    // LET name: Type1 = expr;
                    // DEF name(): Type2 DO func(); END
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 8),
                    new Token(Token.Type.IDENTIFIER, "Type1", 10),
                    new Token(Token.Type.OPERATOR, "=", 16),
                    new Token(Token.Type.IDENTIFIER, "expr", 18),
                    new Token(Token.Type.OPERATOR, ";", 22),
                    new Token(Token.Type.IDENTIFIER, "DEF", 23),
                    new Token(Token.Type.IDENTIFIER, "name", 27),
                    new Token(Token.Type.OPERATOR, "(", 31),
                    new Token(Token.Type.OPERATOR, ")", 32),
                    new Token(Token.Type.OPERATOR, ":", 33),
                    new Token(Token.Type.IDENTIFIER, "Type2", 35),
                    new Token(Token.Type.IDENTIFIER, "DO", 44),
                    new Token(Token.Type.IDENTIFIER, "func", 47),
                    new Token(Token.Type.OPERATOR, "(", 51),
                    new Token(Token.Type.OPERATOR, ")", 52),
                    new Token(Token.Type.OPERATOR, ";", 53),
                    new Token(Token.Type.IDENTIFIER, "END", 55)
                ),
                new Ast.Source(
                    List.of(new Ast.Field("name", "Type1", false, Optional.of(
                        new Ast.Expression.Access(Optional.empty(), "expr")
                    ))),
                    List.of(new Ast.Method("name", List.of(), List.of(), Optional.of("Type2"), List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of()))
                    )))
                )
            ),
            Arguments.of("Nested Method",
                List.of(
                    // LET first: Integer = 1;
                    // DEF main(): Integer DO
                    //     WHILE first != 10 DO
                    //         print(first);
                    //         first = first + 1;
                    //     END
                    // END
                    //LET first: Integer = 1;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "first", 4),
                    new Token(Token.Type.OPERATOR, ":", 10),
                    new Token(Token.Type.IDENTIFIER, "Integer", 11),
                    new Token(Token.Type.OPERATOR, "=", 19),
                    new Token(Token.Type.INTEGER, "1", 21),
                    new Token(Token.Type.OPERATOR, ";", 22),
                    //DEF main(): Integer DO
                    new Token(Token.Type.IDENTIFIER, "DEF", 24),
                    new Token(Token.Type.IDENTIFIER, "main", 28),
                    new Token(Token.Type.OPERATOR, "(", 32),
                    new Token(Token.Type.OPERATOR, ")", 33),
                    new Token(Token.Type.OPERATOR, ":", 34),
                    new Token(Token.Type.IDENTIFIER, "Integer", 36),
                    new Token(Token.Type.IDENTIFIER, "DO", 44),
                    //    WHILE first != 10 DO
                    new Token(Token.Type.IDENTIFIER, "WHILE", 51),
                    new Token(Token.Type.IDENTIFIER, "first", 57),
                    new Token(Token.Type.OPERATOR, "!=", 63),
                    new Token(Token.Type.INTEGER, "10", 66),
                    new Token(Token.Type.IDENTIFIER, "DO", 69),
                    //        print(first);
                    new Token(Token.Type.IDENTIFIER, "print", 80),
                    new Token(Token.Type.OPERATOR, "(", 85),
                    new Token(Token.Type.IDENTIFIER, "first", 86),
                    new Token(Token.Type.OPERATOR, ")", 91),
                    new Token(Token.Type.OPERATOR, ";", 92),
                    //        first = first + 1;
                    new Token(Token.Type.IDENTIFIER, "first", 102),
                    new Token(Token.Type.OPERATOR, "=", 108),
                    new Token(Token.Type.IDENTIFIER, "first", 110),
                    new Token(Token.Type.OPERATOR, "+", 116),
                    new Token(Token.Type.INTEGER, "1", 118),
                    new Token(Token.Type.OPERATOR, ";", 119),
                    //    END
                    new Token(Token.Type.IDENTIFIER, "END", 125),
                    //END
                    new Token(Token.Type.IDENTIFIER, "END", 129)
                ),
                new Ast.Source(
                    List.of(new Ast.Field("first", "Integer", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                    List.of(new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                        new Ast.Statement.While(
                            new Ast.Expression.Binary("!=",
                                new Ast.Expression.Access(Optional.empty(), "first"),
                                new Ast.Expression.Literal(BigInteger.TEN)
                            ),
                            List.of(
                                new Ast.Statement.Expression(
                                    new Ast.Expression.Function(Optional.empty(), "print", List.of(
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
                    )))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSourceError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokens, expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testSourceError() {
        return Stream.of(
            Arguments.of("Field Method",
                List.of(
                    // DEF name(): Type2 DO func(); END
                    // LET name: Type1 = expr;
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.OPERATOR, ":", 10),
                    new Token(Token.Type.IDENTIFIER, "Type2", 12),
                    new Token(Token.Type.IDENTIFIER, "DO", 18),
                    new Token(Token.Type.IDENTIFIER, "func", 21),
                    new Token(Token.Type.OPERATOR, "(", 25),
                    new Token(Token.Type.OPERATOR, ")", 26),
                    new Token(Token.Type.OPERATOR, ";", 27),
                    new Token(Token.Type.IDENTIFIER, "END", 29),
                    new Token(Token.Type.IDENTIFIER, "LET", 32),
                    new Token(Token.Type.IDENTIFIER, "name", 36),
                    new Token(Token.Type.OPERATOR, ":", 40),
                    new Token(Token.Type.IDENTIFIER, "Type1", 42),
                    new Token(Token.Type.OPERATOR, "=", 48),
                    new Token(Token.Type.IDENTIFIER, "expr", 50),
                    new Token(Token.Type.OPERATOR, ";", 54)
                ),
                "Must have all LET statements before DEF",
                32
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testField(String test, List<Token> tokens, Ast.Field expected) {
        testParse(tokens, fieldWrapper(expected));
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
            Arguments.of("Definition",
                List.of(
                    //LET name : Type;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 9),
                    new Token(Token.Type.IDENTIFIER, "Type", 11),
                    new Token(Token.Type.OPERATOR, ";", 13)
                ),
                new Ast.Field("name", "Type", false, Optional.empty())
            ),
            Arguments.of("Initialization",
                List.of(
                    //LET name : Type = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 9),
                    new Token(Token.Type.IDENTIFIER, "Type", 11),
                    new Token(Token.Type.OPERATOR, "=", 16),
                    new Token(Token.Type.IDENTIFIER, "expr", 18),
                    new Token(Token.Type.OPERATOR, ";", 22)
                ),
                new Ast.Field("name", "Type", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
            ),
            Arguments.of("Initialization CONST",
                List.of(
                    //LET CONST name : Type = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "CONST", 4),
                    new Token(Token.Type.IDENTIFIER, "name", 10),
                    new Token(Token.Type.OPERATOR, ":", 15),
                    new Token(Token.Type.IDENTIFIER, "Type", 17),
                    new Token(Token.Type.OPERATOR, "=", 22),
                    new Token(Token.Type.IDENTIFIER, "expr", 24),
                    new Token(Token.Type.OPERATOR, ";", 28)
                ),
                new Ast.Field("name", "Type", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFieldError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokens, expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testFieldError() {
        return Stream.of(
            Arguments.of("Initialization CONST",
                List.of(
                    //LET CONST name : Type;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "CONST", 4),
                    new Token(Token.Type.IDENTIFIER, "name", 10),
                    new Token(Token.Type.OPERATOR, ":", 15),
                    new Token(Token.Type.IDENTIFIER, "Type", 17),
                    new Token(Token.Type.OPERATOR, ";", 21)
                ),
                "CONST must have an initial value",
                21
            ),
            Arguments.of("Missing Type",
                List.of(
                    //LET name;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ";", 8)
                ),
                "Missing: :",
                8
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, List<Token> tokens, Ast.Method expected) {
        testParse(tokens, methodWrapper(expected));
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
            Arguments.of("Definition",
                List.of(
                    // DEF name() DO END
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.IDENTIFIER, "DO", 12),
                    new Token(Token.Type.IDENTIFIER, "END", 14)
                ),
                new Ast.Method("name", List.of(), List.of(), Optional.empty(), List.of())
            ),
            Arguments.of("Initialization",
                List.of(
                    // DEF name() : Type DO END
                    new Token(Token.Type.IDENTIFIER, "DEF", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "(", 8),
                    new Token(Token.Type.OPERATOR, ")", 9),
                    new Token(Token.Type.OPERATOR, ":", 9),
                    new Token(Token.Type.IDENTIFIER, "Type", 11),
                    new Token(Token.Type.IDENTIFIER, "DO", 12),
                    new Token(Token.Type.IDENTIFIER, "END", 14)
                ),
                new Ast.Method("name", List.of(), List.of(), Optional.of("Type"), List.of())
            )
        );
    }

    //
    // TODO testMethodError
    //

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
            Arguments.of("Function Expression",
                List.of(
                    // name();
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5),
                    new Token(Token.Type.OPERATOR, ";", 6)
                ),
                new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "name", List.of()))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExpressionStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenStatementWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testExpressionStatementError() {
        return Stream.of(
            Arguments.of("Missing ;",
                List.of(
                    // func()
                    new Token(Token.Type.IDENTIFIER, "func", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5)
                ),
                "Missing: ;",
                1001 //the following token index (wrapper for END @ 1001)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
            Arguments.of("Definition",
                List.of(
                    //LET name: Type;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 8),
                    new Token(Token.Type.IDENTIFIER, "Type", 10),
                    new Token(Token.Type.OPERATOR, ";", 14)
                ),
                new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.empty())
            ),
            Arguments.of("Initialization",
                List.of(
                    //LET name = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, "=", 9),
                    new Token(Token.Type.IDENTIFIER, "expr", 11),
                    new Token(Token.Type.OPERATOR, ";", 15)
                ),
                new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
            ),
            Arguments.of("Initialization",
                List.of(
                    //LET name: Type = expr;
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "name", 4),
                    new Token(Token.Type.OPERATOR, ":", 8),
                    new Token(Token.Type.IDENTIFIER, "Type", 10),
                    new Token(Token.Type.OPERATOR, "=", 15),
                    new Token(Token.Type.IDENTIFIER, "expr", 17),
                    new Token(Token.Type.OPERATOR, ";", 21)
                ),
                new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
            )
        );
    }

    // TODO testDeclarationError
    // Must have value or type
    // Arguments.of("Definition",
    //                List.of(
    //                    //LET name;
    //                    new Token(Token.Type.IDENTIFIER, "LET", 0),
    //                    new Token(Token.Type.IDENTIFIER, "name", 4),
    //                    new Token(Token.Type.OPERATOR, ";", 8)
    //                ),
    //                new Ast.Statement.Declaration("name", Optional.empty(), Optional.empty())
    //            ),

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
            Arguments.of("Assignment",
                List.of(
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

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenStatementWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testAssignmentStatementError() {
        return Stream.of(
            Arguments.of("Missing Value",
                List.of(
                    // name = ;
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "=", 5),
                    new Token(Token.Type.OPERATOR, ";", 7)
                ),
                "Invalid Primary Expression",
                7
            ),
            Arguments.of( "Missing ;",
                List.of(
                    // name = value
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "=", 5),
                    new Token(Token.Type.IDENTIFIER, "value", 7)
                ),
                "Missing: ;",
                1001
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
            Arguments.of("If",
                List.of(
                    //IF expr DO func(); END
                    new Token(Token.Type.IDENTIFIER, "IF", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 3),
                    new Token(Token.Type.IDENTIFIER, "DO", 8),
                    new Token(Token.Type.IDENTIFIER, "func", 11),
                    new Token(Token.Type.OPERATOR, "(", 15),
                    new Token(Token.Type.OPERATOR, ")", 16),
                    new Token(Token.Type.OPERATOR, ";", 17),
                    new Token(Token.Type.IDENTIFIER, "END", 19)
                ),
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "expr"),
                    List.of(new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of()))),
                    List.of()
                )
            ),
            Arguments.of("Else",
                List.of(
                    //IF expr DO funcOne(); ELSE funcTwo(); END
                    new Token(Token.Type.IDENTIFIER, "IF", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 3),
                    new Token(Token.Type.IDENTIFIER, "DO", 8),
                    new Token(Token.Type.IDENTIFIER, "funcOne", 11),
                    new Token(Token.Type.OPERATOR, "(", 18),
                    new Token(Token.Type.OPERATOR, ")", 19),
                    new Token(Token.Type.OPERATOR, ";", 20),
                    new Token(Token.Type.IDENTIFIER, "ELSE", 22),
                    new Token(Token.Type.IDENTIFIER, "funcTwo", 27),
                    new Token(Token.Type.OPERATOR, "(", 32),
                    new Token(Token.Type.OPERATOR, ")", 33),
                    new Token(Token.Type.OPERATOR, ";", 34),
                    new Token(Token.Type.IDENTIFIER, "END", 36)
                ),
                new Ast.Statement.If(
                    new Ast.Expression.Access(Optional.empty(), "expr"),
                    List.of(new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcOne", List.of()))),
                    List.of(new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "funcTwo", List.of())))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenStatementWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testIfStatementError() {
        return Stream.of(
            Arguments.of("Missing DO",
                List.of(
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
                List.of(
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

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatement(String test, List<Token> tokens, Ast.Statement.For expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
            Arguments.of("For Loop",
                List.of(
                    // FOR (id = expr1; expr2; id = expr3) func(); END
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
                    new Token(Token.Type.IDENTIFIER, "func", 36),
                    new Token(Token.Type.OPERATOR, "(", 41),
                    new Token(Token.Type.OPERATOR, ")", 42),
                    new Token(Token.Type.OPERATOR, ";", 43),
                    new Token(Token.Type.IDENTIFIER, "END", 45)
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
                    List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of()))
                    )
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenStatementWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testForStatementError() {
        return Stream.of(
            Arguments.of("Missing END",
                List.of(
                    // FOR (id = expr1; expr2; id = expr3) func();
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
                    new Token(Token.Type.IDENTIFIER, "func", 36),
                    new Token(Token.Type.OPERATOR, "(", 40),
                    new Token(Token.Type.OPERATOR, ")", 41),
                    new Token(Token.Type.OPERATOR, ";", 42)
                ),
                "Invalid Primary Expression",
                1004 // TODO dig into the index number as 1004 not 1001
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Statement.While expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
            Arguments.of("While",
                List.of(
                    //WHILE expr DO func(); END
                    new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 6),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "func", 14),
                    new Token(Token.Type.OPERATOR, "(", 18),
                    new Token(Token.Type.OPERATOR, ")", 19),
                    new Token(Token.Type.OPERATOR, ";", 20),
                    new Token(Token.Type.IDENTIFIER, "END", 21)
                ),
                new Ast.Statement.While(
                    new Ast.Expression.Access(Optional.empty(), "expr"),
                    List.of(new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "func", List.of())))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatementError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenStatementWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testWhileStatementError() {
        return Stream.of(
            Arguments.of("Missing END",
                List.of(
                    // WHILE expr DO func();
                    new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 6),
                    new Token(Token.Type.IDENTIFIER, "DO", 11),
                    new Token(Token.Type.IDENTIFIER, "func", 14),
                    new Token(Token.Type.OPERATOR, "(", 18),
                    new Token(Token.Type.OPERATOR, ")", 19),
                    new Token(Token.Type.OPERATOR, ";", 20)
                ),
                "Invalid Primary Expression",
                1004 // TODO dig into the index number as 1004 not 1001
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        testParse(tokenStatementWrapper(tokens), statementWrapper(expected));
    }

    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
            Arguments.of("Return Statement",
                List.of(
                    //RETURN expr;
                    new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 7),
                    new Token(Token.Type.OPERATOR, ";", 11)
                ),
                new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
            ),
            Arguments.of("Return Negative Integer",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                    new Token(Token.Type.OPERATOR, "-", 7),
                    new Token(Token.Type.INTEGER, "1", 8),
                    new Token(Token.Type.OPERATOR, ";", 9)
                ),
                new Ast.Statement.Return(new Ast.Expression.Literal(new BigInteger("-1")))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testReturnStatementError(String test, List<Token> tokens, Class<? extends CompilerException> expectedType, String expectedMessage, int expectedIndex) {
        testParseError(tokenStatementWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testReturnStatementError() {
        return Stream.of(
            Arguments.of("Missing value",
                List.of(
                    // RETURN;
                    new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                    new Token(Token.Type.OPERATOR, ";", 6)
                ),
                CompilerException.class,
                "Invalid Primary Expression",
                6
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        testParse(tokenExpressionWrapper(tokens), expressionWrapper(expected));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
            Arguments.of("Boolean Literal",
                List.of(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                new Ast.Expression.Literal(Boolean.TRUE)
            ),
            Arguments.of("Nil Literal",
                List.of(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                new Ast.Expression.Literal(null)
            ),
            Arguments.of("Integer Literal",
                List.of(new Token(Token.Type.INTEGER, "1", 0)),
                new Ast.Expression.Literal(new BigInteger("1"))
            ),
            Arguments.of("Neg Integer Literal",
                List.of(new Token(Token.Type.OPERATOR, "-", 0),
                new Token(Token.Type.INTEGER, "1", 1)),
                new Ast.Expression.Literal(new BigInteger("-1"))
            ),
            Arguments.of("Pos Integer Literal",
                List.of(new Token(Token.Type.OPERATOR, "+", 0),
                    new Token(Token.Type.INTEGER, "1", 1)),
                new Ast.Expression.Literal(new BigInteger("1"))
            ),
            Arguments.of("Decimal Literal",
                List.of(new Token(Token.Type.DECIMAL, "2.0", 0)),
                new Ast.Expression.Literal(new BigDecimal("2.0"))
            ),
            Arguments.of("Neg Decimal Literal",
                List.of(new Token(Token.Type.OPERATOR, "-", 0),
                    new Token(Token.Type.DECIMAL, "1.0", 1)),
                new Ast.Expression.Literal(new BigDecimal("-1.0"))
            ),
            Arguments.of("Pos Decimal Literal",
                List.of(new Token(Token.Type.OPERATOR, "+", 0),
                    new Token(Token.Type.DECIMAL, "1.0", 1)),
                new Ast.Expression.Literal(new BigDecimal("1.0"))
            ),
            Arguments.of("Character Literal",
                List.of(new Token(Token.Type.CHARACTER, "'c'", 0)),
                new Ast.Expression.Literal('c')
            ),
            Arguments.of("Character Escape",
                List.of(new Token(Token.Type.CHARACTER, "'\b'", 0)),
                new Ast.Expression.Literal('\b')
            ),
            Arguments.of("Character Escape",
                List.of(new Token(Token.Type.CHARACTER, "'\\b'", 0)),
                new Ast.Expression.Literal('\b')
            ),
            Arguments.of("Escaped Character Escape",
                List.of(new Token(Token.Type.STRING, "\"\\\\b\"", 0)),
                new Ast.Expression.Literal("\\b")
            ),
            Arguments.of("String Literal",
                List.of(new Token(Token.Type.STRING, "\"string\"", 0)),
                new Ast.Expression.Literal("string")
            ),
            Arguments.of("Escape Character",
                List.of(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                new Ast.Expression.Literal("Hello,\nWorld!")
            ),
            Arguments.of("String Escape Single Char",
                List.of(new Token(Token.Type.STRING, "\"\b\"", 0)),
                new Ast.Expression.Literal("\b")
            ),
            Arguments.of("String Escape Single Char",
                List.of(new Token(Token.Type.STRING, "\"\\b\"", 0)),
                new Ast.Expression.Literal("\b")
            ),
            Arguments.of("Multiple Escape Character",
                List.of(new Token(Token.Type.STRING, "\"Hello,\\nWorld\\n!\"", 0)),
                new Ast.Expression.Literal("Hello,\nWorld\n!")
            )
        );
    }

    //
    // TODO testLiteralExpressionError
    //

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        testParse(tokenExpressionWrapper(tokens), expressionWrapper(expected));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
            Arguments.of("Grouped Variable",
                List.of(
                    //(expr)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 1),
                    new Token(Token.Type.OPERATOR, ")", 5)
                ),
                new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
            ),
            Arguments.of("Grouped Binary",
                List.of(
                    //(expr1 + expr2)
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
                List.of(
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
                List.of(
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
                List.of(
                    // (expr1 + expr2 * expr3)
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

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenExpressionWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testGroupExpressionError() {
        return Stream.of(
            Arguments.of("Missing )",
                List.of(
                    // (expr
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 1)
                ),
                "Missing: )",
                1000
            ),
            Arguments.of("Wrong ]",
                List.of(
                    // (expr]
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr", 1),
                    new Token(Token.Type.OPERATOR, "]", 5)
                ),
                "Missing: )",
                5
            ),
            Arguments.of("Missing Closing ) Binary",
                List.of(
                    // (expr1 + expr2
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.OPERATOR, "+", 7),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9)
                ),
                "Missing: )",
                1000
            ),
            // TODO: need to fix should be "Missing: operator"
            Arguments.of("Missing Operator Binary",
                List.of(
                    // (expr1 expr2)
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "expr1", 1),
                    new Token(Token.Type.IDENTIFIER, "expr2", 7),
                    new Token(Token.Type.OPERATOR, ")", 11)
                ),
                "Missing: )",
                7
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        testParse(tokenExpressionWrapper(tokens), expressionWrapper(expected));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
            Arguments.of("Binary And",
                List.of(
                    //expr1 AND expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "AND", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 10)
                ),
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Binary Or",
                List.of(
                    // expr1 OR expr2
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.IDENTIFIER, "OR", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 10)
                ),
                new Ast.Expression.Binary("OR",
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2")
                )
            ),
            Arguments.of("Multiple Binary And",
                List.of(
                    // expr1 AND expr2 OR expr3
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.IDENTIFIER, "AND", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.IDENTIFIER, "OR", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 18)
                ),
                new Ast.Expression.Binary("OR",
                    new Ast.Expression.Binary("AND",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                )
            ),
            Arguments.of("Binary Equality",
                List.of(
                    //expr1 == expr2
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
                List.of(
                    //expr1 + expr2
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
                List.of(
                    //expr1 * expr2
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
                List.of(
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
                List.of(
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
                List.of(
                    // expr1 AND expr2 OR expr3
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
                List.of(
                    // expr1 == expr2 != expr3
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
            ),
            Arguments.of("Multiple Binary Logic Comparison",
                List.of(
                    // expr1 == expr2 AND expr3 == expr4
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "==", 6),
                    new Token(Token.Type.IDENTIFIER, "expr2", 9),
                    new Token(Token.Type.IDENTIFIER, "AND", 15),
                    new Token(Token.Type.IDENTIFIER, "expr3", 18),
                    new Token(Token.Type.OPERATOR, "==", 24),
                    new Token(Token.Type.IDENTIFIER, "expr4", 27)
                ),
                new Ast.Expression.Binary("AND",
                    new Ast.Expression.Binary("==",
                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                        new Ast.Expression.Access(Optional.empty(), "expr2")
                    ),
                    new Ast.Expression.Binary("==",
                        new Ast.Expression.Access(Optional.empty(), "expr3"),
                        new Ast.Expression.Access(Optional.empty(), "expr4")
                    )
                )
            ),
            Arguments.of("Signed Literal Multiplies Correctly",
                List.of(
                    new Token(Token.Type.OPERATOR, "-", 0),
                    new Token(Token.Type.INTEGER, "1", 1),
                    new Token(Token.Type.OPERATOR, "*", 3),
                    new Token(Token.Type.INTEGER, "2", 5)
                ),
                new Ast.Expression.Binary("*",
                    new Ast.Expression.Literal(new BigInteger("-1")),
                    new Ast.Expression.Literal(new BigInteger("2"))
                )
            ),
            Arguments.of("Signed Literal Adds Correctly",
                List.of(
                    new Token(Token.Type.INTEGER, "10", 0),
                    new Token(Token.Type.OPERATOR, "+", 3),
                    new Token(Token.Type.OPERATOR, "-", 5),
                    new Token(Token.Type.INTEGER, "1", 6)
                ),
                new Ast.Expression.Binary("+",
                    new Ast.Expression.Literal(new BigInteger("10")),
                    new Ast.Expression.Literal(new BigInteger("-1"))
                )
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpressionError(String test, List<Token> tokens,String expectedMessage, int expectedIndex) {
        testParseError(tokenExpressionWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testBinaryExpressionError() {
        return Stream.of(
            Arguments.of("Missing Operand",
                List.of(
                    // expr -
                    new Token(Token.Type.IDENTIFIER, "expr", 0),
                    new Token(Token.Type.OPERATOR, "-", 5)
                ),
                "Invalid Primary Expression",
                1000
            ),
            Arguments.of("Missing Operand Add",
                List.of(
                    // expr1 +
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "+", 6)
                ),
                "Invalid Primary Expression",
                1000
            ),
            Arguments.of("Missing Operand Logical",
                List.of(
                    // expr1 AND
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.IDENTIFIER, "AND", 6)
                ),
                "Invalid Primary Expression",
                1000
            ),
            Arguments.of("Missing Operand Mult",
                List.of(
                    // expr1 *
                    new Token(Token.Type.IDENTIFIER, "expr1", 0),
                    new Token(Token.Type.OPERATOR, "*", 6)
                ),
                "Invalid Primary Expression",
                1000
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        testParse(tokenExpressionWrapper(tokens), expressionWrapper(expected));
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
            Arguments.of("Variable",
                // name
                List.of(new Token(Token.Type.IDENTIFIER, "name", 0)),
                new Ast.Expression.Access(Optional.empty(), "name")
            ),
            Arguments.of("Field Access",
                List.of(
                    //obj.field
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.IDENTIFIER, "field", 4)
                ),
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "field")
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccessExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenExpressionWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testAccessExpressionError() {
        return Stream.of(
            Arguments.of("Missing Operand",
                List.of(
                    // obj.5
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.INTEGER, "5", 4)
                ),
                "Type Error. Expected: IDENTIFIER, Got: INTEGER",
                4
            ),
            Arguments.of("Invalid Expression",
                List.of(
                    // ?
                    new Token(Token.Type.OPERATOR, "?", 0)
                ),
                "Invalid Primary Expression",
                0
            ),
            Arguments.of("Unary Minus Before Identifier Not Allowed",
                List.of(
                    new Token(Token.Type.OPERATOR, "-", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 1)
                ),
                "Invalid Primary Expression",
                0
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        testParse(tokenExpressionWrapper(tokens), expressionWrapper(expected));
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
            Arguments.of("Zero Arguments",
                List.of(
                    //name()
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.OPERATOR, ")", 5)
                ),
                new Ast.Expression.Function(Optional.empty(), "name", List.of())
            ),
            Arguments.of("Multiple Arguments",
                List.of(
                    //name(expr1, expr2, expr3)
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "expr1", 5),
                    new Token(Token.Type.OPERATOR, ",", 10),
                    new Token(Token.Type.IDENTIFIER, "expr2", 12),
                    new Token(Token.Type.OPERATOR, ",", 17),
                    new Token(Token.Type.IDENTIFIER, "expr3", 19),
                    new Token(Token.Type.OPERATOR, ")", 24)
                ),
                new Ast.Expression.Function(Optional.empty(), "name", List.of(
                    new Ast.Expression.Access(Optional.empty(), "expr1"),
                    new Ast.Expression.Access(Optional.empty(), "expr2"),
                    new Ast.Expression.Access(Optional.empty(), "expr3")
                ))
            ),
            Arguments.of("Method Call",
                List.of(
                    //obj.method()
                    new Token(Token.Type.IDENTIFIER, "obj", 0),
                    new Token(Token.Type.OPERATOR, ".", 3),
                    new Token(Token.Type.IDENTIFIER, "method", 4),
                    new Token(Token.Type.OPERATOR, "(", 10),
                    new Token(Token.Type.OPERATOR, ")", 11)
                ),
                new Ast.Expression.Function(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "method", List.of())
            ),
            Arguments.of("Function Field Access",
                List.of(
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

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpressionError(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        testParseError(tokenExpressionWrapper(tokens), expectedMessage, expectedIndex);
    }

    private static Stream<Arguments> testFunctionExpressionError() {
        return Stream.of(
            Arguments.of("Trailing Comma",
                List.of(
                    // name(expr,)
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "expr", 5),
                    new Token(Token.Type.OPERATOR, ",", 9),
                    new Token(Token.Type.IDENTIFIER, ")", 10)
                ),
                "Trailing comma in argument list",
                10
            ),
            Arguments.of("Missing Parameters",
                List.of(
                    // name(expr1, expr2, )
                    new Token(Token.Type.IDENTIFIER, "name", 0),
                    new Token(Token.Type.OPERATOR, "(", 4),
                    new Token(Token.Type.IDENTIFIER, "expr1", 5),
                    new Token(Token.Type.OPERATOR, ",", 10),
                    new Token(Token.Type.IDENTIFIER, "expr2", 12),
                    new Token(Token.Type.OPERATOR, ",", 17),
                    new Token(Token.Type.OPERATOR, ")", 19)
                ),
                "Trailing comma in argument list",
                19
            )
        );
    }

    private static void testParse(List<Token> tokens, Ast.Source expected) {
        Parser parser = new Parser(tokens);
        Assertions.assertEquals(expected, parser.parse());
    }

    private static void testParseError(List<Token> tokens,
                                       String expectedMessage,
                                       int expectedIndex) {
        Parser parser = new Parser(tokens);
        CompilerException ex = Assertions.assertThrows(CompilerException.class, parser::parse);

        Assertions.assertEquals(CompilerException.class, ex.getClass());
        Assertions.assertEquals(expectedMessage, ex.getMessage());
        Assertions.assertEquals(expectedIndex, ex.getIndex().get());
    }

    private static Ast.Source fieldWrapper(Ast.Field subtree) {
        return new Ast.Source(
            List.of(subtree),
            List.of()
        );
    }

    private static Ast.Source methodWrapper(Ast.Method subtree) {
        return new Ast.Source(
            List.of(),
            List.of(subtree)
        );
    }

    private static Ast.Source statementWrapper(Ast.Statement subtree) {
        Ast.Method subtreeMethod = new Ast.Method(
            "main",
            List.of(),
            List.of(),
            Optional.empty(),
            List.of(subtree)
            );
        return methodWrapper(subtreeMethod);
    }

    private static Ast.Source expressionWrapper(Ast.Expression expression) {
        return statementWrapper(new Ast.Statement.Return(expression));
    }

    private static List<Token> tokenStatementWrapper(List<Token> statements) {
        List<Token> out = new java.util.ArrayList<>();

        out.add(new Token(Token.Type.IDENTIFIER, "DEF", 0));
        out.add(new Token(Token.Type.IDENTIFIER, "main", 4));
        out.add(new Token(Token.Type.OPERATOR, "(", 8));
        out.add(new Token(Token.Type.OPERATOR, ")", 9));
        out.add(new Token(Token.Type.IDENTIFIER, "DO", 11));

        out.addAll(statements);

        out.add(new Token(Token.Type.IDENTIFIER, "END", 1001));

        return out;
    }

    private static List<Token> tokenExpressionWrapper(List<Token> expressions) {
        List<Token> out = new java.util.ArrayList<>();
        out.add(new Token(Token.Type.IDENTIFIER, "RETURN", 14));

        out.addAll(expressions);

        out.add(new Token(Token.Type.OPERATOR, ";", 1000));

        return tokenStatementWrapper(out);
    }

}
