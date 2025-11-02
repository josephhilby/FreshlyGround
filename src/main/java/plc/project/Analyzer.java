package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;
    private Ast.Expression type;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        Environment.Function main = scope.lookupFunction("main", 0);
        if (main.getReturnType() != Environment.Type.INTEGER) {
            throw new RuntimeException("main() return type must be an integer");
        }

        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type;

        if (ast.getValue().isPresent()) {
            // spec has no instructions on how to handle explicit and implicit type mismatch
            // how to handel LET x: char = FALSE;
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        } else {
            type = Environment.getType(ast.getTypeName().get());
        }
        scope.defineVariable(ast.getName(), ast.getName(), type, false, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getValue());
        visit(ast.getReceiver()); // must be Access
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        visitStatements(ast.getThenStatements());
        visitStatements(ast.getElseStatements());

        return null;
    }

    private void visitStatements(List<Ast.Statement> statements) {
        for (Ast.Statement statement : statements) {
            try {
                scope = new Scope(scope);
                visit(statement);
            } finally {
                scope = scope.getParent();
            }
        }
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        visit(ast.getInitialization());
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        //requireAssignable(ast.getValue().getType() != type.getType())
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        // TODO cleanup
        var literal = ast.getLiteral();
        if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literal == Environment.NIL) {
            ast.setType(Environment.Type.NIL);
        } else if (literal instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) literal;
            if (bigInteger.intValueExact() > Integer.MAX_VALUE || bigInteger.intValueExact() < Integer.MIN_VALUE) {
                throw new RuntimeException("INT overflow or underflow");
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (literal instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) literal;
            if (bigDecimal.doubleValue() > Double.MAX_VALUE || bigDecimal.doubleValue() < Double.MIN_VALUE) {
                throw new RuntimeException("DOUBLE overflow or underflow");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Group expression must be binary");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // TODO cleanup
        String operator = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());

        if (operator.equals("==") ||
            operator.equals("!=") ||
            operator.equals("<") ||
            operator.equals(">") ||
            operator.equals("<=") ||
            operator.equals(">=")) {

            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if (operator.equals("AND") || operator.equals("OR")) {
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if (operator.equals("+") && (ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING)) {
            ast.setType(Environment.Type.STRING);
        } else if (operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if (ast.getLeft().getType() == Environment.Type.INTEGER) {
                requireAssignable(Environment.Type.INTEGER, ast.getLeft().getType());
                requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                ast.setType(Environment.Type.INTEGER);
            } else if (ast.getLeft().getType() == Environment.Type.DECIMAL) {
                requireAssignable(Environment.Type.DECIMAL, ast.getLeft().getType());
                requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                ast.setType(Environment.Type.DECIMAL);
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            visit(receiver);
            ast.setVariable(receiver.getType().getField(ast.getName()));
        } else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        int i = 0;
        Environment.Function function;
        List<Ast.Expression> arguments = ast.getArguments();
        List<Environment.Type> parameterTypes;

        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            function = ast.getReceiver().get().getType().getFunction(ast.getName(), ast.getArguments().size());
            i++;
        } else {
            function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        }

        parameterTypes = function.getParameterTypes();

        while (i < arguments.size()) {
            visit(arguments.get(i));
            requireAssignable(parameterTypes.get(i), arguments.get(i).getType());
            i++;
        }

        ast.setFunction(function);
        return null;
    }


    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        // accept if:
        // 1. two types are the same
        // 2. target is ANY
        // 3. target is COMPARABLE, to INT, DEC, CHAR, STRING, ~(COMP, ANY, BOOL, NIL)
        // else RuntimeException
        if (target == type ||
            target == Environment.Type.ANY ||
           (target == Environment.Type.COMPARABLE && type == Environment.Type.INTEGER) ||
           (target == Environment.Type.COMPARABLE && type == Environment.Type.DECIMAL) ||
           (target == Environment.Type.COMPARABLE && type == Environment.Type.CHARACTER) ||
           (target == Environment.Type.COMPARABLE && type == Environment.Type.STRING)) {
            return;
        }
        throw new RuntimeException("Type mismatch");
    }

}
