package freshlyground.compiler.frontend.lexer;

import freshlyground.compiler.frontend.artifacts.common.Token;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static freshlyground.compiler.frontend.lexer.LexerTestingSupport.*;

public class LexNumberTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleIntegerToken(String name, String input) {
            testToken(input, Token.Type.INTEGER);
        }
        private static Stream<Arguments> singleIntegerToken() {
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
        void multipleIntegerTokens(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> multipleIntegerTokens() {
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
        void singleDecimalToken(String name, String input) {
            testToken(input, Token.Type.DECIMAL);
        }
        private static Stream<Arguments> singleDecimalToken() {
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
        void multipleDecimalTokens(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> multipleDecimalTokens() {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sad {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleIntegerToken(String name, String input, String msg, int index) {
            testLexError(input, msg, index);
        }
        private static Stream<Arguments> singleIntegerToken() {
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
        void singleDecimalToken(String name, String input, String msg, int index) {
            testLexError(input, msg, index);
        }
        private static Stream<Arguments> singleDecimalToken() {
            return Stream.of(
                // No leading zeros
                Arguments.of("Leading Zero", "01.003", "No leading zeros", 1),
                Arguments.of("Multiple Leading Zeros", "00.3", "No leading zeros", 1),
                Arguments.of("Leading Signed Zero", "+01.003", "No leading zeros", 2)
            );
        }
    }
}
