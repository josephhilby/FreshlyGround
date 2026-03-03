package freshlyground.compiler.semantic;

import java.util.List;

public final class StandardLibrary {
    private StandardLibrary() {}

    public static final class Symbols {
        private Symbols() {}
        public static Environment.Function PRINT;
        public static Environment.Function INPUT;
        public static Environment.Function ANY_STRINGIFY;
        public static Environment.Function STRING_LENGTH;
        public static Environment.Function STRING_SLICE;
    }

    // This guard exists so that tests don't reinstall type member functions between tests
    private static boolean TYPE_MEMBERS_INSTALLED = false;

    public static void install(Scope scope) {
        installGlobals(scope);

        if (!TYPE_MEMBERS_INSTALLED) {
            installTypeMembers();
            TYPE_MEMBERS_INSTALLED = true;
        }
    }

    private static void installGlobals(Scope scope) {
        Symbols.PRINT =
            scope.defineFunction("print", List.of(Types.ANY), Types.NIL);
        Symbols.INPUT =
            scope.defineFunction("input", List.of(), Types.STRING);
    }

    private static void installTypeMembers() {
        Scope any = Environment.lookupType("Any").getScope();
        Symbols.ANY_STRINGIFY =
            any.defineFunction("stringify", List.of(Types.ANY), Types.STRING);

        Scope string = Environment.lookupType("String").getScope();
        Symbols.STRING_LENGTH =
            string.defineFunction("length", List.of(Types.STRING), Types.INTEGER);
        Symbols.STRING_SLICE =
            string.defineFunction("slice", List.of(Types.STRING, Types.INTEGER, Types.INTEGER), Types.STRING);
    }
}
