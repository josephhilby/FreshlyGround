package freshlyground.compiler.backend.java;

import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Builtins;
import freshlyground.compiler.semantic.Scope;

import java.util.Map;

public final class JavaBuiltinLowerings {
    private final Map<Environment.Function, Lowering> functions;

    public JavaBuiltinLowerings(Scope globalScope) {
        // globals
        Environment.Function print = globalScope.lookupFunction("print", 1);

        // members
        Scope any = Environment.lookupType("Any").getScope();
        Environment.Function stringify = any.lookupFunction("stringify", 1);

        Scope string = Environment.lookupType("String").getScope();
        Environment.Function length = string.lookupFunction("length", 1);
        Environment.Function slice  = string.lookupFunction("slice", 3);

        this.functions = Map.of(
            print,     Lowering.staticCall("System.out.println"),
            stringify, Lowering.virtualCall("toString"),
            length,    Lowering.virtualCall("length"),
            slice,     Lowering.virtualCall("substring")
        );
    }

    public Lowering loweringOf(Environment.Function function) {
        return functions.get(function);
    }
}
