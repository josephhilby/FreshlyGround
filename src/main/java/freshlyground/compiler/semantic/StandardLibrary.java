package freshlyground.compiler.semantic;

import java.util.List;

/**
 * <p>{@code StandardLibrary} installs the built-in functions available to all
 * programs. Global functions are defined in the provided global {@link Scope},
 * while member functions are defined in the scopes owned by the singleton
 * types in {@link Types}.</p>
 *
 * <p>Installed member functions are recorded in {@code Symbols} so the analyzer and
 * tests can reference the exact {@code Environment.Function} instances.</p>
 */
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

    public static void install(Scope global) {
        installGlobals(global);

        if (!TYPE_MEMBERS_INSTALLED) {
            installTypeMembers();
            TYPE_MEMBERS_INSTALLED = true;
        }
    }

    private static void installGlobals(Scope global) {
        Symbols.PRINT =
            global.defineFunction("print", List.of(Types.ANY), Types.NIL);
        Symbols.INPUT =
            global.defineFunction("input", List.of(), Types.STRING);
    }

    private static void installTypeMembers() {
        Scope any = Types.lookupType("Any").getScope();
        Symbols.ANY_STRINGIFY =
            any.defineFunction("stringify", List.of(Types.ANY), Types.STRING);

        Scope string = Types.lookupType("String").getScope();
        Symbols.STRING_LENGTH =
            string.defineFunction("length", List.of(Types.STRING), Types.INTEGER);
        Symbols.STRING_SLICE =
            string.defineFunction("slice", List.of(Types.STRING, Types.INTEGER, Types.INTEGER), Types.STRING);
    }
}
