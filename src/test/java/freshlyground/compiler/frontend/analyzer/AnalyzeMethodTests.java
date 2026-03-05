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
import static freshlyground.compiler.frontend.analyzer.AnalyzerTestingSupport.assertCompilerError;

public class AnalyzeMethodTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testNode(String test, Ast.Method ast, Environment.Function expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsFunction(analyzer, ast, expected);
        }
        private static Stream<Arguments> testNode() {
            return Stream.of(
                Arguments.of("Main",
                    // DEF main(): Integer DO
                    //   RETURN 0;
                    // END
                    new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                        new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                    ),
                    new Environment.Function("main", List.of(), Types.INTEGER)
                ),
                Arguments.of("Hello World",
                    // DEF main(): Integer DO
                    //   print("Hello, World!");
                    // END
                    new Ast.Method("main", List.of(), List.of(), Optional.of("Integer"), List.of(
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", List.of(
                            new Ast.Expression.Literal("Hello, World!")
                        )))
                    )),
                    new Environment.Function("main", List.of(), Types.INTEGER)
                ),
                Arguments.of("No Explicit Return Type",
                    // DEF empty() DO
                    // END
                    new Ast.Method("empty", List.of(), List.of(), Optional.empty(), List.of()),
                    new Environment.Function("empty", List.of(), Types.NIL)
                )
            );
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sad {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testNode(String test, Ast.Method ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testNode() {
            return Stream.of(
                Arguments.of("Return Type Mismatch",
                    // DEF increment(num: Integer): Decimal DO RETURN num + 1; END
                    new Ast.Method("increment", List.of("num"), List.of("Integer"), Optional.of("Decimal"), List.of(
                        new Ast.Statement.Return(new Ast.Expression.Binary("+",
                            new Ast.Expression.Access(Optional.empty(), "num"),
                            new Ast.Expression.Literal(BigInteger.ONE)
                        ))
                    )),
                    "Type unassignable: Integer -> Decimal"
                )
            );
        }
    }
}
