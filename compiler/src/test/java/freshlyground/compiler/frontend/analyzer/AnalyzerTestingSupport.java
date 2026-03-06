package freshlyground.compiler.frontend.analyzer;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.semantic.Environment;

import freshlyground.compiler.semantic.Types;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class AnalyzerTestingSupport {
    private AnalyzerTestingSupport() {}

    static <T extends Ast> Analyzer analyze(T node) {
        Analyzer analyzer = new Analyzer();
        analyzer.getScope().defineFunction("function", List.of(), Types.INTEGER);
        analyzer.getScope().defineFunction("function", List.of(Types.INTEGER), Types.INTEGER);
        analyzer.getScope().defineVariable("variable", Types.INTEGER, false);
        analyzer.getScope().defineVariable("object", Types.ANY, false);
        analyzer.getScope().defineVariable("i", Types.INTEGER, false);
        analyzer.visit(node);
        return analyzer;
    }

    static <T extends Ast> Analyzer assertAnalyzes(T node) {
        return Assertions.assertDoesNotThrow(() -> analyze(node));
    }

    static void assertCompilerError(String expectedMessage, Runnable action) {
        String message = Assertions.assertThrows(CompilerException.class, action::run).getMessage();
        Assertions.assertEquals(expectedMessage, message);
    }

    static void assertBindsVariable(Analyzer analyzer, Ast node, Environment.Variable expected) {
        Assertions.assertEquals(expected, analyzer.getBindings().getVariable(node));
        Assertions.assertEquals(expected, analyzer.getScope().lookupVariable(expected.name()));
    }

    // Checks lexical or type scope based on if receiver present
    static void assertBindsFunction(Analyzer analyzer, Ast node, Environment.Function expected) {
        Assertions.assertEquals(expected, analyzer.getBindings().getFunction(node));

        if (node instanceof Ast.Expression.Function call && call.getReceiver().isPresent()) {
            Ast.Expression receiver = call.getReceiver().get();
            Environment.Type receiverType = analyzer.getBindings().getType(receiver);

            Assertions.assertEquals(expected, receiverType.lookupMemberFunction(expected.name(), call.getArguments().size()));
            return;
        }

        Assertions.assertEquals(expected, analyzer.getScope().lookupFunction(expected.name(), expected.parameterTypes().size()));
    }

    static void assertType(Analyzer analyzer, Ast.Expression expr, Environment.Type expected) {
        Assertions.assertEquals(expected, analyzer.getBindings().getType(expr));
    }
}
