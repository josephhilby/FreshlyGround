package freshlyground.compiler.semantic;

public final class Types {
    private Types() {}

    public static final Environment.Type ANY;
    public static final Environment.Type PRIMITIVE;
    public static final Environment.Type NIL;
    public static final Environment.Type STRING;
    public static final Environment.Type BOOLEAN;
    public static final Environment.Type INTEGER;
    public static final Environment.Type DECIMAL;
    public static final Environment.Type CHARACTER;

    static {
        ANY       = new Environment.Type("Any", false, new Scope(null));
        PRIMITIVE = new Environment.Type("Primitive", true, new Scope(ANY.getScope()));

        NIL       = new Environment.Type("Nil", false, new Scope(ANY.getScope()));
        STRING    = new Environment.Type("String", false, new Scope(ANY.getScope()));

        BOOLEAN   = new Environment.Type("Boolean", false, new Scope(PRIMITIVE.getScope()));
        INTEGER   = new Environment.Type("Integer", false, new Scope(PRIMITIVE.getScope()));
        DECIMAL   = new Environment.Type("Decimal", false, new Scope(PRIMITIVE.getScope()));
        CHARACTER = new Environment.Type("Character", false, new Scope(PRIMITIVE.getScope()));

        Environment.registerTypes(
            ANY, PRIMITIVE, NIL, STRING, BOOLEAN, INTEGER, DECIMAL, CHARACTER
        );
    }
}