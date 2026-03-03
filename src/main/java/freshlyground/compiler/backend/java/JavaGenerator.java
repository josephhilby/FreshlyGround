package freshlyground.compiler.backend.java;

import freshlyground.common.CompilerException;
import freshlyground.compiler.backend.core.Generator;
import freshlyground.compiler.semantic.Bindings;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.backend.core.FunctionCallLowering;
import freshlyground.compiler.semantic.Types;

import java.util.List;

public final class JavaGenerator extends Generator {
    private final StandardLibraryLowering standardLibraryLowering;

    public JavaGenerator(Bindings bindings) {
        super(bindings);
        this.standardLibraryLowering = new StandardLibraryLowering();
    }

    private String getJavaType(String typeName) {
        return JavaTypeLowering.getJavaType(Environment.lookupType(typeName));
    }

    private String getJavaType(Environment.Type type) {
        return JavaTypeLowering.getJavaType(type);
    }

    private String getJavaReturnType(Environment.Type type) {
        return JavaTypeLowering.getJavaReturnType(type);
    }

    @FunctionalInterface
    interface JavaPrint {
        void out(Object... parts);
    }

    private JavaPrint printer() {
        return this::print;
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
        print(getJavaReturnType(bindings.getFunction(ast).returnType()), " ", bindings.getFunction(ast).name());

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
        print(getJavaType(bindings.getVariable(ast).type()), " ", bindings.getVariable(ast).name());

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
            Ast.Statement.Assignment increment = ast.getIncrement();
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
        if (bindings.getType(ast).equals(Types.INTEGER)) {
            print(ast.getLiteral());

        } else if (bindings.getType(ast).equals(Types.DECIMAL)) {
            print(ast.getLiteral());

        } else if (bindings.getType(ast).equals(Types.STRING)) {
            print("\"", ast.getLiteral(), "\"");

        } else if (bindings.getType(ast).equals(Types.CHARACTER)) {
            print("'", ast.getLiteral(), "'");

        } else if (bindings.getType(ast).equals(Types.BOOLEAN)) {
            print(ast.getLiteral());

        } else if (bindings.getType(ast).equals(Types.NIL)) {
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
            print(receiver, ".", bindings.getVariable(ast).name());
        } else {
            print(bindings.getVariable(ast).name());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = bindings.getFunction(ast);
        FunctionCallLowering builtin = standardLibraryLowering.lowerBuiltin(function);

        if (builtin != null) {
            standardLibraryLowering.emitCall(printer(), ast, builtin);
            return null;
        }

        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            print(receiver, ".", bindings.getFunction(ast).name(), "(");
        } else {
            print(bindings.getFunction(ast).name(), "(");
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
}
