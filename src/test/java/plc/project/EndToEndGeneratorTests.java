package plc.project;

public class EndToEndGeneratorTests {
    // Source
        // Hello World:
            // DEF main(): Integer DO
            //     print("Hello World");
            //     RETURN 0;
            // END
        // Multiple Fields and Methods:
            // LET x: Integer;
            // LET y: Decimal;
            // LET z: String;
            // DEF f(): Integer DO RETURN x; END
            // DEF g(): Decimal DO RETURN y; END
            // DEF h(): String DO RETURN z; END
            // DEF main(): Integer DO END

    // Field
        // Declaration: LET name: Integer;
        //Initialization: LET name: Decimal = 1.0;

    // Method
        // Square:
            // DEF square(num: Decimal): Decimal DO
            //     RETURN num * num;
            // END
            // Multiple Statements:
            // DEF func(x: Integer, y: Decimal, z: String) DO
            //     print(x);
            //     print(y);
            //     print(z);
            // END

    // Statement
        // Expression (1):
            // Print Expression: print("Hello World");
        // Declaration (2):
            // Variable Declaration: LET name: Integer;
            // Variable Initialization: LET name = 1.0;
        // Assignment (2):
            // Variable: variable = 1;
            // Field: object.field = 1;
        // If (2):
            // If:
                // IF cond DO
                //     print("cond is true.");
                // END
                // Else:
                // IF cond DO
                //     print("cond is true.");
                // ELSE
                //     print("cond is false.");
                // END
        // For (2):
            // For:
                // FOR (num = 0; num < 5; num = num + 1)
                //     sum = sum + num;
                // END
            //Condition Only:
                // FOR (; num < 5;)
                //     print(num);
                //     num = num + 1;
                // END
        //While (2):
            // Empty Statements: WHILE cond DO END
            // Multiple Statements:
                // WHILE num < 10 DO
                //     print(num + "\n");
                //     num = num + 1;
                // END

    // Expression
        // Literal (4):
            // Boolean: TRUE
            // Integer: 1
            // Decimal: 123.456
            // String: "Hello World"
        // Group (1):
            // Binary: (1 + 10)
            // Binary (4):
            // And: TRUE AND FALSE
            // Comparison: 1 > 10
            // Addition: 1 + 10
            // Concatenation: "Ben " + 10
        // Access (2):
            // Variable: variable
            // Field: object.field
        // Function (3):
            // Zero Arguments: function()
            // Print: print("Hello World")
            // String Slice: "string".slice(1, 5)
}
