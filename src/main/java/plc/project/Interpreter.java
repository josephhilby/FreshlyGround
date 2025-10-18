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

        // print
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        // ln(x)
        scope.defineFunction("logarithm", 1, args -> {
            // ln(value) = exponent
            BigDecimal value = requireType(BigDecimal.class, args.get(0));
            BigDecimal exponent = BigDecimal.valueOf(Math.log(value.doubleValue()));
            return Environment.create(exponent);
        });

        // (decimal) x -> (base_y) x, 0 < y <= 10
        scope.defineFunction("converter", 2, args -> {
           int n = 0;
           ArrayList<BigInteger> quotients = new ArrayList<>();
           ArrayList<BigInteger> remainders = new ArrayList<>();

           BigInteger decimal = requireType(BigInteger.class, args.get(0));
           BigInteger base = requireType(BigInteger.class, args.get(1));

           quotients.add(decimal);

           do {
               quotients.add(quotients.get(n).divide(base));
               remainders.add(
                   quotients.get(n).subtract(quotients.get(n+1).multiply(base))
               );
               n++;
           } while (quotients.get(n).compareTo(BigInteger.ZERO) > 0);

           StringBuilder number = new StringBuilder();
           for (int i = 0; i < remainders.size(); i++) {
               number.insert(0, remainders.get(i).toString());
           }

           return Environment.create(number.toString());
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
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        List<Environment.PlcObject> args = new ArrayList<>();
        return scope.lookupFunction("main", 0).invoke(args);
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
    //    Lambda (callback)
    //       Set scope to child of current scope
    //       Define variable(s) for param(s), assume correct arity
    //       Evaluate expressions(s)
    //       Catch Return, else NIL
    //       Restore scope
    // Return NIL
    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
           try {
               scope = new Scope(scope);
               for (String name : ast.getParameters()) {
                   for (Environment.PlcObject value : args) {
                       scope.defineVariable(name, false, value);
                   }
               }
               for (Ast.Statement statement : ast.getStatements()) {
                   visit(statement);
               }
           } catch (Return ret) {
               return ret.value;
           } finally {
               scope = scope.getParent();
           }
           return Environment.NIL;
        });
        return Environment.NIL;
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
        Ast.Expression.Access access = requireNode(Ast.Expression.Access.class, ast.getReceiver());
        Environment.PlcObject value = visit(ast.getValue());

        if (access.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(access.getReceiver().get());
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
        // pre-order traversal: node, left, right
        String operator = ast.getOperator();
        Object left = visit(ast.getLeft()).getValue();

        // interrupt for OR short circuit
        if (operator.equals("OR") && Boolean.parseBoolean(left.toString())) {
            return Environment.create(true);
        }

        // finish pre-order
        Object right = visit(ast.getRight()).getValue();

        // handle type mismatch
        if (left.getClass() != right.getClass()) {
            throw new UnsupportedOperationException();
        }

        return dispatch(left, right, operator);
    }

    // helper
    private Environment.PlcObject dispatch(Object left, Object right, String operator) {
        switch (left) {
            case BigInteger li:
                return handleInt(li, (BigInteger) right, operator);
            case BigDecimal ld:
                return handleDec(ld, (BigDecimal) right, operator);
            case String ls:
                return handleStr(ls, (String) right, operator);
            case Boolean lb:
                return handleBool(lb, (Boolean) right, operator);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleBool(Boolean left, Boolean right, String op) {
        switch (op) {
            case "AND":
                return Environment.create(left && right);
            case "OR":
                return Environment.create(left || right);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleStr(String left, String right, String op) {
        switch (op) {
            case "+":
                return Environment.create(left + right);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleInt(BigInteger left, BigInteger right, String op) {
        switch (op) {
            case "+":
                return Environment.create(left.add(right));
            case "-":
                return Environment.create(left.subtract(right));
            case "*":
                return Environment.create(left.multiply(right));
            case "/":
                if (left.compareTo(BigInteger.ZERO) == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }
                return Environment.create(left.divide(right));
            case "==":
                return Environment.create(left.compareTo(right) == 0);
            case "!=":
                return Environment.create(left.compareTo(right) != 0);
            case ">=":
                return Environment.create(left.compareTo(right) >= 0);
            case "<=":
                return Environment.create(left.compareTo(right) <= 0);
            case ">":
                return Environment.create(left.compareTo(right) > 0);
            case "<":
                return Environment.create(left.compareTo(right) < 0);
            default:
                throw new UnsupportedOperationException();
        }
    }

    // helper
    private Environment.PlcObject handleDec(BigDecimal left, BigDecimal right, String op) {
        switch (op) {
            case "+":
                return Environment.create(left.add(right));
            case "-":
                return Environment.create(left.subtract(right));
            case "*":
                return Environment.create(left.multiply(right));
            case "/":
                if (left.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }
                return Environment.create(left.divide(right, RoundingMode.HALF_EVEN));
            case "==":
                return Environment.create(left.compareTo(right) == 0);
            case "!=":
                return Environment.create(left.compareTo(right) != 0);
            case ">=":
                return Environment.create(left.compareTo(right) >= 0);
            case "<=":
                return Environment.create(left.compareTo(right) <= 0);
            case ">":
                return Environment.create(left.compareTo(right) > 0);
            case "<":
                return Environment.create(left.compareTo(right) < 0);
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
        // Note: arity != arguments.size, checked elsewhere
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
     * Helper function to ensure ast node is of the appropriate type
     */
    private static <T> T requireNode(Class<T> type, Ast node) {
        if (type.isInstance(node)) {
            return type.cast(node);
        }
        throw new RuntimeException("Expected node " + type.getName() + ", received " + node.getClass().getName() + ".");
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
