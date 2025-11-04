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
    private Environment.Type returnType;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    // throws a RuntimeException if: no 'main/0' type INT
    // visit fields, then visit methods, return null
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

    // throws a RuntimeException if: value exists AND not assignable to field (subtype of field)
    //                              or constant and no value assigned.
    // visit the value (if present), define variable in current scope, set variable, return null
    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            Environment.Type value = ast.getValue().get().getType();
            Environment.Type field = Environment.getType(ast.getTypeName());
            requireAssignable(value, field);
            visit(ast.getValue().get());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), false, Environment.NIL));
        return null;
    }

    // throws a RuntimeException if: ...
    // defines function in current scope, visits function statements in new scope (with params = arg),
    // saves returnType, return null
    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> paramTypes = new ArrayList<>();
        for (String name : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(name));
        }

        returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), paramTypes, returnType, args -> Environment.NIL));
        defineArguments(ast.getParameters(), ast.getFunction().getParameterTypes(), ast.getStatements());

        return null;
    }

    private void defineArguments(List<String> param, List<Environment.Type> paramTypes, List<Ast.Statement> statements) {
        try {
            scope = new Scope(scope);
            for (int i = 0; i < param.size(); i++) {
                scope.defineVariable(param.get(i), param.get(i), paramTypes.get(i), false, Environment.NIL);
            }
            for (Ast.Statement statement : statements) {
                visit(statement);
            }
        } finally {
            scope = scope.getParent();
        }
    }

    // throws a RuntimeException if the expression is not an Ast.Expression.Function
    // validate the expression statement, return null
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("expression must be a function");
        }
        visit(ast.getExpression());
        return null;
    }

    // throws a RuntimeException if: value exists AND not assignable to field (subtype of field).
    // visit the value (if present), define variable in current scope, return null
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

    // throws a RuntimeException if: receiver is not Access, value is not assignable to field, change to const
    // validate statement, return null
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("receiver must be an access expression");
        }
        visit(ast.getValue());
        visit(ast.getReceiver());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    // throws a RuntimeException if: condition not Bool, thenStatements empty
    // visit then and else statements inside new scope for each, return null
    @Override
    public Void visit(Ast.Statement.If ast) {
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("if statement must contain at least one statement");
        }
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        visitStatements(ast.getThenStatements());
        visitStatements(ast.getElseStatements());

        return null;
    }

    // throws a RuntimeException if: identifier exists AND not Comparable, condition not Bool,
    //                               expression in inc NOT same as identifier, statements empty
    // validate FOR statement, return null
    @Override
    public Void visit(Ast.Statement.For ast) {
        visit(ast.getInitialization());
        visit(ast.getCondition());
        visit(ast.getIncrement());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        visitStatements(ast.getStatements());

        return null;
    }

    // throws a RuntimeException if: value is not Boolean
    // visits WHILE statements in new scope, return null
    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        visitStatements(ast.getStatements());

        return null;
    }

    // helper
    private void visitStatements(List<Ast.Statement> statements) {
        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : statements) {
                visit(statement);
            }
        } finally {
            scope = scope.getParent();
        }
    }

    // throws a RuntimeException if: value NOT assignable to func. return type (set in visit(Ast.Method))
    // return null
    @Override
    public Void visit(Ast.Statement.Return ast) {
        requireAssignable(ast.getValue().getType(), returnType);
        return null;
    }

    // throws a RuntimeException if: int or double out of range
    // validate and set literal type, return null
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

    // throws a RuntimeException if: expression not a binary expression
    // return null
    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Group expression must be binary");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    // throws a RuntimeException if: all errant casts
    // AND / OR, both bool, set result bool
    // ==, !=, <,..., both comp, set result comp
    // +, either string, set result string
    // +,-,*,/ both same (int or dec), set result int or dec
    // return null
    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // TODO cleanup
        String operator = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());

        if (operator.equals("AND") || operator.equals("OR")) {
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if (operator.equals("==") ||
                   operator.equals("!=") ||
                   operator.equals("<") ||
                   operator.equals(">") ||
                   operator.equals("<=") ||
                   operator.equals(">=")) {

            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
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
        } else {
            throw new RuntimeException("Unknown operator: " + operator);
        }
        return null;
    }

    // throws a RuntimeException if: ...
    // validate access expression and set variable
    //    set type of the expression to type of variable
    //    variable field of receiver if present, otherwise variable of current scope
    // return null
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

    // validate and set function
    //     set function to return type
    //     function of receiver if present, otherwise function of current scope
    // checks provided arguments are assignable to parameter types
    // return null
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


    public static void requireAssignable(Environment.Type target, Environment.Type actual) {
        // accept if:
        // 1. two types are the same
        // 2. target is ANY
        // 3. target is COMPARABLE, to INT, DEC, CHAR, STRING, ~(COMP, ANY, BOOL, NIL)
        // else RuntimeException
        if (target == actual ||
            target == Environment.Type.ANY ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.INTEGER) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.DECIMAL) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.CHARACTER) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.STRING)) {
            return;
        }
        throw new RuntimeException("Type mismatch");
    }

}
