package freshlyground.compiler.frontend.lexer;

import freshlyground.compiler.frontend.artifacts.common.Token;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import java.util.List;
import java.util.stream.Stream;

import static freshlyground.compiler.frontend.lexer.LexerTestingSupport.testTokens;

public class LexEntryPointTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        void singleToken(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> singleToken() {
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
        void multipleTokens(String name, String input, List<Token> expected) {
            testTokens(input, expected);
        }
        private static Stream<Arguments> multipleTokens() {
            return Stream.of(
                Arguments.of("Simple LET assignment", "LET x = 5;", List.of(
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 4),
                    new Token(Token.Type.OPERATOR, "=", 6),
                    new Token(Token.Type.INTEGER, "5", 8),
                    new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Function call with string literal", "print(\"Hello, World!\");", List.of(
                    new Token(Token.Type.IDENTIFIER, "print", 0),
                    new Token(Token.Type.OPERATOR, "(", 5),
                    new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                    new Token(Token.Type.OPERATOR, ")", 21),
                    new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Binary subtraction without spaces", "LET x = 5-2;", List.of(
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 4),
                    new Token(Token.Type.OPERATOR, "=", 6),
                    new Token(Token.Type.INTEGER, "5", 8),
                    new Token(Token.Type.OPERATOR, "-", 9),
                    new Token(Token.Type.INTEGER, "2", 10),
                    new Token(Token.Type.OPERATOR, ";", 11)
                )),
                Arguments.of("Binary subtraction with decimals", "LET x = 5.1-2.2;", List.of(
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 4),
                    new Token(Token.Type.OPERATOR, "=", 6),
                    new Token(Token.Type.DECIMAL, "5.1", 8),
                    new Token(Token.Type.OPERATOR, "-", 11),
                    new Token(Token.Type.DECIMAL, "2.2", 12),
                    new Token(Token.Type.OPERATOR, ";", 15)
                )),
                Arguments.of("Right-adjacent operator spacing", "LET x = 5 -2;", List.of(
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 4),
                    new Token(Token.Type.OPERATOR, "=", 6),
                    new Token(Token.Type.INTEGER, "5", 8),
                    new Token(Token.Type.OPERATOR, "-", 10),
                    new Token(Token.Type.INTEGER, "2", 11),
                    new Token(Token.Type.OPERATOR, ";", 12)
                )),
                Arguments.of("Subtraction with zero literal", "LET x = 5 -0;", List.of(
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 4),
                    new Token(Token.Type.OPERATOR, "=", 6),
                    new Token(Token.Type.INTEGER, "5", 8),
                    new Token(Token.Type.OPERATOR, "-", 10),
                    new Token(Token.Type.INTEGER, "0", 11),
                    new Token(Token.Type.OPERATOR, ";", 12)
                )),
                Arguments.of("Mixed token sequence", "abc 123 456.789 'c' \"string\" /", List.of(
                    new Token(Token.Type.IDENTIFIER, "abc", 0),
                    new Token(Token.Type.INTEGER, "123", 4),
                    new Token(Token.Type.DECIMAL, "456.789", 8),
                    new Token(Token.Type.CHARACTER, "'c'", 16),
                    new Token(Token.Type.STRING, "\"string\"", 20),
                    new Token(Token.Type.OPERATOR, "/", 29)
                )),
                Arguments.of("Arithmetic expression with spaces", "15 - 10", List.of(
                    new Token(Token.Type.INTEGER, "15", 0),
                    new Token(Token.Type.OPERATOR, "-", 3),
                    new Token(Token.Type.INTEGER, "10", 5)
                )),
                Arguments.of("Integer followed by member access", "1.a", List.of(
                    new Token(Token.Type.INTEGER, "1", 0),
                    new Token(Token.Type.OPERATOR, ".", 1),
                    new Token(Token.Type.IDENTIFIER, "a", 2)
                ))
            );
        }
    }
}
