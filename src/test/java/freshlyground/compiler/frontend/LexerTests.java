package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.common.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testEntryPoint(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testEntryPoint() {
        return Stream.of(
            Arguments.of("Empty", "", List.of()),
            Arguments.of("Backspace", "\b", List.of()),
            Arguments.of("Line Feed", "\n", List.of()),
            Arguments.of("Carriage Return", "\r", List.of()),
            Arguments.of("Tab", "\t", List.of()),
            Arguments.of("Leading Whitespace", " LET", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 1)
            )),
            Arguments.of("Trailing Whitespace", "LET ", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 0)
            )),
            Arguments.of("Mixed Whitespace", " \t\r\nLET", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 4)
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIdentifier(String name, String input) {
        testToken(input, Token.Type.IDENTIFIER);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
            Arguments.of("Alphabetic", "getName"),
            Arguments.of("Alphanumeric", "thelegend27"),
            Arguments.of("Caps", "ABC"),
            Arguments.of("Underscore", "_abC01"),
            Arguments.of("Underscores", "____"),
            Arguments.of("Hyphenated", "a-b-c"),
            Arguments.of("Single Char String", "a"),
            Arguments.of("AND", "AND"),
            Arguments.of("OR", "OR")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIdentifiers(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testIdentifiers() {
        return Stream.of(
            Arguments.of("Leading Digit", "1fish2bluefish", List.of(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.IDENTIFIER, "fish2bluefish", 1)
            )),
            Arguments.of("Leading Hyphen", "-five", List.of(
                new Token(Token.Type.OPERATOR, "-", 0),
                new Token(Token.Type.IDENTIFIER, "five", 1)
            )),
            Arguments.of("Trailing Dash", "abcdefghijklmnopqrstuvwxyz012346789_-", List.of(
                new Token(Token.Type.IDENTIFIER, "abcdefghijklmnopqrstuvwxyz012346789_", 0),
                new Token(Token.Type.OPERATOR, "-", 36)
            )),
            Arguments.of("Double Dash", "a--b", List.of(
                new Token(Token.Type.IDENTIFIER, "a", 0),
                new Token(Token.Type.OPERATOR, "-", 1),
                new Token(Token.Type.OPERATOR, "-", 2),
                new Token(Token.Type.IDENTIFIER, "b", 3)
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIntegerHappyPath(String name, String input) {
        testToken(input, Token.Type.INTEGER);
    }

    private static Stream<Arguments> testIntegerHappyPath() {
        return Stream.of(
            // Integer number
            Arguments.of("Single Digit", "1"),
            Arguments.of("Multiple Digits", "1234"),
            Arguments.of("Long Int", "123456789123456789123456789"),

            // Integer zero is merely just zero
            Arguments.of("Zero", "0"),

            // Trailing zeros are allowed
            Arguments.of("Trailing Zeros", "700")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIntegerSadPath(String name, String input, String msg, int index) {
        testLexError(input, msg, index);
    }

    private static Stream<Arguments> testIntegerSadPath() {
        return Stream.of(
            // No leading zeros
            Arguments.of("Zeros", "00", "No leading zeros", 1),
            Arguments.of("Leading Zero", "01", "No leading zeros", 1),
            Arguments.of("Signed (+) Leading Zero", "+01", "No leading zeros", 2),
            Arguments.of("Leading Zeros", "007", "No leading zeros", 1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIntegers(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testIntegers() {
        return Stream.of(
            Arguments.of("Two Integers With Space", "123 456", List.of(
                new Token(Token.Type.INTEGER, "123", 0),
                new Token(Token.Type.INTEGER, "456", 4)
            )),
            Arguments.of("Subtraction No Spaces", "15-10", List.of(
                new Token(Token.Type.INTEGER, "15", 0),
                new Token(Token.Type.OPERATOR, "-", 2),
                new Token(Token.Type.INTEGER, "10", 3)
            )),
            Arguments.of("Subtraction With Spaces", "15 - 10", List.of(
                new Token(Token.Type.INTEGER, "15", 0),
                new Token(Token.Type.OPERATOR, "-", 3),
                new Token(Token.Type.INTEGER, "10", 5)
            )),
            Arguments.of("Comma Separated", "1,234", List.of(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.OPERATOR, ",", 1),
                new Token(Token.Type.INTEGER, "234", 2)
            )),
            Arguments.of("Signed (-) Int", "-1", List.of(
                new Token(Token.Type.OPERATOR, "-", 0),
                new Token(Token.Type.INTEGER, "1", 1)
            )),
            Arguments.of("Signed (+) Zero", "+0", List.of(
                new Token(Token.Type.OPERATOR, "+", 0),
                new Token(Token.Type.INTEGER, "0", 1)
            )),
            Arguments.of("Signed (+) Ints", "+1234", List.of(
                new Token(Token.Type.OPERATOR, "+", 0),
                new Token(Token.Type.INTEGER, "1234", 1)
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDecimalHappyPath(String name, String input) {
        testToken(input, Token.Type.DECIMAL);
    }

    private static Stream<Arguments> testDecimalHappyPath() {
        return Stream.of(
            // Two integer values separated by a decimal point
            Arguments.of("Integers", "123.456"),
            Arguments.of("One", "1.0"),

            // No leading zeros, unless the only digit to the left of the decimal point is a zero (0.[...])
            Arguments.of("Leading Zero Dec", "0.003"),
            Arguments.of("Zero", "0.0"),

            // Trailing zeros are allowed
            Arguments.of("Trailing Zeros", "7.0000"),
            Arguments.of("Trailing Zeros Zero", "0.000")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDecimalSadPath(String name, String input, String msg, int index) {
        testLexError(input, msg, index);
    }

    private static Stream<Arguments> testDecimalSadPath() {
        return Stream.of(
            // No leading zeros
            Arguments.of("Leading Zero", "01.003", "No leading zeros", 1),
            Arguments.of("Multiple Leading Zeros", "00.3", "No leading zeros", 1),
            Arguments.of("Leading Signed Zero", "+01.003", "No leading zeros", 2)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDecimals(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testDecimals() {
        return Stream.of(
            Arguments.of("Leading Decimal", ".5", List.of(
                new Token(Token.Type.OPERATOR, ".", 0),
                new Token(Token.Type.INTEGER, "5", 1)
            )),
            Arguments.of("Trailing Decimal", "1.", List.of(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.OPERATOR, ".", 1)
            )),
            Arguments.of("Trailing Decimal Zero", "0.", List.of(
                new Token(Token.Type.INTEGER, "0", 0),
                new Token(Token.Type.OPERATOR, ".", 1)
            )),
            Arguments.of("Double Decimal", "1..0", List.of(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.OPERATOR, ".", 1),
                new Token(Token.Type.OPERATOR, ".", 2),
                new Token(Token.Type.INTEGER, "0", 3)
            )),
            Arguments.of("Signed (-) Decimal", "-1.0", List.of(
                new Token(Token.Type.OPERATOR, "-", 0),
                new Token(Token.Type.DECIMAL, "1.0", 1)
            )),
            Arguments.of("Signed (-) Zero", "-0.0", List.of(
                new Token(Token.Type.OPERATOR, "-", 0),
                new Token(Token.Type.DECIMAL, "0.0", 1)
            )),
            Arguments.of("Signed (+) Decimal", "+123.321", List.of(
                new Token(Token.Type.OPERATOR, "+", 0),
                new Token(Token.Type.DECIMAL, "123.321", 1)
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testCharacterHappyPath(String name, String input) {
        testToken(input, Token.Type.CHARACTER);
    }

    private static Stream<Arguments> testCharacterHappyPath() {
        return Stream.of(
            // Start and end with a single quote (')
            Arguments.of("Alphabetic", "'c'"),
            Arguments.of("Unicode Character", "'ρ'"),
            Arguments.of("Character", "'&'"),

            // Supports escape characters (\), (bnrt'"\) and considered one character
            Arguments.of("Newline Escape", "'\\n'"),
            Arguments.of("Tab Escape", "'\\t'"),
            Arguments.of("Backslash Escape", "'\\\\'"),

            // Character cannot be a single quote ('), without being preceded by a backslash (\)
            Arguments.of("Terminated Quote", "'\\''")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testCharacterSadPath(String name, String input, String msg, int index) {
        testLexError(input, msg, index);
    }

    private static Stream<Arguments> testCharacterSadPath() {
        return Stream.of(
            // Start and end with a single quote (')
            Arguments.of("Unterminated Char", "'c", "Unterminated character literal or oversized character", 2),
            Arguments.of("Unterminated", "'", "Missing char/string literal or empty/invalid character", 1),
            Arguments.of("Not-Started Char", "c'", "Missing char/string literal or empty/invalid character", 2),
            Arguments.of("Empty", "''", "Missing char/string literal or empty/invalid character", 1),

            // Contain one and only one character
            Arguments.of("Multiple", "'ab'", "Unterminated character literal or oversized character", 2),
            Arguments.of("Multiples", "'abc'", "Unterminated character literal or oversized character", 2),

            // Supports escape characters (\), (bnrt'"\) and considered one character
            Arguments.of("Invalid Escape Character", "'\\x'", "Invalid escape character", 2),
            Arguments.of("Invalid (Unicode) Escape Character", "'\\u12G4'", "Invalid escape character", 2),

            // Character cannot be a single quote ('), without being preceded by a backslash (\)
            Arguments.of("Unterminated Quote", "'''", "Missing char/string literal or empty/invalid character", 1),

            // Cannot span multiple lines, opening and closing quotes must be on the same line, no \n
            Arguments.of("Newline No Escape", "'\n'", "Unescaped new line or carriage return", 2)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testStringHappyPath(String name, String input) {
        testToken(input, Token.Type.STRING);
    }

    private static Stream<Arguments> testStringHappyPath() {
        return Stream.of(
            // strings start and end with a double quote (")
            Arguments.of("Empty", "\"\""),
            Arguments.of("Alphabetic", "\"abc\""),
            Arguments.of("Characters", "\"!@#$%^&*()\""),
            Arguments.of("Unicode", "\"ρ★⚡\""),
            Arguments.of("Whitespaces", "\" ␈␉\""),

            // Supports escape characters (\), (bnrt'"\) and considered one character
            Arguments.of("Newline Escape", "\"Hello,\\nWorld\""),
            Arguments.of("Multiple Escapes", "\"a\\bcdefghijklm\\nopq\\rs\\tuvwxyz\""),
            Arguments.of("Special Escapes", "\"sq\\'dq\\\"bs\\\\\"")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testStringSadPath(String name, String input, String msg, int index) {
        testLexError(input, msg, index);
    }

    private static Stream<Arguments> testStringSadPath() {
        return Stream.of(
            // Start and end with a single quote (')
            Arguments.of("Unterminated", "\"unterminated", "Unterminated string literal", 13),

            // Supports escape characters (\), (bnrt'"\) and considered one character
            Arguments.of("Invalid Escape", "\"invalid\\escape\"", "Invalid escape character", 9),
            Arguments.of("Numeric Invalid Escapes", "\"abc\\0123\"", "Invalid escape character", 5),
            Arguments.of("Unicode Escapes", "\"a\\u0000b\\u12ABc\"", "Invalid escape character", 3),
            Arguments.of("Invalid Escape At Start", "\"\\e then a string\"", "Invalid escape character", 2),

            // Character cannot be a single quote ('), without being preceded by a backslash (\)
            Arguments.of("Quote", "\"\"\"", "Unterminated string literal", 3),

            // Cannot span multiple lines, opening and closing quotes must be on the same line, no \n
            Arguments.of("Newline Escape", "\"Hello,\nWorld\"", "Unescaped new line or carriage return", 8)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testStrings(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testStrings() {
        return Stream.of(
            Arguments.of("Newline at End", "unterminated\n", List.of(
                new Token(Token.Type.IDENTIFIER, "unterminated", 0)
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testOperatorHappyPath(String name, String input) {
        testToken(input, Token.Type.OPERATOR);
    }

    private static Stream<Arguments> testOperatorHappyPath() {
        return Stream.of(
            // Two-character operators
            Arguments.of("Less Than Or Equal", "<="),
            Arguments.of("Greater Than Or Equal", ">="),
            Arguments.of("Equal Equal", "=="),
            Arguments.of("Not Equal", "!="),

            // Single-character comparison
            Arguments.of("Less Than", "<"),
            Arguments.of("Greater Than", ">"),

            // Arithmetic
            Arguments.of("Plus", "+"),
            Arguments.of("Minus", "-"),
            Arguments.of("Multiply", "*"),
            Arguments.of("Divide", "/"),

            // Member access / punctuation
            Arguments.of("Dot", "."),
            Arguments.of("Left Paren", "("),
            Arguments.of("Right Paren", ")"),
            Arguments.of("Comma", ","),
            Arguments.of("Colon", ":"),
            Arguments.of("Assign", "="),
            Arguments.of("Semicolon", ";")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testOperatorSadPath(String name, String input, String msg, int index) {
        testLexError(input, msg, index);
    }

    private static Stream<Arguments> testOperatorSadPath() {
        return Stream.of(
            // Any other single character, excluding whitespace
            Arguments.of("Unicode Character", "★", "Unexpected character", 0),
            Arguments.of("Symbol", "$", "Unexpected character", 0),
            Arguments.of("Formfeed", "\f", "Unexpected character", 0),
            Arguments.of("Percent", "%", "Unexpected character", 0),
            Arguments.of("At", "@", "Unexpected character", 0),
            Arguments.of("Bang", "!", "Unexpected character", 0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testOperators(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testOperators() {
        return Stream.of(
            Arguments.of("Extra Char", "<=>", List.of(
                new Token(Token.Type.OPERATOR, "<=", 0),
                new Token(Token.Type.OPERATOR, ">", 2)
            )),
            Arguments.of("Double Operator", ">>", List.of(
                new Token(Token.Type.OPERATOR, ">", 0),
                new Token(Token.Type.OPERATOR, ">", 1)
            ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExamples(String name, String input, List<Token> expected) {
        testTokens(input, expected);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
            Arguments.of("Example 1", "LET x = 5;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "5", 8),
                new Token(Token.Type.OPERATOR, ";", 9)
            )),
            Arguments.of("Example 2", "print(\"Hello, World!\");", List.of(
                new Token(Token.Type.IDENTIFIER, "print", 0),
                new Token(Token.Type.OPERATOR, "(", 5),
                new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                new Token(Token.Type.OPERATOR, ")", 21),
                new Token(Token.Type.OPERATOR, ";", 22)
            )),
            Arguments.of("Example 4", "LET x = 5-2;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "5", 8),
                new Token(Token.Type.OPERATOR, "-", 9),
                new Token(Token.Type.INTEGER, "2", 10),
                new Token(Token.Type.OPERATOR, ";", 11)
            )),
            Arguments.of("Example 5", "LET x = 5.1-2.2;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.DECIMAL, "5.1", 8),
                new Token(Token.Type.OPERATOR, "-", 11),
                new Token(Token.Type.DECIMAL, "2.2", 12),
                new Token(Token.Type.OPERATOR, ";", 15)
            )),
            Arguments.of("Example 6", "LET x = 5 -2;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "5", 8),
                new Token(Token.Type.OPERATOR, "-", 10),
                new Token(Token.Type.INTEGER, "2", 11),
                new Token(Token.Type.OPERATOR, ";", 12)
            )),
            Arguments.of("Example 7", "LET x = 5 -0;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "5", 8),
                new Token(Token.Type.OPERATOR, "-", 10),
                new Token(Token.Type.INTEGER, "0", 11),
                new Token(Token.Type.OPERATOR, ";", 12)
            )),
            Arguments.of("Example 8", "abc 123 456.789 'c' \"string\" /", List.of(
                new Token(Token.Type.IDENTIFIER, "abc", 0),
                new Token(Token.Type.INTEGER, "123", 4),
                new Token(Token.Type.DECIMAL, "456.789", 8),
                new Token(Token.Type.CHARACTER, "'c'", 16),
                new Token(Token.Type.STRING, "\"string\"", 20),
                new Token(Token.Type.OPERATOR, "/", 29)
            )),
            Arguments.of("Example 9", "15 - 10", List.of(
                new Token(Token.Type.INTEGER, "15", 0),
                new Token(Token.Type.OPERATOR, "-", 3),
                new Token(Token.Type.INTEGER, "10", 5)
            )),
            Arguments.of("Example 10", "1.a", List.of(
                new Token(Token.Type.INTEGER, "1", 0),
                new Token(Token.Type.OPERATOR, ".", 1),
                new Token(Token.Type.IDENTIFIER, "a", 2)
            ))
        );
    }

    /**
     * Happy-path: lexing produces exactly one token
     */
    private static void testToken(String input, Token.Type expected) {
        testTokens(input, List.of(new Token(expected, input, 0)));
    }

    /**
     * Happy-path: lexing produces the expected token list.
     */
    private static void testTokens(String input, List<Token> expected) {
        try {
            Assertions.assertEquals(expected, new Lexer(input).lex());
        } catch (CompilerException e) {
            Assertions.fail("Unexpected CompilerException: " + e.getMessage());
        }
    }

    /**
     * Sad-path: lexing throws CompilerException with expected message and index.
     */
    private static void testLexError(String input, String expectedMessage, int expectedIndex) {
        CompilerException ex = Assertions.assertThrows(
            CompilerException.class,
            () -> new Lexer(input).lex()
        );
        Assertions.assertEquals(expectedMessage, ex.getMessage());
        Assertions.assertEquals(expectedIndex, ex.getIndex().get());
    }
}
