package freshlyground.frontend;

/**
 * A {@code Token} represents a single lexical unit produced by the {@link Lexer}.
 * Each token captures:
 *
 * <ul>
 *   <li>{@link Type} — categorizes the token (e.g., identifier, integer, decimal,
 *       character, string, and operator).</li>
 *   <li>{@code literal} — the exact character sequence consumed from the source.</li>
 *   <li>{@code index} — the zero-based position in the input stream where the token begins.</li>
 * </ul>
 *
 * <p>Tokens are immutable value objects and are compared using structural equality;
 * two tokens are considered equal iff their type, literal, and index
 * are equal.</p>
 */
public record Token(Type type, String literal, int index) {

    public enum Type {
        IDENTIFIER,
        INTEGER,
        DECIMAL,
        CHARACTER,
        STRING,
        OPERATOR
    }

    @Override
    public String toString() {
        return type + "=" + literal + "@" + index;
    }
}
