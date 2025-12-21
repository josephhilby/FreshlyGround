package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.common.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer is responsible for converting a raw source stream of characters
 * into a {@link TokenStream} by means of the selected lexical grammar. Its
 * operation is centered around two core components:
 *
 * <ul>
 *   <li>{@link CharStream} — maintains the lexer’s state in evaluating the given source code,
 *       including the current index, token length, and literal construction.</li>
 *   <li>{@link #lex()} — repeatedly calls {@link #lexToken()} to produce {@link Token}s,
 *       automatically skipping over whitespace.</li>
 * </ul>
 *
 * <p>
 * To use the lexer call: {@code List<Token> tokens = new Lexer(source_code).lex()}
 * <p/>
 *
 * <p>
 * If the lexer encounters invalid syntax in the form of lexical grammar
 * (e.g., an unterminated string or other invalid token), it will throw a
 * {@link CompilerException} at the character index where the error occurred.
 * </p>
 */
public final class Lexer {
    private final CharStream chars;
    private boolean peek(String... patterns) { return chars.peek(patterns); }
    private boolean match(String... patterns) { return chars.match(patterns); }

    public Lexer(String source) {
        chars = new CharStream(source);
    }

    /**
     * Scans the entire source stream and produces a list of {@link Token} objects.
     *
     * <p>
     * This method repeatedly calls {@link #lexToken()} to extract the next token,
     * automatically skipping over whitespace characters such as spaces, tabs,
     * and newlines. The process continues until the end of the character stream
     * is reached.
     * </p>
     *
     * @return a list of all {@link Token} objects produced from the source.
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
     * Determines the next token type and calls the corresponding
     * lexing routine.
     *
     * <p>
     * Since whitespace is already handled by {@link #lex()}, the current
     * character is guaranteed to begin a valid token, as defined by its specific
     * lexical grammar rule.
     * </p>
     *
     * @return the next {@link Token} identified in the source stream.
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        }

        if (peek("[0-9]")
                || (
                    peek("[+-]", "[0-9]")
                    && chars.getPrevious() != Token.Type.INTEGER
                    && chars.getPrevious() != Token.Type.DECIMAL
                    )) {
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
     * Consumes all consecutive characters that match its
     * grammar rule and emits a {@link Token.Type#IDENTIFIER}.
     *
     * <p>
     * Grammar rule:
     * </p>
     *
     * <pre>
     * identifier  := [A-Za-z_] [A-Za-z0-9_-]*
     * </pre>
     *
     * @return a {@link Token} representing an identifier literal.
     */
    public Token lexIdentifier() {
        while (match("[A-Za-z0-9_-]"));
        return chars.emit(Token.Type.IDENTIFIER);
    }

    /**
     * Consumes all consecutive characters that match its
     * grammar rule(s) and emits: {@link Token.Type#INTEGER} or
     * {@link Token.Type#DECIMAL}.
     *
     * <p>
     * Grammar rule(s):
     * </p>
     *
     * <pre>
     * integer    := 0 | [+-]? [1-9] [0-9]*
     * decimal    := [+-]? [0-9]+ \. [0-9]+
     * </pre>
     *
     * @param signed {@code true} if the number includes a leading sign ({@code [+-]?}),
     *               {@code false} otherwise.
     *
     * @return a {@link Token} representing either an integer or decimal literal.
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
     * Consumes all consecutive characters that match its
     * grammar rule and emits a {@link Token.Type#CHARACTER}.
     *
     * <p>
     * Grammar rule:
     * </p>
     *
     * <pre>
     * character  := ^' ([^'\n\r\\] | 'escape') '$
     * </pre>
     *
     * @return a {@link Token} representing a character literal.
     */
    public Token lexCharacter() {
        matchCharacter();
        match("[^']");

        if (match("'") && chars.getLength() > 2) {
            return chars.emit(Token.Type.CHARACTER);
        }

        lexError("Missing char literal or empty/invalid character");

        return null;
    }

    /**
     * Consumes all consecutive characters that match its
     * grammar rule and emits a {@link Token.Type#STRING}.
     *
     * <p>
     * Grammar rule:
     * </p>
     *
     * <pre>
     * string     := ^" ([^"\n\r\\] | 'escape')* "$
     * </pre>
     *
     * @return a {@link Token} representing a string literal.
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

    /**
     * Consumes any single character that matches its
     * grammar rule and emits no token.
     *
     * <p>
     * Grammar rule:
     * </p>
     *
     * <pre>
     * escape     := ^\\ [bnrt'"\\]$
     * </pre>
     */
    public void lexEscape() {
        if (match("[bnrt'\"\\\\]")) {
            return;
        }

        lexError("Invalid escape character");
    }

    /**
     * Consumes all consecutive characters that match its
     * grammar rule and emits a {@link Token.Type#OPERATOR}.
     *
     * <p>
     * Grammar rule:
     * </p>
     *
     * <pre>
     * operator   := [<>!=] =? | 'any character'
     * </pre>
     *
     * @return a {@link Token} representing an operator literal.
     */
    public Token lexOperator() {
        if (match("[<>!=]", "=")) {
        } else {
            chars.advance();
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * A helper function that detects and rejects integer literals with
     * leading zeros (e.g., {@code 0123}), which are not permitted.
     */
    private void checkLeadingZeros() {
        if (peek("0", "[0-9]")) {
            lexError("No leading zeros");
        }
    }

    /**
     * A helper function that detects and rejects signed zero integer
     * literals (e.g., {@code +0} or {@code -0}), unless the literal
     * represents a decimal value.
     */
    private void checkSignedZero(boolean signed) {
        if (signed && match("0") && !peek("\\.", "[0-9]")) {
            lexError("No signed zero integers");
        }
    }

    /**
     * A helper function that processes a character literal, handling
     * valid escape sequences and rejecting unescaped newline or
     * carriage return characters.
     */
    private void matchCharacter() {
        if (match("\\\\")) {
            lexEscape();
        }

        if (match("[\n\r]")) {
            lexError("Unescaped new line or carriage return");
        }
    }

    /**
     * A helper function throws a {@link CompilerException} at the current
     * {@link CharStream} index, with the provided error message.
     */
    private void lexError(String message) {
        throw new CompilerException(
            message,
            chars.getIndex()
        );
    }
}
