package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), false, visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), false, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // Ast.Expression.Literal(literal=Object | null)
    // Returns the literal value
    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() != null) {
            return Environment.create(ast.getLiteral());
        }
        return Environment.NIL;
    }

    // Ast.Expression.Group(expression=Ast.Expression)
    // evaluates the contained expression, returning its value.
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    // Ast.Expression.Binary(operator=String,
    //                      left=Ast.Expression,
    //                      right=Ast.Expression)
    // Evaluates arguments based on the specific binary operator,
    // returning the appropriate result for the operation
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        // pre-order traversal
        String operator = ast.getOperator();
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());

        // handle type mismatch and get initial value for right
        Object rv = requireType(left.getValue().getClass(), right);

        // switch
        Environment.PlcObject result = Environment.NIL;
        switch (rv) {
            case BigInteger i:
                BigInteger li = BigInteger.class.cast(left.getValue());
                BigInteger ri = BigInteger.class.cast(rv);
                result = handleNum(li, ri,  operator);
            case BigDecimal d:
                BigDecimal ld = BigDecimal.class.cast(left.getValue());
                BigDecimal rd = BigDecimal.class.cast(rv);
                result = handleNum(ld, rd, operator);
            default:
                break;
        }
        return result;
    }

    // helper
    private Environment.PlcObject handleNum(BigInteger left, BigInteger right, String operator) {
        switch (operator) {
            case "+":
                return Environment.create(left.add(right));
            case "-":
                return Environment.create(left.subtract(right));
            case "*":
                return Environment.create(left.multiply(right));
            case "/":
                if (right.compareTo(BigInteger.ZERO) == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return Environment.create(left.divide(right));
            default:
                throw new UnsupportedOperationException();
        }
    }

    // Ast.Expression.Access(receiver=Ast.Expression | Optional.empty, name=string)
    // has a receiver, evaluate and return
    // otherwise, return variable in current scope
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isEmpty()) {
            return scope.lookupVariable(ast.getName()).getValue();
        }
        Environment.PlcObject receiver = visit(ast.getReceiver().get());
        return receiver.getField(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
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
