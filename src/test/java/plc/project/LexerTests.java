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
                Arguments.of("Underscores", "____", true),
                Arguments.of("Hyphenated", "a-b-c", true),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Single Char String", "a", true),
                Arguments.of("Long Char String", "abcdefghijklmnopqrstuvwxyz012346789_-", true),
                Arguments.of("AND", "AND", true),
                Arguments.of("OR", "OR", true),

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
                Arguments.of("Whitespace", "123 456", false),
                Arguments.of("Dash", "15-10", false),
                Arguments.of("Long Int", "123456789123456789123456789", true),
                Arguments.of("Comma Separated", "1,234", false),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Decimal Trailing Zero", "1.0", false),
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
                Arguments.of("Signed (+) Zero", "+0", false),
                Arguments.of("Signed (-) Zero", "-0", false),
                Arguments.of("Double Signed", "+0 -0", false)
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
                Arguments.of("Dash", "15-10", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Trailing Decimal Zero", "0.", false),
                Arguments.of("Double Decimal", "1..0", false),
                Arguments.of("Integers", "123.456", true),
                Arguments.of("One", "1.0",  true),

                // No leading zeros, unless the only digit to the left of the decimal point is a zero (0.[...])
                Arguments.of("Leading Zero", "01.003", false),
                Arguments.of("Leading Zero Dec", "0.003", true),
                Arguments.of("Zero", "0.0", true),

                // Trailing zeros are allowed
                Arguments.of("Trailing Zeros", "7.0000", true),
                Arguments.of("Trailing Zeros Zero", "0.000", true),

                // Optional sign + or - may immediately precede any decimal
                Arguments.of("Signed (-) Decimal", "-1.0", true),
                Arguments.of("Signed (-) Zero", "-0.0", true),
                Arguments.of("Leading Signed Zero", "+01.003", false),
                Arguments.of("Signed (+) Decimal", "+123.321", true)
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
                Arguments.of("Unicode Character", "\'ρ\'", true),
                Arguments.of("Character", "\'&\'", true),
                Arguments.of("Unterminated Char", "\'c", false),
                Arguments.of("Unterminated", "\'", false),
                Arguments.of("Not-Started Char", "c\'", false),

                // Contain one and only one character
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'ab\'", false),
                Arguments.of("Multiples", "\'abc\'", false),

                // Supports escape characters (\), (bnrt'"\) and considered one character
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Tab Escape", "\'\\t\'", true),
                Arguments.of("Backslash Escape", "\'\\\\\'", true),
                Arguments.of("Invalid Escape Character", "\'\\x\'", false),
                Arguments.of("Invalid (Unicode) Escape Character", "\'\\u12G4\'", false),

                // Character cannot be a single quote ('), without being preceded by a backslash (\)
                Arguments.of("Unterminated Quote", "\'\'\'", false),
                Arguments.of("Terminated Quote", "\'\\'\'", true),

                // Cannot span multiple lines, opening and closing quotes must be on the same line, no \n
                Arguments.of("Newline No Escape", "\'\n\'", false)
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
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Characters", "\"!@#$%^&*()\"", true),
                Arguments.of("Unicode", "\"ρ★⚡\"", true),
                Arguments.of("Whitespaces", "\" ␈␉\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),

                // Supports escape characters (\), (bnrt'"\) and considered one character
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Numeric Invalid Escapes", "\"abc\\0123\"", false),
                Arguments.of("Multiple Escapes", "\"a\\bcdefghijklm\\nopq\\rs\\tuvwxyz\"", true),
                Arguments.of("Special Escapes", "\"sq\\'dq\\\"bs\\\\\"", true),
                Arguments.of("Unicode Escapes", "\"a\\u0000b\\u12ABc\"", false),
                Arguments.of("Invalid Escape At Start", "\"\\e then a string\"", false),

                // Character cannot be a double quote ("), without being preceded by a backslash (\)
                Arguments.of("Quote", "\"\"\"", false),

                // Cannot span multiple lines, opening and closing quotes must be on the same line, no \n
                Arguments.of("Newline Escape", "\"Hello,\nWorld\"", false),
                Arguments.of("Newline at End", "unterminated␊", false)
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
            Arguments.of("Character", "(", true),
            Arguments.of("Unicode Character", "★", true),
            Arguments.of("Symbol", "$", true),
            Arguments.of("Plus", "+", true),
            Arguments.of("Literal", "'", false),
            Arguments.of("Space", " ", false),
            Arguments.of("Tab", "\t", false),
            Arguments.of("Formfeed", "\f", true),

            // Comparison (<=, >=, !=, ==) operators are special cases
            Arguments.of("Comparison", "<=", true),
            Arguments.of("Bang", "!=", true),
            Arguments.of("Equal", "==", true),
            Arguments.of("Extra Char", "<=>", false),
            Arguments.of("Double Operator", ">>", false)
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
                Arguments.of("Example 3", LexerTestData.source, LexerTestData.tokens),
                Arguments.of("Example 4", "LET x = 5-2;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, "-", 9),
                        new Token(Token.Type.INTEGER, "2", 10),
                        new Token(Token.Type.OPERATOR, ";", 11)
                )),
                Arguments.of("Example 5", "LET x = 5.1-2.2;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.DECIMAL, "5.1", 8),
                        new Token(Token.Type.OPERATOR, "-", 11),
                        new Token(Token.Type.DECIMAL, "2.2", 12),
                        new Token(Token.Type.OPERATOR, ";", 15)
                )),
                Arguments.of("Example 6", "LET x = 5 -2;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, "-", 10),
                        new Token(Token.Type.INTEGER, "2", 11),
                        new Token(Token.Type.OPERATOR, ";", 12)
                )),
                Arguments.of("Example 7", "LET x = 5 -0;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, "-", 10),
                        new Token(Token.Type.INTEGER, "0", 11),
                        new Token(Token.Type.OPERATOR, ";", 12)
                ))
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
