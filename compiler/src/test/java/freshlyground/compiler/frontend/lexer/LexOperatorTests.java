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

public class LexOperatorTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleToken(String name, String input) {
            testToken(input, Token.Type.OPERATOR);
        }
        private static Stream<Arguments> singleToken() {
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
        void multipleTokens(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> multipleTokens() {
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sad {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleToken(String name, String input, String msg, int index) {
            testLexError(input, msg, index);
        }
        private static Stream<Arguments> singleToken() {
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
    }
}
