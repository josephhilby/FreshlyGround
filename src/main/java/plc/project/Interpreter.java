package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    // Ast.Source(List<Field> fields, List<Method> methods)
    // Evaluates globals, then functions
    // Return result of calling the main/0
    // If no main function, evaluation fails
    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // Ast.Field(String name, boolean constant, Optional<Ast.Expression> value)
    // Define variable (or const) in current scope
    // NIL if no initial value
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), ast.getConstant(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), ast.getConstant(), Environment.NIL);
        }
        return Environment.NIL;
    }

    // Ast.Method(String name, List<String> parameters, List<Statement> statements)
    // Define function in current scope
    // Lambda (callback)
    //    Set scope to child of current scope
    //    Define variable(s) for param(s), assume correct arity
    //    Evaluate
    //    Restore scope
    //    Return in Return exception, else NIL
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // Ast.Statement.Expression(Ast.Expression expression)
    // Evaluate expression
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    // Ast.Statement.Declaration(String name, Optional<Ast.Expression> value)
    // Define variable in current scope
    // NIL if no initial value
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), false, visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), false, Environment.NIL);
        }
        return Environment.NIL;
    }

    // Ast.Statement.Assignment(Ast.Expression receiver, Ast.Expression value)
    // Ensure receiver is Ast.Expression.Access
    // If receiver has receiver, evaluate and return
    // Else, return variable in the current scope
    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new UnsupportedOperationException();
        }
        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject value = visit(ast.getValue());

        if (access.getReceiver().isPresent()) {
            Environment.PlcObject receiver = requireType(Environment.PlcObject.class, visit(access.getReceiver().get()));
            receiver.setField(access.getName(), value);
        } else {
            Environment.Variable variable = scope.lookupVariable(access.getName());
            variable.setValue(value);
        }
        return Environment.NIL;
    }

    // Ast.Statement.If(Ast.Expression condition,
    //                  List<Statement> thenStatements,
    //                  List<Statement> elseStatements)
    // Condition evaluates to Boolean
    // Inside new scope,
    //     If true, evaluate thenStatement(s)
    //     Else, evaluate elseStatement(s)
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        List<Ast.Statement> list = condition ? ast.getThenStatements() : ast.getElseStatements();

        try {
            scope = new Scope(scope);
            for (Ast.Statement statement : list) {
                visit(statement);
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    // Ast.Statement.For(Statement initialization,
    //                   Ast.Expression condition,
    //                   Statement increment,
    //                   List<Statement> statements)
    // Init
    // Condition evaluates to Boolean
    // Inside new scope,
    //     If true, evaluate statement(s)
    // Inc
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        visit(ast.getInitialization());

        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }

            visit(ast.getIncrement());
        }
        return Environment.NIL;
    }

    // Ast.Statement.While(Ast.Expression condition, List<Statement> statements)
    // Condition evaluates to Boolean
    // Inside new scope,
    //     If true, evaluate statement(s)
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    // Ast.Statement.Return(Ast.Expression value)
    // Evaluate the value and throw Return exception
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    // Ast.Expression.Literal(Object literal)
    // Return the literal value
    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() != null) {
            return Environment.create(ast.getLiteral());
        }
        return Environment.NIL;
    }

    // Ast.Expression.Group(Ast.Expression expression)
    // Evaluate the contained expression
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    // Ast.Expression.Binary(String operator, Ast.Expression left, Ast.Expression right)
    // Evaluate argument based on operator
    // Return result
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        // TODO: function still to big, split into handler
        // pre-order traversal
        String operator = ast.getOperator();
        Environment.PlcObject left = visit(ast.getLeft());

        // interrupt for OR short circuit
        Object lv = left.getValue();
        if (operator.equals("OR") && Boolean.parseBoolean(lv.toString())) {
            return Environment.create(true);
        }

        // finish pre-order
        Environment.PlcObject right = visit(ast.getRight());

        // handle type mismatch and get initial values
        Object rv = requireType(lv.getClass(), right);

        // switchcase for datatype
        switch (lv) {
            case BigInteger i:
                return handleInt(lv, rv,  operator);
            case BigDecimal d:
                return handleDec(lv, rv, operator);
            case String s:
                return handleStr(lv, rv, operator);
            case Boolean b:
                return handleBool(lv, rv, operator);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleBool(Object lv, Object rv, String operator) {
        Boolean l = Boolean.parseBoolean(lv.toString());
        Boolean r = Boolean.parseBoolean(rv.toString());

        switch (operator) {
            case "AND":
                return Environment.create(l && r);
            case "OR":
                return Environment.create(l || r);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleStr(Object lv, Object rv, String operator) {
        String str1 = lv.toString();
        String str2 = rv.toString();

        switch (operator) {
            case "+":
                return Environment.create(str1 + str2);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleInt(Object left, Object right, String operator) {
        BigInteger l = (BigInteger) left;
        BigInteger r = (BigInteger) right;

        switch (operator) {
            case "+":
                return Environment.create(l.add(r));
            case "-":
                return Environment.create(l.subtract(r));
            case "*":
                return Environment.create(l.multiply(r));
            case "/":
                if (l.compareTo(BigInteger.ZERO) == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }
                return Environment.create(l.divide(r));
            case "==":
                return Environment.create(l.compareTo(r) == 0);
            case "!=":
                return Environment.create(l.compareTo(r) != 0);
            case ">=":
                return Environment.create(l.compareTo(r) >= 0);
            case "<=":
                return Environment.create(l.compareTo(r) <= 0);
            case ">":
                return Environment.create(l.compareTo(r) > 0);
            case "<":
                return Environment.create(l.compareTo(r) < 0);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleDec(Object left, Object right, String operator) {
        BigDecimal l = (BigDecimal) left;
        BigDecimal r = (BigDecimal) right;

        switch (operator) {
            case "+":
                return Environment.create(l.add(r));
            case "-":
                return Environment.create(l.subtract(r));
            case "*":
                return Environment.create(l.multiply(r));
            case "/":
                if (l.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }
                return Environment.create(l.divide(r, RoundingMode.HALF_EVEN));
            case "==":
                return Environment.create(l.compareTo(r) == 0);
            case "!=":
                return Environment.create(l.compareTo(r) != 0);
            case ">=":
                return Environment.create(l.compareTo(r) >= 0);
            case "<=":
                return Environment.create(l.compareTo(r) <= 0);
            case ">":
                return Environment.create(l.compareTo(r) > 0);
            case "<":
                return Environment.create(l.compareTo(r) < 0);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // Ast.Expression.Access(Optional<Ast.Expression> receiver, String name)
    // If receiver, evaluate and return
    // Else, return variable in current scope
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    // Ast.Expression.Function(Optional<Ast.Expression> receiver, String name, List<Ast.Expression> arguments)
    // If receiver, evaluate and return
    // Else, return value of function in current scope
    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> arguments = new ArrayList<>(ast.getArguments().size());
        for (Ast.Expression argument : ast.getArguments()) {
            arguments.add(visit(argument));
        }
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.callMethod(ast.getName(), arguments);
        }

        return scope.lookupFunction(ast.getName(), arguments.size()).invoke(arguments);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
