package freshlyground.compiler.backend.core;

import java.util.Objects;

public record TypeLowering(Kind kind, String representation) {

    public enum Kind {
        PRIMITIVE,   // scalar: int/double/boolean/char or wasm i32/f64
        REFERENCE,   // object/handle: Object/String or wasm i32/externref
        VOID         // no result (return position only)
    }

    public TypeLowering {
        kind = Objects.requireNonNull(kind, "kind");
        representation = Objects.requireNonNull(representation, "representation");
    }

    public boolean isVoid() { return kind == Kind.VOID; }

    public static TypeLowering primitive(String name) {
        return new TypeLowering(Kind.PRIMITIVE, name);
    }

    public static TypeLowering reference(String name) {
        return new TypeLowering(Kind.REFERENCE, name);
    }

    public static TypeLowering voidType() {
        return new TypeLowering(Kind.VOID, "void");
    }
}
