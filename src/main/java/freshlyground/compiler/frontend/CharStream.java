package freshlyground.compiler.frontend;

import freshlyground.common.Stream;
import freshlyground.common.Token;

/**
 * A helper class maintaining the state of the input string, by tracking the input string's
 * current {@code index} location, the current {@link Token}'s {@code length}, and the {@code previous}
 * {@link Token.Type}.
 */
public final class CharStream extends Stream<Character, String> {
    private final String source;
    private int length = 0;
    private Token.Type previous = null;

    public CharStream(String source) {
        super((character, regex) -> String.valueOf(character).matches(regex));
        this.source = source;
    }

    @Override
    public boolean has(int offset) {
        return index + offset < source.length();
    }

    @Override
    public Character get(int offset) {
        return source.charAt(index + offset);
    }

    @Override
    public void advance() {
        super.advance();
        length++;
    }

    public void skip() {
        length = 0;
    }

    public int getLength() {
        return length;
    }

    public Token.Type getPrevious() {
        return previous;
    }

    public Token emit(Token.Type type) {
        previous = type;
        int start = index - length;
        String literal = source.substring(start, index);
        skip();
        return new Token(type, literal, start);
    }
}
