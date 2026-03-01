package freshlyground.compiler.semantic;

public record Lowering(Kind kind, String target) {
    public enum Kind {
        STATIC_CALL,   // e.g. function(...);
        VIRTUAL_CALL  // e.g. receiver.function(...);
    }

    public static Lowering staticCall(String global) {
        return new Lowering(Kind.STATIC_CALL, global);
    }

    public static Lowering virtualCall(String memberFunction) {
        return new Lowering(Kind.VIRTUAL_CALL, memberFunction);
    }
}
