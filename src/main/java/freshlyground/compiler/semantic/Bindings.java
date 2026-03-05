package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.artifacts.Ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * <p>{@code Bindings} stores the semantic attachments produced during analysis, by mapping AST nodes to their
 * resolved {@code Environment.Variable variables}, {@code Environment.Function functions}, and
 * {@code Environment.Type types}. It provides a single, centralized binding layer so the AST can remain
 * immutable and free of semantic state.</p>
 *
 * <p>Bindings are keyed by specific node kinds:</p>
 * <ul>
 *   <li>{@code Ast.Field}, {@code Ast.Statement.Declaration}, and {@code Ast.Expression.Access}
 *       bind to {@code Environment.Variable}.</li>
 *   <li>{@code Ast.Method} and {@code Ast.Expression.Function}
 *       bind to {@code Environment.Function}.</li>
 *   <li>{@code Ast.Expression} nodes bind to {@code Environment.Type}.</li>
 * </ul>
 */
public final class Bindings {

    public void setVariable(Ast node, Environment.Variable env) {
        variableDispatch(
            node,
            field -> { fieldBindings.set(field, env); return null; },
            decl  -> { declarationBindings.set(decl, env); return null; },
            acc   -> { accessBindings.set(acc, env); return null; }
        );
    }
    public Environment.Variable getVariable(Ast node) {
        return variableDispatch(
            node,
            fieldBindings::get,
            declarationBindings::get,
            accessBindings::get
        );
    }

    public void setFunction(Ast node, Environment.Function env) {
        functionDispatch(
            node,
            method -> { methodBindings.set(method, env); return null; },
            fn     -> { functionBindings.set(fn, env); return null; }
        );
    }
    public Environment.Function getFunction(Ast node) {
        return functionDispatch(
            node,
            methodBindings::get,
            functionBindings::get
        );
    }

    public void setType(Ast.Expression node, Environment.Type type) {
        typeBindings.set(node, type);
    }
    public Environment.Type getType(Ast.Expression node) {
        // Derived types from bindings should win (no separate entry needed).
        if (node instanceof Ast.Expression.Function fn) {
            return functionBindings.get(fn).returnType();
        }
        if (node instanceof Ast.Expression.Access acc) {
            return accessBindings.get(acc).type();
        }
        return typeBindings.get(node);
    }

    private static final class BindingMap<K, V> {
        private final Map<K, V> map = new HashMap<>();
        private final String nodeType;

        private BindingMap(String nodeType) {
            this.nodeType = Objects.requireNonNull(nodeType, "nodeType");
        }

        private void set(K node, V binding) {
            map.put(Objects.requireNonNull(node, "node"),
                Objects.requireNonNull(binding, "binding"));
        }

        private V get(K node) {
            V binding = map.get(node);
            if (binding == null) {
                throw new CompilerException("No " + nodeType + " binding for node: " + node);
            }
            return binding;
        }
    }

    private final BindingMap<Ast.Field, Environment.Variable> fieldBindings =
        new BindingMap<>("Ast.Field");

    private final BindingMap<Ast.Statement.Declaration, Environment.Variable> declarationBindings =
        new BindingMap<>("Ast.Statement.Declaration");

    private final BindingMap<Ast.Expression.Access, Environment.Variable> accessBindings =
        new BindingMap<>("Ast.Expression.Access");

    private final BindingMap<Ast.Method, Environment.Function> methodBindings =
        new BindingMap<>("Ast.Method");

    private final BindingMap<Ast.Expression.Function, Environment.Function> functionBindings =
        new BindingMap<>("Ast.Expression.Function");

    private final BindingMap<Ast.Expression, Environment.Type> typeBindings =
        new BindingMap<>("Ast.Expression");

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
        if (node instanceof Ast.Method m) return onMethod.apply(m);
        if (node instanceof Ast.Expression.Function f) return onFunction.apply(f);
        throw new CompilerException("Unhandled node type: " + node.getClass().getName());
    }
}