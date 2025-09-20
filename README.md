# FreshlyGround

FreshlyGround is a **novel programming language** whose source code is interpreted into **Java** for 
execution on the **Java Virtual Machine (JVM)**. This project was developed for **COP 4020** at 
the **University of Florida** and follows the methodology outlined in the 
book [*Crafting Interpreters*](https://www.craftinginterpreters.com/).

## Lexical Tokens
```regexp 
identifier ::= [A-Za-z_] [A-Za-z0-9_-]*
escape     ::= [\] [bnrt'"\]
operator   ::= [<>!=] '='? | 'any character'

integer    ::= [+-]?[0-9]+
decimal    ::= [+-]?[0-9]+\.[0-9]+
character  ::= ['] ([^'\\] | escape) [']
string     ::= ["] ([^"\n\r\\] | escape)* ["]
```

## Context Free Grammar

The context free grammar (CFG) of this project is presented in Extended Backus-Naur From (EBNF). This grammar enables 
a top-down (recursive-descent) method to parse the language in linear time. Starting as source code from the user
that is lexed (`Lexer.java`) into an array of tokens (`Token.java`), that will then be parsed (`Parser.java`) into an 
Abstract Syntax Tree (AST) (`Ast.java`)... 

**TODO: write the rest of this explanation**

The syntax map of this CFG is represented as:

*CFG* = (*Σ*, *N*, *P*, *S*), where:
- **Σ** – terminal symbols (array of tokens produced by the lexer)
- **N** – non-terminal symbols (see `source`, `statement`, `expression` below)
- **P** – production rules (right side of `::=`)
- **S** – start symbol (`source`), which constitutes the instantiation of the AST

Note: In the syntax rules below, each line should be read as `non-terminal symbol ::= production rule`.

### Syntax Rules
```ebnf
source                    ::= { field } { method }

field                     ::= "LET" identifier [ "=" expression ] ";"

method                    ::= "DEF" identifier "(" [ identifier { "," identifier } ] ")"
                            "DO" { statement } "END"

statement                 ::= "LET" identifier [ "=" expression ] ";"
                            | "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
                            | "FOR" identifier "IN" expression "DO" { statement } "END"
                            | "WHILE" expression "DO" { statement } "END"
                            | "RETURN" expression ";"
                            | expression [ "=" expression ] ";"

expression                ::= logical_expression

logical_expression        ::= comparison_expression 
                              { ( "AND" | "OR" ) comparison_expression }

comparison_expression     ::= additive_expression
                              { ( "<" | "<=" | ">" | ">=" | "==" | "!=" ) additive_expression }

additive_expression       ::= multiplicative_expression
                              { ( "+" | "-" ) multiplicative_expression }

multiplicative_expression ::= secondary_expression
                              { ( "*" | "/" ) secondary_expression }
                              
secondary_expression      ::= primary_expression
                              { "." identifier [ "(" [ expression { "," expression } ] ")" ] }

primary_expression        ::= "NIL" | "TRUE" | "FALSE"
                              | integer | decimal | character | string
                              | "(" expression ")"
                              | identifier [ "(" [ expression { "," expression } ] ")" ]
```

**Legend:**
- `{ … }` = zero or more
- `[ … ]` = optional (zero or one)
- `|` = alternative
- Keywords (`"LET"`, `"DEF"`, etc.) are case-sensitive

## CSG -> Code
```text
source ─> Ast.Source
 ├─ field ─> Ast.Field
 │    └─ "LET" identifier [ "=" expression ] ";"
 │        └─> Field(constant=true, name=identifier, value=expression)
 │ 
 └─ method ─> Ast.Method
      └─ "DEF" identifier "(" [ identifier { "," identifier } ] ")" "DO" { statement } "END"
          └─> Method(name=identifier, parameters=identifier(s), statements=statement)

statement ─> Ast.Statement.*
 ├─ "LET" identifier [ "=" expression ] ";"
 │    └─> Declaration(name=identifier, value=expression)
 │
 ├─ "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END" 
 │    └─> If(condition=expression, thenStatements=statement(s), elseStatements=statement(s))
 │
 ├─ "FOR" identifier "IN" expression "DO" { statement } "END"
 │    └─> For(initialization=Declaration(name=identifier, value=Optional.empty), 
 │            condition=expression, 
 │            increment=null, 
 │            statements=statement(s))
 │
 ├─ "WHILE" expression "DO" { statement } "END"
 │    └─> While(condition=expression, statements=statement(s))
 │
 ├─ "RETURN" expression ";"
 │    └─> Return(value=expression)
 │
 ├─ expression "=" expression ";"
 │    └─> Assignment(receiver=expression, value=expression)
 │
 └─ expression ";"
      └─> Expression(expression=expression)

expression ──> Ast.Expression.*
 ├─ logical_expression
 │    └─comparison_expression { ("AND"|"OR") comparison_expression }
 │        └─> Binary(operator=*from set*, left=comparison_expression, right=comparison_expression)
 │
 ├─ comparison_expression
 │    └─ additive_expression { ("<"|"<="|">"|">="|"=="|"!=") additive_expression }
 │        └─> Binary(operator=*from set*, left=additive_expression, right=additive_expression)
 │
 ├─ additive_expression
 │    └─ multiplicative_expression { ("+"|"-") multiplicative_expression }
 │        └─> Binary(operator=*from set*, left=multiplicative_expression, right=multiplicative_expression)
 │            
 ├─ multiplicative_expression
 │    └─ secondary_expression { ("*"|"/") secondary_expression }
 │        └─> Binary(operator=*from set*, left=secondary_expression, right=secondary_expression)
 │
 ├─ secondary_expression
 │    └─ primary_expression { "." identifier [ "(" [ expression { "," expression } ] ")" ] }
 │        ├─ ".identifier"       ──> Access(receiver=*previous token*, name=identifier)
 │        └─ ".identifier(args)" ──> Function(receiver=*previous token*, name=identifier, arguments=expression(s))
 │
 └─ primary_expression
      ├─ "NIL"  
      │   └─> Literal(literal=null)
      │
      ├─ "TRUE" | "FALSE"                          
      │   └─> Literal(literal=Boolean)
      │
      ├─ integer | decimal | character | string    
      │   └─> Literal(literal=Number|Character|String)
      │
      ├─ "(" expression ")"                        
      │   └─> Group(expression=expression)
      │
      ├─ identifier                                
      │   └─> Access(receiver=Optional.empty, name=identifier)
      │
      └─ identifier "(" [ expression { "," expression } ] ")"
          └─> Function(receiver=Optional.empty, name=identifier, arguments=expression(s))
```

### Examples:
#### Example 1:
```
LET x = 10;
```
In this example source code, as with all source code in this language, the entry point will be `source`. This will then
match the pattern `"LET" identifier "=" expression ";"`, where the `identifier` will map to `x`, and the `expression`
will follow the recursive chain: 

> `expression` → `logical_expression` → `comparison_expression` → `additive_expression` → `multiplicative_expression` 
> → `secondary_expression` → `primary_expression` → `integer`

This will result in `"LET" "x" "=" integer ";"` and `integer` will map to `10`. With the following AST:

```text
source
└── field
├── "LET"
├── identifier("x")
├── "="
├── expression
│   └── logical_expression
│       └── comparison_expression
│           └── additive_expression
│               └── multiplicative_expression
│                   └── secondary_expression
│                       └── primary_expression
│                           └── integer("10")
└── ";"
```

Getting this one step closer to the actual coded implementation for this project. The tree can be thought of as:

```yaml
Ast.Source
└─ fields: [
     Ast.Field
     ├─ name: "x"
     ├─ constant: true # LET → constant
     └─ value: Optional.of(
          Ast.Expression.Literal
          └─ literal: 10
        )
   ]
└─ methods: []
```

Finally, in Java, this can be written as:

```java
Ast ast =
    new Ast.Source(
        List.of(
            new Ast.Field(
                "x",
                true,
                Optional.of(new Ast.Expression.Literal(10))
            )
        ),
        List.of()
    );
```

#### Example 2:
```
DEF main() DO
    print("Hello", 42);
END
```
Again, start at `source`...

```yaml
Ast.Source
└─ fields: []
└─ methods: [
    Ast.Method
    ├─ name: "main"
    ├─ parameters: []
    └─ statements: [
        Ast.Statement.Expression
        └─ expression:
             Ast.Expression.Function
             ├─ receiver: Optional.empty
             ├─ name: "print"
             └─ arguments: [
                  Ast.Expression.Literal
                  ├─ literal: "Hello"
                  Ast.Expression.Literal
                  └─ literal: 42
              ]
        ]
    ]
```

```java
Ast program =
        new Ast.Source(
                List.of(), // no fields
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
                                                                new Ast.Expression.Literal("Hello"),
                                                                new Ast.Expression.Literal(42)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
```