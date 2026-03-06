package freshlyground.compiler.frontend.artifacts;

import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.Parser;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * An {@code Ast} represents an abstract syntax tree (AST) produced by the
 * {@link Parser}, after successfully recognizing the program’s context-free grammar
 * (CFG). Each AST captures an assembled, syntactically valid, representation of
 * the users program. Each AST consists of:
 *
 * <ul>
 *   <li>{@link Source} — the root node representing the entire program.</li>
 *   <li>{@link Field} — a declaration of program-level state.</li>
 *   <li>{@link Method} — a declaration of program-level behavior.</li>
 *   <li>{@link Statement} — an executable unit that performs an action,
 *       potentially involving one or more expressions.</li>
 *   <li>{@link Expression} — a value-producing construct that describes
 *       computation and resolves to a type and value.</li>
 * </ul>
 *
 * <p>
 * AST nodes are immutable with respect to syntactic structure; semantic
 * information (such as resolved symbols and types) are attached during
 * the {@link Analyzer} pass.
 * </p>
 */
public abstract class Ast {

    public abstract Object[] components();
    public abstract String[] componentNames();

    @Override
    public final int hashCode() {
        return Arrays.deepHashCode(components());
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || this.getClass() != obj.getClass()) return false;
        return Arrays.deepEquals(this.components(), ((Ast) obj).components());
    }

    @Override
    public String toString() {
        Object[] values = components();
        String[] names = componentNames();

        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("{");

        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names[i]).append("=").append(values[i]);
        }

        sb.append("}");
        return sb.toString();
    }

    protected static <T> T require(T value, String message) {
        if (value == null) throw new CompilerException(message);
        return value;
    }

    public interface Visitor<T> {

        default T visit(Ast ast) {
            return switch (ast) {
                case Ast.Source node -> visit(node);
                case Ast.Field node -> visit(node);
                case Ast.Method node -> visit(node);

                case Ast.Statement.Expression node -> visit(node);
                case Ast.Statement.Declaration node -> visit(node);
                case Ast.Statement.Assignment node -> visit(node);
                case Ast.Statement.If node -> visit(node);
                case Ast.Statement.For node -> visit(node);
                case Ast.Statement.While node -> visit(node);
                case Ast.Statement.Return node -> visit(node);

                case Ast.Expression.Literal node -> visit(node);
                case Ast.Expression.Group node -> visit(node);
                case Ast.Expression.Binary node -> visit(node);
                case Ast.Expression.Access node -> visit(node);
                case Ast.Expression.Function node -> visit(node);

                default -> throw new CompilerException("Unimplemented AST type: " + ast.getClass().getName());
            };
        }

        T visit(Ast.Source ast);
        T visit(Ast.Field ast);
        T visit(Ast.Method ast);
        T visit(Ast.Statement.Expression ast);
        T visit(Ast.Statement.Declaration ast);
        T visit(Ast.Statement.Assignment ast);
        T visit(Ast.Statement.If ast);
        T visit(Ast.Statement.For ast);
        T visit(Ast.Statement.While ast);
        T visit(Ast.Statement.Return ast);
        T visit(Ast.Expression.Literal ast);
        T visit(Ast.Expression.Group ast);
        T visit(Ast.Expression.Binary ast);
        T visit(Ast.Expression.Access ast);
        T visit(Ast.Expression.Function ast);
    }

    public static final class Source extends Ast {
        private final List<Ast.Field> fields;
        private final List<Ast.Method> methods;

        public Source(List<Ast.Field> fields, List<Ast.Method> methods) {
            this.fields = List.copyOf(require(fields, "fields are required, may be ArrayList<>()"));
            this.methods = List.copyOf(require(methods, "methods are required, may be ArrayList<>()"));
        }

        public List<Ast.Field> getFields() { return fields; }
        public List<Ast.Method> getMethods() { return methods; }

        @Override
        public String[] componentNames() {
            return new String[]{ "fields", "methods" };
        }

        @Override
        public Object[] components() {
            return new Object[]{ fields, methods };
        }
    }

    public static final class Field extends Ast {
        private final String name;
        private final String typeName;
        private final boolean constant;
        private final Optional<Ast.Expression> value;

        public Field(
            String name,
            String typeName,
            boolean constant,
            Optional<Ast.Expression> value
        ) {
            this.name = require(name, "name is required");
            this.typeName = require(typeName, "typeName is required");
            this.constant = constant;
            this.value = require(value, "value is required, may be Optional.empty()");
        }

        public String getName() { return name; }
        public String getTypeName() { return typeName; }
        public boolean getConstant() { return constant; }
        public Optional<Ast.Expression> getValue() { return value; }

        @Override
        public String[] componentNames() {
            return new String[]{ "name", "typeName", "constant", "value" };
        }

        @Override
        public Object[] components() {
            return new Object[]{ name, typeName, constant, value };
        }
    }

    public static final class Method extends Ast {
        private final String name;
        private final List<String> parameters;
        private final List<String> parameterTypeNames;
        private final Optional<String> returnTypeName;
        private final List<Ast.Statement> statements;

        public Method(
            String name,
            List<String> parameters,
            List<String> parameterTypeNames,
            Optional<String> returnTypeName,
            List<Ast.Statement> statements
        ) {
            this.name = require(name, "name is required");
            this.parameters = List.copyOf(require(parameters, "parameters are required, may be ArrayList<>()"));
            this.parameterTypeNames = List.copyOf(require(parameterTypeNames, "parameterTypeNames is required, may be ArrayList<>()"));
            this.returnTypeName = require(returnTypeName, "returnTypeName is required, may be Optional.empty()");
            this.statements = List.copyOf(require(statements, "statements are required, may be ArrayList<>()"));
        }

        public String getName() { return name; }
        public List<String> getParameters() { return parameters; }
        public List<String> getParameterTypeNames() { return parameterTypeNames; }
        public Optional<String> getReturnTypeName() { return returnTypeName; }
        public List<Ast.Statement> getStatements() { return statements; }

        @Override
        public String[] componentNames() {
            return new String[]{ "name", "parameters", "parameterTypeNames", "returnTypeName", "statements" };
        }

        @Override
        public Object[] components() {
            return new Object[]{ name, parameters, parameterTypeNames, returnTypeName, statements };
        }
    }

    public static abstract class Statement extends Ast {

        public static final class Expression extends Statement {
            private final Ast.Expression.Function expression;

            public Expression(Ast.Expression.Function expression) {
                this.expression = require(expression, "expression is required");
            }

            public Ast.Expression.Function getExpression() { return expression; }

            @Override
            public String[] componentNames() {
                return new String[]{ "expression" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ expression };
            }
        }

        public static final class Declaration extends Statement {
            private final String name;
            private final Optional<String> typeName;
            private final Optional<Ast.Expression> value;

            public Declaration(
                String name,
                Optional<String> typeName,
                Optional<Ast.Expression> value
            ) {
                this.name = require(name, "name is required");
                this.typeName = require(typeName, "typeName is required, may be Optional.empty()");
                this.value = require(value, "value is required, may be Optional.empty()");

                if (typeName.isEmpty() && value.isEmpty()) {
                    throw new CompilerException("Must have declared type or value");
                }
            }

            public String getName() { return name; }
            public Optional<String> getTypeName() { return typeName; }
            public Optional<Ast.Expression> getValue() { return value; }

            @Override
            public String[] componentNames() {
                return new String[]{ "name", "typeName", "value" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ name, typeName, value };
            }
        }

        public static final class Assignment extends Statement {
            private final Ast.Expression.Access receiver;
            private final Ast.Expression value;

            public Assignment(Ast.Expression.Access receiver, Ast.Expression value) {
                this.receiver = require(receiver, "receiver is required");
                this.value = require(value, "value is required");
            }

            public Ast.Expression.Access getReceiver() { return receiver; }
            public Ast.Expression getValue() { return value; }

            @Override
            public String[] componentNames() {
                return new String[]{ "receiver", "value" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ receiver, value };
            }
        }

        public static final class If extends Statement {
            private final Ast.Expression condition;
            private final List<Ast.Statement> thenStatements;
            private final List<Ast.Statement> elseStatements;

            public If(
                Ast.Expression condition,
                List<Ast.Statement> thenStatements,
                List<Ast.Statement> elseStatements
            ) {
                this.condition = require(condition, "condition is required");
                this.thenStatements = List.copyOf(require(thenStatements, "thenStatements is required, may be ArrayList<>()"));
                this.elseStatements = List.copyOf(require(elseStatements, "elseStatements is required, may be ArrayList<>()"));
            }

            public Ast.Expression getCondition() { return condition; }
            public List<Ast.Statement> getThenStatements() { return thenStatements; }
            public List<Ast.Statement> getElseStatements() { return elseStatements; }

            @Override
            public String[] componentNames() {
                return new String[]{ "condition", "thenStatements", "elseStatements" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ condition, thenStatements, elseStatements };
            }
        }

        public static final class For extends Statement {
            private final Optional<Ast.Statement.Assignment> initialization;
            private final Ast.Expression condition;
            private final Optional<Ast.Statement.Assignment> increment;
            private final List<Ast.Statement> statements;

            public For(
                Ast.Statement.Assignment initialization,
                Ast.Expression condition,
                Ast.Statement.Assignment increment,
                List<Ast.Statement> statements
            ) {
                this.initialization = Optional.ofNullable(initialization);
                this.condition = require(condition, "condition is required");
                this.increment = Optional.ofNullable(increment);
                this.statements = List.copyOf(require(statements, "statements is required, may be ArrayList<>()"));
            }

            public Optional<Ast.Statement.Assignment> getInitialization() { return initialization; }
            public Ast.Expression getCondition() { return condition; }
            public Optional<Ast.Statement.Assignment> getIncrement() { return increment; }
            public List<Statement> getStatements() { return statements; }

            @Override
            public String[] componentNames() {
                return new String[]{ "initialization", "condition", "increment", "statements" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ initialization, condition, increment, statements };
            }
        }

        public static final class While extends Statement {
            private final Ast.Expression condition;
            private final List<Ast.Statement> statements;

            public While(Ast.Expression condition, List<Statement> statements) {
                this.condition = require(condition, "condition is required");
                this.statements = List.copyOf(require(statements, "statements is required, may be ArrayList<>()"));
            }

            public Ast.Expression getCondition() { return condition; }
            public List<Ast.Statement> getStatements() { return statements; }

            @Override
            public String[] componentNames() {
                return new String[]{ "condition", "statements" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ condition, statements };
            }
        }

        public static final class Return extends Statement {
            private final Ast.Expression value;

            public Return(Ast.Expression value) {
                this.value = require(value, "value is required");
            }

            public Ast.Expression getValue() { return value; }

            @Override
            public String[] componentNames() {
                return new String[]{ "value" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ value };
            }
        }
    }

    public static abstract class Expression extends Ast {

        public static final class Literal extends Ast.Expression {
            private final Object literal;

            public Literal(Object literal) {
                this.literal = literal;
            }

            public Object getLiteral() { return literal; }

            @Override
            public String[] componentNames() {
                return new String[]{ "literal" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ literal };
            }
        }

        public static final class Group extends Ast.Expression {
            private final Ast.Expression expression;

            public Group(Ast.Expression expression) {
                this.expression = require(expression, "expression is required");
            }

            public Ast.Expression getExpression() { return expression; }

            @Override
            public String[] componentNames() {
                return new String[]{ "expression" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ expression };
            }
        }

        public static final class Binary extends Ast.Expression {
            private final String operator;
            private final Ast.Expression left;
            private final Ast.Expression right;

            public Binary(String operator, Ast.Expression left, Ast.Expression right) {
                this.operator = require(operator, "operator is required");
                this.left = require(left, "left is required");
                this.right = require(right, "right is required");
            }

            public String getOperator() { return operator; }
            public Ast.Expression getLeft() { return left; }
            public Ast.Expression getRight() { return right; }

            @Override
            public String[] componentNames() {
                return new String[]{ "operator", "left", "right" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ operator, left, right };
            }
        }

        public static final class Access extends Ast.Expression {
            private final Optional<Ast.Expression> receiver;
            private final String name;

            public Access(Optional<Ast.Expression> receiver, String name) {
                this.receiver = require(receiver, "receiver is required, may be Optional.empty()");
                this.name = require(name , "name is required");
            }

            public Optional<Ast.Expression> getReceiver() { return receiver; }
            public String getName() { return name; }

            @Override
            public String[] componentNames() {
                return new String[]{ "receiver", "name" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ receiver, name };
            }
        }

        public static final class Function extends Ast.Expression {
            private final Optional<Ast.Expression> receiver;
            private final String name;
            private final List<Ast.Expression> arguments;

            public Function(Optional<Ast.Expression> receiver, String name, List<Ast.Expression> arguments) {
                this.receiver = require(receiver, "receiver is required, may be Optional.empty()");
                this.name = require(name, "name is required");
                this.arguments = List.copyOf(require(arguments, "arguments is required, may be ArrayList<>()"));
            }

            public Optional<Ast.Expression> getReceiver() { return receiver; }
            public String getName() { return name; }
            public List<Ast.Expression> getArguments() { return arguments; }

            @Override
            public String[] componentNames() {
                return new String[]{ "receiver", "name", "arguments" };
            }

            @Override
            public Object[] components() {
                return new Object[]{ receiver, name, arguments };
            }
        }
    }
}
