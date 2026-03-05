package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scope {
    private final Optional<Scope> parent;
    public final Map<String, Environment.Function> functions = new HashMap<>();
    public final Map<String, Environment.Variable> variables = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = Optional.ofNullable(parent);
    }

    public Scope getParent() { return parent.get(); }

    public Environment.Function defineFunction(String name, List<Environment.Type> parameterTypes, Environment.Type returnType) {
        if (functions.containsKey(name + "/" + parameterTypes.size())) {
            throw new CompilerException("The function " + name + "/" + parameterTypes.size() + " is already defined in this scope.");
        }

        Environment.Function func = new Environment.Function(name, parameterTypes, returnType);
        functions.put(func.name() + "/" + func.parameterTypes().size(), func);
        return func;
    }
    public Environment.Function lookupFunction(String name, int arity) {
        if (functions.containsKey(name + "/" + arity)) {
            return functions.get(name + "/" + arity);
        }
        if (parent.isPresent()) {
            return getParent().lookupFunction(name, arity);
        }

        throw new CompilerException("The function " + name + "/" + arity + " is not defined in this scope.");
    }

    public Environment.Variable defineVariable(String name, Environment.Type type, boolean constant) {
        if (variables.containsKey(name)) {
            throw new CompilerException("The variable " + name + " is already defined in this scope.");
        }

        Environment.Variable variable = new Environment.Variable(name, type, constant);
        variables.put(variable.name(), variable);
        return variables.get(name);
    }
    public Environment.Variable lookupVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent.isPresent()) {
            return getParent().lookupVariable(name);
        }

        throw new CompilerException("The variable " + name + " is not defined in this scope.");
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
