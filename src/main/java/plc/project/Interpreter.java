package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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

        Object lv = left.getValue();
        if (operator.equals("OR") && Boolean.parseBoolean(lv.toString())) {
            return Environment.create(true);
        }

        Environment.PlcObject right = visit(ast.getRight());

        // handle type mismatch and get initial values
        Object rv = requireType(lv.getClass(), right);

        // switchcase for datatype
        Environment.PlcObject result = Environment.NIL;
        switch (lv) {
            case BigInteger i:
                result = handleInt(lv, rv,  operator);
                break;
            case BigDecimal d:
                result = handleDec(lv, rv, operator);
                break;
            case String s:
                result = handleStr(lv, rv, operator);
                break;
            case Boolean b:
                result = handleBool(lv, rv, operator);
                break;
            default:
                break;
        }
        return result;
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
        String str = lv.toString();
        String str2 = rv.toString();

        switch (operator) {
            case "+":
                return Environment.create(str + str2);
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
