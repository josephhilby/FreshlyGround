<!-- PRACTICAL IMPLEMENTATION -->
Although FreshlyGround is not object-oriented, it supports a dot (.) access operator to express type-associated 
operations in a readable, statically dispatched form. Member access resolves functions and fields defined in a value’s 
type scope, enabling clean semantic analysis and straightforward lowering to JVM bytecode.

This is why the arity of the function would be increased by one as x.compare(y) becomes compare(x, y).

## Practical Implementation
### AST Mapping Diagram
>```text
>source ─> Ast.Source(fields=field(s), methods=method(s))
>
>field
> └─ "LET" [ CONST ] identifier ":" identifier [ "=" expression ] ";"
>     └─> Ast.Field(name=identifier, typeName=identifier, constant=bool, value=expression)
>  
>method
> └─ "DEF" identifier "(" [ identifier ":" identifier { "," identifier ":" identifier } ] ")" [ ":" identifier ] 
>     │ "DO" { statement } "END"
>     └─> Ast.Method(name=identifier, 
>                    parameters=identifier(s), 
>                    parameterTypeNames=identifier(s), 
>                    returnTypeName=identifier,
>                    statements=statement(s))
>```
>
>```text
>statement
> ├─ expression "=" expression ";"
> │   └─> Ast.Statement.Assignment(receiver=expression, value=expression)
> │
> ├─ expression ";"
> │   └─> Ast.Statement.Expression(expression=(Ast.Expression.Function))
> │
> ├─ "LET" identifier [ ":" identifier ] [ "=" expression ] ";"
> │   └─> Ast.Statement.Declaration(name=identifier, typeName=identifier, value=expression)
> │
> ├─ "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END" 
> │   └─> Ast.Statement.If(condition=expression, 
> │                        thenStatements=statement(s), 
> │                        elseStatements=statement(s))
> │
> ├─ "FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")" 
> │   │   { statement } "END"
> │   └─> Ast.Statement.For(initialization=(Ast.Statement.Assignment), 
> │                         condition=expression, 
> │                         increment=(Ast.Statement.Assignment), 
> │                         statements=statement(s))
> │
> ├─ "WHILE" expression "DO" { statement } "END"
> │   └─> Ast.Statement.While(condition=expression, statements=statement(s))
> │
> └─ "RETURN" expression ";"
>     └─> Ast.Statement.Return(value=expression)
>```
>
>```text
>expression
> └─ logical_expression
>     └─ comparison_expression { ("AND"|"OR") comparison_expression }
>         └─> Ast.Expression.Binary(operator=*from set*, 
>                                   left=comparison_expression, 
>                                   right=comparison_expression)
>  
> └─ comparison_expression
>     └─ additive_expression { ("<"|"<="|">"|">="|"=="|"!=") additive_expression }
>         └─> Ast.Expression.Binary(operator=*from set*, 
>                                   left=additive_expression, 
>                                   right=additive_expression)
>  
> └─ additive_expression
>     └─ multiplicative_expression { ("+"|"-") multiplicative_expression }
>         └─> Ast.Expression.Binary(operator=*from set*, 
>                                   left=multiplicative_expression, 
>                                   right=multiplicative_expression)
>              
> └─ multiplicative_expression
>     └─ secondary_expression { ("*"|"/") secondary_expression }
>         └─> Ast.Expression.Binary(operator=*from set*, 
>                                   left=secondary_expression, 
>                                   right=secondary_expression)
>  
> └─ secondary_expression
>     └─ primary_expression { "." identifier [ "(" [ expression { "," expression } ] ")" ] }
>         ├─ ".identifier"       ──> Ast.Expression.Access(receiver=primary_expression, 
>         │                                                name=identifier)
>         └─ ".identifier(args)" ──> Ast.Expression.Function(receiver=primary_expression, 
>                                                            name=identifier, 
>                                                            arguments=expression(s))
> └─ primary_expression
>     ├─ "NIL"  
>     │   └─> Ast.Expression.Literal(literal=null)
>     │
>     ├─ "TRUE" | "FALSE"                          
>     │   └─> Ast.Expression.Literal(literal=Boolean)
>     │
>     ├─ integer | decimal | character | string    
>     │   └─> Ast.Expression.Literal(literal=Number|Character|String)
>     │
>     ├─ "(" expression ")"                        
>     │   └─> Ast.Expression.Group(expression=expression)
>     │
>     ├─ identifier                                
>     │   └─> Ast.Expression.Access(receiver=Optional.empty, name=identifier)
>     │
>     └─ identifier "(" [ expression { "," expression } ] ")"
>         └─> Ast.Expression.Function(receiver=Optional.empty, 
>                                     name=identifier, 
>                                     arguments=expression(s))
>```