<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/<your_repo>">
    <img src="assets/banner.png" alt="Logo" width="80%">
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
FreshlyGround is a **novel programming language compiler** whose source code is compiled into bytecode
for execution on the **Java Virtual Machine (JVM)**. This project was adapted from an academic compiler
project created in **COP 4020** at the **University of Florida**, and follows the methodology  outlined in the 
book [*Crafting Interpreters*](https://www.craftinginterpreters.com/).

The compiler is structured as a sequence of well-defined, ordered, single-responsibility passes. Each pass performs 
a distinct transformation on the program representation, following a clear separation of concerns between tokenization, 
syntactic analysis, semantic analysis, and bytecode generation.

### Compilation Pipeline (Passes)

> - **Tokenization**
>   *Source Code* → `Lexer.java` → *Token Stream*
>
> - **Syntactic Analysis**  
>   *Token Stream* → `Parser.java` → *Abstract Syntax Tree (AST)*
>
> - **Semantic Analysis**  
>   *AST* → `Analyzer.java` → *Decorated AST*
>
> - **Bytecode Generation**  
>   *Decorated AST* → `Generator.java` → *Java Bytecode*

Digging a bit deeper:

The lexer (`Lexer.java`) performs lexical analysis (or tokenization), converting raw characters into a stream of typed tokens while preserving 
positional information. The parser (`Parser.java`) iterates over the token stream, validating the program syntax against the 
language grammar and constructs a hierarchical Abstract Syntax Tree (AST) that captures the program's syntactic structure, 
without interpreting semantics or types. 

The analyzer (`Analyzer.java`) then traverses the AST and applies the languages scoping and environment rules,
performing semantic analysis such as name resolution, type checking, and local type inference for untyped declarations. 
This pass 'decorates' the AST with resolved symbols and concrete types without altering the original syntax. 

Finally, the generator (`Generator.java`) lowers the fully analyzed program into executable JVM bytecode, relying on 
the decorated AST to ensure all identifiers, scopes, and types are resolved prior to code generation.

<!-- DESIGN AND THEORY -->
## Design and Theory
### Token Definitions
At the start is the lexer. This will take a source code file and lex it into an array of tokens (Token Stream) as
defined in the following the rules: 

```regexp 
identifier := [A-Za-z_] [A-Za-z0-9_-]*
operator   := [<>!=] =? | 'any character'

integer    := 0 | [+-]? [1-9] [0-9]*
decimal    := [+-]? [0-9]+ \. [0-9]+
character  := ^' ([^'\n\r\\] | 'escape') '$
string     := ^" ([^"\n\r\\] | 'escape')* "$
escape     := ^\\ [bnrt'"\\]$
```

### Context Free Grammar Syntax Tree
After lexical analysis, the token stream is passed to the parser. The parser transforms the linear sequence 
of tokens into a structured representation of the program known as the Abstract Syntax Tree (AST). AST construction 
is governed by a context-free grammar (CFG) that defines the syntactic structure of the language. 

This project defines its grammar using Extended Backus–Naur Form (EBNF). EBNF was selected because it naturally 
supports a top-down (recursive-descent) parsing strategy with linear-time complexity. 

This parser begins at a predetermined start symbol (**S**) and incrementally constructs the AST by:
- Consuming a terminal symbols (**Σ**) produced by the lexer, and placing it as leaf nodes.
- Creating non-terminal symbols (**N**) according to the CFG production rules (**P**), and placing them as internal nodes.

The resulting AST captures the hierarchical structure of the program and serves as the input to subsequent 
semantic analysis and code generation stages.

#### Extended Backus–Naur Form Definition
*EBNF* := (*Σ*, *N*, *P*, *S*), where:
- **Σ** – terminal symbols (tokens produced by the lexer)
- **N** – non-terminal symbols (see `source`, `statement`, `expression` below)
- **P** – production rules (right side of `::=`)
- **S** – start symbol (`source`), which constitutes the instantiation of the AST

#### Syntax Rules
>```ebnf
>non-terminal symbol       ::= production rule
>---                           ---
>source                    ::= { field } { method }
>
>field                     ::= "LET" [ CONST ] identifier ":" identifier [ "=" expression ] ";"
>
>method                    ::= "DEF" identifier "(" [ identifier ":" identifier { "," identifier ":" identifier } ] ")"
>                            [ ":" identifier ] "DO" { statement } "END"
>```
>
>```ebnf
>statement                 ::= "LET" identifier [ ":" identifier ] [ "=" expression ] ";"
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

### Semantic Model
FreshlyGround follows a static, early-binding semantic model, similar to Java. The compiler resolves names, 
types, and scopes at compile time in order to produce portable and efficient JVM bytecode. Runtime execution is 
delegated entirely to the JVM.

#### Definitions
>- Name := a symbolic identifier in the source program (e.g., x)
>- Value := the data or object a name refers to at runtime (e.g., 10)
>- Binding := the association between a name and its meaning (i.e., Environment.Type/Variable/Function)
>- Binding Time := the point at which the binding is created
>- Lifetime := period of time from binding creation to destruction
>- Scope := region of the program where a binding is visible

#### Binding Times
Bindings are established progressively throughout the compilation process:

> 1. Design: language grammar, syntax rules, and primitive type definitions
> 2. Implementation: Mapping language-level types to JVM representations (Environment.Type)
> 3. Writing: Variable and function declarations in source code
> 4. Compile: Name resolution, scope construction, and type checking (Environment.Variable/Function)

Runtime value binding is left to the JVM.

#### Scope Rules
FreshlyGround enforces lexical (static) scoping with the following rules:

> 1. Declarations bind names in the current scope
> 2. References are resolved by walking outward through parent scopes
> 3. Redeclaration outside same scope (Shadowing) is allowed.
> 4. Redeclaration in same scope is not allowed.

<!-- PRACTICAL IMPLEMENTATION -->
## Practical Implementation
### AST Mapping Diagram
>```text
>source ─> Ast.Source(fields=field(s), methods=method(s))
>
>field
> └─ "LET" [ CONST ] identifier ":" identifier [ "=" expression ] ";"
>     └─> Ast.Field(name=identifier, typeName=identifier, constant=bool, value=expression)
>  
> method
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
> │   └─> Ast.Statement.Expression(expression=expression)
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

## Example
```
LET x: Integer = 10;
```
Before getting too far into the weeds, note that this snippet would fail to compile because 
FreshlyGround requires a `main()` method as an entry point. This example will ignore that and remain intentionally minimal 
in order to illustrate the compilation pipeline on a small, readable input.

### Syntactic Analysis (Lexing and Parsing)
The lexer tokenizes the source code into a typed token stream. For this input, the relevant tokens correspond to:

`token stream = { "LET", "x", ":", "Integer", "=", "10", ";" }`

Because the token stream is syntactically valid, the parser matches it to the field production: 

```ebnf
field ::= "LET" [ CONST ] identifier ":" identifier [ "=" expression ] ";"
``` 

With the field identifier mapping to `x`, the declared type mapping to `Integer`, and the initialized expression 
to the literal `10`, through the following precedence chain:

> `expression` → `logical_expression` → `comparison_expression` → `additive_expression` → `multiplicative_expression` 
> → `secondary_expression` → `primary_expression` → `integer`

A simplified tree view of this mapping would look like:

```text
field
 ├─ "LET"
 ├─ identifier("x")
 ├─ ":"
 ├─ identifier("Integer")
 ├─ "="
 ├─ expression
 │   └─ logical_expression
 │       └─ ...
 │           └─ integer("10")
 └─ ";"
```

At this time the parser has enforced only syntax. While the field type (`Integer`) and expression literal type 
(`integer`) do match -- that is to say the statement is semantically correct -- if they did not, the parser would 
not care. That will be checked later.

All programs in FreshlyGround parse from the source entry point. Conceptually, this example becomes a `source` 
node containing a single field and no methods:

```yaml
Ast.Source
 └─ fields: [
    Ast.Field
     ├─ name: "x"
     ├─ typeName: "Integer"
     ├─ constant: false
     └─ value:
        Ast.Expression.Literal
         ├─ literal: 10
         └─ type: null
     └─ variable: null
     ]
 └─ methods: []
```

### Semantic Analysis (Analyzing)
The analyzer performs a pre-order traversal over the AST and applies the language’s scope and environment rules. 
Where the scope will define visibility and environment will define meaning. During this pass, it:
- Declares `x` in the current scope as an `Environment.Variable`
- Resolves the declared type to a concrete `Environment.Type`
- Infers and assigns the literal type to a concrete `Environment.Type`.
- Attaches the variable and type metadata onto the AST ("Decorates").

```java
// Current Scope
Scope{ parent=null, 
       variables={ "x" -> Environment.Variable(
           name="x",
           jvmName="x",
           constant=false,
           type=Environment.Type(
               name="Integer", 
               jvmName="int",
               // triple nested as `Integer` has a scope chain (Integer → Comparable → Any)
               scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
       )},
       functions={}
}
```

```yaml
Ast.Source
 └─ fields: [
    Ast.Field
     ├─ name: "x"
     ├─ typeName: "Integer"
     ├─ constant: false
     └─ value:
        Ast.Expression.Literal
         ├─ literal: 10
         └─ type:
            Environment.Type
             ├─ name: "Integer"
             ├─ jvmName: "int"
             └─ scope: Integer.scope ⊆ Comparable.scope ⊆ Any.scope
     └─ variable:
        Environment.Variable
         ├─ name: "x"
         ├─ jvmName: "x"
         ├─ constant: false
         └─ type:
            Environment.Type
             ├─ name: "Integer"
             ├─ jvmName: "int"
             └─ scope: Integer.scope ⊆ Comparable.scope ⊆ Any.scope
    ]
 └─ methods: []
```