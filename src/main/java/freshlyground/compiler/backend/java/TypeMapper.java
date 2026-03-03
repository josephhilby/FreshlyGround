package freshlyground.compiler.backend.java;

import freshlyground.common.CompilerException;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Types;

import java.util.Map;

public class TypeMapper {
    private static final Map<Environment.Type, String> TYPES = Map.of(
        Types.ANY, "Object",
        Types.NIL, "void",
        Types.STRING, "String",
        Types.BOOLEAN, "boolean",
        Types.INTEGER, "int",
        Types.DECIMAL, "double",
        Types.CHARACTER, "char"
    );

    public static String getJavaType(Environment.Type type) {
        String out = TYPES.get(type);
        if (out == null) throw new CompilerException("No Java mapping for type: " + type.getName());
        return out;
    }
}
