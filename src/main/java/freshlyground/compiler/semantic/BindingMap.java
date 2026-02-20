package freshlyground.compiler.semantic;

import freshlyground.compiler.frontend.Ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
            throw new RuntimeException("No " + nodeType + " binding for node: " + node);
        }
        return env;
    }

    public static final class VariableBindings<K> extends BindingMap<K, Environment.Variable> {
        public VariableBindings(String nodeType) { super(nodeType); }
        public void setVariable(K node, Environment.Variable variable) { setBinding(node, variable); }
        public Environment.Variable getVariable(K node) { return getBinding(node); }
    }

    public static final class FunctionBindings<K> extends BindingMap<K, Environment.Function> {
        public FunctionBindings(String nodeType) { super(nodeType); }
        public void setFunction(K node, Environment.Function function) { setBinding(node, function); }
        public Environment.Function getFunction(K node) { return getBinding(node); }
    }

    public static final class TypeBindings<K> extends BindingMap<K, Environment.Type> {
        public TypeBindings(String nodeType) { super(nodeType); }
        public void setType(K node, Environment.Type type) { setBinding(node, type); }
        public Environment.Type getType(K node) { return getBinding(node); }
    }

    public static final class Bindings {
        private final VariableBindings<Ast.Field> fieldBindings =
            new VariableBindings<>("Ast.Field");

        private final VariableBindings<Ast.Statement.Declaration> declarationBindings =
            new VariableBindings<>("Ast.Statement.Declaration");

        private final VariableBindings<Ast.Expression.Access> accessBindings =
            new VariableBindings<>("Ast.Expression.Access");

        private final FunctionBindings<Ast.Method> methodBindings =
            new FunctionBindings<>("Ast.Method");

        private final FunctionBindings<Ast.Expression.Function> functionBindings =
            new FunctionBindings<>("Ast.Expression.Function");

        private final TypeBindings<Ast.Expression> typeBindings =
            new TypeBindings<>("Ast.Expression");

        private BindingMap.VariableBindings<?> variableDispatch(Ast node) {
            if (node instanceof Ast.Field) return fieldBindings;
            if (node instanceof Ast.Statement.Declaration) return declarationBindings;
            if (node instanceof Ast.Expression.Access) return accessBindings;
            throw new RuntimeException("Unhandled node type: " + node.getClass().getName());
        }

        private BindingMap.FunctionBindings<?> functionDispatch(Ast node) {
            if (node instanceof Ast.Method) return methodBindings;
            if (node instanceof Ast.Expression.Function) return functionBindings;
            throw new RuntimeException("Unhandled node type: " + node.getClass().getName());
        }

        public Bindings() {}

        public void setVariable(Ast node, Environment.Variable variable) {
            BindingMap.VariableBindings<Ast> binding = (BindingMap.VariableBindings<Ast>) variableDispatch(node);
            binding.setVariable(node, variable);
        }

        public Environment.Variable getVariable(Ast node) {
            BindingMap.VariableBindings<Ast> binding = (BindingMap.VariableBindings<Ast>) variableDispatch(node);
            return binding.getVariable(node);
        }

        public void setFunction(Ast node, Environment.Function function) {
            BindingMap.FunctionBindings<Ast> binding = (BindingMap.FunctionBindings<Ast>) functionDispatch(node);
            binding.setFunction(node, function);
        }

        public Environment.Function getFunction(Ast node) {
            BindingMap.FunctionBindings<Ast> binding = (BindingMap.FunctionBindings<Ast>) functionDispatch(node);
            return binding.getFunction(node);
        }

        public void setType(Ast.Expression node, Environment.Type type) {
            typeBindings.setType(node, type);
        }

        public Environment.Type getType(Ast.Expression node) {
            if (node instanceof Ast.Expression.Function function) {
                return functionBindings.getFunction(function).getType();
            }
            if (node instanceof Ast.Expression.Access access) {
                return accessBindings.getVariable(access).getType();
            }
            return typeBindings.getType(node);
        }
    }
}
