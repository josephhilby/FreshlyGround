package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    // throws a RuntimeException if: no 'main/0' type INT
    // visit fields, then visit methods, return null
    //
    // [ fields ] [ methods ]
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

    // throws a RuntimeException if: value exists AND not assignable to field (requireAssignable)
    //                              or constant and no value assigned.
    // visit the value (if present), define variable in current scope, set variable, return null
    //
    // LET [ CONST ] name : typeName [ = value ];
    @Override
    public Void visit(Ast.Field ast) {
        boolean constant = ast.getConstant();
        String name = ast.getName();
        String typeName = ast.getTypeName();
        Optional<Ast.Expression> value = ast.getValue();

        // check value exists and assignable then visit
        if (value.isPresent()) {
            Environment.Type actual = value.get().getType();
            Environment.Type target = Environment.getType(typeName);
            requireAssignable(target, actual);

            visit(value.get());
        }

        // throw error if constant and no value
        if (constant && value.isEmpty()) {
            throw new RuntimeException("constant must not be empty");
        }

        // define and set
        Environment.Variable variable = scope.defineVariable(name, name, Environment.getType(typeName), constant, Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    // define and set function in current scope, visits function arguments and statements in new scope,
    // saves returnType, return null
    //
    // DEF name([ parameter : paramType ]) [ : returnType ]
    // DO statements END
    @Override
    public Void visit(Ast.Method ast) {
        // save method for later use
        method = ast;

        String name = ast.getName();
        List<String> parameters = ast.getParameters();
        List<Environment.Type> paramTypes = new ArrayList<>();
        Environment.Type returnType = Environment.Type.NIL;
        List<Ast.Statement> statements = ast.getStatements();

        // check and set parameter types
        for (String type : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(type));
        }

        // check and set return type
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }

        // define and set function in current scope
        Environment.Function function = scope.defineFunction(name, name, paramTypes, returnType, args -> Environment.NIL);
        ast.setFunction(function);

        // define arguments and visit statements in new scope
        defineArguments(parameters, paramTypes, statements);
        return null;
    }

    private void defineArguments(List<String> parameters, List<Environment.Type> paramTypes, List<Ast.Statement> statements) {
        try {
            scope = new Scope(scope);
            for (int i = 0; i < parameters.size(); i++) {
                scope.defineVariable(parameters.get(i), parameters.get(i), paramTypes.get(i), false, Environment.NIL);
            }
            for (Ast.Statement statement : statements) {
                visit(statement);
            }
        } finally {
            scope = scope.getParent();
        }
    }

    // throws a RuntimeException if: the expression is not an Ast.Expression.Function
    // validate the expression statement, return null
    //
    // expression [ = expression ];
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // check expression is Ast.Expression.Function
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("expression must be a function");
        }

        // visit expression
        visit(ast.getExpression());
        return null;
    }

    // throws a RuntimeException if: value exists AND not assignable to field (subtype of field).
    // visit the value (if present), define variable in current scope, return null
    //
    // LET name [ : type ] [ = value ];
    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        Environment.Type type;

        // check that there is type or value
        if (ast.getValue().isEmpty() && ast.getTypeName().isEmpty()) {
            throw new RuntimeException("Must have type of value");
        }

        // visit value (if present) then set type
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        } else {
            type = Environment.getType(ast.getTypeName().get());
        }

        // if type and value, check assignable
        if (ast.getValue().isPresent() && ast.getTypeName().isPresent()) {
            requireAssignable(Environment.getType(ast.getTypeName().get()),ast.getValue().get().getType());
        }

        // define and set variable
        scope.defineVariable(name, name, type, false, Environment.NIL);
        ast.setVariable(scope.lookupVariable(name));
        return null;
    }

    // throws a RuntimeException if: receiver is not Access, value is not assignable to field, change to const
    // validate statement, return null
    //
    // receiver = value;
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
    //
    // IF condition DO statements [ ELSE statements ] END
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
    //
    // FOR ([ initialization ]; condition; [ increment ])
    // statements END
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
    //
    // WHILE condition DO statements END
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
    //
    // RETURN value;
    @Override
    public Void visit(Ast.Statement.Return ast) {
        // TODO test this, it should not work
        Environment.Type returnType = Environment.getType(method.getReturnTypeName().get());
        requireAssignable(returnType, ast.getValue().getType());
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
    //
    // receiver.identifier
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
    //
    // receiver.identifier([ arguments ])
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
