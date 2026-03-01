package freshlyground.compiler.backend.java;

public record Lowering(Kind kind, String target) {
    public enum Kind {
        STATIC_CALL,   // e.g. System.out.println(...)
        VIRTUAL_CALL  // e.g. receiver.toString(...)
    }

    public static Lowering staticCall(String global) {
        return new Lowering(Kind.STATIC_CALL, global);
    }

    public static Lowering virtualCall(String memberFunction) {
        return new Lowering(Kind.VIRTUAL_CALL, memberFunction);
    }
}
