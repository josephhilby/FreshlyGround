package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scope {
    private final Scope parent;
    public final Map<String, Environment.Function> functions = new HashMap<>();
    public final Map<String, Environment.Variable> variables = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public Scope getParent() { return parent; }

    public Environment.Function defineFunction(String name, String jvmName, List<Environment.Type> parameterTypes, Environment.Type returnType) {
        if (functions.containsKey(name + "/" + parameterTypes.size())) {
            throw new CompilerException("The function " + name + "/" + parameterTypes.size() + " is already defined in this scope.");

        } else {
            Environment.Function func = new Environment.Function(name, jvmName, parameterTypes, returnType);
            functions.put(func.getName() + "/" + func.getParameterTypes().size(), func);
            return func;
        }
    }

    public Environment.Function lookupFunction(String name, int arity) {
        if (functions.containsKey(name + "/" + arity)) {
            return functions.get(name + "/" + arity);

        } else if (parent != null) {
            return parent.lookupFunction(name, arity);

        } else {
            throw new CompilerException("The function " + name + "/" + arity + " is not defined in this scope.");
        }
    }

    public Environment.Variable defineVariable(String name, String jvmName, Environment.Type type, boolean constant) {
        if (variables.containsKey(name)) {
            throw new CompilerException("The variable " + name + " is already defined in this scope.");

        } else {
            Environment.Variable variable = new Environment.Variable(name, jvmName, type, constant);
            variables.put(variable.getName(), variable);
            return variables.get(name);
        }
    }

    public Environment.Variable lookupVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);

        } else if (parent != null) {
            return parent.lookupVariable(name);

        } else {
            throw new CompilerException("The variable " + name + " is not defined in this scope.");
        }
    }

    @Override
    public String toString() {
        return "Scope{" +
            "parent=" + parent +
            ", variables=" + variables.keySet() +
            ", functions=" + functions.keySet() +
            '}';
    }
}
