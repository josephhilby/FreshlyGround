package plc.project;

public class EndToEndInterpreterTests {

    // log
    // StringBuilder builder = new StringBuilder();
    //        scope.defineFunction("log", 1, args -> {
    //            builder.append(args.get(0).getValue());
    //            return args.get(0);
    //        });

    // Source
        // Field Addition: LET x: Integer = 1; LET y: Integer = 10; DEF main(): Integer DO RETURN x + y; END
        // Source Invoke Main (predefined): <empty>, scope = {main/0 = ...} (returns 0)

    // Field
        // Declaration: LET name: Integer;
            // scope = {name = NIL}
        // Initialization: VAL name: Integer = 1;
            // scope = {name = 1}

    // Method
        // Main: DEF main(): Integer DO RETURN 0; END
        // One Parameter: DEF square(x: Integer): Integer DO RETURN x * x; END

    // Statement
        // Expression (1):
            // Log: log(1);
                // scope = {log/1 = ...}
        // Declaration (2):
            // Declaration: LET name;
                // scope = {name = NIL}
            // Initialization: LET name = 1;
                // scope = {name = 1}
        // Assignment (2):
            // Variable: variable = 1;, scope = {variable = NIL}
                // scope = {variable = 1}
            // List: object.field = 1;, scope = {object = PlcObject{field = NIL}}
                // scope = {object = PlcObject{field = 1}}
        // If (2):
            // True Condition: IF TRUE DO num = 1; END, scope = {num = NIL}
                // scope = {num = 1}
            // False Condition: IF FALSE DO ELSE num = 10; END, scope = {num = NIL}
                // scope = {num = 10}
        // For (1):
            // For: FOR (num = 0; num < 5; num = num + 1) sum = sum + num; END, scope = {sum = 0, num = NIL}
                // scope = {sum = 10, num = 5}
        // While (1):
            // While: WHILE num < 10 DO num = num + 1; END, scope = {num = 0}
                // scope = {num = 10}

    // Expression
        // Literal (3):
            // Nil: NIL
            // Integer: 1
            // String: "string"
            // Boolean: TRUE
        // Group (2):
            // Literal: (1)
            // Binary: (1 + 10)
        // Binary (7):
            // And: TRUE AND FALSE
            // Or (Short Circuit): TRUE OR undefined
            // Less Than: 1 < 10
            // Equal: 1 == 10
            // Concatenation: "a" + "b"
            // Addition: 1 + 10
            // Division: 1.2 / 3.4
        // Access (2):
            // Variable: variable, scope = {variable = 1}
            // Field: object.field, scope = {object = PlcObject{field = 1}}
        // Function (2):
            // Function: function(), scope = {function/0 = ... (returns 1)
            // Log: log(1), scope = {log/1 = ...} (returns the argument, 1)

    // Scope
        // If Scope:
            // DEF main(): Integer DO
            //     LET x = 1;
            //     LET y = 2;
            //     log(x);
            //     log(y);
            //     IF TRUE DO
            //         LET x = 3;
            //         y = 4;
            //         log(x);
            //         log(y);
            //     END
            //     log(x);
            //     log(y);
            // END
        // Function Scope:
            // LET x: Integer = 1;
            // LET y: Integer = 2;
            // LET z: Integer = 3;
            // DEF f(z: Integer): Integer DO
            //     RETURN x + y + z;
            // END
            // DEF main(): Integer DO
            //     LET y = 4;
            //     RETURN f(5);
            // END

    // Error
        // Integer Decimal Subtraction: 1 - 1.0
        // While w/ String: WHILE "false" DO END
        // Redefined Field: LET name: Integer; LET name: Integer = 1;
}
