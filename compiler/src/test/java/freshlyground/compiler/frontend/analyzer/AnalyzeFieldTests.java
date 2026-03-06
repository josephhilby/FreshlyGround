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
import java.util.Optional;
import java.util.stream.Stream;

import static freshlyground.compiler.frontend.analyzer.AnalyzerTestingSupport.*;

public class AnalyzeFieldTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Happy {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testNode(String test, Ast.Field ast, Environment.Variable expected) {
            Analyzer analyzer = analyze(ast);
            assertBindsVariable(analyzer, ast, expected);
        }
        private static Stream<Arguments> testNode() {
            return Stream.of(
                Arguments.of("Declaration",
                    // LET name: Decimal;
                    new Ast.Field("name","Decimal", false, Optional.empty()),
                    new Environment.Variable("name", Types.DECIMAL, false)
                ),
                Arguments.of("Initialization",
                    // LET name: Integer = 1;
                    new Ast.Field("name","Integer", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                    new Environment.Variable("name", Types.INTEGER, false)
                )
            );
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Sad {
        @ParameterizedTest(name = "{0}")
        @MethodSource
        public void testNode(String test, Ast.Field ast, String expectedMessage) {
            Analyzer analyzer = new Analyzer();
            assertCompilerError(expectedMessage, () -> analyzer.visit(ast));
        }
        private static Stream<Arguments> testNode() {
            return Stream.of(
                // TODO add test for Primitive type
                Arguments.of("Unknown Type",
                    // LET name: Unknown;
                    new Ast.Field("name","Unknown", false, Optional.empty()),
                    "Unknown type: Unknown."
                )
            );
        }
    }
}
