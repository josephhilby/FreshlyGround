package plc.project;

import java.io.PrintWriter;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create a "class Main {"
        print("public class Main {");
        newline(indent);

        //     declare fields -> properties
        newline(++indent);
//        for (Ast.Field field : ast.getFields()) {
//            print(field);
//            newline(indent);
//        }

        //     declare "public static void main(String[] args) {
        print("public static void main(String[] args) {");

        //         System.exit(new Main().main());
        newline(++indent);
        print("System.exit(new Main().main());");

        //     }"
        newline(--indent);
        print("}");

        //     declare functions -> methods
        //     one of our functions -> methods is called main()
        newline(--indent);
        newline(++indent);
        for (Ast.Method method : ast.getMethods()) {
            print(method);
        }

        // print "}" to close the "class Main {"
        newline(--indent);
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // int main() {
    //     System.out.println(\"Hello, World!\");
    //     return 0;
    // }
    @Override
    public Void visit(Ast.Method ast) {
        if (ast.getReturnTypeName().isPresent()) {
            print(getJavaType(ast.getReturnTypeName().get()), " ");
        }

        print(ast.getName(), "(");
        // TODO list params
        print(") {");
        // TODO list statements (done multiple times factor into helper)
        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent);
                print(statement);
            }
            newline(--indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ") {");

        newline(++indent);
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            if (i != 0) {
                newline(++indent);
            }
            print(ast.getThenStatements().get(i));
        }
        newline(--indent);

        if (!ast.getElseStatements().isEmpty()) {
            print("} else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                if (i != 0) {
                    newline(++indent);
                }
                print(ast.getElseStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        Object init = (ast.getInitialization() != null) ? ast.getInitialization() : "";
        Object incr = (ast.getIncrement() != null) ? ast.getIncrement() : "";
        String trail = (incr != "") ? " " : "";
        print("for ( ", init, "; ", ast.getCondition(), "; ", incr, trail, ") {");

        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ")");

        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(++indent);
                }
                print(ast.getStatements().get(i), ";");
            }
            newline(--indent);
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType() == Environment.Type.INTEGER) {
            print(ast.getLiteral());
        } else if (ast.getType() == Environment.Type.STRING) {
            print("\"", ast.getLiteral(), "\"");
        } else if (ast.getType() == Environment.Type.BOOLEAN) {
            print(ast.getLiteral());
        } else if (ast.getType() == Environment.Type.DECIMAL) {
            print(ast.getLiteral());
        } else {
            throw new RuntimeException("Unknown Type: " + ast.getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator;
        // TODO this is a bad way to handle this, also test for recursive/nested bin
        switch (ast.getOperator()) {
            case "AND":
                operator = " && ";
                break;
            case "+":
                operator = " + ";
                break;
            case "<":
                operator = " < ";
                break;
            case "*":
                operator = " * ";
                break;
            default:
                throw new RuntimeException("Unknown Operator: " + ast.getOperator());
        }
        print(ast.getLeft(), operator,  ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            print(receiver, ".", ast.getName());
        } else {
            print(ast.getName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            print(receiver, ".", ast.getFunction().getJvmName(), "(");
        } else {
            print(ast.getFunction().getJvmName(), "(");
        }

        if (!ast.getArguments().isEmpty()) {
            for (Ast.Expression argument : ast.getArguments()) {
                print(argument);
            }
        }

        print(")");
        return null;
    }

    private String getJavaType(String plcTypeName) {
        return Environment.getType(plcTypeName).getJvmName();
    }
}
