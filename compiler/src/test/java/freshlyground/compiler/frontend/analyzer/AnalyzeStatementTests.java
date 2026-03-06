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

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static freshlyground.compiler.frontend.analyzer.AnalyzerTestingSupport.*;

public class AnalyzeStatementTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testAssignmentNode(String test, Ast.Statement.Assignment ast, Environment.Variable expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsVariable(analyzer, ast.getReceiver(), expected);
        }
        private static Stream<Arguments> testAssignmentNode() {
            return Stream.of(
                Arguments.of("Variable",
                    // variable = 1;
                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        new Ast.Expression.Literal(BigInteger.ONE)
                    ),
                    new Environment.Variable("variable", Types.INTEGER, false)
                )
                // TODO find new way to do object.field = 1 test
//            Arguments.of("Field",
//                // object.field = 1;
//                new Ast.Statement.Assignment(
//                    new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
//                    new Ast.Expression.Literal(BigInteger.ONE)
//                ),
//                new Environment.Variable("field", Types.INTEGER, false)
//            )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testDeclarationNode(String test, Ast.Statement.Declaration ast, Environment.Variable expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsVariable(analyzer, ast, expected);
        }
        private static Stream<Arguments> testDeclarationNode() {
            return Stream.of(
                Arguments.of("Declaration",
                    // LET name: Integer;
                    new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                    new Environment.Variable("name", Types.INTEGER, false)
                ),
                Arguments.of("Initialization",
                    // LET name = 1;
                    new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                    new Environment.Variable("name", Types.INTEGER, false)
                )
            );
        }

        /*
         * Ast.Statement.Expression is a wrapper to elevate Ast.Expression.Function to a statement, no sad path needed
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testExpressionNode(String test, Ast.Statement.Expression ast, Environment.Function expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsFunction(analyzer, ast.getExpression(), expected);
        }
        private static Stream<Arguments> testExpressionNode() {
            return Stream.of(
                Arguments.of("Function",
                    // print(1);
                    new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", List.of(
                        new Ast.Expression.Literal(BigInteger.ONE)
                    ))),
                    new Environment.Function("print", List.of(Types.ANY), Types.NIL)
                )
            );
        }

        /*
         * Remaining tests only check for no errors as the interior bindings are checked elsewhere.
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testIfNode(String test, Ast.Statement.If ast) {
            assertAnalyzes(ast);
        }
        private static Stream<Arguments> testIfNode() {
            return Stream.of(
                Arguments.of("Valid Condition",
                    // IF TRUE DO LET x = 1; END
                    new Ast.Statement.If(
                        new Ast.Expression.Literal(Boolean.TRUE),
                        List.of(new Ast.Statement.Expression(
                            new Ast.Expression.Function(Optional.empty(), "print", List.of(
                                new Ast.Expression.Literal(BigInteger.ONE)
                            ))
                        )),
                        List.of(new Ast.Statement.Declaration(
                            "x",
                            Optional.empty(),
                            Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                        ))
                    )
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testForNode(String test, Ast.Statement.For ast) {
            assertAnalyzes(ast);
        }
        private static Stream<Arguments> testForNode() {
            return Stream.of(
                // FOR (i = 0; TRUE; i = i + 1) LET x = 1; END
                Arguments.of("Valid Condition",
                    new Ast.Statement.For(
                        new Ast.Statement.Assignment(
                            new Ast.Expression.Access(Optional.empty(), "i"),
                            new Ast.Expression.Literal(BigInteger.ZERO)),

                        new Ast.Expression.Literal(Boolean.TRUE),

                        new Ast.Statement.Assignment(
                            new Ast.Expression.Access(Optional.empty(), "i"),
                            new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "i"),
                                new Ast.Expression.Literal(BigInteger.ONE))),

                        List.of(new Ast.Statement.Declaration(
                            "x",
                            Optional.empty(),
                            Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                        ))
                    )
                ),
                // FOR (; TRUE; ) LET x = 1; END
                Arguments.of("Init and Incr Not Present",
                    new Ast.Statement.For(
                        null,
                        new Ast.Expression.Literal(Boolean.TRUE),
                        null,
                        List.of(new Ast.Statement.Declaration(
                            "x",
                            Optional.empty(),
                            Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                        ))
                    )
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testWhileNode(String test, Ast.Statement.While ast) {
            assertAnalyzes(ast);
        }
        private static Stream<Arguments> testWhileNode() {
            return Stream.of(
                // WHILE TRUE DO END
                Arguments.of("Valid Condition",
                    new Ast.Statement.While(new Ast.Expression.Literal(Boolean.TRUE), List.of())
                )
            );
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sad {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testAssignmentNode(String test, Ast.Statement.Assignment ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testAssignmentNode() {
            return Stream.of(
                Arguments.of("Invalid Type",
                    // variable = "string";
                    new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        new Ast.Expression.Literal("string")
                    ),
                    "The variable variable is not defined in this scope."
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testDeclarationNode(String test, Ast.Statement.Declaration ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testDeclarationNode() {
            return Stream.of(
                // MOVE TO PARSER TESTS
//            Arguments.of("Missing Type",
//                // LET name;
//                new Ast.Statement.Declaration("name", Optional.empty(), Optional.empty()),
//                "Must have declared type or value"
//            ),
                Arguments.of("Unknown Type",
                    // LET name: Unknown;
                    new Ast.Statement.Declaration("name", Optional.of("Unknown"), Optional.empty()),
                    "Unknown type: Unknown."
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testIfNode(String test, Ast.Statement.If ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testIfNode() {
            return Stream.of(
                Arguments.of("Invalid Condition",
                    // IF "FALSE" DO LET x = 1; END
                    new Ast.Statement.If(
                        new Ast.Expression.Literal("FALSE"),
                        List.of(new Ast.Statement.Expression(
                            new Ast.Expression.Function(Optional.empty(), "print", List.of(
                                new Ast.Expression.Literal(BigInteger.ONE)
                            ))
                        )),
                        List.of(new Ast.Statement.Declaration(
                            "x",
                            Optional.empty(),
                            Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                        ))
                    ),
                    "Type unassignable: String -> Boolean"
                ),
                Arguments.of("Empty Statements",
                    // IF TRUE DO END
                    new Ast.Statement.If(
                        new Ast.Expression.Literal(Boolean.TRUE),
                        List.of(),
                        List.of()
                    ),
                    "IF block must contain at least one then statement"
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testForNode(String test, Ast.Statement.For ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testForNode() {
            return Stream.of(
                // FOR (; 0; ) LET x = 1; END
                Arguments.of("Invalid Condition",
                    new Ast.Statement.For(null, new Ast.Expression.Literal(BigInteger.ZERO), null, List.of()),
                    "Type unassignable: Integer -> Boolean"
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testWhileNode(String test, Ast.Statement.While ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testWhileNode() {
            return Stream.of(
                // WHILE 0 DO END
                Arguments.of("Invalid Condition",
                    new Ast.Statement.While(new Ast.Expression.Literal(BigInteger.ZERO), List.of()),
                    "Type unassignable: Integer -> Boolean"
                )
            );
        }
    }
}
