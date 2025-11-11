package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Environment.Type returnType;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    // [ fields ] [ methods ]
    @Override
    public Void visit(Ast.Source ast) {
        // visit fields, then visit methods, return null
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }

        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }

        // throws a RuntimeException if: no 'main/0' type INT
        Environment.Function main = scope.lookupFunction("main", 0);
        if (main.getReturnType() != Environment.Type.INTEGER) {
            throw new RuntimeException("main() return type must be an integer");
        }

        return null;
    }

    // LET [ constant ] name : typeName [ = value ];
    @Override
    public Void visit(Ast.Field ast) {
        boolean constant = ast.getConstant();
        String name = ast.getName();
        String typeName = ast.getTypeName();
        Optional<Ast.Expression> value = ast.getValue();

        // visit the value, if exists
        // throws a RuntimeException if: value not assignable declared type
        if (value.isPresent()) {
            visit(value.get());

            Environment.Type actual = value.get().getType();
            Environment.Type target = Environment.getType(typeName);
            requireAssignable(target, actual);
        }

        // throws a RuntimeException if: constant and no value assigned
        if (constant && value.isEmpty()) {
            throw new RuntimeException("CONST must have a value");
        }

        // define variable in current scope, then set
        Environment.Variable variable = scope.defineVariable(name, name, Environment.getType(typeName), constant, Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    // DEF name([ parameter : paramType ]) [ : returnType ]
    //   DO statements END
    @Override
    public Void visit(Ast.Method ast) {
        String name = ast.getName();
        List<String> parameters = ast.getParameters();
        List<Environment.Type> paramTypes = new ArrayList<>();
        List<Ast.Statement> statements = ast.getStatements();

        // check and set parameter types
        for (String type : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(type));
        }

        // check and save return type
        returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }

        // define and set function in current scope
        Environment.Function function = scope.defineFunction(name, name, paramTypes, returnType, args -> Environment.NIL);
        ast.setFunction(function);

        // visit statements (including parameters) in new scope
        visitAllStatements(parameters, paramTypes, statements);
        return null;
    }

    // helper
    private void visitAllStatements(List<String> parameters, List<Environment.Type> paramTypes, List<Ast.Statement> statements) {
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

    // expression [ = expression ];
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // throws a RuntimeException if: the expression is not an Ast.Expression.Function
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression must be a function");
        }

        // visit expression
        visit(ast.getExpression());
        return null;
    }

    // LET name [ : type ] [ = value ];
    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        Environment.Type type;

        // throws a RuntimeException if: no value AND no type
        if (ast.getValue().isEmpty() && ast.getTypeName().isEmpty()) {
            throw new RuntimeException("Must have declared type or value");
        }

        // visit value (if present) then set type
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();

        } else {
            type = Environment.getType(ast.getTypeName().get());
        }

        // throws a RuntimeException if: value exists AND not assignable to variable
        if (ast.getValue().isPresent() && ast.getTypeName().isPresent()) {
            requireAssignable(Environment.getType(ast.getTypeName().get()), ast.getValue().get().getType());
        }

        // define and set variable in current scope
        Environment.Variable variable = scope.defineVariable(name, name, type, false, Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    // receiver = value;
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // throws a RuntimeException if: receiver is not Access
        if (!(ast.getReceiver() instanceof Ast.Expression.Access receiver)) {
            throw new RuntimeException("Receiver must be an access expression");
        }

        visit(receiver);
        visit(ast.getValue());

        // can't assign to constant
        if (receiver.getVariable().getConstant()) {
            throw new RuntimeException("Cannot reassign constant");
        }

        // throws a RuntimeException if: value is not assignable to receiver
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    // IF condition DO statements [ ELSE statements ] END
    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression condition = ast.getCondition();

        visit(condition);

        // throws a RuntimeException if: condition not Bool
        requireAssignable(Environment.Type.BOOLEAN, condition.getType());

        // throws a RuntimeException if: thenStatements empty
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("IF block must contain at least one then statement");
        }

        // visit then statements inside new scope
        visitStatements(ast.getThenStatements());

        // if existed, visit else statements inside new scope
        if (!ast.getElseStatements().isEmpty()) {
            visitStatements(ast.getElseStatements());
        }

        return null;
    }

    // FOR ([ initialization ]; condition; [ increment ])
    //   statements END
    @Override
    public Void visit(Ast.Statement.For ast) {
        Ast.Statement.Assignment initialization;
        Ast.Statement.Assignment increment;

        // throws a RuntimeException if: statements empty
        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("FOR block must contain at least one statement");
        }

        visitStatements(ast.getStatements());

        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
            initialization = (Ast.Statement.Assignment) ast.getInitialization();

            // throws a RuntimeException if: initialization exists AND not Comparable
            requireAssignable(Environment.Type.COMPARABLE, initialization.getReceiver().getType());

            if (ast.getIncrement() != null) {
                visit(ast.getIncrement());
                increment = (Ast.Statement.Assignment) ast.getIncrement();

                // throws a RuntimeException if: initialization AND increment exists AND NOT same type
                requireAssignable(initialization.getReceiver().getType(), increment.getReceiver().getType());
            }
        }

        visit(ast.getCondition());

        // throws a RuntimeException if: condition not Bool
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        return null;
    }

    // WHILE condition DO statements END
    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());

        // throws a RuntimeException if: value is not Boolean
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        // visits WHILE statements in new scope
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

    // RETURN value;
    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        Environment.Type actualReturn = ast.getValue().getType();

        // throws a RuntimeException if: value NOT assignable to return type
        requireAssignable(returnType, actualReturn);

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        var literal = ast.getLiteral();

        // validate and set literal type
        if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);

        } else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);

        } else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);

        } else if (literal == Environment.NIL) {
            ast.setType(Environment.Type.NIL);

        } else if (literal instanceof BigInteger bigInteger) {
            if (bigInteger.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                bigInteger.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {

                // throws a RuntimeException if: INT out of range
                throw new RuntimeException("INT Overflow or Underflow");
            }
            ast.setType(Environment.Type.INTEGER);

        } else if (literal instanceof BigDecimal bigDecimal) {
            if (bigDecimal.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 ||
                bigDecimal.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0) {

                // throws a RuntimeException if: DOUBLE out of range
                throw new RuntimeException("DOUBLE Overflow or Underflow");
            }
            ast.setType(Environment.Type.DECIMAL);

        } else {
            // throws a RuntimeException if: Unknown Type
            throw new RuntimeException("Unknown Type");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        // throws a RuntimeException if: expression not a binary expression
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Group expression must be binary");
        }

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());

        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        // throws a RuntimeException if: all errant casts
        if (check(operator, "AND", "OR")) {
            requireAssignables(Environment.Type.BOOLEAN, leftType, rightType);
            ast.setType(Environment.Type.BOOLEAN);

        } else if (check(operator, "==", "!=", "<=", ">=", "<", ">")) {
            requireAssignables(Environment.Type.COMPARABLE, leftType, rightType);
            ast.setType(Environment.Type.BOOLEAN);

        } else if (check(operator, "+") && check(Environment.Type.STRING, leftType, rightType)) {
            ast.setType(Environment.Type.STRING);

        } else if (check(operator, "+", "-", "*", "/")) {
            if (check(Environment.Type.INTEGER, leftType)) {
                requireAssignables(Environment.Type.INTEGER, leftType, rightType);
                ast.setType(Environment.Type.INTEGER);

            } else if (check(Environment.Type.DECIMAL, leftType)) {
                requireAssignables(Environment.Type.DECIMAL, leftType, rightType);
                ast.setType(Environment.Type.DECIMAL);

            } else {
                throw new RuntimeException("Arithmetic Operators must have matching types, INT or DECIMAL");
            }

        } else {
            // throws a RuntimeException if: Unknown Operator
            throw new RuntimeException("Unknown Operator: " + operator);
        }

        return null;
    }

    // helper
    private boolean check(Environment.Type expectedType, Environment.Type... actualTypes) {
        for (Environment.Type actualType : actualTypes) {
            if (expectedType.equals(actualType)) {
                return true;
            }
        }
        return false;
    }

    // helper
    private boolean check(String operator, String... literals) {
        for (String literal : literals) {
            if (operator.equals(literal)) {
                return true;
            }
        }
        return false;
    }

    // [ receiver. ] variable
    @Override
    public Void visit(Ast.Expression.Access ast) {
        Environment.Variable variable;
        // if receiver, visit
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            visit(receiver);
            variable = receiver.getType().getField(ast.getName());

        } else {
            variable = scope.lookupVariable(ast.getName());
        }

        // set variable
        ast.setVariable(variable);

        return null;
    }

    // [ receiver.] function([ arguments ])
    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function;
        List<Ast.Expression> arguments = ast.getArguments();
        List<Environment.Type> parameterTypes;
        int i = 0;

        // if receiver, visit AND increment argument starting index (account for invoking object)
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            visit(receiver);
            function = receiver.getType().getFunction(ast.getName(), ast.getArguments().size());
            i++;

        } else {
            function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        }

        // set function
        ast.setFunction(function);

        // checks provided arguments are assignable to parameter types
        parameterTypes = function.getParameterTypes();
        while (i < arguments.size()) {
            visit(arguments.get(i));
            requireAssignable(parameterTypes.get(i), arguments.get(i).getType());
            i++;
        }

        return null;
    }

    // helper
    public static void requireAssignables(Environment.Type target, Environment.Type... actuals) {
        for (Environment.Type actual : actuals) {
            requireAssignable(target, actual);
        }
    }

    // accept if:
    // 1. two types are the same
    // 2. target is ANY
    // 3. target is COMPARABLE, if INT, DEC, CHAR, STRING
    public static void requireAssignable(Environment.Type target, Environment.Type actual) {
        if (target == actual ||
            target == Environment.Type.ANY ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.INTEGER) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.DECIMAL) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.CHARACTER) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.STRING)) {
            return;
        }

        // else RuntimeException
        throw new RuntimeException("Type mismatch");
    }

}
