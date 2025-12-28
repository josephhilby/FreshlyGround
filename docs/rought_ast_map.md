### AST Mapping Diagram (with semantic/type restrictions)
# Notation:
#   - [Rule] indicates a semantic/type restriction enforced by the Analyzer (not the Parser).
#   - (T=...) indicates a required static type relationship.
#   - (Node=...) indicates a required AST shape/class at analysis time.

source
└─ { field } { method }
└─> Ast.Source(fields=field(s), methods=method(s))
[Rule] main/0 must exist in global scope
[Rule] main/0 must return Integer

field
└─ "LET" [ "CONST" ] identifier ":" identifier [ "=" expression ] ";"
└─> Ast.Field(
name=identifier,
typeName=identifier,
constant=bool,
value=Optional<expression>
)
[Rule] if constant=true then value must be present
[Rule] if value present then (T: value.type assignable to declared typeName)
[Rule] declares Environment.Variable in current scope and attaches to ast.variable

method
└─ "DEF" identifier "(" [ identifier ":" identifier { "," identifier ":" identifier } ] ")"
[ ":" identifier ] "DO" { statement } "END"
└─> Ast.Method(
name=identifier,
parameters=identifier(s),
parameterTypeNames=identifier(s),
returnTypeName=Optional<identifier>,
statements=statement(s)
)
[Rule] parameterTypeNames must resolve to Environment.Type
[Rule] returnType defaults to Nil if omitted
[Rule] declares Environment.Function in current scope and attaches to ast.function
[Rule] analyzes body in a new nested scope (parameters bound as locals)
[Rule] each RETURN must be (T: returnExpr.type assignable to method returnType)


statement
├─ expression "=" expression ";"
│   └─> Ast.Statement.Assignment(receiver=expression, value=expression)
│       [Rule] (Node: receiver must be Ast.Expression.Access)
│       [Rule] receiver must not be constant
│       [Rule] (T: value.type assignable to receiver.type)
│
├─ expression ";"
│   └─> Ast.Statement.Expression(expression=expression)
│       [Rule] (Node: expression must be Ast.Expression.Function)   # call-only statement
│
├─ "LET" identifier [ ":" identifier ] [ "=" expression ] ";"
│   └─> Ast.Statement.Declaration(name=identifier, typeName=Optional<identifier>, value=Optional<expression>)
│       [Rule] must have a typeName or a value (cannot omit both)
│       [Rule] if value present then inferred var type = value.type
│       [Rule] if typeName present then typeName must resolve to Environment.Type
│       [Rule] if both present then (T: value.type assignable to declared typeName)
│       [Rule] declares Environment.Variable in current scope and attaches to ast.variable
│
├─ "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
│   └─> Ast.Statement.If(condition=expression, thenStatements=statement(s), elseStatements=statement(s))
│       [Rule] (T: condition.type must be Boolean)
│       [Rule] thenStatements must be non-empty
│       [Rule] then/else bodies analyzed in new nested scopes
│
├─ "FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")"
│   { statement } "END"
│   └─> Ast.Statement.For(initialization=statement?, condition=expression, increment=statement?, statements=statement(s))
│       [Rule] statements must be non-empty
│       [Rule] (T: condition.type must be Boolean)
│       [Rule] if initialization present then (Node: initialization must be Ast.Statement.Assignment)
│       [Rule] if increment present then (Node: increment must be Ast.Statement.Assignment)
│       [Rule] if initialization present then (T: init.receiver.type assignable to Comparable)   # as implemented
│       [Rule] if init and incr present then (T: init.receiver.type assignable to incr.receiver.type)
│       [Rule] loop body analyzed in a new nested scope
│
├─ "WHILE" expression "DO" { statement } "END"
│   └─> Ast.Statement.While(condition=expression, statements=statement(s))
│       [Rule] (T: condition.type must be Boolean)
│       [Rule] body analyzed in a new nested scope
│
└─ "RETURN" expression ";"
    └─> Ast.Statement.Return(value=expression)
        [Rule] (T: value.type assignable to current method returnType)


expression  ::= logical_expression

logical_expression
 └─ comparison_expression { ("AND" | "OR") comparison_expression }
     └─> Ast.Expression.Binary(operator=("AND"|"OR"), left=expr, right=expr)
[Rule] (T: left.type == Boolean and right.type == Boolean)
[Rule] result.type = Boolean

comparison_expression
└─ additive_expression { ("<" | "<=" | ">" | ">=" | "==" | "!=") additive_expression }
└─> Ast.Expression.Binary(operator=*from set*, left=expr, right=expr)
[Rule] (T: left.type and right.type assignable to Comparable)
[Rule] (T: left.type == right.type)
[Rule] result.type = Boolean

additive_expression
└─ multiplicative_expression { ("+" | "-") multiplicative_expression }
└─> Ast.Expression.Binary(operator=("+"|"-"), left=expr, right=expr)
[Rule] if operator="+" and (left.type==String OR right.type==String) then result.type=String
[Rule] else arithmetic: (T: both Integer or both Decimal), result.type = that numeric type

multiplicative_expression
└─ secondary_expression { ("*" | "/") secondary_expression }
└─> Ast.Expression.Binary(operator=("*"|"/"), left=expr, right=expr)
[Rule] arithmetic: (T: both Integer or both Decimal), result.type = that numeric type

secondary_expression
└─ primary_expression { "." identifier [ "(" [ expression { "," expression } ] ")" ] }
├─ ".identifier"
│   └─> Ast.Expression.Access(receiver=Optional.of(primary_expression), name=identifier)
│       [Rule] receiver is analyzed first; member variable resolved via receiver.type scope
└─ ".identifier(args)"
└─> Ast.Expression.Function(receiver=Optional.of(primary_expression), name=identifier, arguments=expression(s))
[Rule] receiver is analyzed first; member function resolved via receiver.type scope
[Rule] args analyzed left-to-right and must be assignable to parameter types

primary_expression
├─ "NIL"
│   └─> Ast.Expression.Literal(literal=null)
│       [Rule] result.type = Nil
│
├─ "TRUE" | "FALSE"
│   └─> Ast.Expression.Literal(literal=Boolean)
│       [Rule] result.type = Boolean
│
├─ integer | decimal | character | string
│   └─> Ast.Expression.Literal(literal=Number|Character|String)
│       [Rule] numeric bounds checked; sets Integer/Decimal accordingly; sets Character/String accordingly
│
├─ "(" expression ")"
│   └─> Ast.Expression.Group(expression=expression)
│       [Rule] (Node: inner expression must be Ast.Expression.Binary)   # as implemented
│       [Rule] group.type = inner.type
│
├─ identifier
│   └─> Ast.Expression.Access(receiver=Optional.empty, name=identifier)
│       [Rule] variable resolved via current lexical scope chain
│
└─ identifier "(" [ expression { "," expression } ] ")"
└─> Ast.Expression.Function(receiver=Optional.empty, name=identifier, arguments=expression(s))
[Rule] function resolved via current lexical scope chain
[Rule] args analyzed left-to-right and must be assignable to parameter types
