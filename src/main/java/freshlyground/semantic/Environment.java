package freshlyground.semantic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Environment {

    private static final Map<String, Type> TYPES = new HashMap<>();

    public static Type getType(String name) {
        if (!TYPES.containsKey(name)) {
            throw new RuntimeException("Unknown type " + name + ".");
        }
        return TYPES.get(name);
    }

    public static void registerType(Type type) {
        if (TYPES.containsKey(type.getName())) {
            throw new IllegalArgumentException("Duplicate registration of type " + type.getName() + ".");
        }
        TYPES.put(type.getName(), type);
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
        public static final Type ANY = new Type("Any", "Object", new Scope(null));
        public static final Type NIL = new Type("Nil", "Void", new Scope(ANY.scope));
        public static final Type COMPARABLE = new Type("Comparable", "Comparable", new Scope(ANY.scope));
        public static final Type BOOLEAN = new Type("Boolean", "boolean", new Scope(ANY.scope));
        public static final Type INTEGER = new Type("Integer", "int", new Scope(COMPARABLE.scope));
        public static final Type DECIMAL = new Type("Decimal", "double", new Scope(COMPARABLE.scope));
        public static final Type CHARACTER = new Type("Character", "char", new Scope(COMPARABLE.scope));
        public static final Type STRING = new Type("String", "String", new Scope(COMPARABLE.scope));

        private final String name;
        private final String jvmName;
        private final Scope scope;

        public Type(String name, String jvmName, Scope scope) {
            this.name = name;
            this.jvmName = jvmName;
            this.scope = scope;
        }

        public String getName() {
            return name;
        }

        public String getJvmName() {
            return jvmName;
        }

        public Scope getScope() {
            return this.scope;
        }

        public Variable getField(String name) {
            return scope.lookupVariable(name);
        }

        public Function getFunction(String name, int arity) {
            return scope.lookupFunction(name, arity + 1);
        }

        @Override
        public String toString() {
            return "Type{" +
                    "name='" + name + '\'' +
                    ", jvmName='" + jvmName + '\'' +
                    ", scope='" + scope + '\'' +
                    '}';
        }

    }

    /**
     * A thing which is Typed implements this class.
     *
     * Declares methods for getting the type of the thing.
     */
    public interface Typed<T> {
        T getType();
    }

    /**
     * A thing which is Named implements this interface.
     *
     * Declares methods for getting the name and jvmName for the thing.
     */
    public interface Named {
        String getName();
        String getJvmName();
    }

    public static final class Variable implements Named, Typed<Type> {

        private final String name;
        private final String jvmName;
        private final boolean constant;
        private final Type type;

        public Variable(String name, String jvmName, Type type, boolean constant) {
            this.name = name;
            this.jvmName = jvmName;
            this.type = type;
            this.constant = constant;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getJvmName() {
            return jvmName;
        }
        
        public boolean getConstant() {
            return constant;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Variable &&
                    name.equals(((Variable) obj).name) &&
                    jvmName.equals(((Variable) obj).jvmName) &&
                    constant == ((Variable) obj).constant &&
                    type.equals(((Variable) obj).type);
        }

        @Override
        public String toString() {
            return "Variable{" +
                    "name='" + name + '\'' +
                    ", jvmName'" + jvmName + '\'' +
                    ", type=" + type +
                    ", constant=" + constant +
//                    ", value=" + value +
                    '}';
        }
    }

    public static final class Function implements Named, Typed<Type> {

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

        public String getName() {
            return name;
        }

        public String getJvmName() {
            return jvmName;
        }

        public List<Type> getParameterTypes() {
            return parameterTypes;
        }

        public Type getReturnType() {
            return returnType;
        }

        public Type getType() {
            return returnType;
        }


        @Override
        public boolean equals(Object obj) {
            return obj instanceof Function &&
                name.equals(((Function) obj).name) &&
                jvmName.equals(((Function) obj).jvmName) &&
                parameterTypes.equals(((Function) obj).parameterTypes) &&
                returnType.equals(((Function) obj).returnType);
        }

        @Override
        public String toString() {
            return "Function{" +
                "name='" + name + '\'' +
                ", jvmName='" + jvmName + '\'' +
                ", arity=" + parameterTypes.size() +
                ", parameterTypes=" + parameterTypes +
                ", returnType=" + returnType +
                '}';
        }
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
        Type.ANY.scope.defineFunction("stringify", "toString", Arrays.asList(), Type.STRING);
        Type.COMPARABLE.scope.defineFunction("compare", "compareTo", Arrays.asList(Type.ANY, Type.COMPARABLE), Type.COMPARABLE);
        Type.INTEGER.scope.defineFunction("compare", "compareTo", Arrays.asList(Type.ANY, Type.INTEGER), Type.INTEGER);
        Type.DECIMAL.scope.defineFunction("compare", "compareTo", Arrays.asList(Type.ANY, Type.DECIMAL), Type.DECIMAL);
        Type.CHARACTER.scope.defineFunction("compare", "compareTo", Arrays.asList(Type.ANY, Type.CHARACTER), Type.CHARACTER);
        Type.STRING.scope.defineVariable("length", "length()", Type.INTEGER, false);
        Type.STRING.scope.defineFunction("slice", "substring", Arrays.asList(Type.ANY, Type.INTEGER, Type.INTEGER), Type.STRING);
        Type.STRING.scope.defineFunction("compare", "compareTo", Arrays.asList(Type.ANY, Type.STRING), Type.STRING);
    }
}
