package freshlyground.compiler.semantic;

import java.util.Arrays;
import java.util.List;

public final class Builtins {
    private Builtins() {}

    public static final class Symbols {
        private Symbols() {}
        public static Environment.Function PRINT;
        public static Environment.Function INPUT;
        public static Environment.Function ANY_STRINGIFY;
        public static Environment.Function STRING_LENGTH;
        public static Environment.Function STRING_SLICE;

        static void reset() {
            PRINT         = null;
            INPUT         = null;
            ANY_STRINGIFY = null;
            STRING_LENGTH = null;
            STRING_SLICE  = null;
        }
    }

    private static boolean INSTALLED = false;

    public static void install(Scope scope) {
        installGlobals(scope);

        if (!INSTALLED) {
            installTypeMembers();
            INSTALLED = true;
        }
    }

    private static void installGlobals(Scope scope) {
        Symbols.PRINT =
            scope.defineFunction("print", List.of(Environment.Type.ANY), Environment.Type.NIL);
        Symbols.INPUT =
            scope.defineFunction("input", List.of(), Environment.Type.STRING);
    }

    private static void installTypeMembers() {
        Scope any = Environment.lookupType("Any").getScope();
        Symbols.ANY_STRINGIFY =
            any.defineFunction("stringify", List.of(Environment.Type.ANY), Environment.Type.STRING);

        Scope string = Environment.lookupType("String").getScope();
        Symbols.STRING_LENGTH =
            string.defineFunction("length", List.of(Environment.Type.STRING), Environment.Type.INTEGER);
        Symbols.STRING_SLICE =
            string.defineFunction("slice", List.of(Environment.Type.STRING, Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING);
    }
}
