package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }
    // TODO: On refactor consider
    //  using overloaded functions to handle optional events ([]),
    //  iterative helper to handle zero or multiple events

    /**
     * Parses the {@code source} rule.
     */
    // source ::= { field } { method }
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (tokens.has(0)) {
            if (match("LET")) {
                fields.add(parseField());
            } else if (match("DEF")) {
                methods.add(parseMethod());
            } else {
                parseError("Must be a Field (LET) or Method (DEF)");
            }
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    // field ::= "LET" identifier [ "=" expression ] ";"
    public Ast.Field parseField() throws ParseException {
        // TODO: Clean up, add min token check (3 - LET)
        String identifier = tokens.get(0).getLiteral();
        boolean constant = false;
        Optional<Ast.Expression> expression = Optional.empty();

        typeCheck(Token.Type.IDENTIFIER);
        if (match("=")) {
            expression = Optional.of(parseExpression());
        }
        keywordCheck(";");
        return new Ast.Field(identifier, constant, expression);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    // method ::= "DEF" identifier "(" [ identifier { "," identifier } ] ")" "DO" { statement } "END"
    public Ast.Method parseMethod() throws ParseException {
        // TODO: Clean up, add min token check (6 - DEF)
        String identifier = tokens.get(0).getLiteral();
        List<String> identifiers = new ArrayList<>();
        List<Ast.Statement> statements = new ArrayList<>();

        typeCheck(Token.Type.IDENTIFIER);
        keywordCheck("(");
        if (!peek(")")) {
            do {
                String parameter = tokens.get(0).getLiteral();
                identifiers.add(parameter);
            } while (match(","));
        }
        keywordCheck(")");
        keywordCheck("DO");
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        keywordCheck("END");
        return new Ast.Method(identifier, identifiers, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    // statement ::= "LET" | "IF" | "FOR" | "WHILE" | "RETURN" | expression [ "=" expression ] ";"
    public Ast.Statement parseStatement() throws ParseException {
        if (!tokens.has(2)) {
            parseError("Incomplete statement", 2);
        }
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
    // "LET" identifier [ "=" expression ] ";"
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        // TODO: Clean up, add min token check (3 - LET)
        String identifier = tokens.get(0).getLiteral();
        Optional<Ast.Expression> expression = Optional.empty();

        typeCheck(Token.Type.IDENTIFIER);
        if (match("=")) {
            expression = Optional.of(parseExpression());
        }
        keywordCheck(";");
        return new Ast.Statement.Declaration(identifier, expression);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    // "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // TODO: Clean up, add min token check (4 - IF)
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
    // "FOR" identifier "IN" expression "DO" { statement } "END"
    public Ast.Statement.For parseForStatement() throws ParseException {
        //TODO: Make tests for FOR Statement, complete func, add min token check (6 - FOR)
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    // "WHILE" expression "DO" { statement } "END"
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        // TODO: Clean up, add min token check (4 - WHILE)
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
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        // TODO: Add min token check (3 - RETURN)
        Ast.Expression expression = parseExpression();
        keywordCheck(";");
        return new Ast.Statement.Return(expression);
    }

    // expression "=" expression ";"
    public Ast.Statement.Assignment parseAssignmentStatement(Ast.Expression receiver) throws ParseException {
        // TODO: Add min token check (4 - receiver)
        Ast.Expression value = parseExpression();
        keywordCheck(";");
        return new Ast.Statement.Assignment(receiver, value);
    }

    // expression ";"
    public Ast.Statement.Expression parseExpressionStatement(Ast.Expression expression) throws ParseException {
        keywordCheck(";");
        return new Ast.Statement.Expression(expression);
    }

    /**
     * Parses the {@code expression} rule.
     */
    // expression ::= logical_expression
    public Ast.Expression parseExpression() throws ParseException {
        // TODO: check there is at a minimum an
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    // logical_expression ::= comparison_expression
    //     { ( "AND" | "OR" ) comparison_expression }
    public Ast.Expression parseLogicalExpression() throws ParseException {
          return parseBinaryExpression(this::parseEqualityExpression, "AND", "&&",  "OR", "||");
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    // comparison_expression ::= additive_expression
    //      { ( "<" | "<=" | ">" | ">=" | "==" | "!=" ) additive_expression }
    public Ast.Expression parseEqualityExpression() throws ParseException {
        return parseBinaryExpression(this::parseAdditiveExpression, "<", "<=", ">", ">=", "==", "!=");
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    // additive_expression ::= multiplicative_expression
    //      { ( "+" | "-" ) multiplicative_expression }
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        return parseBinaryExpression(this::parseMultiplicativeExpression, "+", "-");
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    // multiplicative_expression ::= secondary_expression
    //      { ( "*" | "/" ) secondary_expression }
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        return parseBinaryExpression(this::parseSecondaryExpression, "*", "/");
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    // secondary_expression ::= primary_expression
    //      { "." identifier [ "(" [ expression { "," expression } ] ")" ] }
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression receiver = parsePrimaryExpression();
        while (match(".") && receiver instanceof Ast.Expression.Access) {
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
    //     "NIL"
    //     | "TRUE" | "FALSE"
    //     | integer | decimal
    //     | character | string
    //     | "(" expression ")"
    //     | identifier [ "(" [ expression { "," expression } ] ")" ]
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        return parsePrimaryExpression(Optional.empty());
    }

    public Ast.Expression parsePrimaryExpression(Optional<Ast.Expression> receiver) throws ParseException {
        Token.Type type = tokens.get(0).getType();
        String literal = tokens.get(0).getLiteral();

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
            if (type == Token.Type.STRING) {
                String substring = literal.substring(1, literal.length() - 1);
                ArrayList<Integer> indexes = findSlashIndices(substring);
                if (!indexes.isEmpty()) {
                    substring = clean(substring, indexes);
                }
                return new Ast.Expression.Literal(substring);
            }
            Character character = literal.charAt(1);
            return new Ast.Expression.Literal(character);
        }

        // "(" expression ")"
        if (match("(")) {
            Ast.Expression expression = parseExpression();
            keywordCheck(")");
            return new Ast.Expression.Group(expression);
        }

        // identifier [ "(" [ expression { "," expression } ] ")" ]
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
                                                 String... operators) throws ParseException {
        // TODO: Add min token check (1)
        Ast.Expression left = expression.get();

        while (check(operators)) {
            String operator = tokens.get(-1).getLiteral();
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

    // remove found escape characters from locations
    private String clean(String string, ArrayList<Integer> indexes) {
        StringBuilder builder = new StringBuilder(string);
        for (int i = indexes.size()-1; i >= 0; i--) {
            int index = indexes.get(i);
            char c = string.charAt(index+1);
            builder.deleteCharAt(index+1);
            builder.deleteCharAt(index);

            switch (c) {
                case 'b':
                    builder.insert(index, '\b');
                    break;
                case 'n':
                    builder.insert(index, '\n');
                    break;
                case 'r':
                    builder.insert(index, '\r');
                    break;
                case 't':
                    builder.insert(index, '\t');
                    break;
                case '\'':
                    builder.insert(index, '\'');
                    break;
                case '\\':
                    builder.insert(index, '\\');
                    break;
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
        throw new ParseException(message, index);
    }

    // helper
    private int tokenLocation(int offset) {
        int prev =  offset - 1;
        return tokens.has(offset) ? tokens.get(offset).getIndex()
                : tokens.get(prev).getIndex() + tokens.get(prev).getLiteral().length();
    }

    // helper
    private boolean keywordCheck(String word) {
        if (!match(word)) {
            String msg = "Missing: " + word;
            parseError(msg);
        }
        return true;
    }

    // helper
    private boolean typeCheck(Token.Type type) {
        if (!match(type)) {
            Token.Type actual = tokens.get(0).getType();
            String msg = "Type Error. Expected: " + actual + ", Got: " + type;
            parseError(msg);
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
