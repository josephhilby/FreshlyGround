package freshlyground.compiler.frontend.lexer;

import freshlyground.compiler.frontend.artifacts.common.Token;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static freshlyground.compiler.frontend.lexer.LexerTestingSupport.*;

public class LexCharacterTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleToken(String name, String input) {
            testToken(input, Token.Type.CHARACTER);
        }
        private static Stream<Arguments> singleToken() {
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
    }
}
