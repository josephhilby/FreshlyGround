package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The interpreter takes a structured representation of the program, the Abstract
 * Syntax Tree (AST), created by the parser and traverses it via pre-order
 * traversal.
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link } and {@link
 * } are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope;

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

        // (base_10) x -> (base_y) x, 0 < y <= 10
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

    /**
     * Visits the root {@link Ast.Source} node of the program.
     * <p>
     * This method is responsible for evaluating all global field declarations
     * and registering all method definitions within the current {@code scope}.
     * Once globals and functions have been processed, it attempts to invoke
     * the program’s entry point — the {@code main} method with zero parameters.
     * </p>
     *
     * <p>If no {@code main()} method is defined, an exception will be thrown
     * when {@code lookupFunction("main", 0)} fails.</p>
     *
     * @param ast the abstract syntax tree (AST) source node containing
     *            the program’s top-level fields and methods
     * @return the result of invoking {@code main()} with no arguments
     * @throws RuntimeException if no zero-parameter {@code main()} function exists
     */
    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        // debug
//        System.out.println("Global interpreter scope before main: " + scope);
//        scope.lookupFunction("h", 1);

        List<Environment.PlcObject> args = new ArrayList<>();
        return scope.lookupFunction("main", 0).invoke(args);
    }

    /**
     * Visits a {@link Ast.Field} node and defines a corresponding variable
     * or constant in the current {@code scope}.
     * <p>
     * If the field has an initializer expression, it is evaluated and used
     * as the variable’s initial value. Otherwise, the variable is initialized
     * to {@link Environment#NIL}.
     * </p>
     *
     * <p>The method always returns {@code Environment.NIL}, as field
     * declarations themselves do not produce a value.</p>
     *
     * @param ast the field node containing the variable name, constant flag,
     *            and optional initializer expression
     * @return {@link Environment#NIL}, since field declarations have no runtime value
     */
    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), ast.getConstant(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), ast.getConstant(), Environment.NIL);
        }
        return Environment.NIL;
    }

    /**
     * Visits a {@link Ast.Method} node and defines a new function within the
     * current {@link Scope}.
     * <p>
     * The function is represented as a lambda (callback) that, when invoked:
     * </p>
     * <ol>
     *   <li>Creates a new child {@code Scope} extending the defining scope,
     *       not the invoking scope.</li>
     *   <li>Defines local variables for each parameter, binding them to the
     *       provided argument values (assuming correct arity).</li>
     *   <li>Evaluates each statement in the method body sequentially.</li>
     *   <li>If a {@code return} statement is encountered, returns its value.</li>
     *   <li>Otherwise, returns {@link Environment#NIL} after all statements execute.</li>
     * </ol>
     *
     * @param ast the method node containing the function name, parameter list,
     *            and body statements
     * @return {@link Environment#NIL}, since method declarations have no direct value
     */
    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Scope defining = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {

            Scope calling = defining;
            try {
                scope = new Scope(defining);
                List<String> parameters = ast.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    scope.defineVariable(parameters.get(i), false, args.get(i));
                }
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } catch (Return ret) {
                return ret.value;
            } finally {
                scope = calling;
            }
           return Environment.NIL;
        });
        return Environment.NIL;
    }

    /**
     * Visits an {@link Ast.Statement.Expression} node and evaluates its
     * contained {@link Ast.Expression}.
     * <p>
     * Expression statements are executed for their side effects (e.g., function
     * calls or assignments) rather than for producing a value. As such, the result
     * of the expression is evaluated and then discarded.
     * </p>
     *
     * <p>The method always returns {@link Environment#NIL}, since expression
     * statements do not yield a value in the surrounding context.</p>
     *
     * @param ast the statement node containing the expression to be evaluated
     * @return {@link Environment#NIL}, as expression statements have no value
     */
    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    /**
     * Visits an {@link Ast.Statement.Declaration} node and defines a new variable
     * in the current {@code scope}.
     * <p>
     * If the declaration includes an initializer expression, it is evaluated
     * and used as the variable’s initial value. Otherwise, the variable is
     * initialized to {@link Environment#NIL}.
     * </p>
     *
     * <p>The declaration itself does not produce a runtime value, so this method
     * always returns {@link Environment#NIL}.</p>
     *
     * @param ast the declaration statement containing the variable name and
     *            optional initializer expression
     * @return {@link Environment#NIL}, since declarations have no runtime value
     */
    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), false, visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), false, Environment.NIL);
        }
        return Environment.NIL;
    }

    /**
     * Visits an {@link Ast.Statement.Assignment} node and performs an assignment
     * operation to either a variable or an object field.
     * <p>
     * The method first ensures that the left-hand side (receiver) of the
     * assignment is an {@link Ast.Expression.Access}. It then evaluates the
     * right-hand side (value) expression and assigns the resulting value as follows:
     * </p>
     * <ul>
     *   <li>If the access expression has its own receiver (e.g., {@code object.field}),
     *       the corresponding field of that receiver object is updated.</li>
     *   <li>Otherwise, the assignment targets a variable in the current {@code scope}.</li>
     * </ul>
     *
     * <p>Regardless of target type, the method returns {@link Environment#NIL},
     * since assignments do not produce a standalone value.</p>
     *
     * @param ast the assignment statement containing the receiver (LHS)
     *             and value (RHS) expressions
     * @return {@link Environment#NIL}, since assignments have no direct value
     * @throws RuntimeException if the receiver is not an {@link Ast.Expression.Access}
     */
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

    /**
     * Visits an {@link Ast.Statement.If} node and conditionally evaluates
     * one of two statement blocks based on a Boolean condition.
     * <p>
     * The condition expression is first evaluated and required to produce a
     * {@link Boolean} value. Then, within a new {@link Scope}:
     * </p>
     * <ul>
     *   <li>If the condition evaluates to {@code true}, all statements in
     *       {@code thenStatements} are executed sequentially.</li>
     *   <li>Otherwise, all statements in {@code elseStatements} are executed.</li>
     * </ul>
     *
     * <p>The {@code if} statement itself does not produce a value and therefore
     * always returns {@link Environment#NIL}.</p>
     *
     * @param ast the {@code if} statement node containing the condition,
     *            then-block, and optional else-block
     * @return {@link Environment#NIL}, since conditional statements have no value
     * @throws RuntimeException if the condition does not evaluate to a Boolean
     */
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

    /**
     * Visits an {@link Ast.Statement.For} node and executes a loop consisting of
     * initialization, condition checking, body execution, and incrementation.
     * <p>
     * The {@code for} loop is evaluated as follows:
     * </p>
     * <ol>
     *   <li>Execute the initialization statement once before the loop begins.</li>
     *   <li>Evaluate the loop condition, which must produce a {@link Boolean} value.</li>
     *   <li>While the condition is {@code true}:
     *     <ul>
     *       <li>Create a new {@link Scope} for the loop body.</li>
     *       <li>Execute each statement within the loop body sequentially.</li>
     *       <li>After the body completes, restore the previous scope and
     *           execute the increment statement.</li>
     *     </ul>
     *   </li>
     *   <li>When the condition evaluates to {@code false}, the loop terminates.</li>
     * </ol>
     *
     * <p>The {@code for} statement itself does not yield a runtime value, so this
     * method always returns {@link Environment#NIL}.</p>
     *
     * @param ast the {@code for} statement node containing the initialization,
     *            condition, increment, and body statements
     * @return {@link Environment#NIL}, since loops have no standalone value
     * @throws RuntimeException if the condition does not evaluate to a Boolean
     */
    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }

        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }

            if (ast.getIncrement() != null) {
                visit(ast.getIncrement());
            }
        }
        return Environment.NIL;
    }

    /**
     * Visits an {@link Ast.Statement.While} node and repeatedly executes
     * a block of statements while a Boolean condition remains {@code true}.
     * <p>
     * The {@code while} loop is evaluated as follows:
     * </p>
     * <ol>
     *   <li>Evaluate the loop condition, which must produce a {@link Boolean} value.</li>
     *   <li>While the condition evaluates to {@code true}:
     *     <ul>
     *       <li>Create a new {@link Scope} for the loop body.</li>
     *       <li>Execute each statement in the loop body sequentially.</li>
     *       <li>After the body finishes, restore the previous scope and
     *           re-evaluate the condition.</li>
     *     </ul>
     *   </li>
     *   <li>When the condition becomes {@code false}, the loop terminates.</li>
     * </ol>
     *
     * <p>The {@code while} statement itself does not yield a runtime value and
     * always returns {@link Environment#NIL}.</p>
     *
     * @param ast the {@code while} statement node containing the loop condition
     *            and body statements
     * @return {@link Environment#NIL}, since loops do not produce a value
     * @throws RuntimeException if the condition does not evaluate to a Boolean
     */
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

    /**
     * Visits an {@link Ast.Statement.Return} node and performs a function return
     * by evaluating the associated expression and throwing a {@link Return} exception.
     * <p>
     * In this interpreter, function returns are implemented using an exception-based
     * control flow mechanism. When a {@code return} statement is encountered:
     * </p>
     * <ol>
     *   <li>The return expression is evaluated to obtain its {@link Environment.PlcObject} value.</li>
     *   <li>A new {@link Return} exception is thrown, carrying that value.</li>
     *   <li>This exception is caught by the enclosing function’s lambda defined in
     *       {@link #visit(Ast.Method)}, which then returns the contained value to the caller.</li>
     * </ol>
     *
     * <p>Since control flow is transferred via exception, this method never returns normally.</p>
     *
     * @param ast the {@code return} statement node containing the expression to evaluate
     * @return never returns normally; this method always throws a {@link Return} exception
     * @throws Return the exception carrying the evaluated return value
     */
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    /**
     * Visits an {@link Ast.Expression.Literal} node and returns its corresponding
     * runtime value within the {@link Environment}.
     * <p>
     * Literal expressions represent constant values directly embedded in the source
     * code (e.g., numbers, strings, booleans, etc.). When evaluated:
     * </p>
     * <ul>
     *   <li>If the literal is non-{@code null}, an {@link Environment.PlcObject}
     *       is created wrapping the literal value.</li>
     *   <li>If the literal is {@code null}, {@link Environment#NIL} is returned
     *       to represent the absence of a value.</li>
     * </ul>
     *
     * @param ast the literal expression node containing the underlying value
     * @return an {@link Environment.PlcObject} wrapping the literal value,
     *         or {@link Environment#NIL} if the literal is {@code null}
     */
    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() != null) {
            return Environment.create(ast.getLiteral());
        }
        return Environment.NIL;
    }

    /**
     * Visits an {@link Ast.Expression.Group} node and evaluates its contained
     * expression.
     * <p>
     * Group expressions exist primarily to enforce evaluation order, such as
     * through parentheses in arithmetic or logical expressions (e.g.,
     * {@code (a + b) * c}). The grouping itself has no runtime effect beyond
     * ensuring the inner expression is evaluated as a single unit.
     * </p>
     *
     * @param ast the group expression node containing the inner expression
     * @return the result of evaluating the contained expression
     */
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    /**
     * Visits an {@link Ast.Expression.Binary} node and evaluates a binary
     * operation between two sub-expressions.
     * <p>
     * The method performs a pre-order traversal, evaluating the left operand
     * first, followed by (optionally) the right operand depending on the operator.
     * It supports short-circuiting for logical {@code OR} operations and delegates
     * operator-specific behavior to {@link #dispatch(Object, Object, String)}.
     * </p>
     * <p>
     * Evaluation proceeds as follows:
     * </p>
     * <ol>
     *   <li>Capture the operator.</li>
     *   <li>Evaluate the left operand and retrieve its value.</li>
     *   <li>If the operator is {@code OR} and the left operand is {@code true},
     *       short-circuit by returning {@code true}.</li>
     *   <li>Evaluate the right operand and retrieve its value.</li>
     *   <li>If the operand types do not match, throw an {@link RuntimeException}.</li>
     *   <li>Delegate to {@link #dispatch(Object, Object, String)} to compute the final result.</li>
     * </ol>
     *
     * @param ast the binary expression node containing the operator and its left
     *            and right sub-expressions
     * @return an {@link Environment.PlcObject} containing the result of the binary operation
     * @throws RuntimeException if the operand types are incompatible
     */
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Object left = visit(ast.getLeft()).getValue();

        // OR short circuit
        if (operator.equals("OR") && Boolean.parseBoolean(left.toString())) {
            return Environment.create(true);
        }

        // AND short circuit
        if (operator.equals("AND") && !Boolean.parseBoolean(left.toString())) {
            return Environment.create(false);
        }

        Object right = visit(ast.getRight()).getValue();

        // ==/!= override
        if (operator.equals("==")) {
            return Environment.create(Objects.equals(left, right));
        }

        if (operator.equals("!=")) {
            return Environment.create(!Objects.equals(left, right));
        }

        if (operator.equals("+") && (left.getClass() == String.class || right.getClass() == String.class)) {
            return Environment.create(String.valueOf(left) + String.valueOf(right));
        }

        if (left.getClass() != right.getClass()) {
            throw new RuntimeException();
        }

        return dispatch(left, right, operator);
    }

    /**
     * Visits an {@link Ast.Expression.Access} node and retrieves the value of
     * a variable or an object field.
     * <p>
     * Access expressions represent variable lookups or field accesses depending
     * on whether a receiver expression is present:
     * </p>
     * <ul>
     *   <li>If a receiver expression is present (e.g., {@code object.field}),
     *       the receiver is evaluated first, and the named field’s value is returned.</li>
     *   <li>If no receiver is present, the variable is looked up directly in the
     *       current {@link Scope} and its value is returned.</li>
     * </ul>
     *
     * <p>In both cases, this method retrieves and returns the evaluated
     * {@link Environment.PlcObject} value associated with the given name.</p>
     *
     * @param ast the access expression node containing an optional receiver
     *            and the identifier name to retrieve
     * @return the {@link Environment.PlcObject} value of the accessed variable or field
     * @throws RuntimeException if the variable or field cannot be found in the current scope
     */
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    /**
     * Visits an {@link Ast.Expression.Function} node and performs a function or
     * method call, evaluating all argument expressions before invocation.
     * <p>
     * Function expressions may represent either:
     * </p>
     * <ul>
     *   <li><b>Method calls</b> — when a receiver expression is present (e.g.,
     *       {@code object.method(arg1, arg2)}). The receiver is evaluated first,
     *       and the named method is invoked on that object.</li>
     *   <li><b>Function calls</b> — when no receiver is present. The function is
     *       looked up in the current {@link Scope} by name and arity, and then invoked.</li>
     * </ul>
     *
     * <p>All argument expressions are evaluated in order before the call occurs.
     * Arity validation (i.e., ensuring the number of arguments matches the
     * function’s parameter count) is handled elsewhere.</p>
     *
     * @param ast the function expression node containing an optional receiver,
     *            the function name, and a list of argument expressions
     * @return the {@link Environment.PlcObject} result returned by the invoked function or method
     * @throws RuntimeException if the target function or method cannot be found in the current scope
     */
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
     * Helper function to dispatch a {@link #visit(Ast.Expression.Binary ast)} request to the appropriate
     * handler, according to datatype.
     */
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
                throw new RuntimeException();
        }
    }

    /**
     * Helper function to handle boolean {@link #visit(Ast.Expression.Binary ast)} requests.
     */
    private Environment.PlcObject handleBool(Boolean left, Boolean right, String op) {
        switch (op) {
            case "AND":
                return Environment.create(left && right);
            case "OR":
                return Environment.create(left || right);
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Helper function to handle string {@link #visit(Ast.Expression.Binary ast)} requests.
     */
    private Environment.PlcObject handleStr(String left, String right, String op) {
        switch (op) {
            case ">":
                return Environment.create(left.compareTo(right) > 0);
            case "<":
                return Environment.create(left.compareTo(right) < 0);
            case ">=":
                return Environment.create(left.compareTo(right) >= 0);
            case "<=":
                return Environment.create(left.compareTo(right) <= 0);
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Helper function to handle integer {@link #visit(Ast.Expression.Binary ast)} requests.
     */
    private Environment.PlcObject handleInt(BigInteger left, BigInteger right, String op) {
        switch (op) {
            case "+":
                return Environment.create(left.add(right));
            case "-":
                return Environment.create(left.subtract(right));
            case "*":
                return Environment.create(left.multiply(right));
            case "/":
                if (right.compareTo(BigInteger.ZERO) == 0) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return Environment.create(left.divide(right));
            case ">=":
                return Environment.create(left.compareTo(right) >= 0);
            case "<=":
                return Environment.create(left.compareTo(right) <= 0);
            case ">":
                return Environment.create(left.compareTo(right) > 0);
            case "<":
                return Environment.create(left.compareTo(right) < 0);
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Helper function to handle decimal {@link #visit(Ast.Expression.Binary ast)} requests.
     */
    private Environment.PlcObject handleDec(BigDecimal left, BigDecimal right, String op) {
        switch (op) {
            case "+":
                return Environment.create(left.add(right));
            case "-":
                return Environment.create(left.subtract(right));
            case "*":
                return Environment.create(left.multiply(right));
            case "/":
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return Environment.create(left.divide(right, RoundingMode.HALF_EVEN));
            case ">=":
                return Environment.create(left.compareTo(right) >= 0);
            case "<=":
                return Environment.create(left.compareTo(right) <= 0);
            case ">":
                return Environment.create(left.compareTo(right) > 0);
            case "<":
                return Environment.create(left.compareTo(right) < 0);
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Helper function to ensure an {@link Environment.PlcObject} is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Helper function to ensure an {@link Ast} node is of the appropriate type
     */
    private static <T> T requireNode(Class<T> type, Ast node) {
        if (type.isInstance(node)) {
            return type.cast(node);
        }
        throw new RuntimeException("Expected node " + type.getName() + ", received " + node.getClass().getName() + ".");
    }

    /**
     * Helper exception for returning {@link #visit(Ast.Method)} values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
