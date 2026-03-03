package freshlyground.compiler.backend.core;

import freshlyground.compiler.semantic.Bindings;
import freshlyground.compiler.frontend.artifacts.Ast;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class Generator implements Ast.Visitor<Void> {
    private final StringWriter stringWriter;
    protected final PrintWriter writer;

    protected final Bindings bindings;
    protected int indent = 0;

    public Generator(Bindings bindings) {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.bindings = bindings;
    }

    public String emit(Ast ast) {
        visit(ast);
        return stringWriter.toString();
    }
    protected void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }
    protected void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }
}
