package freshlyground.compiler.frontend.lexer;

import freshlyground.compiler.frontend.artifacts.common.Token;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static freshlyground.compiler.frontend.lexer.LexerTestingSupport.testToken;
import static freshlyground.compiler.frontend.lexer.LexerTestingSupport.testTokens;

public class LexIdentifierTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleToken(String name, String input) {
            testToken(input, Token.Type.IDENTIFIER);
        }
        private static Stream<Arguments> singleToken() {
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
        void multipleTokens(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> multipleTokens() {
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
    }
}
