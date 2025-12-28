package freshlyground.common;

import freshlyground.compiler.frontend.Lexer;

/**
 * A {@code Token} represents a single lexical unit of lexical grammar
 * produced by the {@link Lexer}. Each token captures:
 *
 * <ul>
 *   <li>{@link Type} — categorizes the token (e.g., identifier, integer, decimal,
 *       character, string, and operator).</li>
 *   <li>{@code literal} — the exact character sequence consumed from the source.</li>
 *   <li>{@code index} — the zero-based position in the input stream where the token begins.</li>
 * </ul>
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
