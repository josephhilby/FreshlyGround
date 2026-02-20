package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.compiler.semantic.BindingMap.Bindings;
import freshlyground.compiler.semantic.Builtins;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Scope;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    public Bindings bindings;
    private Environment.Type currentReturnType;

    public Analyzer() {
        scope = new Scope(null);
        bindings = new Bindings();
        Builtins.install(scope);
    }

    public Scope getScope() { return scope; }
    public Bindings getBindings() { return bindings; }
    public void setReturnType(Environment.Type returnType) { this.currentReturnType = returnType; }

    public Bindings decorate(Ast ast) {
        visit(ast);
        return bindings;
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

        // throws a CompilerException if: no 'main/0' type INT
        Environment.Function main = scope.lookupFunction("main", 0);
        if (main.getType() != Environment.Type.INTEGER) {
            throw new CompilerException("main() return type must be an integer");
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
        // throws a CompilerException if: value not assignable declared type
        if (value.isPresent()) {
            visit(value.get());

            Environment.Type actual = bindings.getType(value.get());
            Environment.Type target = Environment.lookupType(typeName);
            requireAssignable(target, actual);
        }

        // throws a CompilerException if: constant and no value assigned
        if (constant && value.isEmpty()) {
            throw new CompilerException("CONST must have a value");
        }

        // define variable in current scope, then set
        Environment.Variable variable = scope.defineVariable(name, name, Environment.lookupType(typeName), constant);
        bindings.setVariable(ast, variable);
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
            paramTypes.add(Environment.lookupType(type));
        }

        // check and save return type
        currentReturnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            currentReturnType = Environment.lookupType(ast.getReturnTypeName().get());
        }

        // define and set function in current scope
        Environment.Function function = scope.defineFunction(name, name, paramTypes, currentReturnType);
        bindings.setFunction(ast, function);

        // visit statements (including parameters) in new scope
        visitAllStatements(parameters, paramTypes, statements);
        return null;
    }

    // helper
    private void visitAllStatements(List<String> parameters, List<Environment.Type> paramTypes, List<Ast.Statement> statements) {
        try {
            scope = new Scope(scope);

            for (int i = 0; i < parameters.size(); i++) {
                scope.defineVariable(parameters.get(i), parameters.get(i), paramTypes.get(i), false);
            }

            for (Ast.Statement statement : statements) {
                visit(statement);
            }

        } finally {
            scope = scope.getParent();
        }
    }

    // expression;
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return null;
    }

    // LET name [ : type ] [ = value ];
    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();

        Environment.Type type;

        if (ast.getTypeName().isPresent() && ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = Environment.lookupType(ast.getTypeName().get());
        } else if (ast.getTypeName().isPresent()) {
            type = Environment.lookupType(ast.getTypeName().get());
        } else {
            visit(ast.getValue().get());
            type = bindings.getType(ast.getValue().get());
        }

        // throws a CompilerException if: value exists AND not assignable to variable
        if (ast.getValue().isPresent() && ast.getTypeName().isPresent()) {
            Environment.Type target = Environment.lookupType(ast.getTypeName().get());
            requireAssignable(target, type);
        }

        // define and set variable in current scope
        Environment.Variable variable = scope.defineVariable(name, name, type, false);
        bindings.setVariable(ast, variable);
        return null;
    }

    // receiver = value;
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());

        // can't assign to constant
        if (bindings.getVariable(ast.getReceiver()).getConstant()) {
            throw new CompilerException("Cannot reassign constant");
        }

        // throws a CompilerException if: value is not assignable to receiver
        requireAssignable(bindings.getVariable(ast.getReceiver()).getType(), bindings.getType(ast.getValue()));
        return null;
    }

    // IF condition DO statements [ ELSE statements ] END
    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression condition = ast.getCondition();

        visit(condition);

        // throws a CompilerException if: condition not Bool
        requireAssignable(Environment.Type.BOOLEAN, bindings.getType(condition));

        // throws a CompilerException if: thenStatements empty
        if (ast.getThenStatements().isEmpty()) {
            throw new CompilerException("IF block must contain at least one then statement");
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

        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
            initialization = ast.getInitialization();

            // throws a CompilerException if: initialization exists AND not Comparable
            requireAssignable(Environment.Type.COMPARABLE, bindings.getType(initialization.getReceiver()));
        }

        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());
            increment = ast.getIncrement();

            if (ast.getInitialization() != null) {
                initialization = ast.getInitialization();
                // throws a CompilerException if: initialization AND increment exists AND NOT same type
                requireAssignable(bindings.getType(initialization.getReceiver()), bindings.getType(increment.getReceiver()));
            }
        }

        visit(ast.getCondition());

        // throws a CompilerException if: condition not Bool
        requireAssignable(Environment.Type.BOOLEAN, bindings.getType(ast.getCondition()));

        visitStatements(ast.getStatements());

        return null;
    }

    // WHILE condition DO statements END
    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());

        // throws a CompilerException if: value is not Boolean
        requireAssignable(Environment.Type.BOOLEAN, bindings.getType(ast.getCondition()));

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
        Environment.Type actualReturn = bindings.getType(ast.getValue());

        // throws a CompilerException if: value NOT assignable to return type
        requireAssignable(currentReturnType, actualReturn);

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        var literal = ast.getLiteral();

        // validate and set literal type
        if (literal instanceof String) {
            bindings.setType(ast, Environment.Type.STRING);

        } else if (literal instanceof Character) {
            bindings.setType(ast, Environment.Type.CHARACTER);

        } else if (literal instanceof Boolean) {
            bindings.setType(ast, Environment.Type.BOOLEAN);

        } else if (literal == null) {
            bindings.setType(ast, Environment.Type.NIL);

        } else if (literal instanceof BigInteger bigInteger) {
            if (bigInteger.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                bigInteger.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {

                // throws a CompilerException if: INT out of range
                throw new CompilerException("INT Overflow or Underflow");
            }
            bindings.setType(ast, Environment.Type.INTEGER);

        } else if (literal instanceof BigDecimal bigDecimal) {
            if (bigDecimal.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 ||
                bigDecimal.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0) {

                // throws a CompilerException if: DOUBLE out of range
                throw new CompilerException("DOUBLE Overflow or Underflow");
            }
            bindings.setType(ast, Environment.Type.DECIMAL);

        } else {
            // throws a CompilerException if: Unknown Type
            throw new CompilerException("Unknown Type");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        bindings.setType(ast, bindings.getType(ast.getExpression()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());

        Environment.Type leftType = bindings.getType(ast.getLeft());
        Environment.Type rightType = bindings.getType(ast.getRight());

        // throws a CompilerException if: all errant casts
        if (check(operator, "AND", "OR")) {
            requireAssignables(Environment.Type.BOOLEAN, leftType, rightType);
            bindings.setType(ast, Environment.Type.BOOLEAN);

        } else if (check(operator, "==", "!=", "<=", ">=", "<", ">")) {
            requireAssignables(Environment.Type.COMPARABLE, leftType, rightType);
            compareTypes(leftType, rightType);
            bindings.setType(ast, Environment.Type.BOOLEAN);

        } else if (check(operator, "+") && check(Environment.Type.STRING, leftType, rightType)) {
            bindings.setType(ast, Environment.Type.STRING);

        } else if (check(operator, "+", "-", "*", "/")) {
            if (check(Environment.Type.INTEGER, leftType)) {
                requireAssignables(Environment.Type.INTEGER, leftType, rightType);
                bindings.setType(ast, Environment.Type.INTEGER);

            } else if (check(Environment.Type.DECIMAL, leftType)) {
                requireAssignables(Environment.Type.DECIMAL, leftType, rightType);
                bindings.setType(ast, Environment.Type.DECIMAL);

            } else {
                throw new CompilerException("Arithmetic Operators must have matching types, INT or DECIMAL");
            }

        } else {
            // throws a CompilerException if: Unknown Operator
            throw new CompilerException("Unknown Operator: " + operator);
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

            Environment.Type receiverType = bindings.getType(receiver);
            if (receiverType == null) {
                throw new CompilerException("Receiver: type is unresolved");
            }

            variable = receiverType.lookupVariable(ast.getName());

        } else {
            variable = scope.lookupVariable(ast.getName());
        }

        // set variable and type
        bindings.setVariable(ast, variable);
        bindings.setType(ast, variable.getType());
        return null;
    }

    // [ receiver.] function([ arguments ])
    @Override
    public Void visit(Ast.Expression.Function ast) {
        int parameterOffset = 0;
        Environment.Function function;
        List<Environment.Type> parameterTypes;
        List<Ast.Expression> arguments = ast.getArguments();

        // if receiver, visit AND increment argument starting index (account for invoking object)
        if (ast.getReceiver().isPresent()) {
            Ast.Expression receiver = ast.getReceiver().get();
            visit(receiver);

            Environment.Type receiverType = bindings.getType(receiver);
            if (receiverType == null) {
                throw new CompilerException("Receiver: type is unresolved");
            }

            function = receiverType.lookupFunction(ast.getName(), arguments.size());
            parameterOffset = 1;
        } else {
            function = scope.lookupFunction(ast.getName(), arguments.size());
        }

        // set function
        bindings.setFunction(ast, function);

        // checks provided arguments are assignable to parameter types
        parameterTypes = function.getParameterTypes();
        for (Ast.Expression argument : arguments) {
            visit(argument);
            requireAssignable(parameterTypes.get(parameterOffset), bindings.getType(argument));
            parameterOffset++;
        }

        return null;
    }

    // helper
    private void compareTypes(Environment.Type type1, Environment.Type type2) {
        if (type1.equals(type2)) {
            return;
        }
        throw new CompilerException("Types mismatch");
    }

    // helper
    public static void requireAssignables(Environment.Type target, Environment.Type... actuals) {
        for (Environment.Type actual : actuals) {
            requireAssignable(target, actual);
        }
    }

    /**
     * Validates that a value of the {@code actual} type may be assigned to a target of the
     * {@code target} type.
     *
     * <p>Assignability follows the nominal type relationships established by
     * {@link Environment.Type} and does not perform general subtype traversal.
     * A value is assignable if:</p>
     *
     * <ol>
     *   <li>The target and actual types are identical</li>
     *   <li>The target type is {@code Any}</li>
     *   <li>The target type is {@code Comparable} and the actual type is one of
     *       {@code Integer}, {@code Decimal}, {@code Character}, or {@code String}</li>
     * </ol>
     *
     * <p>This method enforces the same type lattice implied by the scoped type hierarchy,
     * ensuring consistency between semantic type checking and type-level scope inheritance.</p>
     *
     * @throws CompilerException if the assignment is not permitted by the language rules
     */
    public static void requireAssignable(Environment.Type target, Environment.Type actual) {
        if (target == actual ||
            target == Environment.Type.ANY ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.INTEGER) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.DECIMAL) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.CHARACTER) ||
           (target == Environment.Type.COMPARABLE && actual == Environment.Type.STRING)) {
            return;
        }

        // else CompilerException
        throw new CompilerException("Type mismatch");
    }
}
