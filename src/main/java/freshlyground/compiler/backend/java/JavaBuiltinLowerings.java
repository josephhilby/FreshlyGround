package freshlyground.compiler.backend.java;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Ast;
import freshlyground.compiler.semantic.Builtins.Symbols;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.backend.shared.Lowering;

import java.util.List;
import java.util.Map;

public final class JavaBuiltinLowerings {
    private final Map<Environment.Function, Lowering> functions = Map.of(
        Symbols.PRINT,        Lowering.staticCall("System.out.println"),
        Symbols.ANY_STRINGIFY,Lowering.virtualCall("toString"),
        Symbols.STRING_LENGTH,Lowering.virtualCall("length"),
        Symbols.STRING_SLICE, Lowering.virtualCall("substring")
    );

    public Lowering lowerBuiltin(Environment.Function function) {
        return functions.get(function);
    }

    public void emitCall(Generator.JavaPrint print, Ast.Expression.Function ast, Lowering low) {
        switch (low.kind()) {
            case STATIC_CALL -> {
                print.out(low.target(), "(");
                emitArgs(print, ast.getArguments());
                print.out(")");
            }

            case VIRTUAL_CALL -> {
                // Prefer explicit receiver from AST
                if (ast.getReceiver().isPresent()) {
                    print.out(ast.getReceiver().get(), ".", low.target(), "(");
                    emitArgs(print, ast.getArguments());
                    print.out(")");
                    return;
                }

                // Fallback if receiver was injected into args[0]
                List<Ast.Expression> args = ast.getArguments();
                if (args.isEmpty()) {
                    throw new CompilerException("Missing receiver for virtual builtin: " + low.target());
                }

                print.out(args.get(0), ".", low.target(), "(");
                for (int i = 1; i < args.size(); i++) {
                    if (i > 1) print.out(", ");
                    print.out(args.get(i));
                }
                print.out(")");
            }
        }
    }

    private static void emitArgs(Generator.JavaPrint print, List<Ast.Expression> args) {
        for (int i = 0; i < args.size(); i++) {
            print.out(args.get(i));
            if (i < args.size() - 1) {
                print.out(", ");
            }
        }
    }
}
