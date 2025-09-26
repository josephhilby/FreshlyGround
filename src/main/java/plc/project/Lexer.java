package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
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
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
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

    // identifier ::= [A-Za-z_] [A-Za-z0-9_#x2D]*
    // #x2D is -
    public Token lexIdentifier() {
        while (match("[A-Za-z0-9_-]"));
        return chars.emit(Token.Type.IDENTIFIER);
    }

    // number ::= [+#x2D]? [0-9]+ ( '.' [0-9]+ )?
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

    // character ::= "'" ( [^'\nr] | escape ) "'"
    public Token lexCharacter() {
        matchCharacter();
        match("[^']");
        if (match("'") && chars.length > 2) {
            return chars.emit(Token.Type.CHARACTER);
        }
        lexError("Missing char literal or empty/invalid character");
        return null;
    }

    // string ::= '"' ( [^"\nr] | escape )* '"'
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

    // helper
    private void matchCharacter() {
        if (match("\\\\")) {
            lexEscape();
        }
        if (match("[\n\r]")) {
            lexError("Unescaped new line or carriage return");
        }
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
        chars.advance();
        match("=");
        return chars.emit(Token.Type.OPERATOR);
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
