package freshlyground.compiler.semantic;

import freshlyground.common.CompilerException;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the type singletons in the language's type system.
 *
 * <p>Each {@code Type} owns a {@link Scope} that is parented to the scope of its
 * supertype, forming a simple type hierarchy (e.g., {@code Decimal → Primitive → Any}).
 * This scope chain is used to model inherited type-level operations and built-in
 * functions via lexical lookup.</p>
 *
 * <p>The type hierarchy is intentionally shallow and closed: all types are singletons
 * defined by the runtime, and subtyping relationships are fixed at initialization time.</p>
 */
public final class Types {
    private Types() {}

    public static final Environment.Type ANY;
    public static final Environment.Type PRIMITIVE;
    public static final Environment.Type NIL;
    public static final Environment.Type STRING;
    public static final Environment.Type BOOLEAN;
    public static final Environment.Type INTEGER;
    public static final Environment.Type DECIMAL;
    public static final Environment.Type CHARACTER;

    private static final Map<String, Environment.Type> TYPES = new HashMap<>();
    public static void registerTypes(Environment.Type... types) {
        for (Environment.Type type : types) {
            if (TYPES.containsKey(type.getName())) {
                throw new IllegalArgumentException("Duplicate registration of type " + type.getName() + ".");
            }

            TYPES.put(type.getName(), type);
        }
    }
    public static Environment.Type lookupType(String name) {
        if (!TYPES.containsKey(name)) {
            throw new CompilerException("Unknown type: " + name + ".");
        }

        return TYPES.get(name);
    }
    public static boolean isAssignable(Environment.Type target, Environment.Type actual) {
        if (target == ANY || target == actual) {
            return true;
        }

        if (target == PRIMITIVE) {
            return actual == INTEGER
                || actual == DECIMAL
                || actual == CHARACTER
                || actual == BOOLEAN;
        }

        return false;
    }

    static {
        ANY       = new Environment.Type("Any", false, new Scope(null));
        PRIMITIVE = new Environment.Type("Primitive", true, new Scope(ANY.getScope()));

        NIL       = new Environment.Type("Nil", false, new Scope(ANY.getScope()));
        STRING    = new Environment.Type("String", false, new Scope(ANY.getScope()));

        BOOLEAN   = new Environment.Type("Boolean", false, new Scope(PRIMITIVE.getScope()));
        INTEGER   = new Environment.Type("Integer", false, new Scope(PRIMITIVE.getScope()));
        DECIMAL   = new Environment.Type("Decimal", false, new Scope(PRIMITIVE.getScope()));
        CHARACTER = new Environment.Type("Character", false, new Scope(PRIMITIVE.getScope()));

        registerTypes(
            ANY, PRIMITIVE, NIL, STRING, BOOLEAN, INTEGER, DECIMAL, CHARACTER
        );
    }
}