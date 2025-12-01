/*
<==== Test Case Scope ====>

The source or "input" for the example:

DEF main() DO
    LET x = 1;
    LET y = 2;
    print(x);
    print(y);
    IF TRUE DO
        LET x = 3;
        y = 4;
        print(x);
        print(y);
    END
    print(x);
    print(y);
END

*/

package plc.project;

public class ScopeTestData {
    ScopeTestData() {}

    // expect {1, 2, 3, 4, 1, 4}
    public static final String input1 = "DEF main() DO\n" +
        "    LET x = 1;\n" +
        "    LET y = 2;\n" +
        "    print(x);\n" +
        "    print(y);\n" +
        "    IF TRUE DO\n" +
        "        LET x = 3;\n" +
        "        y = 4;\n" +
        "        print(x);\n" +
        "        print(y);\n" +
        "    END\n" +
        "    print(x);\n" +
        "    print(y);\n" +
        "END";

    // expect 8
    public static final String input2 = "LET x: Integer = 1;\n" +
        "LET y: Integer = 2;\n" +
        "LET z: Integer = 3;\n" +
        "DEF f(z: Integer) DO\n" +
        "    RETURN x + y + z;\n" +
        "END\n" +
        "DEF main() DO\n" +
        "    LET y = 4;\n" +
        "    RETURN f(5);\n" +
        "END";
}
