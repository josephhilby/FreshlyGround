package freshlyground.compiler.frontend;

import freshlyground.common.CompilerException;

import java.util.List;
import java.util.Objects;
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

    private static <T> T require(T value, String message) {
        if (value == null) throw new CompilerException(message);
        return value;
    }

    public static final class Source extends Ast {
        private final List<Field> fields;
        private final List<Method> methods;

        public Source(List<Field> fields, List<Method> methods) {
            this.fields = List.copyOf(require(fields, "fields are required, may be ArrayList<>()"));
            this.methods = List.copyOf(require(methods, "methods are required, may be ArrayList<>()"));
        }

        public List<Ast.Field> getFields() { return fields; }
        public List<Method> getMethods() { return methods; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Source other)) return false;

            return Objects.equals(fields, other.fields) &&
                Objects.equals(methods, other.methods);
        }

        @Override
        public String toString() {
            return "Ast.Source{" +
                    "fields=" + fields +
                    ", methods=" + methods +
                    '}';
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
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Field other)) return false;

            return Objects.equals(name, other.name) &&
                Objects.equals(typeName, other.typeName) &&
                Objects.equals(constant, other.constant) &&
                Objects.equals(value, other.value);
        }

        @Override
        public String toString() {
            return "Ast.Field{" +
                    "name='" + name + '\'' +
                    ", typeName=" + typeName +
                    ", constant=" + constant +
                    ", value=" + value +
                    '}';
        }
    }

    public static final class Method extends Ast {
        private final String name;
        private final List<String> parameters;
        private final List<String> parameterTypeNames;
        private final Optional<String> returnTypeName;
        private final List<Statement> statements;

        public Method(
            String name,
            List<String> parameters,
            List<String> parameterTypeNames,
            Optional<String> returnTypeName,
            List<Statement> statements
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
        public List<Statement> getStatements() { return statements; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Method other)) return false;

            return Objects.equals(name, other.name) &&
                Objects.equals(parameters, other.parameters) &&
                Objects.equals(parameterTypeNames, other.parameterTypeNames) &&
                Objects.equals(returnTypeName, other.returnTypeName) &&
                Objects.equals(statements, other.statements);
        }

        @Override
        public String toString() {
            return "Method{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", parameterTypeNames=" + parameterTypeNames +
                    ", returnTypeName='" + returnTypeName + '\'' +
                    ", statements=" + statements +
                    '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Statement.Expression other)) return false;

                return Objects.equals(expression, other.expression);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Expression{" +
                        "expression=" + expression +
                        '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Declaration other)) return false;

                return Objects.equals(name, other.name) &&
                    Objects.equals(typeName, other.typeName) &&
                    Objects.equals(value, other.value);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Declaration{" +
                        "name='" + name + '\'' +
                        ", typeName=" + typeName +
                        ", value=" + value +
                        '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Assignment other)) return false;

                return Objects.equals(receiver, other.receiver) &&
                    Objects.equals(value, other.value);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Assignment{" +
                        "receiver=" + receiver +
                        ", value=" + value +
                        '}';
            }
        }

        public static final class If extends Statement {
            private final Ast.Expression condition;
            private final List<Statement> thenStatements;
            private final List<Statement> elseStatements;

            public If(
                Ast.Expression condition,
                List<Statement> thenStatements,
                List<Statement> elseStatements
            ) {
                this.condition = require(condition, "condition is required");
                this.thenStatements = List.copyOf(require(thenStatements, "thenStatements is required, may be ArrayList<>()"));
                this.elseStatements = List.copyOf(require(elseStatements, "elseStatements is required, may be ArrayList<>()"));
            }

            public Ast.Expression getCondition() { return condition; }
            public List<Statement> getThenStatements() { return thenStatements; }
            public List<Statement> getElseStatements() { return elseStatements; }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof If other)) return false;

                return Objects.equals(condition, other.condition) &&
                    Objects.equals(thenStatements, other.thenStatements) &&
                    Objects.equals(elseStatements, other.elseStatements);
            }

            @Override
            public String toString() {
                return "Ast.Statement.If{" +
                        "condition=" + condition +
                        ", thenStatements=" + thenStatements +
                        ", elseStatements=" + elseStatements +
                        '}';
            }
        }

        public static final class For extends Statement {
            private final Ast.Statement.Assignment initialization;
            private final Ast.Expression condition;
            private final Ast.Statement.Assignment increment;
            private final List<Statement> statements;

            public For(
                Statement.Assignment initialization,
                Ast.Expression condition,
                Statement.Assignment increment,
                List<Statement> statements
            ) {
                this.initialization = initialization;
                this.condition = require(condition, "condition is required");
                this.increment = increment;
                this.statements = List.copyOf(require(statements, "statements is required, may be ArrayList<>()"));
            }

            public Ast.Statement.Assignment getInitialization() { return initialization; }
            public Ast.Expression getCondition() { return condition; }
            public Ast.Statement.Assignment getIncrement() { return increment; }
            public List<Statement> getStatements() { return statements; }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof For other)) return false;

                return Objects.equals(initialization, other.initialization) &&
                    Objects.equals(condition, other.condition) &&
                    Objects.equals(increment, other.increment) &&
                    Objects.equals(statements, other.statements);
            }

            @Override
            public String toString() {
                return "For{" +
                        "initialization=" + initialization +
                        ", condition=" + condition +
                        ", increment=" + increment +
                        ", statements=" + statements +
                        '}';
            }
        }

        public static final class While extends Statement {
            private final Ast.Expression condition;
            private final List<Statement> statements;

            public While(Ast.Expression condition, List<Statement> statements) {
                this.condition = require(condition, "condition is required");
                this.statements = List.copyOf(require(statements, "statements is required, may be ArrayList<>()"));
            }

            public Ast.Expression getCondition() { return condition; }
            public List<Statement> getStatements() { return statements; }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof While other)) return false;

                return Objects.equals(condition, other.condition) &&
                    Objects.equals(statements, other.statements);
            }

            @Override
            public String toString() {
                return "Ast.Statement.While{" +
                        "condition=" + condition +
                        ", statements=" + statements +
                        '}';
            }
        }

        public static final class Return extends Statement {
            private final Ast.Expression value;

            public Return(Ast.Expression value) {
                this.value = require(value, "value is required");
            }

            public Ast.Expression getValue() { return value; }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Return other)) return false;

                return Objects.equals(value, other.value);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Return{" +
                        "value=" + value +
                        '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Literal other)) return false;

                return Objects.equals(literal, other.literal);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Literal{" +
                        "literal=" + literal +
                        '}';
            }
        }

        public static final class Group extends Ast.Expression {
            private final Ast.Expression expression;

            public Group(Ast.Expression expression) {
                this.expression = require(expression, "expression is required");
            }

            public Ast.Expression getExpression() { return expression; }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Group other)) return false;

                return Objects.equals(expression, other.expression);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Group{" +
                        "expression=" + expression +
                        '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Binary other)) return false;

                return Objects.equals(operator, other.operator) &&
                    Objects.equals(left, other.left) &&
                    Objects.equals(right, other.right);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Binary{" +
                        "operator='" + operator + '\'' +
                        ", left=" + left +
                        ", right=" + right +
                        '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Access other)) return false;

                return Objects.equals(receiver, other.receiver) &&
                    Objects.equals(name, other.name);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Access{" +
                        "receiver=" + receiver +
                        ", name='" + name + '\'' +
                        '}';
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
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Function other)) return false;

                return Objects.equals(receiver, other.receiver) &&
                    Objects.equals(name, other.name) &&
                    Objects.equals(arguments, other.arguments);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Function{" +
                        "receiver=" + receiver +
                        "name='" + name + '\'' +
                        ", arguments=" + arguments +
                        '}';
            }
        }
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
}
