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

public class LexStringTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleToken(String name, String input) {
            testToken(input, Token.Type.STRING);
        }
        private static Stream<Arguments> singleToken() {
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
        void multipleTokens(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> multipleTokens() {
            return Stream.of(
                Arguments.of("Newline at End", "unterminated\n", List.of(
                    new Token(Token.Type.IDENTIFIER, "unterminated", 0)
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
    }
}
