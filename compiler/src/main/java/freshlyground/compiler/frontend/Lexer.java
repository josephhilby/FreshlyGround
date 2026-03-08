package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.frontend.artifacts.CharStream;
import freshlyground.compiler.frontend.artifacts.TokenStream;

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
    private Token emit(Token.Type type) { return chars.emit(type); }

    public Lexer(String source) {
        chars = new CharStream(source);
    }

    /**
     * Scans the entire source stream and produces a list of {@link Token} objects.
     *
     * <p>
     * This method repeatedly calls {@code #lexToken()} to extract the next token,
     * automatically skipping over whitespace characters ({@code ' '}, {@code \b},
     * {@code \n}, {@code \r}, {@code \t}). The process continues until the end of
     * the character stream is reached.
     * </p>
     *
     * <h3>Token Classes</h3>
     *
     * <pre>{@code
     * identifier := [A-Za-z_] [A-Za-z0-9_]* ( - [A-Za-z0-9_]+ )*
     *
     * operator   := <= | >= | == | != | [<>+-*\/().,:=;]
     *
     * integer    := 0 | [1-9] [0-9]*
     * decimal    := 0 \. [0-9]+ | [1-9] [0-9]* \. [0-9]+
     *
     * character  := ' ( [^'\n\r\\] | escape ) '
     * string     := " ( [^"\n\r\\] | escape )* "
     *
     * escape     := \\ [bnrt'"\\]
     * }</pre>
     *
     * <p>
     * Notes:
     * <ul>
     *   <li>Whitespace is not emitted as tokens.</li>
     *   <li>Keywords (e.g., {@code LET}, {@code DEF}, {@code IF}, {@code WHILE}) are lexed as identifiers
     *       and can be promoted during parsing.</li>
     *   <li>All emitted tokens include source position metadata for diagnostics.</li>
     * </ul>
     * </p>
     *
     * @return a list of all {@link Token} objects produced from the source.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();

        while (chars.has(0)) {
            if (match("[ \b\n\r\t]")) {
                chars.skip();

            } else if (peek("/", "/")) {
                lexComment();

            } else {
                tokens.add(lexToken());
            }
        }

        return tokens;
    }

    private Token lexToken() {
        if (peek("[A-Za-z_]")) { return lexIdentifier(); }
        if (peek("'"))         { return lexCharacter(); }
        if (peek("\""))        { return lexString(); }
        if (peek("[0-9]"))     { return lexNumber(); }
        return lexOperator();
    }

    private Token lexIdentifier() {
        while (match("[A-Za-z0-9_]"));

        while (peek("-", "[A-Za-z0-9_]")) {
            match("-");
            match("[A-Za-z0-9_]");
            while (match("[A-Za-z0-9_]"));
        }

        return emit(Token.Type.IDENTIFIER);
    }

    private Token lexNumber() {
        if (match("0")) {
            if (match("\\.", "[0-9]")) {
                while (match("[0-9]"));
                return emit(Token.Type.DECIMAL);
            }

            if (peek("[0-9]")) {
                lexError("No leading zeros");
            }

            return emit(Token.Type.INTEGER);
        }

        match("[1-9]");
        while (match("[0-9]"));
        if (match("\\.", "[0-9]")) {
            while (match("[0-9]"));
            return emit(Token.Type.DECIMAL);
        }

        return emit(Token.Type.INTEGER);
    }

    private Token lexCharacter() {
        match("'");
        matchCharacter(false);

        if (!match("'")) {
            lexError("Unterminated character literal or oversized character");
        }

        return emit(Token.Type.CHARACTER);
    }

    private Token lexString() {
        match("\"");

        while (chars.has(0) && !peek("\"")) {
            matchCharacter(true);
        }

        if (!match("\"")) {
            lexError("Unterminated string literal");
        }

        return emit(Token.Type.STRING);
    }

    private Token lexOperator() {
        if (match("[<>!=]", "=")) {
            return emit(Token.Type.OPERATOR);
        }

        if (match("[<>+\\-*/\\.,:();=]")) {
            return emit(Token.Type.OPERATOR);
        }

        lexError("Unexpected character");
        return null;
    }

    private void lexEscape() {
        if (match("[bnrt'\"\\\\]")) {
            return;
        }

        lexError("Invalid escape character");
    }

    private void lexComment() {
        match("/", "/");

        while (chars.has(0) && !peek("[\n\r]")) {
            match("[^\n\r]");
        }

        chars.skip();
    }

    private void lexError(String message) {
        throw new CompilerException(
            message,
            chars.getIndex()
        );
    }

    private void matchCharacter(boolean isString) {
        if (match("\\\\")) {
            lexEscape();
            return;
        }

        if (match("[\\n\\r]")) {
            lexError("Unescaped new line or carriage return");
        }

        if (!isString) {
            if (match("[^'\\\\]")) return;
        } else {
            if (match("[^\"\\\\]")) return;
        }

        lexError("Missing char/string literal or empty/invalid character");
    }
}
