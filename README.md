<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/<your_repo>">
    <img src="images/banner.png" alt="Logo" width="80%">
  </a>

  <h3>
    A novel programming language for the Java Virtual Machine
  </h3>
</div>

---

<!-- TABLE OF CONTENTS -->
## Table of Contents
<ol>
  <li><a href="#about-the-project">About The Project</a></li>
  <li><a href="#design-and-theory">Design and Theory</a></li>
  <li><a href="#practical-implementation">Practical Implementation</a></li>
  <li><a href="#examples">Examples</a></li>
  <li><a href="#built-with">Built With</a></li>
</ol>

<!-- ABOUT THE PROJECT -->
## About The Project
FreshlyGround is a **novel programming language** whose source code is interpreted into **Java 21** for 
execution on the **Java Virtual Machine (JVM)**. This project was developed for **COP 4020** at 
the **University of Florida** and follows the methodology outlined in the 
book [*Crafting Interpreters*](https://www.craftinginterpreters.com/).

<!-- DESIGN AND THEORY -->
## Design and Theory
### The Pipeline
- Source Code → `Lexer.java` → Array of Tokens
- Array of Tokens → `Parser.java` → Abstract Syntax Tree (AST)
- AST → `Interpreter.java` → Scope Object

### Lexical Tokens
At the start of the pipeline is the lexer. This will take a source code file and lex it into an array of tokens following 
the rules below. 

```regexp 
identifier := [A-Za-z_] [A-Za-z0-9_-]*
operator   := [<>!=] =? | 'any character'

integer    := 0 | [+-]? [1-9] [0-9]*
decimal    := [+-]? [0-9]+ \. [0-9]+
character  := ^' ([^'\n\r\\] | 'escape') '$
string     := ^" ([^"\n\r\\] | 'escape')* "$
escape     := ^\\ [bnrt'"\\]$
```

### Context Free Grammar Tree
The context free grammar (CFG) of this project is presented in Extended Backus–Naur Form (EBNF).
It enables a top-down (recursive-descent) parser to run in linear time. This parser takes the original 
array of tokens (**Σ**) and places them as leaf nodes into a tree structure. This tree is assembled by
starting at a predetermined root (**S**) and placing each token by traversing and constructing internal
nodes (**N**) according to a set of rules (**P**).
 

#### Extended Backus–Naur Form
*CFG* = (*Σ*, *N*, *P*, *S*), where:
- **Σ** – terminal symbols (tokens produced by the lexer)
- **N** – non-terminal symbols (see `source`, `statement`, `expression` below)
- **P** – production rules (right side of `::=`)
- **S** – start symbol (`source`), which constitutes the instantiation of the AST

**Note:** 
In the syntax rules below, each line should be read as `non-terminal symbol ::= production rule`.

#### Syntax Rules
>```ebnf
>source                    ::= { field } { method }
>
>field                     ::= "LET" [ CONST ] identifier [ "=" expression ] ";"
>
>method                    ::= "DEF" identifier "(" [ identifier { "," identifier } ] ")"
>                            "DO" { statement } "END"
>```
>
>```ebnf
>statement                 ::= "LET" identifier [ "=" expression ] ";"
>                            | "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
>                            | "FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")" { statement } "END"
>                            | "WHILE" expression "DO" { statement } "END"
>                            | "RETURN" expression ";"
>                            | expression [ "=" expression ] ";"
>```
>
>```ebnf
>expression                ::= logical_expression
>
>logical_expression        ::= comparison_expression 
>                              { ( "AND" | "OR" ) comparison_expression }
>
>comparison_expression     ::= additive_expression
>                              { ( "<" | "<=" | ">" | ">=" | "==" | "!=" ) additive_expression }
>
>additive_expression       ::= multiplicative_expression
>                              { ( "+" | "-" ) multiplicative_expression }
>
>multiplicative_expression ::= secondary_expression
>                              { ( "*" | "/" ) secondary_expression }
>                              
>secondary_expression      ::= primary_expression
>                              { "." identifier [ "(" [ expression { "," expression } ] ")" ] }
>
>primary_expression        ::= "NIL" | "TRUE" | "FALSE"
>                              | integer | decimal | character | string
>                              | "(" expression ")"
>                              | identifier [ "(" [ expression { "," expression } ] ")" ]
>```
>
>**Legend:**
>- `{ … }` = zero or more
>- `[ … ]` = optional (zero or one)
>- `|` = alternative
>- Keywords (`"LET"`, `"DEF"`, etc.) are case-sensitive

### Interpretation to Java 21


<!-- PRACTICAL IMPLEMENTATION -->
## Practical Implementation
### AST Mapping Diagram
>```text
>source ─> Ast.Source(fields=field(s), methods=method(s))
>
>field
> └─ "LET" [ CONST ] identifier [ "=" expression ] ";"
>     └─> Ast.Field(constant=boolean, name=identifier, value=expression)
>  
> method
> └─ "DEF" identifier "(" [ identifier { "," identifier } ] ")" "DO" { statement } "END"
>     └─> Ast.Method(name=identifier, parameters=identifier(s), statements=statement(s))
>```
>
>```text
>statement
> ├─ "LET" identifier [ "=" expression ] ";"
> │   └─> Ast.Statement.Declaration(name=identifier, value=expression)
> │
> ├─ "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END" 
> │   └─> Ast.Statement.If(condition=expression, 
> │                        thenStatements=statement(s), 
> │                        elseStatements=statement(s))
> │
> ├─ "FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")" 
> │   │                     { statement } "END"
> │   └─> Ast.Statement.For(initialization=Declaration(name=identifier, 
> │                                                    value=Optional.empty
> │                                                    ), 
> │                         condition=expression, 
> │                         increment=null, 
> │                         statements=statement(s))
> │
> ├─ "WHILE" expression "DO" { statement } "END"
> │   └─> Ast.Statement.While(condition=expression, statements=statement(s))
> │
> ├─ "RETURN" expression ";"
> │   └─> Ast.Statement.Return(value=expression)
> │
> ├─ expression "=" expression ";"
> │   └─> Ast.Statement.Assignment(receiver=expression, value=expression)
> │
> └─ expression ";"
>     └─> Ast.Statement.Expression(expression=expression)
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

## Examples
### Example 1:
```
LET x = 10;
```
In this example source code, as with all source code in this language, the entry point will be `source`. This will then
match the `field` pattern `"LET" identifier "=" expression ";"`, where the `identifier` will map to `x`, and the `expression`
will follow the recursive chain: 

> `expression` → `logical_expression` → `comparison_expression` → `additive_expression` → `multiplicative_expression` 
> → `secondary_expression` → `primary_expression` → `integer`

This will result in `"LET" "x" "=" integer ";"` and `integer` will map to `10`. With the following AST:

```text
source
└─ field
    ├─ "LET"
    ├─ identifier("x")
    ├─ "="
    ├─ expression
    │   └─ logical_expression
    │       └─ comparison_expression
    │           └─ additive_expression
    │               └─ multiplicative_expression
    │                   └─ secondary_expression
    │                       └─ primary_expression
    │                           └─ integer("10")
    └─ ";"
```

Getting this one step closer to the actual coded implementation for this project. The tree can be thought of as:

```yaml
Ast.Source
└─ fields: [
    Ast.Field
    ├─ name: "x"
    ├─ constant: false
    └─ value: Optional.of(
        Ast.Expression.Literal
        └─ literal: 10
        )
    ]
└─ methods: []
```

Finally, in code, this can be structured as:

```java
Ast ast =
    new Ast.Source(
        List.of(
            new Ast.Field(
                "x",
                false,
                Optional.of(new Ast.Expression.Literal(BigInteger.TEN))
            )
        ),
        List.of()
    );
```

This structure can then be interpreted by traversing the tree. Starting at the source and recursively moving down, the 
interpreter constructs a scope object:

```java
Scope{ parent    = Scope{ }, 
       variables = { x = Variable{
                                   name  = 'x', 
                                   value = Object{ 
                                                   scope = Scope{ }, 
                                                   value = 10 
                                                  } 
                                 } 
       },
       functions = {}
}
```
**Note:** `Scope{ parent = null, variables = {}, functions = {} }` has been shortened to `Scope{ }` for readability.

### Example 2:
```
DEF main() DO
    print("Hello Wrold");
END
```
Again, start at `source`, however this time the `method` pattern will be matched. As before the tokens will match to their respective
parts, and the AST will be constructed.

```yaml
Ast.Source
├─ fields: []
└─ methods: [
    Ast.Method
    ├─ name: "main"
    ├─ parameters: []
    └─ statements: [
        Ast.Statement.Expression
        └─ expression: Ast.Expression.Function
            ├─ receiver: Optional.empty
            ├─ name: "print"
            └─ arguments: [
                Ast.Expression.Literal,
                └─ literal: "Hello World"
                ]
        ]
    ]
```

```java
Ast program =
    new Ast.Source(
        List.of(),
        List.of(
            new Ast.Method(
                "main",
                List.of(),
                List.of(
                    new Ast.Statement.Expression(
                        new Ast.Expression.Function(
                            Optional.empty(),
                            "print",
                            List.of(
                                new Ast.Expression.Literal("Hello World")
                            )
                        )
                    )
                )
            )
        )
    );
```

The AST can then be interpreted and a scope object constructed:

```java
-> NIL,
Scope{ parent=Scope{parent=null, variables={}, functions={} }, 
       variables={name=Variable{name='x', value=Object{scope=Scope{parent=null, variables={}, functions={}}, value=10}}},
       functions={print/1=Function{name='print', arity=1, function=plc.project.Interpreter$$Lambda/0x000000b801103ac8@4659191b}}}}
}
```