package freshlyground.compiler.frontend.analyzer;

import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Types;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static freshlyground.compiler.frontend.analyzer.AnalyzerTestingSupport.*;

public class AnalyzeExpressionTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testLiteralNode(String test, Ast.Expression.Literal ast, Environment.Type expected) {
            Analyzer analyzer = analyze(ast);
            assertType(analyzer, ast, expected);
        }
        private static Stream<Arguments> testLiteralNode() {
            return Stream.of(
                Arguments.of("Nil",
                    // NIL
                    new Ast.Expression.Literal(null),
                    Types.NIL
                ),
                Arguments.of("Boolean",
                    // TRUE
                    new Ast.Expression.Literal(true),
                    Types.BOOLEAN
                ),
                Arguments.of("Integer Valid",
                    // MAX_INT
                    new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                    Types.INTEGER
                ),
                Arguments.of("Integer Valid Min",
                    // MIN_INT
                    new Ast.Expression.Literal(BigInteger.valueOf(Integer.MIN_VALUE)),
                    Types.INTEGER
                ),
                Arguments.of("Decimal Valid",
                    // MAX_DEC
                    new Ast.Expression.Literal(BigDecimal.valueOf(Double.MAX_VALUE)),
                    Types.DECIMAL
                ),
                Arguments.of("Decimal Valid Min",
                    // MIN_DEC
                    new Ast.Expression.Literal(BigDecimal.valueOf(Double.MIN_VALUE)),
                    Types.DECIMAL
                )
            );
        }

        /*
         * Ast.Statement.Expression is a wrapper to elevate Ast.Expression.Function to a statement, no sad path needed
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testGroupNode(String test, Ast.Expression.Group ast, Environment.Type expected) {
            Analyzer analyzer = analyze(ast);
            assertType(analyzer, ast, expected);
        }
        private static Stream<Arguments> testGroupNode() {
            return Stream.of(
                Arguments.of("Group Binary",
                    // (1 + 10)
                    new Ast.Expression.Group(
                        new Ast.Expression.Binary("+",
                            new Ast.Expression.Literal(BigInteger.ONE),
                            new Ast.Expression.Literal(BigInteger.TEN)
                        )
                    ),
                    Types.INTEGER
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testBinaryNode(String test, Ast.Expression.Binary ast, Environment.Type expected) {
            Analyzer analyzer = analyze(ast);
            assertType(analyzer, ast, expected);
        }
        private static Stream<Arguments> testBinaryNode() {
            return Stream.of(
                Arguments.of("Logical AND Valid",
                    // TRUE AND FALSE
                    new Ast.Expression.Binary("AND",
                        new Ast.Expression.Literal(Boolean.TRUE),
                        new Ast.Expression.Literal(Boolean.FALSE)
                    ),
                    Types.BOOLEAN
                ),
                Arguments.of("String Concatenation",
                    // "Ben" + 10
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Literal("Ben"),
                        new Ast.Expression.Literal(BigInteger.TEN)
                    ),
                    Types.STRING
                ),
                Arguments.of("Integer Addition",
                    // 1 + 10
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigInteger.TEN)
                    ),
                    Types.INTEGER
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testAccessNode(String test, Ast.Expression.Access ast, Environment.Variable expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsVariable(analyzer, ast, expected);
        }
        private static Stream<Arguments> testAccessNode() {
            return Stream.of(
                Arguments.of("Variable",
                    // variable
                    new Ast.Expression.Access(Optional.empty(), "variable"),
                    new Environment.Variable("variable", Types.INTEGER, false)
                )
                // TODO swap these object.field tests to the builtins
                //
//            Arguments.of("Field",
//                // object.field
//                new Ast.Expression.Access(Optional.of(
//                    new Ast.Expression.Access(Optional.empty(), "object")
//                ), "field"),
//                new Environment.Variable("field", Types.INTEGER, false)
//            )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testFunctionNode(String test, Ast.Expression.Function ast, Environment.Function expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsFunction(analyzer, ast, expected);
        }
        private static Stream<Arguments> testFunctionNode() {
            return Stream.of(
                Arguments.of("Function",
                    // function()
                    new Ast.Expression.Function(Optional.empty(), "function", List.of()),
                    new Environment.Function("function", List.of(), Types.INTEGER)
                ),
                Arguments.of("Function Valid Arg",
                    // function(1)
                    new Ast.Expression.Function(Optional.empty(), "function", List.of(new Ast.Expression.Literal(BigInteger.ONE))),
                    new Environment.Function("function", List.of(Types.INTEGER), Types.INTEGER)
                ),
                Arguments.of("Method",
                    // object.stringify()
                    new Ast.Expression.Function(Optional.of(
                        new Ast.Expression.Access(Optional.empty(), "object")
                    ), "stringify", List.of()),
                    new Environment.Function("stringify", List.of(Types.ANY), Types.STRING)
                )
            );
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sad {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testLiteralNode(String test, Ast.Expression.Literal ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        // TODO add over and underflow
        // new Ast.Expression.Literal(BigDecimal.valueOf(Double.MIN_VALUE).subtract(BigDecimal.ONE))
        // new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE))
        private static Stream<Arguments> testLiteralNode() {
            return Stream.of(
                Arguments.of("Return Type Mismatch",
                    // 9223372036854775807
                    new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                    "INT Overflow or Underflow"
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testBinaryNode(String test, Ast.Expression.Binary ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testBinaryNode() {
            return Stream.of(
                Arguments.of("Integer Decimal Addition",
                    // 1 + 1.0
                    new Ast.Expression.Binary("+",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigDecimal.ONE)
                    ),
                    "Type unassignable: Decimal -> Integer"
                ),
                Arguments.of("GT Different Types",
                    // 1 > 10.0
                    new Ast.Expression.Binary(">",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigDecimal.TEN)
                    ),
                    "Types mismatch: must be type Integer but was Decimal"
                ),
                Arguments.of("Not Equal Different Types",
                    // 1 != 10.0
                    new Ast.Expression.Binary("!=",
                        new Ast.Expression.Literal(BigInteger.ONE),
                        new Ast.Expression.Literal(BigDecimal.TEN)
                    ),
                    "Types mismatch: must be type Integer but was Decimal"
                ),
                Arguments.of("Logical AND Invalid",
                    // TRUE AND "FALSE"
                    new Ast.Expression.Binary("AND",
                        new Ast.Expression.Literal(Boolean.TRUE),
                        new Ast.Expression.Literal("FALSE")
                    ),
                    "Type unassignable: String -> Boolean"
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testFunctionNode(String test, Ast.Expression.Function ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testFunctionNode() {
            return Stream.of(
                Arguments.of("Function Invalid Arg",
                    // function(1.0)
                    new Ast.Expression.Function(Optional.empty(), "function", List.of(new Ast.Expression.Literal(BigDecimal.ONE))),
                    "The function function/1 is not defined in this scope."
                )
            );
        }
    }
}
