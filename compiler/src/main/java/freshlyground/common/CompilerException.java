package freshlyground.common;

import java.util.Optional;

/**
 * A {@code CompilerException} represents an unrecoverable error encountered during
 * compilation. It may originate from any compiler phase, including lexing,
 * parsing, semantic analysis, or code generation.
 *
 * <ul>
 *   <li>{@code message} — a human-readable description of the syntax error.</li>
 *   <li>{@code index} — an optional zero-based character position in the input stream
 *       associated with the error.</li>
 * </ul>
 *
 * <p>{@code CompilerException} represent user-facing compiler errors. Internal compiler
 * bugs should be given as unchecked exceptions.</p>
 */
public final class CompilerException extends RuntimeException {
    private final Integer index;

    public CompilerException(String message) {
        super(message);
        this.index = null;
    }

    public CompilerException(String message, int index) {
        super(message);
        this.index = index;
    }

    public Optional<Integer> getIndex() {
        return Optional.ofNullable(index);
    }
}
