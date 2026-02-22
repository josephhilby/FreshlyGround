package freshlyground.compiler.semantic;

import java.util.Arrays;

public final class Builtins {
    private Builtins() {}

    private static boolean INSTALLED = false;

    public static void install(Scope scope) {
        installGlobals(scope);

        if (!INSTALLED) {
            installTypeMembers();
            INSTALLED = true;
        }
    }

    private static void installGlobals(Scope scope) {
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL);
    }

    private static void installTypeMembers() {
        // TODO: change from type Any to type Primitive (changing Comparable to Primitive)
        Scope any = Environment.lookupType("Any").getScope();
        any.defineFunction("stringify", "toString", Arrays.asList(), Environment.Type.STRING);

        Scope string = Environment.lookupType("String").getScope();
        string.defineVariable("length", "length()", Environment.Type.INTEGER, false);
        string.defineFunction("slice", "substring", Arrays.asList(Environment.Type.ANY, Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING);
    }
}
