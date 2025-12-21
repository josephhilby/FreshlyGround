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
 *   <li>{@link #parseSource()} — iterates over the token stream repeatedly calling {@link #parseField()} or
 *       {@link #parseMethod()} to produce {@link Ast.Field}s or {@link Ast.Method}s, automatically skipping
 *       over whitespace.</li>
 * </ul>
 *
 * <p>
 * To use the parser call: {@code Ast ast = new Parser(tokens).parseSource()}
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

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    // field ::= "LET" [ CONST ] identifier ":" identifier [ "=" expression ] ";"
    //            LET CONST name : type = expression;
    public Ast.Field parseField() throws CompilerException {
        keywordCheck("LET");
        boolean constant = match("CONST");
        String name = currentToken().literal();
        typeCheck(Token.Type.IDENTIFIER);

        keywordCheck(":");
        String type = currentToken().literal();
        typeCheck(Token.Type.IDENTIFIER);
        Optional<Ast.Expression> expression = Optional.empty();

        if (match("=")) {
            expression = Optional.of(parseExpression());
        }

        if (constant && expression.isEmpty()) {
            parseError("CONST must have an initial value");
        }

        keywordCheck(";");
        return new Ast.Field(name, type, constant, expression);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    // method ::= "DEF" identifier "(" [ identifier ":" identifier { "," identifier ":" identifier } ] ")" [ ":" identifier ] "DO" { statement } "END"
    //             DEF name(parameter : parameterType) : returnType DO statement(s) END
    public Ast.Method parseMethod() throws CompilerException {
        // TODO: Clean up
        keywordCheck("DEF");
        String name = currentToken().literal();
        typeCheck(Token.Type.IDENTIFIER);

        List<String> parameters = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();
        Optional<String> returnType = Optional.empty();
        List<Ast.Statement> statements = new ArrayList<>();

        keywordCheck("(");
        if (!peek(")")) {
            do {
                String parameter = currentToken().literal();
                typeCheck(Token.Type.IDENTIFIER);
                keywordCheck(":");
                String parameterType = currentToken().literal();
                typeCheck(Token.Type.IDENTIFIER);
                parameters.add(parameter);
                parameterTypes.add(parameterType);
            } while (match(","));
        }
        keywordCheck(")");
        if (match(":")) {
            returnType = Optional.of(currentToken().literal());
            typeCheck(Token.Type.IDENTIFIER);
        }
        keywordCheck("DO");
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        keywordCheck("END");
        return new Ast.Method(name, parameters, parameterTypes, returnType, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    // statement ::= "LET" | "IF" | "FOR" | "WHILE" | "RETURN" | expression [ "=" expression ] ";"
    public Ast.Statement parseStatement() throws CompilerException {
        if (match("LET")) {
            return parseDeclarationStatement();
        }
        if (match("IF")) {
            return parseIfStatement();
        }
        if (match("FOR")) {
            return parseForStatement();
        }
        if (match("WHILE")) {
            return parseWhileStatement();
        }
        if (match("RETURN")) {
            return parseReturnStatement();
        }

        Ast.Expression expression = parseExpression();
        if (match("=")) {
            return parseAssignmentStatement(expression);
        }

        return parseExpressionStatement(expression);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    // "LET" identifier [ ":" identifier ] [ "=" expression ] ";"
    //  LET name : type = expression;
    public Ast.Statement.Declaration parseDeclarationStatement() throws CompilerException {
        String name = currentToken().literal();
        typeCheck(Token.Type.IDENTIFIER);

        Optional<String> type = Optional.empty();
        Optional<Ast.Expression> expression = Optional.empty();

        if (match(":")) {
            type = Optional.of(currentToken().literal());
            typeCheck(Token.Type.IDENTIFIER);
        }

        if (match("=")) {
            expression = Optional.of(parseExpression());
        }
        keywordCheck(";");
        return new Ast.Statement.Declaration(name, type, expression);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    // "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
    //  IF condition DO thenStatements ELSE elseStatements END
    public Ast.Statement.If parseIfStatement() throws CompilerException {
        Ast.Expression expression = parseExpression();
        List<Ast.Statement> thenStatements = new ArrayList<>();
        List<Ast.Statement> elseStatements = new ArrayList<>();

        keywordCheck("DO");
        while (!match("ELSE")) {
            if (match("END")) {
                return new Ast.Statement.If(expression, thenStatements, elseStatements);
            }
            thenStatements.add(parseStatement());
        }
        while (!peek("END")) {
            elseStatements.add(parseStatement());
        }
        keywordCheck("END");
        return new Ast.Statement.If(expression, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    // "FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")" { statement } "END"
    //  FOR (initialization; condition; increment) statements END
    public Ast.Statement.For parseForStatement() throws CompilerException {
        keywordCheck("(");
        Ast.Statement initialization = null;
        if (tokens.get(0).type() == Token.Type.IDENTIFIER) {
            initialization = parseLoopStatement();
        }
        keywordCheck(";");

        Ast.Expression expression = parseExpression();
        keywordCheck(";");

        Ast.Statement increment = null;
        if (tokens.get(0).type() == Token.Type.IDENTIFIER) {
            increment = parseLoopStatement();
        }
        keywordCheck(")");

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        keywordCheck("END");
        return new Ast.Statement.For(initialization, expression, increment, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    // "WHILE" expression "DO" { statement } "END"
    //  WHILE condition DO statements END
    public Ast.Statement.While parseWhileStatement() throws CompilerException {
        Ast.Expression expression = parseExpression();
        List<Ast.Statement> statements = new ArrayList<>();

        keywordCheck("DO");
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        keywordCheck("END");
        return new Ast.Statement.While(expression, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    // "RETURN" expression ";"
    //  RETURN value;
    public Ast.Statement.Return parseReturnStatement() throws CompilerException {
        Ast.Expression expression = parseExpression();
        keywordCheck(";");
        return new Ast.Statement.Return(expression);
    }

    // expression "=" expression ";"
    // receiver = value;
    public Ast.Statement.Assignment parseAssignmentStatement(Ast.Expression receiver) throws CompilerException {
        Ast.Expression value = parseExpression();
        keywordCheck(";");
        return new Ast.Statement.Assignment(receiver, value);
    }

    // expression ";"
    // receiver;
    public Ast.Statement.Expression parseExpressionStatement(Ast.Expression expression) throws CompilerException {
        keywordCheck(";");
        return new Ast.Statement.Expression(expression);
    }

    // identifier "=" expression
    // receiver = value
    private Ast.Statement.Assignment parseLoopStatement() throws CompilerException {
        Ast.Expression receiver = parseExpression();
        keywordCheck("=");
        Ast.Expression value = parseExpression();
        return new Ast.Statement.Assignment(receiver, value);
    }

    /**
     * Parses the {@code expression} rule.
     */
    // expression ::= logical_expression
    public Ast.Expression parseExpression() throws CompilerException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    // logical_expression ::= comparison_expression
    //     { ( "AND" | "OR" ) comparison_expression }
    public Ast.Expression parseLogicalExpression() throws CompilerException {
        return parseBinaryExpression(this::parseEqualityExpression, "AND",  "OR");
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    // comparison_expression ::= additive_expression
    //      { ( "<" | "<=" | ">" | ">=" | "==" | "!=" ) additive_expression }
    public Ast.Expression parseEqualityExpression() throws CompilerException {
        return parseBinaryExpression(this::parseAdditiveExpression, "<", "<=", ">", ">=", "==", "!=");
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    // additive_expression ::= multiplicative_expression
    //      { ( "+" | "-" ) multiplicative_expression }
    public Ast.Expression parseAdditiveExpression() throws CompilerException {
        return parseBinaryExpression(this::parseMultiplicativeExpression, "+", "-");
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    // multiplicative_expression ::= secondary_expression
    //      { ( "*" | "/" ) secondary_expression }
    public Ast.Expression parseMultiplicativeExpression() throws CompilerException {
        return parseBinaryExpression(this::parseSecondaryExpression, "*", "/");
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    // secondary_expression ::= primary_expression
    //      { "." identifier [ "(" [ expression { "," expression } ] ")" ] }
    //
    // receiver.literal(parameters)
    public Ast.Expression parseSecondaryExpression() throws CompilerException {
        Ast.Expression receiver = parsePrimaryExpression();
        while (match(".")) {
            typeCheck(Token.Type.IDENTIFIER, false);
            receiver = parsePrimaryExpression(Optional.of(receiver));
        }
        return receiver;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    // primary_expression ::=
    //     "NIL"              |
    //     "TRUE" | "FALSE"   |
    //     integer | decimal  |
    //     character | string |
    //     "(" expression ")" |
    //     identifier [ "(" [ expression { "," expression } ] ")" ]
    public Ast.Expression parsePrimaryExpression() throws CompilerException {
        return parsePrimaryExpression(Optional.empty());
    }

    public Ast.Expression parsePrimaryExpression(Optional<Ast.Expression> receiver) throws CompilerException {
        Token.Type type = currentToken().type();
        String literal = currentToken().literal();

        // "NIL"
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }

        // "TRUE" | "FALSE"
        if (match("TRUE") || match("FALSE")) {
            if (literal.equals("TRUE")) {
                return new Ast.Expression.Literal(true);
            }
            return new Ast.Expression.Literal(false);
        }

        // integer | decimal
        if (match(Token.Type.INTEGER) || match(Token.Type.DECIMAL)) {
            if (type == Token.Type.INTEGER) {
                return new Ast.Expression.Literal(new BigInteger(literal));
            }
            return new Ast.Expression.Literal(new BigDecimal(literal));
        }

        // character | string
        if (match(Token.Type.CHARACTER) || match(Token.Type.STRING)) {
            String substring = literal.substring(1, literal.length() - 1);
            ArrayList<Integer> indexes = findSlashIndices(substring);
            if (!indexes.isEmpty()) {
                substring = clean(substring, indexes);
            }
            if (type == Token.Type.STRING) {
                return new Ast.Expression.Literal(substring);
            }
            return new Ast.Expression.Literal(substring.charAt(0));
        }

        // "(" expression ")"
        // (expression)
        if (match("(")) {
            Ast.Expression expression = parseExpression();
            keywordCheck(")");
            return new Ast.Expression.Group(expression);
        }

        // identifier [ "(" [ expression { "," expression } ] ")" ]
        // receiver.literal(parameters)
        if (match(Token.Type.IDENTIFIER)) {
            List<Ast.Expression> expressions = new ArrayList<>();
            if (match("(", ")")) {
                return new Ast.Expression.Function(receiver, literal, expressions);
            }
            if (match("(")) {
                do {
                    expressions.add(parseExpression());
                } while (match(","));
                keywordCheck(")");
                return new Ast.Expression.Function(receiver, literal, expressions);
            }
            return new Ast.Expression.Access(receiver, literal);
        }
        parseError("Invalid Primary Expression");
        return null;
    }

    // generic to parse for binary expression
    private Ast.Expression parseBinaryExpression(Supplier<Ast.Expression> expression,
                                                 String... operators) throws CompilerException {
        Ast.Expression left = expression.get();

        while (check(operators)) {
            String operator = tokens.get(-1).literal();
            Ast.Expression right = expression.get();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    // dynamic OR chain, (A || B || ... )
    private boolean check(String... literals) {
        for (int i = 0; i < literals.length; i++) {
            if (match(literals[i])) {
                return true;
            }
        }
        return false;
    }

    // find escape characters locations
    private ArrayList<Integer> findSlashIndices(String string) {
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\\') {
                indexes.add(i);
            }
        }
        return indexes;
    }

    // remove found escape characters from locations but keep escaped escape chars
    private String clean(String string, ArrayList<Integer> indexes) {
        StringBuilder builder = new StringBuilder();
        int i = 0;

        while (i < string.length()) {
            char c = string.charAt(i);

            if (c == '\\' && i + 1 < string.length()) {
                char next = string.charAt(i + 1);
                switch (next) {
                    case 'b':
                        builder.append('\b');
                        i += 2;
                        break;
                    case 'n':
                        builder.append('\n');
                        i += 2;
                        break;
                    case 'r':
                        builder.append('\r');
                        i += 2;
                        break;
                    case 't':
                        builder.append('\t');
                        i += 2;
                        break;
                    case '\'':
                        builder.append('\'');
                        i += 2;
                        break;
                    case '\\':
                        builder.append('\\');
                        i += 2;
                        break;
                    default:
                        builder.append(c);
                        i++;
                        break;
                }
            } else {
                builder.append(c);
                i++;
            }
        }

        return builder.toString();
    }

    // helper
    private void parseError(String message) {
        parseError(message, 0);
    }

    // helper
    private void parseError(String message, int tokenOffset) {
        int index = tokenLocation(tokenOffset);
        throw new CompilerException(message, index);
    }

    // helper
    private int tokenLocation(int offset) {
        int prev =  offset - 1;
        return tokens.has(offset) ? tokens.get(offset).index()
                : tokens.get(prev).index() + tokens.get(prev).literal().length();
    }

    // helper
    private boolean keywordCheck(String keyword) {
        if (!match(keyword)) {
            String msg = "Missing: " + keyword;
            parseError(msg);
        }
        return true;
    }

    // helper
    private boolean typeCheck(Token.Type type) {
        return typeCheck(type, true);
    }

    private boolean typeCheck(Token.Type expectation, boolean consume) {
        if (!peek(expectation)) {
            Token.Type actual = currentToken().type();
            String msg = "Type Error. Expected: " + expectation + ", Got: " + actual;
            parseError(msg);
        }
        if (consume) {
            tokens.advance();
        }
        return true;
    }

    // helper
    private Token currentToken() {
        remainingCheck(0,1);
        return tokens.get(0);
    }

    // helper
    private boolean remainingCheck(int remaining, int expected) {
        if (!tokens.has(remaining)) {
            parseError("Invalid Length. Remaining: " + remaining + " Expected: " + expected);
        }
        return true;
    }
}
