package freshlyground.compiler.backend.core;

public record FunctionCallLowering(Kind kind, String target) {
    public enum Kind {
        STATIC_CALL,   // e.g. function(...);
        VIRTUAL_CALL   // e.g. receiver.function(...);
    }

    public static FunctionCallLowering staticCall(String global) {
        return new FunctionCallLowering(Kind.STATIC_CALL, global);
    }

    public static FunctionCallLowering virtualCall(String memberFunction) {
        return new FunctionCallLowering(Kind.VIRTUAL_CALL, memberFunction);
    }
}
