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

    public Ast parse() { return parseSource(); }

    /**
     * Parses the {@code source} rule.
     */
    // source ::= { field } { method }
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

    // field ::= LET CONST name : type = expression;
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

    // method ::= DEF name(parameter(s) : parameterType(s)) : returnType DO statement(s) END
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

    // statement ::= LET | IF | "FOR | WHILE | RETURN | expression.access | expression.function
    public Ast.Statement parseStatement() throws CompilerException {
        if (tokens.match("LET"))    return parseDeclarationStatement();
        if (tokens.match("IF"))     return parseIfStatement();
        if (tokens.match("FOR"))    return parseForStatement();
        if (tokens.match("WHILE"))  return parseWhileStatement();
        if (tokens.match("RETURN")) return parseReturnStatement();

        Ast.Expression expr = parseExpression();

        if (tokens.match("=")) {
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

    // LET name : type = expression;
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

    // IF condition DO thenStatements ELSE elseStatements END
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

    // FOR (initialization; condition; increment) statements END
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

    // WHILE condition DO statements END
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

    // RETURN value;
    public Ast.Statement.Return parseReturnStatement() throws CompilerException {
        Ast.Expression value = parseExpression();
        tokens.expectLiteral(";");
        return new Ast.Statement.Return(value);
    }

    // receiver = value;
    public Ast.Statement.Assignment parseAssignmentStatement(Ast.Expression.Access receiver) throws CompilerException {
        Ast.Expression value = parseExpression();
        tokens.expectLiteral(";");
        return new Ast.Statement.Assignment(receiver, value);
    }

    // expression;
    public Ast.Statement.Expression parseExpressionStatement(Ast.Expression.Function expression) throws CompilerException {
        tokens.expectLiteral(";");
        return new Ast.Statement.Expression(expression);
    }

    // receiver = value (used in For loop init and inc)
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

    // expression ::= logical_expression
    public Ast.Expression parseExpression() throws CompilerException {
        return parseLogicalExpression();
    }

    // comparison_expression ( AND | OR ) comparison_expression
    public Ast.Expression parseLogicalExpression() throws CompilerException {
        return parseBinaryExpression(this::parseEqualityExpression, "AND",  "OR");
    }

    // additive_expression ( < | <= | > | >= | == | != ) additive_expression
    public Ast.Expression parseEqualityExpression() throws CompilerException {
        return parseBinaryExpression(this::parseAdditiveExpression, "<", "<=", ">", ">=", "==", "!=");
    }

    // multiplicative_expression ( + | - ) multiplicative_expression
    public Ast.Expression parseAdditiveExpression() throws CompilerException {
        return parseBinaryExpression(this::parseMultiplicativeExpression, "+", "-");
    }

    // secondary_expression ( * | / ) secondary_expression
    public Ast.Expression parseMultiplicativeExpression() throws CompilerException {
        return parseBinaryExpression(this::parseSecondaryExpression, "*", "/");
    }

    // expression operator expression
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

    // receiver.literal(arguments)
    public Ast.Expression parseSecondaryExpression() throws CompilerException {
        Ast.Expression receiver = parsePrimaryExpression();

        while (match(".")) {
            receiver = parseMemberAccessOrCall(receiver);;
        }
        return receiver;
    }

    // primary_expression ::=
    //       NIL
    //     | TRUE | FALSE
    //     | integer | decimal
    //     | character | string
    //     | (expression)
    //     | literal[ (arguments) ]
    public Ast.Expression parsePrimaryExpression() throws CompilerException {
        // NIL
        if (match("NIL")) return new Ast.Expression.Literal(null);

        // TRUE | FALSE
        if (match("TRUE")) return new Ast.Expression.Literal(true);
        if (match("FALSE")) return new Ast.Expression.Literal(false);

        // sign
        boolean negative = false;
        if (peek("+") || peek("-")) {
            if (peek("+", Token.Type.INTEGER) || peek("+", Token.Type.DECIMAL) ||
                peek("-", Token.Type.INTEGER) || peek("-", Token.Type.DECIMAL)) {
                negative = match("-");
                if (!negative) match("+");
            }
        }

        // (sign) integer | decimal
        if (peek(Token.Type.INTEGER)) {
            BigInteger n = new BigInteger(tokens.consume().literal());
            return new Ast.Expression.Literal(negative ? n.negate() : n);
        }
        if (peek(Token.Type.DECIMAL)) {
            BigDecimal n = new BigDecimal(tokens.consume().literal());
            return new Ast.Expression.Literal(negative ? n.negate() : n);
        }

        // character | string
        if (peek(Token.Type.CHARACTER) || peek(Token.Type.STRING)) {
            Token token = tokens.consume();
            String literal = token.literal();

            String inner = literal.substring(1, literal.length() - 1);

            inner = decodeEscapes(inner, token.index() + 1);

            if (peek(Token.Type.STRING)) {
                return new Ast.Expression.Literal(inner);
            }

            if (inner.length() != 1) {
                throw new CompilerException("Missing char/string literal or empty/invalid character", token.index() + 1);
            }
            return new Ast.Expression.Literal(inner.charAt(0));
        }

        // (expression)
        if (match("(")) {
            Ast.Expression expression = parseExpression();
            tokens.expectLiteral(")");
            return new Ast.Expression.Group(expression);
        }

        // literal[ (arguments) ]
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

    private List<Ast.Expression> parseArgumentList() throws CompilerException {
        List<Ast.Expression> args = new ArrayList<>();

        if (peek(")")) {
            return args;
        }

        do {
            args.add(parseExpression());
        } while (match(","));

        return args;
    }

    // swap slashed escape characters with escape chars
    private String decodeEscapes(String s, int baseIndexForErrors) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c != '\\') {
                out.append(c);
                continue;
            }

            // '\' at end shouldn't happen if lexer is correct, but keep parser robust
            if (i + 1 >= s.length()) {
                throw new CompilerException("Invalid escape character", baseIndexForErrors + i);
            }

            char next = s.charAt(++i);
            switch (next) {
                case 'b'  -> out.append('\b');
                case 'n'  -> out.append('\n');
                case 'r'  -> out.append('\r');
                case 't'  -> out.append('\t');
                case '\'' -> out.append('\'');
                case '"'  -> out.append('"');
                case '\\' -> out.append('\\');
                default   -> throw new CompilerException("Invalid escape character", baseIndexForErrors + (i - 1));
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
