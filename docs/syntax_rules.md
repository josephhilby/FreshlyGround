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
- Consuming a terminal symbol (**Σ**) produced by the lexer, and placing it as leaf nodes.
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