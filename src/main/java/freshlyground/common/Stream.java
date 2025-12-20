package freshlyground.common;

import java.util.Objects;

public abstract class Stream<E, P> {
    @FunctionalInterface
    public interface PatternMatcher<E, P> {
        boolean matches(E element, P pattern);
    }

    private final PatternMatcher<E, P> matcher;
    protected int index = 0;

    protected Stream(PatternMatcher<E, P> matcher) {
        this.matcher = Objects.requireNonNull(matcher);
    }

    public abstract boolean has(int offset);
    public abstract E get(int offset);

    public int getIndex() { return index; }

    public void advance() { index++; }

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
}
