package freshlyground.compiler.backend.java;

import freshlyground.common.CompilerException;
import freshlyground.compiler.backend.core.TypeLowering;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Types;

import java.util.Map;

public final class JavaTypeLowering {
    private JavaTypeLowering() {}

    private static final Map<Environment.Type, TypeLowering> VALUE_TYPES = Map.of(
        Types.ANY,       TypeLowering.reference("Object"),
        Types.STRING,    TypeLowering.reference("String"),
        Types.BOOLEAN,   TypeLowering.primitive("boolean"),
        Types.INTEGER,   TypeLowering.primitive("int"),
        Types.DECIMAL,   TypeLowering.primitive("double"),
        Types.CHARACTER, TypeLowering.primitive("char")
    );

    // Forbids Nil
    public static String getJavaType(Environment.Type type) {
        if (type == Types.NIL) {
            throw new CompilerException("Nil only allowed for returns.");
        }
        TypeLowering lowering = VALUE_TYPES.get(type);
        if (lowering == null) {
            throw new CompilerException("No Java mapping for type: " + type.getName());
        }
        return lowering.representation();
    }

    // Allows Nil
    public static String getJavaReturnType(Environment.Type type) {
        if (type == Types.NIL) {
            return TypeLowering.voidType().representation();
        }
        return getJavaType(type);
    }
}