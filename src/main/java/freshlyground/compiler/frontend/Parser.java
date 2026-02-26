package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.common.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The parser is responsible for converting a stream of tokens into an {@link Ast}
 * by means of a context-free grammar. Its operation is centered around two core
 * components:
 *
 * <ul>
 *   <li>{@link TokenStream} — maintains the parser’s state in evaluating the given token stream,
 *       including the current index, and AST construction.</li>
 *   <li>{@link #parse()} — returns an AST after iterating over the token stream.</li>
 * </ul>
 *
 * <p>
 * To use the parser call: {@code Ast ast = new Parser(tokens).parse()}
 * <p/>
 *
 * <p>
 * If the parser encounters invalid syntax in the form of context-free grammar
 * (e.g., an unterminated declaration or other invalid production rule), it will throw a
 * {@link CompilerException} at the character index where the error occurred.
 * </p>
 */
public final class Parser {
    private final TokenStream tokens;
    private boolean peek(Object... patterns) { return tokens.peek(patterns); }
    private boolean match(Object... patterns) { return tokens.match(patterns); }

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses a complete token stream into an {@link Ast} (specifically an {@link Ast.Source}).
     *
     * <p>
     * This method consumes the provided {@link Token} sequence according to the language grammar,
     * constructing an Abstract Syntax Tree (AST). The parse is single-pass over the token stream
     * (via {@link TokenStream}) and continues until the end of the stream is reached.
     * </p>
     *
     * <p>
     * The parser assumes the lexer has already performed lexical validation (token classes,
     * unterminated literals, invalid escape sequences, etc.). Parsing is responsible for enforcing
     * context-free structure (ordering, delimiters, grouping, and production rules).
     * </p>
     *
     * <h3>Top-Level Grammar</h3>
     *
     * <pre>{@code
     * source := { field } { method }
     *
     * field  := "LET" [ "CONST" ] identifier ":" identifier [ "=" expression ] ";"
     *
     * method := "DEF" identifier "(" [ parameters ] ")" [ ":" identifier ] "DO" { statement } "END"
     * parameters := identifier ":" identifier { "," identifier ":" identifier }
     * }</pre>
     *
     * <h3>Statements</h3>
     *
     * <pre>{@code
     * statement :=
     *     "LET" identifier [ ":" identifier ] [ "=" expression ] ";"
     *   | "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
     *   | "FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")" { statement } "END"
     *   | "WHILE" expression "DO" { statement } "END"
     *   | "RETURN" expression ";"
     *   | expression "=" expression ";"          // assignment (target must be a variable access)
     *   | expression ";"                         // expression statement (must be a function call)
     * }</pre>
     *
     * <h3>Expressions</h3>
     *
     * <pre>{@code
     * expression :=
     *     logical
     *
     * logical :=
     *     equality { ("AND" | "OR") equality }
     *
     * equality :=
     *     additive { ("<" | "<=" | ">" | ">=" | "==" | "!=") additive }
     *
     * additive :=
     *     multiplicative { ("+" | "-") multiplicative }
     *
     * multiplicative :=
     *     secondary { ("*" | "/") secondary }
     *
     * secondary :=
     *     primary { "." identifier [ "(" [ arguments ] ")" ] }
     *
     * arguments :=
     *     expression { "," expression }           // trailing commas are rejected
     *
     * primary :=
     *     "NIL" | "TRUE" | "FALSE"
     *   | [ "+" | "-" ] (integer | decimal)      // sign is parsed here (no unary expression node)
     *   | character | string                     // escape sequences are interpreted into AST values
     *   | "(" expression ")"
     *   | identifier [ "(" [ arguments ] ")" ]   // variable access or function call
     * }</pre>
     *
     * <p>
     * Notes:
     * <ul>
     *   <li>Keywords (e.g., {@code LET}, {@code DEF}, {@code IF}) are expected as identifier literals
     *       because the lexer emits them as {@link Token.Type#IDENTIFIER}.</li>
     *   <li>Signed numeric literals are formed by pairing an optional leading {@code +} or {@code -}
     *       operator token with an immediately following {@code INTEGER} or {@code DECIMAL} token.</li>
     *   <li>String/character escape sequences ({@code \b \n \r \t \' \" \\}) are converted to their
     *       corresponding character values in the produced AST literals.</li>
     *   <li>All parse errors are reported as {@link CompilerException} with source index metadata.</li>
     * </ul>
     * </p>
     *
     * @return the parsed {@link Ast} root ({@link Ast.Source}).
     * @throws CompilerException if the token stream does not conform to the grammar.
     */
    public Ast parse() { return parseSource(); }

    public Ast.Source parseSource() throws CompilerException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (peek("LET")) {
            fields.add(parseField());
        }

        while (peek("DEF")) {
            methods.add(parseMethod());
        }

        if (tokens.has(0)) {
            parseError("Must have all LET statements before DEF");
        }

        return new Ast.Source(fields, methods);
    }

    public Ast.Field parseField() throws CompilerException {
        tokens.expectLiteral("LET");
        boolean constant = match("CONST");
        String name = tokens.expectType(Token.Type.IDENTIFIER).literal();

        tokens.expectLiteral(":");
        String type = tokens.expectType(Token.Type.IDENTIFIER).literal();
        Optional<Ast.Expression> expression = Optional.empty();

        if (match("=")) {
            expression = Optional.of(parseExpression());
        }

        if (constant && expression.isEmpty()) {
            parseError("CONST must have an initial value");
        }

        tokens.expectLiteral(";");
        return new Ast.Field(name, type, constant, expression);
    }

    public Ast.Method parseMethod() throws CompilerException {
        tokens.expectLiteral("DEF");
        String name = tokens.expectType(Token.Type.IDENTIFIER).literal();

        List<String> parameters = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();
        Optional<String> returnType = Optional.empty();
        List<Ast.Statement> statements = new ArrayList<>();

        tokens.expectLiteral("(");
        if (!peek(")")) {
            do {
                String paramName = tokens.expectType(Token.Type.IDENTIFIER).literal();
                tokens.expectLiteral(":");
                String paramType = tokens.expectType(Token.Type.IDENTIFIER).literal();

                parameters.add(paramName);
                parameterTypes.add(paramType);
            } while (match(","));
        }

        tokens.expectLiteral(")");

        if (match(":")) {
            returnType = Optional.of(tokens.expectType(Token.Type.IDENTIFIER).literal());;
        }

        tokens.expectLiteral("DO");

        while (!peek("END")) {
            statements.add(parseStatement());
        }
        tokens.expectLiteral("END");

        return new Ast.Method(name, parameters, parameterTypes, returnType, statements);
    }

    public Ast.Statement parseStatement() throws CompilerException {
        if (match("LET"))    return parseDeclarationStatement();
        if (match("IF"))     return parseIfStatement();
        if (match("FOR"))    return parseForStatement();
        if (match("WHILE"))  return parseWhileStatement();
        if (match("RETURN")) return parseReturnStatement();

        Ast.Expression expr = parseExpression();

        if (match("=")) {
            if (expr instanceof Ast.Expression.Access access) {
                return parseAssignmentStatement(access);
            }
            parseError("Invalid assignment target");
        }

        if (expr instanceof Ast.Expression.Function func) {
            return parseExpressionStatement(func);
        }

        parseError("Invalid statement (expected function call or assignment)");
        return null;
    }

    public Ast.Statement.Declaration parseDeclarationStatement() throws CompilerException {
        String name = tokens.expectType(Token.Type.IDENTIFIER).literal();

        Optional<String> type = Optional.empty();
        Optional<Ast.Expression> expression = Optional.empty();

        if (match(":")) {
            type = Optional.of(tokens.expectType(Token.Type.IDENTIFIER).literal());
        }

        if (match("=")) {
            expression = Optional.of(parseExpression());
        }

        if (type.isEmpty() && expression.isEmpty()) {
            parseError("Declaration must have a type or initial value");
        }

        tokens.expectLiteral(";");
        return new Ast.Statement.Declaration(name, type, expression);
    }

    public Ast.Statement.If parseIfStatement() throws CompilerException {
        Ast.Expression condition = parseExpression();
        tokens.expectLiteral("DO");

        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }

        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
        }

        tokens.expectLiteral("END");
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    public Ast.Statement.For parseForStatement() throws CompilerException {
        tokens.expectLiteral("(");

        Ast.Statement.Assignment initialization = parseLoopControlStatement("initialization");
        tokens.expectLiteral(";");

        Ast.Expression expression = parseExpression();
        tokens.expectLiteral(";");

        Ast.Statement.Assignment increment = parseLoopControlStatement("increment");

        tokens.expectLiteral(")");

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        tokens.expectLiteral("END");
        return new Ast.Statement.For(initialization, expression, increment, statements);
    }

    public Ast.Statement.While parseWhileStatement() throws CompilerException {
        Ast.Expression expression = parseExpression();
        List<Ast.Statement> statements = new ArrayList<>();

        tokens.expectLiteral("DO");
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        tokens.expectLiteral("END");
        return new Ast.Statement.While(expression, statements);
    }

    public Ast.Statement.Return parseReturnStatement() throws CompilerException {
        Ast.Expression value = parseExpression();
        tokens.expectLiteral(";");
        return new Ast.Statement.Return(value);
    }

    public Ast.Statement.Assignment parseAssignmentStatement(Ast.Expression.Access receiver) throws CompilerException {
        Ast.Expression value = parseExpression();
        tokens.expectLiteral(";");
        return new Ast.Statement.Assignment(receiver, value);
    }

    public Ast.Statement.Expression parseExpressionStatement(Ast.Expression.Function expression) throws CompilerException {
        tokens.expectLiteral(";");
        return new Ast.Statement.Expression(expression);
    }

    // helper
    private Ast.Statement.Assignment parseLoopControlStatement(String section) {
        if (!peek(Token.Type.IDENTIFIER)) {
            return null;
        }

        Ast.Expression receiver = parseExpression();

        if (!(receiver instanceof Ast.Expression.Access)) {
            parseError("Invalid FOR " + section + " assignment target");
        }

        Ast.Expression.Access access = (Ast.Expression.Access) receiver;

        if (access.getReceiver().isPresent()) {
            parseError("Invalid FOR " + section + " assignment target");
        }

        tokens.expectLiteral("=");
        Ast.Expression value = parseExpression();
        return new Ast.Statement.Assignment(access, value);
    }

    public Ast.Expression parseExpression() throws CompilerException {
        return parseLogicalExpression();
    }

    public Ast.Expression parseLogicalExpression() throws CompilerException {
        return parseBinaryExpression(this::parseEqualityExpression, "AND",  "OR");
    }

    public Ast.Expression parseEqualityExpression() throws CompilerException {
        return parseBinaryExpression(this::parseAdditiveExpression, "<", "<=", ">", ">=", "==", "!=");
    }

    public Ast.Expression parseAdditiveExpression() throws CompilerException {
        return parseBinaryExpression(this::parseMultiplicativeExpression, "+", "-");
    }

    public Ast.Expression parseMultiplicativeExpression() throws CompilerException {
        return parseBinaryExpression(this::parseSecondaryExpression, "*", "/");
    }

    // helper
    private Ast.Expression parseBinaryExpression(
        Supplier<Ast.Expression> operand,
        String... operators
    ) {
        Ast.Expression left = operand.get();

        Optional<Token> operator;
        while ((operator = tokens.matchAny(operators)).isPresent()) {
            Ast.Expression right = operand.get();
            left = new Ast.Expression.Binary(operator.get().literal(), left, right);
        }

        return left;
    }

    public Ast.Expression parseSecondaryExpression() throws CompilerException {
        Ast.Expression receiver = parsePrimaryExpression();

        while (match(".")) {
            receiver = parseMemberAccessOrCall(receiver);;
        }
        return receiver;
    }

    public Ast.Expression parsePrimaryExpression() throws CompilerException {
        if (match("NIL")) return new Ast.Expression.Literal(null);

        if (match("TRUE")) return new Ast.Expression.Literal(true);
        if (match("FALSE")) return new Ast.Expression.Literal(false);

        boolean negative = false;
        if (peek("+") || peek("-")) {
            if (peek("+", Token.Type.INTEGER) || peek("+", Token.Type.DECIMAL) ||
                peek("-", Token.Type.INTEGER) || peek("-", Token.Type.DECIMAL)) {
                negative = match("-");
                if (!negative) match("+");
            }
        }

        if (peek(Token.Type.INTEGER)) {
            BigInteger n = new BigInteger(tokens.consume().literal());
            return new Ast.Expression.Literal(negative ? n.negate() : n);
        }
        if (peek(Token.Type.DECIMAL)) {
            BigDecimal n = new BigDecimal(tokens.consume().literal());
            return new Ast.Expression.Literal(negative ? n.negate() : n);
        }

        if (peek(Token.Type.CHARACTER) || peek(Token.Type.STRING)) {
            Token token = tokens.consume();
            String literal = token.literal();

            String inner = literal.substring(1, literal.length() - 1);

            String value = decodeEscapes(inner);

            if (token.type() == Token.Type.STRING) {
                return new Ast.Expression.Literal(value);
            }

            if (value.length() != 1) {
                parseError("Invalid character literal");
            }
            return new Ast.Expression.Literal(value.charAt(0));
        }

        if (match("(")) {
            Ast.Expression expression = parseExpression();
            tokens.expectLiteral(")");
            return new Ast.Expression.Group(expression);
        }

        if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.consume().literal();

            // function(args)
            if (match("(")) {
                List<Ast.Expression> args = parseArgumentList();
                match(")");
                return new Ast.Expression.Function(Optional.empty(), name, args);
            }

            // access
            return new Ast.Expression.Access(Optional.empty(), name);
        }
        parseError("Invalid Primary Expression");
        return null;
    }

    // helper
    private Ast.Expression parseMemberAccessOrCall(Ast.Expression receiver) throws CompilerException {
        Token member = tokens.expectType(Token.Type.IDENTIFIER);
        String name = member.literal();

        if (match("(")) {
            List<Ast.Expression> args = parseArgumentList();
            tokens.expectLiteral(")");
            return new Ast.Expression.Function(Optional.of(receiver), name, args);
        }

        return new Ast.Expression.Access(Optional.of(receiver), name);
    }

    // helper
    private List<Ast.Expression> parseArgumentList() throws CompilerException {
        List<Ast.Expression> args = new ArrayList<>();

        if (peek(")")) {
            return args;
        }

        args.add(parseExpression());

        while (match(",")) {
            if (peek(")")) {
                parseError("Trailing comma in argument list");
            }
            args.add(parseExpression());
        }

        return args;
    }

    // helper
    private String decodeEscapes(String str) {
        StringBuilder out = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);

            if (chr != '\\') {
                out.append(chr);
                continue;
            }

            if (i + 1 >= str.length()) {
                parseError("Invalid escape character");
            }

            char next = str.charAt(++i);
            switch (next) {
                case 'b'  -> out.append('\b');
                case 'n'  -> out.append('\n');
                case 'r'  -> out.append('\r');
                case 't'  -> out.append('\t');
                case '\'' -> out.append('\'');
                case '"'  -> out.append('"');
                case '\\' -> out.append('\\');
                default   -> parseError("Invalid escape character");
            }
        }
        return out.toString();
    }

    // helper
    private void parseError(String message) {
        parseError(message, 0);
    }

    // helper
    private void parseError(String message, int tokenOffset) {
        int index = tokens.location(tokenOffset);
        throw new CompilerException(message, index);
    }
}
