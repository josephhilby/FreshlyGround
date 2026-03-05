package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;

import java.util.List;

/**
 * <p>{@code Environment} defines the semantic model objects used during analysis and code generation
 * (nominal {@code Type}s, {@code Variable}s, and {@code Function}s) and provides shared semantic
 * validation helpers (e.g., assignability/equality checks). It does not model runtime values or execution.</p>
 *
 * <p>The language's closed set of type singletons and type-system operations are defined by {@link Types}.</p>
 *
 * <p>Built-in bindings (global functions and type members) are installed separately by {@link StandardLibrary}.</p>
 */
public final class Environment {
    public static Type sourceLookupType(String name) {
        Type type = Types.lookupType(name);
        if (type.isInternalType()) {
            throw new CompilerException("Type: " + name + " is an internal classification for semantic resolution.");
        }
        return type;
    }

    public static boolean requireSame(Type target, Type actual) {
        if (actual.equals(target)) {
            return true;
        }
        throw new CompilerException("Types mismatch: must be type " + target.getName() + " but was " + actual.getName());
    }
    public static boolean requireNot(Type target, Type actual) {
        if (actual.equals(target)) {
            throw new CompilerException("Type must not be: " + target.getName());
        }
        return true;
    }
    public static boolean requireAssignables(Type target, Type... actuals) {
        for (Environment.Type actual : actuals) {
            requireAssignable(target, actual);
        }
        return true;
    }
    public static boolean requireAssignable(Type target, Type actual) {
        if (Types.isAssignable(target, actual)) {
            return true;
        }

        throw new CompilerException("Type unassignable: " + actual.getName() + " -> " + target.getName());
    }

    public static final class Type {
        private final String name;
        private final boolean internalType;
        private final Scope typeScope;

        public Type(String name, boolean internalType, Scope typeScope) {
            this.name = name;
            this.internalType = internalType;
            this.typeScope = typeScope;
        }

        public String getName() { return name; }
        public boolean isInternalType() { return internalType; }
        public Scope getScope() { return this.typeScope; }

        /**
         * Looks up a member function by name and explicit arity.
         *
         * <p>Member functions are stored in the type scope with an implicit receiver
         * parameter as argument 0 (e.g., calling {@code rec.func(arg)}, invokes {@code func(rec, arg)}).
         * Therefore, this method adds 1 to the explicit arity when performing lookup.</p>
         *
         * @param name the member function name
         * @param arity the number of explicit arguments at the call site
         */
        public Function lookupMemberFunction(String name, int arity) { return typeScope.lookupFunction(name, arity + 1); }
        public Variable lookupMemberVariable(String name) { return typeScope.lookupVariable(name); }

        @Override
        public String toString() {
            return "Type{" +
                    "name='" + name + '\'' +
                    ", scope='" + typeScope + '\'' +
                    '}';
        }
    }

    public record Function(String name, List<Type> parameterTypes, Type returnType) {
        public Function {
            parameterTypes = List.copyOf(parameterTypes);
        }

        public int arity() {
            return parameterTypes.size();
        }

        @Override
        public String toString() {
            return "Function{" +
                "name='" + name + '\'' +
                ", arity=" + arity() +
                ", parameterTypes=" + parameterTypes +
                ", returnType=" + returnType +
                '}';
        }
    }

    public record Variable(String name, Type type, boolean constant) {
        @Override
        public String toString() {
            return "Variable{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", constant=" + constant +
                '}';
        }
    }
}
