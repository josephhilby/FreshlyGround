package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class EndToEndInterpreterTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, String source, Object expected) {
        Scope scope = new Scope(null);
        test(source, expected, Parser::parseSource, scope);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            // LET x: Integer = 1;
            // LET y: Integer = 10;
            // DEF main(): Integer DO RETURN x + y; END
            Arguments.of("Field Addition",
                String.join(System.lineSeparator(),
                    "LET x: Integer = 1;",
                    "LET y: Integer = 10;",
                    "DEF main(): Integer DO RETURN x + y; END"
                ),
                BigInteger.valueOf(11)
            )
            // Source Invoke Main (predefined): <empty>, scope = {main/0 = ...} (returns 0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testField(String test, String source, Object expected) {
        ScopeAst ret = test(source, Environment.NIL.getValue(), Parser::parseField, new Scope(null));

        Scope scope = ret.getScope();
        Ast.Field ast = (Ast.Field) ret.getAst();
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
            // LET name: Integer;
            Arguments.of("Declaration",
                String.join(System.lineSeparator(),
                    "LET name: Integer;"
                ),
                // scope = {name = NIL}
                Environment.NIL.getValue()
            ),
            // LET name: Integer = 1;
            Arguments.of("Initialization",
                String.join(System.lineSeparator(),
                    "LET name: Integer = 1;"
                ),
                // scope = {name = 1}
                BigInteger.valueOf(1)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, String source, Object expected, List<Environment.PlcObject> args) {
        ScopeAst ret = test(source, Environment.NIL.getValue(), Parser::parseMethod, new Scope(null));

        Scope scope = ret.getScope();
        Ast.Method ast = (Ast.Method) ret.getAst();
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
            // DEF main(): Integer DO RETURN 0; END
            Arguments.of("Main",
                String.join(System.lineSeparator(),
                    "DEF main(): Integer DO RETURN 0; END"
                ),
                BigInteger.valueOf(0),
                Arrays.asList()
            ),
            // DEF square(x: Integer): Integer DO RETURN x * x; END
            Arguments.of("One Parameter",
                String.join(System.lineSeparator(),
                    "DEF square(x: Integer): Integer DO RETURN x * x; END"
                ),
                BigInteger.valueOf(81),
                Arrays.asList(Environment.create(BigInteger.valueOf(9)))
            )
        );
    }

    @Test
    void testPrintStatement() {
        // print("Hello, World!");
        // scope = {}
        // %System.out.println("Hello, World!");%
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(
                "print(\"Hello, World!\");",
                Environment.NIL.getValue(),
                Parser::parseStatement,
                new Scope(null)
            );
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @Test
    void testLogStatement() {
        // Log: log(1);
        // scope = {log/1 = ...}

        Scope scope = new Scope(null);
        StringWriter writer = new StringWriter();
        scope.defineFunction("log", 1, args -> {
            writer.write(String.valueOf(args.get(0).getValue()));
            return args.get(0);
        });
        test(
            "log(1);",
            Environment.NIL.getValue(),
            Parser::parseStatement,
            scope
        );
        Assertions.assertEquals("1", writer.toString());
    }

    @Test
    void testLogSource() {
        // DEF main(): Integer DO
        //     LET x = 1;
        //     LET y = 2;
        //     log(x);
        //     log(y);
        //     IF TRUE DO
        //         LET x = 3;
        //         y = 4;
        //         log(x);
        //         log(y);
        //     END
        //     log(x);
        //     log(y);
        // END

        Scope scope = new Scope(null);
        StringWriter writer = new StringWriter();
        scope.defineFunction("log", 1, args -> {
            writer.write(String.valueOf(args.get(0).getValue()));
            return args.get(0);
        });
        test(
            String.join(System.lineSeparator(),
                "DEF main(): Integer DO",
                "    LET x = 1;",
                "    LET y = 2;",
                "    log(x);",
                "    log(y);",
                "    IF TRUE DO",
                "        LET x = 3;",
                "        y = 4;",
                "        log(x);",
                "        log(y);",
                "    END",
                "    log(x);",
                "    log(y);",
                "END"
            ),
            Environment.NIL.getValue(),
            Parser::parseSource,
            scope
        );
        Assertions.assertEquals("123414", writer.toString());
    }

    @Test
    void testOutputSource() {
        // LET x: Integer = 1;
        // LET y: Integer = 2;
        // LET z: Integer = 3;
        // DEF f(z: Integer): Integer DO
        //     RETURN x + y + z;
        // END
        // DEF main(): Integer DO
        //     LET y = 4;
        //     RETURN f(5);
        // END

        Scope scope = new Scope(null);
        test(
            String.join(System.lineSeparator(),
                "LET x: Integer = 1;",
                "LET y: Integer = 2;",
                "LET z: Integer = 3;",
                "DEF f(z: Integer): Integer DO",
                "    RETURN x + y + z;",
                "END",
                "DEF main(): Integer DO",
                "    LET y = 4;",
                "    RETURN f(100);",
                "END"
            ),
            BigInteger.valueOf(103),
            Parser::parseSource,
            scope
        );
    }

    // Statement

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclaration(String test, String source, Object expected) {
        ScopeAst ret = test(source, Environment.NIL.getValue(), Parser::parseStatement, new Scope(null));

        Scope scope = ret.getScope();
        Ast.Statement.Declaration ast = (Ast.Statement.Declaration) ret.getAst();
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclaration() {
        return Stream.of(
            // LET name;
            // scope = {name = NIL}
            Arguments.of("Declaration",
                String.join(System.lineSeparator(),
                    "LET name;"
                ),
                Environment.NIL.getValue()
            ),
            // LET name = 1;
            // scope = {name = 1}
            Arguments.of("Initialization",
                String.join(System.lineSeparator(),
                    "LET name = 1;"
                ),
                BigInteger.valueOf(1)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentVariable(String test, String source, Object expected, String name) {
        Scope root = new Scope(null);
        root.defineVariable("variable", false, Environment.create("variable"));

        test(source, Environment.NIL.getValue(), Parser::parseStatement, root);
        Assertions.assertEquals(expected, root.lookupVariable(name).getValue().getValue());
    }

    private static Stream<Arguments> testAssignmentVariable() {
        return Stream.of(
            // variable = 1;, scope = {variable = NIL}
            // scope = {variable = 11}
            Arguments.of("Variable",
                String.join(System.lineSeparator(),
                    "variable = 11;"
                ),
                BigInteger.valueOf(11),
                "variable"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentField(String test, String source, Object expected, String name) {
        Scope root = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create("object.field"));
        root.defineVariable("object", false, new Environment.PlcObject(object, "object"));

        test(source, Environment.NIL.getValue(), Parser::parseStatement, root);
        Assertions.assertEquals(expected, object.lookupVariable(name).getValue().getValue());
    }

    private static Stream<Arguments> testAssignmentField() {
        return Stream.of(
            // object.field = 13;, scope = {object = PlcObject{field = NIL}}
            // scope = {object = PlcObject{field = 13}}
            Arguments.of("List",
                String.join(System.lineSeparator(),
                    "object.field = 13;"
                ),
                BigInteger.valueOf(13),
                "field"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIf(String test, String source, Object expected) {
        Scope initialScope = new Scope(null);
        initialScope.defineVariable("num", false, Environment.create("num"));
        ScopeAst ret = test(source, Environment.NIL.getValue(), Parser::parseStatement, initialScope);

        Scope scope = ret.getScope();
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIf() {
        return Stream.of(
            // IF TRUE DO num = 1; END, scope = {num = NIL}
            // scope = {num = 1}
            Arguments.of("True Condition",
                String.join(System.lineSeparator(),
                    "IF TRUE DO num = 1; ELSE num = 10; END"
                ),
                BigInteger.valueOf(1)
            ),
            // IF FALSE DO ELSE num = 10; END, scope = {num = NIL}
            // scope = {num = 10}
            Arguments.of("False Condition",
                String.join(System.lineSeparator(),
                    "IF FALSE DO num = 1; ELSE num = 10; END"
                ),
                BigInteger.valueOf(10)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFor(String test, String source, Object expected) {
        Scope initialScope = new Scope(null);
        initialScope.defineVariable("num", false, Environment.create("num"));
        initialScope.defineVariable("sum", false, Environment.create(BigInteger.ZERO));
        ScopeAst ret = test(source, Environment.NIL.getValue(), Parser::parseStatement, initialScope);

        Scope scope = ret.getScope();
        Assertions.assertEquals(expected, scope.lookupVariable("sum").getValue().getValue());
    }

    private static Stream<Arguments> testFor() {
        return Stream.of(
            // FOR (num = 0; num < 5; num = num + 1) sum = sum + num; END, scope = {sum = 0, num = NIL}
            // scope = {sum = 10, num = 5}
            Arguments.of("For",
                String.join(System.lineSeparator(),
                    "FOR (num = 0; num < 5; num = num + 1) sum = sum + num; END"
                ),
                BigInteger.valueOf(10)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhile(String test, String source, Object expected) {
        Scope initialScope = new Scope(null);
        initialScope.defineVariable("num", false, Environment.create(BigInteger.ZERO));
        ScopeAst ret = test(source, Environment.NIL.getValue(), Parser::parseStatement, initialScope);

        Scope scope = ret.getScope();
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testWhile() {
        return Stream.of(
            // WHILE num < 10 DO num = num + 1; END, scope = {num = 0}
            // scope = {num = 10}
            Arguments.of("While",
                String.join(System.lineSeparator(),
                    "WHILE num < 20 DO num = num + 1; END"
                ),
                BigInteger.valueOf(20)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteral(String test, String source, Object expected) {
        Scope initialScope = new Scope(null);
        test(source, expected, Parser::parseExpression, initialScope);
    }

    private static Stream<Arguments> testLiteral() {
        return Stream.of(
            // NIL
            Arguments.of("Nil",
                String.join(System.lineSeparator(),
                    "NIL"
                ),
                Environment.NIL.getValue()
            ),
            // 1
            Arguments.of("Integer",
                String.join(System.lineSeparator(),
                    "1"
                ),
                BigInteger.valueOf(1)
            ),
            // "string"
            Arguments.of("String",
                String.join(System.lineSeparator(),
                    "\"string\""
                ),
                "string"
            ),
            // Boolean: TRUE
            Arguments.of("Boolean",
                String.join(System.lineSeparator(),
                    "TRUE"
                ),
                true
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroup(String test, String source, Object expected) {
        Scope initialScope = new Scope(null);
        test(source, expected, Parser::parseExpression, initialScope);
    }

    private static Stream<Arguments> testGroup() {
        return Stream.of(
            // (1)
            Arguments.of("Literal",
                String.join(System.lineSeparator(),
                    "(15)"
                ),
                BigInteger.valueOf(15)
            ),
            // (1 + 10)
            Arguments.of("Binary",
                String.join(System.lineSeparator(),
                    "(1 + 100)"
                ),
                BigInteger.valueOf(101)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinary(String test, String source, Object expected) {
        Scope initialScope = new Scope(null);
        test(source, expected, Parser::parseExpression, initialScope);
    }

    private static Stream<Arguments> testBinary() {
        return Stream.of(
            // TRUE AND FALSE
            Arguments.of("And",
                String.join(System.lineSeparator(),
                    "TRUE AND FALSE"
                ),
                false
            ),
            // TRUE OR undefined
            Arguments.of("Or (Short Circuit)",
                String.join(System.lineSeparator(),
                    "TRUE OR undefined"
                ),
                true
            ),
            // 1 < 10
            Arguments.of("Less Than",
                String.join(System.lineSeparator(),
                    "1 < 10"
                ),
                true
            ),
            // 1 == 10
            Arguments.of("Equal",
                String.join(System.lineSeparator(),
                    "1 == 10"
                ),
                false
            ),
            // "a" + "b"
            Arguments.of("Concatenation",
                String.join(System.lineSeparator(),
                    "\"a\" + \"b\""
                ),
                "ab"
            ),
            // 1 + 10
            Arguments.of("Addition",
                String.join(System.lineSeparator(),
                    "1 + 10"
                ),
                BigInteger.valueOf(11)
            ),
            // 1.2 / 3.4
            Arguments.of("Division",
                String.join(System.lineSeparator(),
                    "1.2 / 3.4"
                ),
                BigDecimal.valueOf(0.4)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccessVariable(String test, String source, Object expected, String name) {
        Scope root = new Scope(null);
        root.defineVariable("variable", false, Environment.create(BigInteger.valueOf(28)));

        test(source, Environment.NIL.getValue(), Parser::parseStatement, root);
        Assertions.assertEquals(expected, root.lookupVariable(name).getValue().getValue());
    }

    private static Stream<Arguments> testAccessVariable() {
        return Stream.of(
            // variable, scope = {variable = 28}
            Arguments.of("Variable",
                String.join(System.lineSeparator(),
                    "variable;"
                ),
                BigInteger.valueOf(28),
                "variable"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccessField(String test, String source, Object expected, String name) {
        Scope root = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create(BigInteger.valueOf(13)));
        root.defineVariable("object", false, new Environment.PlcObject(object, "object"));

        test(source, Environment.NIL.getValue(), Parser::parseStatement, root);
        Assertions.assertEquals(expected, object.lookupVariable(name).getValue().getValue());
    }

    private static Stream<Arguments> testAccessField() {
        return Stream.of(
            // object.field, scope = {object = PlcObject{field = 13}}
            Arguments.of("List",
                String.join(System.lineSeparator(),
                    "object.field;"
                ),
                BigInteger.valueOf(13),
                "field"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunction(String test, String source, Object expected, String name, int arity, List<Environment.PlcObject> params) {
        Scope scope = new Scope(null);
        StringWriter writer = new StringWriter();
        scope.defineFunction("log", 1, args -> {
            writer.write(String.valueOf(args.get(0).getValue()));
            return args.get(0);
        });
        scope.defineFunction("function", 0, args -> {return Environment.create(BigInteger.ZERO);});

        test(source, expected, Parser::parseExpression, scope);
        Assertions.assertEquals(expected, scope.lookupFunction(name, arity).invoke(params).getValue());
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
            // function(), scope = {function/0 = ... (returns 1)
            Arguments.of("Function",
                String.join(System.lineSeparator(),
                    "function()"
                ),
                BigInteger.valueOf(0),
                "function",
                0,
                Arrays.asList()
            ),
            // log(1), scope = {log/1 = ...} (returns the argument, 1)
            Arguments.of("Log",
                String.join(System.lineSeparator(),
                    "log(5)"
                ),
                BigInteger.valueOf(5),
                "log",
                1,
                Arrays.asList(Environment.create(BigInteger.valueOf(5)))
            )
        );
    }

    // Error
        // Integer Decimal Subtraction: 1 - 1.0
        // While w/ String: WHILE "false" DO END
        // Redefined Field: LET name: Integer; LET name: Integer = 1;

    static class ScopeAst {
        private Scope scope;
        private Ast ast;

        public ScopeAst(Scope scope, Ast ast) {
            this.scope = scope;
            this.ast = ast;
        }

        public Scope getScope() { return scope; }
        public Ast getAst() { return ast; }
    }

    // LEXER - PARSER - INTERPRETER
    private static <T extends Ast> ScopeAst test(String input,
                                             Object expected,
                                             Function<Parser, T> function,
                                             Scope scope) {
        // code -> LEXER -> tokens
        List<Token> tokens = new Lexer(input).lex();

        // tokens -> PARSER -> ast
        Parser parser = new Parser(tokens);
        Ast ast = function.apply(parser);

        // ast -> INTERPRETER -> java
        Interpreter interpreter = new Interpreter(scope);

        Assertions.assertEquals(expected, interpreter.visit(ast).getValue());

        return new ScopeAst(interpreter.getScope(), ast);
    }
}
