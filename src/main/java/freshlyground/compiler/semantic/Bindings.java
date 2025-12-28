package freshlyground.compiler.semantic;

import freshlyground.compiler.frontend.Ast;

import java.util.HashMap;
import java.util.Map;

public final class Bindings {
    // Variable Bindings
    private final Map<Ast.Field, Environment.Variable> fieldBindings = new HashMap<>();
    private final Map<Ast.Statement.Declaration, Environment.Variable> declarationBindings = new HashMap<>();
    private final Map<Ast.Expression.Access, Environment.Variable> accessBindings = new HashMap<>();

    public void setVariable(Ast.Field node, Environment.Variable variable) { fieldBindings.put(node, variable); }
    public Environment.Variable getVariable(Ast.Field node) { return fieldBindings.get(node); }
    public Environment.Type getType(Ast.Field node) { return declarationBindings.get(node).getType(); }

    public void setVariable(Ast.Statement.Declaration node, Environment.Variable variable) { declarationBindings.put(node, variable); }
    public Environment.Variable getVariable(Ast.Statement.Declaration node) { return declarationBindings.get(node); }
    public Environment.Type getType(Ast.Statement.Declaration node) { return declarationBindings.get(node).getType(); }

    public void setVariable(Ast.Expression.Access node, Environment.Variable variable) { accessBindings.put(node, variable); }
    public Environment.Variable getVariable(Ast.Expression.Access node) { return accessBindings.get(node); }

    // Function Bindings
    private final Map<Ast.Method, Environment.Function> methodBindings = new HashMap<>();
    private final Map<Ast.Expression.Function, Environment.Function> functionBindings = new HashMap<>();

    public void setMethod(Ast.Method method, Environment.Function function) { methodBindings.put(method, function); }
    public Environment.Function getMethod(Ast.Method method) { return methodBindings.get(method); }
    public Environment.Type getType(Ast.Method method) { return methodBindings.get(method).getType(); }

    public void setFunction(Ast.Expression.Function node, Environment.Function function) { functionBindings.put(node, function); }
    public Environment.Function getFunction(Ast.Expression.Function node) { return functionBindings.get(node); }

    // Type Bindings
    private final Map<Ast.Expression, Environment.Type> typeBindings = new HashMap<>();

    public void setType(Ast.Expression node, Environment.Type type) { typeBindings.put(node, type); }
    public Environment.Type getType(Ast.Expression node) {
        if (node instanceof Ast.Expression.Function) {
            return functionBindings.get(node).getType();
        }
        if (node instanceof Ast.Expression.Access) {
            return accessBindings.get(node).getType();
        }
        return typeBindings.get(node);
    }
}
