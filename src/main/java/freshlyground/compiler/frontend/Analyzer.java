package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;
import freshlyground.compiler.semantic.Bindings;
import freshlyground.compiler.semantic.StandardLibrary;
import freshlyground.compiler.semantic.Environment;
import freshlyground.compiler.semantic.Scope;
import freshlyground.compiler.semantic.Types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static freshlyground.compiler.semantic.Environment.*;


public final class Analyzer implements Ast.Visitor<Void> {
    public Scope lexicalScope;
    public Bindings bindings;
    private Environment.Type currentReturnType;

    public Analyzer() {
        lexicalScope = new Scope(null);
        bindings = new Bindings();
        StandardLibrary.install(lexicalScope);
    }

    public Scope getScope() { return lexicalScope; }
    public Bindings getBindings() { return bindings; }
    public void setReturnType(Environment.Type returnType) { this.currentReturnType = returnType; }

    public Bindings decorate(Ast ast) {
        visit(ast);
        return bindings;
    }

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
        Environment.Function main = lexicalScope.lookupFunction("main", 0);
        if (main.returnType() != Types.INTEGER) {
            throw new CompilerException("main() return type must be an integer");
        }

        return null;
    }

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
            Environment.Type target = Environment.sourceLookupType(typeName);
            requireAssignable(target, actual);
        }

        // throws a CompilerException if: constant and no value assigned
        if (constant && value.isEmpty()) {
            throw new CompilerException("CONST must have a value");
        }

        // define variable in current lexical scope, then set
        Environment.Variable variable = lexicalScope.defineVariable(name, Environment.sourceLookupType(typeName), constant);
        bindings.setVariable(ast, variable);
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        String name = ast.getName();
        List<String> parameters = ast.getParameters();
        List<Environment.Type> paramTypes = new ArrayList<>();
        List<Ast.Statement> statements = ast.getStatements();

        // check and set parameter types
        for (String type : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.sourceLookupType(type));
        }

        // check and save return type
        currentReturnType = Types.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            currentReturnType = Environment.sourceLookupType(ast.getReturnTypeName().get());
        }

        // define and set function in current lexical scope
        Environment.Function function = lexicalScope.defineFunction(name, paramTypes, currentReturnType);
        bindings.setFunction(ast, function);

        // visit statements (including parameters) in new scope
        visitAllStatements(parameters, paramTypes, statements);
        return null;
    }

    // helper
    private void visitAllStatements(
        List<String> parameters,
        List<Environment.Type> paramTypes,
        List<Ast.Statement> statements
    ) {
        try {
            lexicalScope = new Scope(lexicalScope);

            for (int i = 0; i < parameters.size(); i++) {
                lexicalScope.defineVariable(parameters.get(i), paramTypes.get(i), false);
            }

            for (Ast.Statement statement : statements) {
                visit(statement);
            }

        } finally {
            lexicalScope = lexicalScope.getParent();
        }
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();

        Environment.Type type;

        if (ast.getTypeName().isPresent() && ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = Environment.sourceLookupType(ast.getTypeName().get());
        } else if (ast.getTypeName().isPresent()) {
            type = Environment.sourceLookupType(ast.getTypeName().get());
        } else {
            visit(ast.getValue().get());
            type = bindings.getType(ast.getValue().get());
        }

        // throws a CompilerException if: value exists AND not assignable to variable
        if (ast.getValue().isPresent() && ast.getTypeName().isPresent()) {
            Environment.Type target = Environment.sourceLookupType(ast.getTypeName().get());
            requireAssignable(target, type);
        }

        // define and set variable in current lexical scope
        Environment.Variable variable = lexicalScope.defineVariable(name, type, false);
        bindings.setVariable(ast, variable);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());

        // can't assign to constant
        if (bindings.getVariable(ast.getReceiver()).constant()) {
            throw new CompilerException("Cannot reassign constant");
        }

        // throws a CompilerException if: value is not assignable to receiver
        requireAssignable(bindings.getVariable(ast.getReceiver()).type(), bindings.getType(ast.getValue()));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression condition = ast.getCondition();

        visit(condition);

        // throws a CompilerException if: condition not Bool
        requireAssignable(Types.BOOLEAN, bindings.getType(condition));

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

    @Override
    public Void visit(Ast.Statement.For ast) {
        Ast.Statement.Assignment initialization;
        Ast.Statement.Assignment increment;

        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
            initialization = ast.getInitialization();

            // throws a CompilerException if: initialization exists AND not Comparable
            requireAssignable(Types.PRIMITIVE, bindings.getType(initialization.getReceiver()));
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
        requireAssignable(Types.BOOLEAN, bindings.getType(ast.getCondition()));

        visitStatements(ast.getStatements());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());

        // throws a CompilerException if: value is not Boolean
        requireAssignable(Types.BOOLEAN, bindings.getType(ast.getCondition()));

        // visits WHILE statements in new scope
        visitStatements(ast.getStatements());

        return null;
    }

    // helper
    private void visitStatements(List<Ast.Statement> statements) {
        try {
            lexicalScope = new Scope(lexicalScope);

            for (Ast.Statement statement : statements) {
                visit(statement);
            }

        } finally {
            lexicalScope = lexicalScope.getParent();
        }
    }

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
            bindings.setType(ast, Types.STRING);

        } else if (literal instanceof Character) {
            bindings.setType(ast, Types.CHARACTER);

        } else if (literal instanceof Boolean) {
            bindings.setType(ast, Types.BOOLEAN);

        } else if (literal == null) {
            bindings.setType(ast, Types.NIL);

        } else if (literal instanceof BigInteger bigInteger) {
            if (bigInteger.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                bigInteger.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {

                // throws a CompilerException if: INT out of range
                throw new CompilerException("INT Overflow or Underflow");
            }
            bindings.setType(ast, Types.INTEGER);

        } else if (literal instanceof BigDecimal bigDecimal) {
            if (bigDecimal.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 ||
                bigDecimal.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0) {

                // throws a CompilerException if: DOUBLE out of range
                throw new CompilerException("DOUBLE Overflow or Underflow");
            }
            bindings.setType(ast, Types.DECIMAL);

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
            requireAssignables(Types.BOOLEAN, leftType, rightType);
            bindings.setType(ast, Types.BOOLEAN);

        } else if (check(operator, "==", "!=")) {
            requireSame(leftType, rightType);
            bindings.setType(ast, Types.BOOLEAN);

        } else if (check(operator, "<=", ">=", "<", ">")) {
            requireAssignables(Types.PRIMITIVE, leftType, rightType);
            requireSame(leftType, rightType);
            requireNot(Types.BOOLEAN, leftType);
            bindings.setType(ast, Types.BOOLEAN);

        } else if (check(operator, "+") && check(Types.STRING, leftType, rightType)) {
            bindings.setType(ast, Types.STRING);

        } else if (check(operator, "+", "-", "*", "/")) {
            if (check(Types.INTEGER, leftType)) {
                requireAssignables(Types.INTEGER, leftType, rightType);
                bindings.setType(ast, Types.INTEGER);

            } else if (check(Types.DECIMAL, leftType)) {
                requireAssignables(Types.DECIMAL, leftType, rightType);
                bindings.setType(ast, Types.DECIMAL);

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

            variable = receiverType.lookupMemberVariable(ast.getName());

        } else {
            variable = lexicalScope.lookupVariable(ast.getName());
        }

        // set variable and type
        bindings.setVariable(ast, variable);
        bindings.setType(ast, variable.type());
        return null;
    }

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

            function = receiverType.lookupMemberFunction(ast.getName(), arguments.size());
            parameterOffset = 1;
        } else {
            function = lexicalScope.lookupFunction(ast.getName(), arguments.size());
        }

        // set function
        bindings.setFunction(ast, function);

        // checks provided arguments are assignable to parameter types
        parameterTypes = function.parameterTypes();
        for (Ast.Expression argument : arguments) {
            visit(argument);
            requireAssignable(parameterTypes.get(parameterOffset), bindings.getType(argument));
            parameterOffset++;
        }

        return null;
    }
}
