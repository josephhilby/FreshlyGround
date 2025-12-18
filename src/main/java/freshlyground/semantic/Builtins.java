package freshlyground.semantic;

import java.util.Arrays;

public final class Builtins {
    private Builtins() {}

    private static boolean INSTALLED = false;

    public static void install(Scope scope) {
        installGlobalFunctions(scope);

        if (!INSTALLED) {
            installTypeMemberFunctions();
            INSTALLED = true;
        }
    }

    private static void installGlobalFunctions(Scope scope) {
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL);
    }

    private static void installTypeMemberFunctions() {
        Scope any = Environment.lookupType("Any").getScope();
        any.defineFunction("stringify", "toString", Arrays.asList(), Environment.Type.STRING);

        Scope comparable = Environment.lookupType("Comparable").getScope();
        comparable.defineFunction("compare", "compareTo", Arrays.asList(Environment.Type.ANY, Environment.Type.COMPARABLE), Environment.Type.COMPARABLE);

        Scope integer = Environment.lookupType("Integer").getScope();
        integer.defineFunction("compare", "compareTo", Arrays.asList(Environment.Type.ANY, Environment.Type.INTEGER), Environment.Type.INTEGER);

        Scope decimal = Environment.lookupType("Decimal").getScope();
        decimal.defineFunction("compare", "compareTo", Arrays.asList(Environment.Type.ANY, Environment.Type.DECIMAL), Environment.Type.DECIMAL);

        Scope character = Environment.lookupType("Character").getScope();
        character.defineFunction("compare", "compareTo", Arrays.asList(Environment.Type.ANY, Environment.Type.CHARACTER), Environment.Type.CHARACTER);

        Scope string = Environment.lookupType("String").getScope();
        string.defineVariable("length", "length()", Environment.Type.INTEGER, false);
        string.defineFunction("slice", "substring", Arrays.asList(Environment.Type.ANY, Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING);
        string.defineFunction("compare", "compareTo", Arrays.asList(Environment.Type.ANY, Environment.Type.STRING), Environment.Type.STRING);
    }
}
