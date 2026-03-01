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
        Scope any = Environment.lookupType("Any").getScope();
        any.defineFunction("stringify", "toString", Arrays.asList(Environment.Type.ANY), Environment.Type.STRING);

        Scope string = Environment.lookupType("String").getScope();
        string.defineFunction("length", "length()", Arrays.asList(Environment.Type.STRING), Environment.Type.INTEGER);
        string.defineFunction("slice", "substring", Arrays.asList(Environment.Type.STRING, Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING);
    }
}
