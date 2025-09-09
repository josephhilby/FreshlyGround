package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                // Allows alphanumeric characters, underscores, and hyphens ([A-Za-z0-9_-])
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Underscore", "_abC01", true),
                Arguments.of("Hyphenated", "a-b-c", true),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Single Char", "a", true),

                // But cannot start with a digit or a hyphen [-]
                Arguments.of("Leading Hyphen", "-five", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                // Integer number
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "1234", true),
                Arguments.of("Comma Separated", "1,234", false),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),

                // Integer zero is merely just zero
                Arguments.of("Zero", "0", true),
                Arguments.of("Zeros", "00", false),

                // Leading zeros are not permitted
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Signed (+) Leading Zero", "+01", false),
                Arguments.of("Leading Zeros", "007", false),

                // Trailing zeros are allowed
                Arguments.of("Trailing Zeros", "700", true),

                // Optional sign + or - is allowed to prefix
                Arguments.of("Signed (-) Int", "-1", true),
                Arguments.of("Signed (+) Int", "+1", true),
                Arguments.of("Signed (+) Ints", "+1234", true),
                Arguments.of("Signed Decimal", "-1.0", false),

                // Except for a zero (0) integer
                Arguments.of("Signed (-) Zero", "+0", true),
                Arguments.of("Signed (+) Zero", "-0", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                // Two integer values separated by a decimal point
                Arguments.of("Integer", "1", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Trailing Decimal Zero", "0.", false),
                Arguments.of("Double Decimal", "1..0", false),
                Arguments.of("Integers", "123.456", true),

                // No leading zeros, unless the only digit to the left of the decimal point is a zero (0.[...])
                Arguments.of("Leading Zero", "01.003", false),
                Arguments.of("Leading Zero Dec", "0.003", true),
                Arguments.of("Zero", "0.0", true),

                // Trailing zeros are allowed
                Arguments.of("Trailing Zeros", "7.0000", true),

                // Optional sign + or - may immediately precede any decimal
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Leading Signed Zero", "+01.003", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                // Start and end with a single quote (')
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Unterminated", "\'c", false),
                Arguments.of("Not-Started", "c\'", false),

                // Contain one and only one character
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'ab\'", false),
                Arguments.of("Multiples", "\'abc\'", false),

                // Supports escape characters (\), (bnrt'"\) and considered one character
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Tab Escape", "\'\\t\'", true),

                // Character cannot be a single quote ('), without being preceded by a backslash (\)
                Arguments.of("Unterminated Quote", "\'\'\'", false),
                Arguments.of("Terminated Quote", "\'\\'\'", true),

                // Cannot span multiple lines, opening and closing quotes must be on the same line, no \n
                Arguments.of("Newline No Escape", "\'\n\'", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                // strings start and end with a double quote (")
                // Supports escape characters (\), (bnrt'"\) and considered one character
                // Character cannot be a double quote ("), without being preceded by a backslash (\)
                // Cannot span multiple lines, opening and closing quotes must be on the same line, no \n
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                // Any other single character, excluding whitespace
                // Comparison (<=, >=, !=, ==) operators are special cases
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "<=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", LexerTestData.source, LexerTestData.tokens)
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
