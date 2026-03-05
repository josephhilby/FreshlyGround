package freshlyground.compiler.frontend.lexer;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.artifacts.common.Token;
import org.junit.jupiter.api.Assertions;

import java.util.List;

final class LexerTestingSupport {
    private LexerTestingSupport() {}

    public static void testToken(String input, Token.Type expected) {
        testTokens(input, List.of(new Token(expected, input, 0)));
    }

    public static void testTokens(String input, List<Token> expected) {
        try {
            Assertions.assertEquals(expected, new Lexer(input).lex());
        } catch (CompilerException e) {
            Assertions.fail("Unexpected CompilerException: " + e.getMessage());
        }
    }

    public static void testLexError(String input, String expectedMessage, int expectedIndex) {
        CompilerException ex = Assertions.assertThrows(
            CompilerException.class,
            () -> new Lexer(input).lex()
        );
        Assertions.assertEquals(expectedMessage, ex.getMessage());
        Assertions.assertEquals(expectedIndex, ex.getIndex().get());
    }
}
