package plc.project;

import java.util.Arrays;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
