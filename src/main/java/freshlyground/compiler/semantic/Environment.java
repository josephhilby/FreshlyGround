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

    private static void registerType(Type type) {
        if (TYPES.containsKey(type.getName())) {
            throw new IllegalArgumentException("Duplicate registration of type " + type.getName() + ".");
        }

        TYPES.put(type.getName(), type);
    }

    static {
        registerType(Type.ANY);
        registerType(Type.NIL);
        registerType(Type.COMPARABLE);
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
     * supertype, forming a simple type hierarchy (e.g., {@code Decimal → Comparable → Any}).
     * This scope chain is used to model inherited type-level operations and built-in
     * functions via lexical lookup.</p>
     *
     * <p>The type hierarchy is intentionally shallow and closed: all types are singletons
     * defined by the runtime, and subtyping relationships are fixed at initialization time.</p>
     */
    public static final class Type {
        public static final Type ANY        = new Type("Any", "Object", new Scope(null));
        public static final Type NIL        = new Type("Nil", "Void", new Scope(ANY.scope));
        public static final Type COMPARABLE = new Type("Comparable", "Comparable", new Scope(ANY.scope));
        public static final Type BOOLEAN    = new Type("Boolean", "boolean", new Scope(ANY.scope));
        public static final Type INTEGER    = new Type("Integer", "int", new Scope(COMPARABLE.scope));
        public static final Type DECIMAL    = new Type("Decimal", "double", new Scope(COMPARABLE.scope));
        public static final Type CHARACTER  = new Type("Character", "char", new Scope(COMPARABLE.scope));
        public static final Type STRING     = new Type("String", "String", new Scope(COMPARABLE.scope));

        private final String name;
        private final String jvmName;
        private final Scope scope;

        public Type(String name, String jvmName, Scope scope) {
            this.name = name;
            this.jvmName = jvmName;
            this.scope = scope;
        }

        public String getName() { return name; }
        public String getJvmName() { return jvmName; }
        public Scope getScope() { return this.scope; }

        /**
         * Looks up a member function by name and explicit arity.
         *
         * <p>Member functions are stored in the type scope with an implicit receiver
         * parameter as argument 0 (i.e., {@code this}). Therefore, this method adds 1
         * to the explicit arity when performing lookup.</p>
         *
         * @param name the member function name
         * @param arity the number of explicit arguments at the call site
         */
        public Function lookupFunction(String name, int arity) { return scope.lookupFunction(name, arity + 1); }
        public Variable lookupVariable(String name) { return scope.lookupVariable(name); }

        @Override
        public String toString() {
            return "Type{" +
                    "name='" + name + '\'' +
                    ", scope='" + scope + '\'' +
                    '}';
        }
    }

    public static final class Function {
        private final String name;
        private final String jvmName;
        private final List<Type> parameterTypes;
        private final Type returnType;

        public Function(String name, String jvmName, List<Type> parameterTypes, Type returnType) {
            this.name = name;
            this.jvmName = jvmName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }

        public String getName() { return name; }
        public String getJvmName() { return jvmName; }
        public List<Type> getParameterTypes() { return parameterTypes; }
        public Type getType() { return returnType; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Function other)) return false;

            return Objects.equals(name, other.name) &&
                Objects.equals(jvmName, other.jvmName) &&
                Objects.equals(parameterTypes, other.parameterTypes) &&
                Objects.equals(returnType, other.returnType);
        }

        @Override
        public String toString() {
            return "Function{" +
                "name='" + name + '\'' +
                ", arity=" + parameterTypes.size() +
                ", parameterTypes=" + parameterTypes +
                ", returnType=" + returnType +
                '}';
        }
    }

    public static final class Variable {
        private final String name;
        private final String jvmName;
        private final Type type;
        private final boolean constant;

        public Variable(String name, String jvmName, Type type, boolean constant) {
            this.name = name;
            this.jvmName = jvmName;
            this.type = type;
            this.constant = constant;
        }

        public String getName() { return name; }
        public String getJvmName() { return jvmName; }
        public Type getType() { return type; }
        public boolean getConstant() { return constant; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Variable other)) return false;

            return Objects.equals(name, other.name) &&
                Objects.equals(jvmName, other.jvmName) &&
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
