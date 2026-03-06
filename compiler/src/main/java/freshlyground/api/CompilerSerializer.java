package freshlyground.api;

import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.frontend.artifacts.Ast;

import java.util.*;

public final class CompilerSerializer {
    private CompilerSerializer() {}

    /* ---------------- Tokens ---------------- */

    public static Map<String, Object> serializeToken(Token token) {
        return Map.of(
            "type", token.type().toString(),
            "literal", token.literal(),
            "index", token.index()
        );
    }

    public static List<Map<String, Object>> serializeTokens(List<Token> tokens) {
        List<Map<String, Object>> out = new ArrayList<>(tokens.size());
        for (Token token : tokens) {
            out.add(serializeToken(token));
        }
        return out;
    }

    /* ---------------- AST ---------------- */

    public static Map<String, Object> serializeAst(Ast node) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("node", node.getClass().getSimpleName());

        Object[] values = node.components();
        String[] names = node.componentNames();

        for (int i = 0; i < names.length; i++) {
            out.put(names[i], serializeValue(values[i]));
        }

        return out;
    }

    private static Object serializeValue(Object value) {
        if (value == null) {
            return null;

        } else if (value instanceof Ast ast) {
            return serializeAst(ast);

        } else if (value instanceof Optional<?> opt) {
            return opt.map(CompilerSerializer::serializeValue).orElse(null);

        } else if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object v : list) {
                out.add(serializeValue(v));
            }
            return out;

        } else {
            return value;
        }
    }
}
