package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer is responsible for converting a raw input stream of characters
 * into a sequence of tokens for later parsing. Its operation is centered around
 * three core components:
 *
 * <ul>
 *   <li>{@link #lex()} — repeatedly calls {@link #lexToken()} to produce tokens,
 *       automatically skipping over whitespace and comments.</li>
 *   <li>{@link #lexToken()} — performs the actual tokenization of the next
 *       lexical unit in the input stream.</li>
 *   <li>{@link CharStream} — maintains the lexer’s state, including the current
 *       position, character buffer, and literal construction.</li>
 * </ul>
 *
 * <p>If the lexer encounters invalid syntax (e.g., an unterminated string or
 * malformed literal), it must throw a {@link ParseException} at the character
 * index where the error occurred.</p>
 *
 * <p>Utility methods {@link #peek(String...)} and {@link #match(String...)} are
 * provided to simplify token recognition and should be used whenever possible
 * to keep the implementation concise and reliable.</p>
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Scans the entire input stream and produces a list of {@link Token} objects.
     * <p>
     * This method repeatedly calls {@link #lexToken()} to extract the next token
     * from the input, automatically skipping over whitespace characters such as
     * spaces, tabs, and newlines. The process continues until the end of the
     * character stream is reached.
     * </p>
     *
     * @return a list of all {@link Token} instances produced from the input
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (match("[ \b\n\r\t]")) {
                chars.skip();
            } else {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    /**
     * Determines the type of the next token and delegates to the corresponding
     * lexing routine.
     *
     * <p>Since whitespace is already handled by {@link #lex()}, the current
     * character is guaranteed to begin a valid token.</p>
     *
     * @return the next {@link Token} identified in the input stream
     */
    public Token lexToken() {
        if (match("[A-Za-z_]")) {
            return lexIdentifier();
        }
        if (peek("[0-9]")
                || (peek("[+-]", "[0-9]")
                && chars.previous != Token.Type.INTEGER
                && chars.previous != Token.Type.DECIMAL)) {
            return lexNumber(match("[+-]"));
        }
        if (match("'")) {
            return lexCharacter();
        }
        if (match("\"")) {
            return lexString();
        }
        return lexOperator();
    }

    /**
     * Lexes an identifier token from the input stream.
     * <p>
     * Grammar rule:
     * </p>
     * <pre>
     * identifier ::= [A-Za-z_] [A-Za-z0-9_-]*
     * </pre>
     * <p>
     * Identifiers begin with a letter or underscore, followed by any combination
     * of letters, digits, underscores, or hyphens. This method consumes all
     * consecutive characters that match this pattern and emits an
     * {@link Token.Type#IDENTIFIER}.
     * </p>
     *
     * @return a {@link Token} representing the lexed identifier
     */
    public Token lexIdentifier() {
        while (match("[A-Za-z0-9_-]"));
        return chars.emit(Token.Type.IDENTIFIER);
    }

    /**
     * Lexes a numeric literal from the input stream.
     * <p>
     * Grammar rule:
     * </p>
     * <pre>
     * number ::= [+-]? [0-9]+ ( '.' [0-9]+ )?
     * </pre>
     * <p>
     * A number may optionally begin with a sign ({@code +} or {@code -}),
     * followed by one or more digits. If a decimal point appears and is followed
     * by at least one digit, the token is classified as
     * {@link Token.Type#DECIMAL}; otherwise, it is classified as
     * {@link Token.Type#INTEGER}.
     * </p>
     *
     * <p>Leading zeros and signed zero cases are validated by
     * {@link #checkLeadingZeros()} and {@link #checkSignedZero(boolean)} before
     * consuming digits.</p>
     *
     * @param signed {@code true} if the number includes a leading sign,
     *               {@code false} otherwise
     * @return a {@link Token} representing either an integer or decimal literal
     */
    public Token lexNumber(boolean signed) {
        checkLeadingZeros();
        checkSignedZero(signed);
        while (match("[0-9]"));
        if (match("\\.", "[0-9]")) {
            while (match("[0-9]"));
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }

    /**
     * Lexes a character literal from the input stream.
     * <p>
     * Grammar rule:
     * </p>
     * <pre>
     * character ::= "'" ( [^'\n\r] | escape ) "'"
     * </pre>
     * <p>
     * A character literal begins and ends with single quotes and contains either
     * a single non-quote, non-newline character or a valid escape sequence
     * (e.g., {@code '\n'}, {@code '\\'}, {@code '\''}). This method validates
     * that the literal is properly formed and emits a
     * {@link Token.Type#CHARACTER} token.
     * </p>
     *
     * <p>If the literal is empty, unclosed, or invalid, a
     * {@link ParseException} is thrown via {@link #lexError(String)}.</p>
     *
     * @return a {@link Token} representing the parsed character literal
     * @throws ParseException if the character literal is malformed or unterminated
     */
    public Token lexCharacter() {
        matchCharacter();
        match("[^']");
        if (match("'") && chars.length > 2) {
            return chars.emit(Token.Type.CHARACTER);
        }
        lexError("Missing char literal or empty/invalid character");
        return null;
    }

    /**
     * Lexes a string literal from the input stream.
     * <p>
     * Grammar rule:
     * </p>
     * <pre>
     * string ::= '"' ( [^"\n\r] | escape )* '"'
     * </pre>
     * <p>
     * A string literal begins and ends with double quotes and may contain any
     * sequence of characters except unescaped quotes or newlines. Valid escape
     * sequences (e.g., {@code \"}, {@code \\n}, {@code \\t}) are processed within
     * the literal. This method consumes all valid characters until the closing
     * quote is reached and emits a {@link Token.Type#STRING} token.
     * </p>
     *
     * <p>If the string literal is missing a closing quote or is otherwise
     * malformed, a {@link ParseException} is thrown via
     * {@link #lexError(String)}.</p>
     *
     * @return a {@link Token} representing the parsed string literal
     * @throws ParseException if the string literal is unterminated or invalid
     */
    public Token lexString() {
        do {
            matchCharacter();
        } while (match("[^\"]"));
        if (match("\"")) {
            return chars.emit(Token.Type.STRING);
        }
        lexError("Missing str literal");
        return null;
    }

    // escape ::= '\' [bnrt'"\]
    public void lexEscape() {
        if (match("[bnrt'\"\\\\]")) {
            return;
        }
        lexError("Invalid escape character");
    }

    // operator ::= [<>!=] '='? | 'any character'
    public Token lexOperator() {
        if (match("[<>!=]", "=")) {
        } else {
            chars.advance();
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    // helper
    private void checkLeadingZeros() {
        if (peek("0", "[0-9]")) {
            lexError("No leading zeros");
        }
    }

    // helper
    private void checkSignedZero(boolean signed) {
        if (signed && match("0") && !peek("\\.", "[0-9]")) {
            lexError("No signed zero integers");
        }
    }

    // helper
    private void matchCharacter() {
        if (match("\\\\")) {
            lexEscape();
        }
        if (match("[\n\r]")) {
            lexError("Unescaped new line or carriage return");
        }
    }

    // helper
    private void lexError(String message) {
        lexError(message, chars.index);
    }

    // helper
    private void lexError(String message, int index) {
        throw new ParseException(
            message,
            chars.index
        );
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;
        private Token.Type previous = null;

        public CharStream(String input) { this.input = input; }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            previous = type;
            return new Token(type, input.substring(start, index), start);
        }
    }
}
