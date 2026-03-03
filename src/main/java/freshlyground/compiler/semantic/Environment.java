package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>{@code Environment} defines the data structures used during semantic analysis
 * and code generation (types, variables, functions). It does not model runtime values
 * or execution.</p>
 *
 * <p>Built-in bindings (global functions and type members) are installed separately
 * by {@link Builtins}.</p>
 */
public final class Environment {

    /**
     * Closed set of nominal types supported by the compiler.
     */
    private static final Map<String, Type> TYPES = new HashMap<>();

    public static Type lookupType(String name) {
        if (!TYPES.containsKey(name)) {
            throw new CompilerException("Unknown type: " + name + ".");
        }

        return TYPES.get(name);
    }
    public static Type sourceLookupType(String name) {
        Type type = lookupType(name);
        if (type.internalType) {
            throw new CompilerException("Type: " + name + "is an internal classification for semantic resolution.");
        }
        return type;
    }
    private static void registerType(Type type) {
        if (TYPES.containsKey(type.getName())) {
            throw new IllegalArgumentException("Duplicate registration of type " + type.getName() + ".");
        }

        TYPES.put(type.getName(), type);
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
        if (
            target.equals(Type.ANY) || target.equals(actual) ||
           (target.equals(Type.PRIMITIVE) && actual.equals(Type.INTEGER)) ||
           (target.equals(Type.PRIMITIVE) && actual.equals(Type.DECIMAL)) ||
           (target.equals(Type.PRIMITIVE) && actual.equals(Type.CHARACTER)) ||
           (target.equals(Type.PRIMITIVE) && actual.equals(Type.BOOLEAN))
        ) {
            return true;
        }
        throw new CompilerException("Type unassignable: " + actual.getName() + " -> " + target.getName());
    }

    static {
        registerType(Type.ANY);
        registerType(Type.NIL);
        registerType(Type.PRIMITIVE);
        registerType(Type.BOOLEAN);
        registerType(Type.INTEGER);
        registerType(Type.DECIMAL);
        registerType(Type.CHARACTER);
        registerType(Type.STRING);
    }

    /**
     * Represents a nominal type in the language's type system.
     *
     * <p>Each {@code Type} owns a {@link Scope} that is parented to the scope of its
     * supertype, forming a simple type hierarchy (e.g., {@code Decimal → Primitive → Any}).
     * This scope chain is used to model inherited type-level operations and built-in
     * functions via lexical lookup.</p>
     *
     * <p>The type hierarchy is intentionally shallow and closed: all types are singletons
     * defined by the runtime, and subtyping relationships are fixed at initialization time.</p>
     */
    public static final class Type {
        public static final Type ANY       = new Type("Any", false, new Scope(null));
        public static final Type NIL       = new Type("Nil", false, new Scope(ANY.typeScope));
        public static final Type STRING    = new Type("String", false, new Scope(ANY.typeScope));
        public static final Type PRIMITIVE = new Type("Primitive", true, new Scope(ANY.typeScope));
        public static final Type BOOLEAN   = new Type("Boolean", false, new Scope(PRIMITIVE.typeScope));
        public static final Type INTEGER   = new Type("Integer", false, new Scope(PRIMITIVE.typeScope));
        public static final Type DECIMAL   = new Type("Decimal", false, new Scope(PRIMITIVE.typeScope));
        public static final Type CHARACTER = new Type("Character", false, new Scope(PRIMITIVE.typeScope));

        private final String name;
        private final boolean internalType;
        private final Scope typeScope;

        public Type(String name, boolean internalType, Scope typeScope) {
            this.name = name;
            this.internalType = internalType;
            this.typeScope = typeScope;
        }

        public String getName() { return name; }
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

    public record Function(
        String name,
        List<Type> parameterTypes,
        Type returnType
    ) {
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

    public static final class Variable {
        private final String name;
        private final Type type;
        private final boolean constant;

        public Variable(String name, Type type, boolean constant) {
            this.name = name;
            this.type = type;
            this.constant = constant;
        }

        public String getName() { return name; }
        public Type getType() { return type; }
        public boolean getConstant() { return constant; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Variable other)) return false;

            return Objects.equals(name, other.name) &&
                Objects.equals(constant, other.constant) &&
                Objects.equals(type, other.type);
        }

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
