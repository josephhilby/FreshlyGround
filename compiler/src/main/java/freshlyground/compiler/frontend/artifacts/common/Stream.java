package freshlyground.compiler.frontend.artifacts.common;

import freshlyground.compiler.frontend.artifacts.CharStream;
import freshlyground.compiler.frontend.artifacts.TokenStream;

import java.util.Objects;

/**
 * {@code Stream} is an abstraction that generalizes the sequential logic
 * shared by both {@link CharStream} (lexer)
 * and {@link TokenStream} (parser). It
 * provides a common mechanism for:
 *
 * <ul>
 *   <li>{@link #index} — the zero-based position in the input sequence of elements.</li>
 *   <li>{@link #peek} — matching elements to a given pattern and NOT advancing the index.</li>
 *   <li>{@link #match} — matching elements to a given pattern and advancing the index.</li>
 * </ul>
 *
 * <p>
 * The stream is parameterized by:
 * </p>
 *
 * <ul>
 *   <li>{@code E} — the type of elements in the stream (e.g., {@code Character},
 *       {@code Token})</li>
 *   <li>{@code P} — the pattern type used for comparison (e.g., {@code String},
 *       {@code Token.Type})</li>
 * </ul>
 */
public abstract class Stream<E, P> {
    protected int index = 0;
    private final PatternMatcher<E, P> matcher;

    protected Stream(PatternMatcher<E, P> matcher) {
        this.matcher = Objects.requireNonNull(matcher);
    }

    public int getIndex() { return index; }
    public void advance() { index++; }

    public abstract boolean has(int offset);
    public abstract E get(int offset);

    @SafeVarargs
    public final boolean peek(P... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!has(i)) {
                return false;
            }

            if (!matcher.matches(get(i), patterns[i])) {
                return false;
            }
        }

        return true;
    }

    @SafeVarargs
    public final boolean match(P... patterns) {
        if (!peek(patterns)) {
            return false;
        }

        for (int i = 0; i < patterns.length; i++) {
            advance();
        }

        return true;
    }

    @FunctionalInterface
    public interface PatternMatcher<E, P> {
        boolean matches(E element, P pattern);
    }
}
