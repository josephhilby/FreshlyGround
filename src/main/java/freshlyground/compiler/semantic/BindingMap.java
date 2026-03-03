package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public abstract class BindingMap<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final String nodeType;

    protected BindingMap(String nodeType) {
        this.nodeType = Objects.requireNonNull(nodeType);
    }

    protected final void setBinding(K node, V env) {
        map.put(Objects.requireNonNull(node), Objects.requireNonNull(env));
    }

    protected final V getBinding(K node) {
        V env = map.get(node);
        if (env == null) {
            throw new CompilerException("No " + nodeType + " binding for node: " + node);
        }
        return env;
    }

    public static final class Bindings {
        public Bindings() {}

        private final BindingMap<Ast.Field, Environment.Variable> fieldBindings =
            new BindingMap<>("Ast.Field") {};

        private final BindingMap<Ast.Statement.Declaration, Environment.Variable> declarationBindings =
            new BindingMap<>("Ast.Statement.Declaration") {};

        private final BindingMap<Ast.Expression.Access, Environment.Variable> accessBindings =
            new BindingMap<>("Ast.Expression.Access") {};

        private final BindingMap<Ast.Method, Environment.Function> methodBindings =
            new BindingMap<>("Ast.Method") {};

        private final BindingMap<Ast.Expression.Function, Environment.Function> functionBindings =
            new BindingMap<>("Ast.Expression.Function") {};

        private final BindingMap<Ast.Expression, Environment.Type> typeBindings =
            new BindingMap<>("Ast.Expression") {};

        private <R> R variableDispatch(
            Ast node,
            Function<Ast.Field, R> onField,
            Function<Ast.Statement.Declaration, R> onDeclaration,
            Function<Ast.Expression.Access, R> onAccess
        ) {
            if (node instanceof Ast.Field f) return onField.apply(f);
            if (node instanceof Ast.Statement.Declaration d) return onDeclaration.apply(d);
            if (node instanceof Ast.Expression.Access a) return onAccess.apply(a);
            throw new CompilerException("Unhandled node type: " + node.getClass().getName());
        }

        private <R> R functionDispatch(
            Ast node,
            Function<Ast.Method, R> onMethod,
            Function<Ast.Expression.Function, R> onFunction
        ) {
            if (node instanceof Ast.Method method) return onMethod.apply(method);
            if (node instanceof Ast.Expression.Function function) return onFunction.apply(function);
            throw new CompilerException("Unhandled node type: " + node.getClass().getName());
        }

        public void setVariable(Ast node, Environment.Variable environment) {
            variableDispatch(
                node,
                field -> { fieldBindings.setBinding(field, environment); return null; },
                declaration -> { declarationBindings.setBinding(declaration, environment); return null; },
                access -> { accessBindings.setBinding(access, environment); return null; }
            );
        }

        public Environment.Variable getVariable(Ast node) {
            return variableDispatch(
                node,
                fieldBindings::getBinding,
                declarationBindings::getBinding,
                accessBindings::getBinding
            );
        }

        public void setFunction(Ast node, Environment.Function environment) {
            functionDispatch(
                node,
                method -> { methodBindings.setBinding(method, environment); return null; },
                function -> { functionBindings.setBinding(function, environment); return null; }
            );
        }

        public Environment.Function getFunction(Ast node) {
            return functionDispatch(
                node,
                methodBindings::getBinding,
                functionBindings::getBinding
            );
        }

        public void setType(Ast.Expression node, Environment.Type type) {
            typeBindings.setBinding(node, type);
        }

        public Environment.Type getType(Ast.Expression node) {
            if (node instanceof Ast.Expression.Function function) {
                return functionBindings.getBinding(function).returnType();
            }
            if (node instanceof Ast.Expression.Access access) {
                return accessBindings.getBinding(access).type();
            }
            return typeBindings.getBinding(node);
        }
    }
}
