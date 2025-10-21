package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.List;

public class IntegrationTests {

    @Test
    void testIfScope() {
        String code = ScopeTestData.input1;
        List<Token> tokens = new Lexer(code).lex();
        Ast ast = new Parser(tokens).parseSource();

        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(ast, Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("1\n2\n3\n4\n1\n4" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @Test
    void testFunctionScope() {
        String code = ScopeTestData.input2;
        List<Token> tokens = new Lexer(code).lex();
        Ast ast = new Parser(tokens).parseSource();

        test(ast, BigInteger.valueOf(8), new Scope(null));
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }
}
