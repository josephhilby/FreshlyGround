package freshlyground.compiler.frontend;

import freshlyground.common.Stream;
import freshlyground.common.Token;

import java.util.List;

public final class TokenStream extends Stream<Token, Object> {
    private final List<Token> tokens;

    public TokenStream(List<Token> tokens) {
        super((token, pattern) -> {
            if (pattern instanceof Token.Type type) {
                return token.type() == type;
            }

            if (pattern instanceof String literal) {
                return token.literal().equals(literal);
            }

            return false;
        });
        this.tokens = tokens;
    }

    @Override
    public boolean has(int offset) {
        return index + offset < tokens.size();
    }

    @Override
    public Token get(int offset) {
        return tokens.get(index + offset);
    }
}
