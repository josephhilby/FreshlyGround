package freshlyground.compiler.frontend.artifacts;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.artifacts.common.Stream;
import freshlyground.compiler.frontend.artifacts.common.Token;

import java.util.List;
import java.util.Optional;

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

    public Token consume() {
        if (!has(0)) {
            throw new IllegalStateException("No current token");
        }
        Token token = get(0);
        advance();
        return token;
    }

    public Optional<Token> matchAny(String... literals) {
        for (String lit : literals) {
            if (peek(lit)) return Optional.of(consume());
        }
        return Optional.empty();
    }

    public Token expectType(Token.Type type) {
        if (!peek(type)) {
            Token.Type actual = has(0) ? get(0).type() : null;
            throw new CompilerException(
                "Type Error. Expected: " + type + ", Got: " + actual,
                location(0)
            );
        }
        return consume();
    }

    public void expectLiteral(String literal) {
        if (!peek(literal)) {
            throw new CompilerException(
                "Missing: " + literal,
                location(0)
            );
        }
        consume();
    }

    public int location(int offset) {
        if (has(offset)) return get(offset).index();
        int prev = offset - 1;
        if (has(prev)) {
            Token t = get(prev);
            return t.index() + t.literal().length();
        }
        return 0;
    }
}
