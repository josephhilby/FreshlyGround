package freshlyground.compiler.backend;

import freshlyground.common.CompilerException;
import freshlyground.compiler.semantic.Bindings;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.frontend.Ast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {
    private final StringWriter stringWriter;
    private final PrintWriter writer;
    private final Bindings bindings;
    private int indent = 0;

    public Generator(Bindings bindings) {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.bindings = bindings;
    }

    public String emit(Ast ast) {
        visit(ast);
        return stringWriter.toString();
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
        print("public class Main {");

        newline(indent);
        if (!ast.getFields().isEmpty()) {
            indent++;
            for (Ast.Field field : ast.getFields()) {
                newline(indent);
                print(field);
            }
            newline(--indent);
        }

        newline(++indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        indent--;

        newline(indent);
        if (!ast.getMethods().isEmpty()) {
            for (Ast.Method method : ast.getMethods()) {
                newline(++indent);
                print(method);
                newline(--indent);
            }
            newline(--indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getConstant()) {
            print("final ");
        }

        print(getJavaType(ast.getTypeName()), " ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(bindings.getMethod(ast).getType().getJvmName(), " ", bindings.getMethod(ast).getName());

        if (ast.getParameters().isEmpty()) {
            print("() {");
        } else {
            print("(");
            for (int i = 0; i < ast.getParameters().size(); i++) {
                print(getJavaType(ast.getParameterTypeNames().get(i)), " ", ast.getParameters().get(i));
                if (i < ast.getParameters().size() - 1) {
                    print(", ");
                }
            }
            print(") {");
        }

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
        print(bindings.getVariable(ast).getType().getJvmName(), " ", bindings.getVariable(ast).getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ") {");

        indent++;
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            newline(indent);
            print(ast.getThenStatements().get(i));
        }
        newline(--indent);

        if (!ast.getElseStatements().isEmpty()) {
            print("} else {");
            indent++;
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                newline(indent);
                print(ast.getElseStatements().get(i));
            }
            newline(--indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        print("for ( ");
        if (ast.getInitialization() != null) {
            print(ast.getInitialization(), " ");
        } else {
            print("; ");
        }

        print(ast.getCondition(), "; ");

        if (ast.getIncrement() != null) {
            Ast.Statement.Assignment increment = (Ast.Statement.Assignment) ast.getIncrement();
            print(increment.getReceiver(), " = ", increment.getValue(), " ");
        }

        print(") {");
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
        print("while (", ast.getCondition(), ") {");

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
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (bindings.getType(ast).equals(Environment.Type.INTEGER)) {
            print(ast.getLiteral());

        } else if (bindings.getType(ast).equals(Environment.Type.DECIMAL)) {
            print(ast.getLiteral());

        } else if (bindings.getType(ast).equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");

        } else if (bindings.getType(ast).equals(Environment.Type.CHARACTER)) {
            print("'", ast.getLiteral(), "'");

        } else if (bindings.getType(ast).equals(Environment.Type.BOOLEAN)) {
            print(ast.getLiteral());

        } else if (bindings.getType(ast).equals(Environment.Type.NIL)) {
            print("null");

        } else {
            throw new CompilerException("Unknown Type: " + bindings.getType(ast));
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
        String operator = ast.getOperator();
        switch (ast.getOperator()) {
            case "AND":
                operator = "&&";
                break;
            case "OR":
                operator = "||";
                break;
            case "^":
                operator = "math.pow";
        }
        print(ast.getLeft(), " ",operator, " ", ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            print(receiver, ".", bindings.getVariable(ast).getJvmName());
        } else {
            print(bindings.getVariable(ast).getJvmName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            print(receiver, ".", bindings.getFunction(ast).getJvmName(), "(");
        } else {
            print(bindings.getFunction(ast).getJvmName(), "(");
        }

        if (!ast.getArguments().isEmpty()) {
            List<Ast.Expression> arguments = ast.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                print(arguments.get(i));
                if (i < arguments.size() - 1) {
                    print(", ");
                }
            }
        }

        print(")");
        return null;
    }

    private String getJavaType(String plcTypeName) {
        return Environment.lookupType(plcTypeName).getJvmName();
    }
}
