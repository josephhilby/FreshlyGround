package freshlyground.compiler.backend.java;

import freshlyground.common.CompilerException;
import freshlyground.compiler.semantic.Environment;

import java.util.Map;

public class TypeMapper {
    private static final Map<Environment.Type, String> TYPES = Map.of(
        Environment.Type.ANY, "Object",
        Environment.Type.NIL, "void",
        Environment.Type.STRING, "String",
        Environment.Type.BOOLEAN, "boolean",
        Environment.Type.INTEGER, "int",
        Environment.Type.DECIMAL, "double",
        Environment.Type.CHARACTER, "char"
    );

    public static String getJavaType(Environment.Type type) {
        String out = TYPES.get(type);
        if (out == null) throw new CompilerException("No Java mapping for type: " + type.getName());
        return out;
    }
}
