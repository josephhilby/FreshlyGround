package plc.project;

/**
 * A {@code ParseException} represents an unrecoverable syntax error encountered
 * during lexical analysis or parsing. The exception records both a descriptive
 * error message and the character {@code index} in the source input where the
 * error was detected.
 *
 * <ul>
 *   <li>{@code message} — a human-readable description of the syntax error.</li>
 *   <li>{@code index} — the zero-based character position in the input stream
 *       associated with the error.</li>
 * </ul>
 *
 * <p>{@code ParseException} is thrown by the {@link Lexer} or {@link Parser} when invalid
 * syntax is encountered (e.g., malformed literals or unterminated strings) and
 * parsing cannot continue.</p>
 */
public final class ParseException extends RuntimeException {

    private final int index;

    public ParseException(String message, int index) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
