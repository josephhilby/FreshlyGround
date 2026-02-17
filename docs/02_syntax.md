# 02 — Language Syntax Specification

This document specifies the **syntax layer** of the FreshlyGround language: a formal grammatical model describing 
how valid programs are structured.

The syntax layer specifies:
- the lexical structure of tokens
- the context-free grammar governing program form
- the structural categories of program constructs

---

## Grammatical Model (EBNF)
FreshlyGround uses **Extended Backus–Naur Form (EBNF)** to define its **context-free grammar (CFG)** over a set of 
terminal symbols (tokens). The grammar describes how tokens may be combined into well-formed syntactic constructs.
It serves as the formal specification for the parser.

### Formal Definition

The grammar is defined as a 4-tuple:

**G = (Σ, N, P, S)**

Where:

* **Σ** — Terminal symbols (tokens)
* **N** — Non-terminal symbols (syntactic categories)
* **P** — Production rules (rule that defines a non-terminal)
* **S** — Start symbol (`source`; the starting point for all programs)

---

## Lexical Model (Tokens)
The lexical layer partitions raw source text into a sequence of tokens. Tokens form the terminal alphabet (Σ) 
of the grammar.

### Token Classes

```regexp
identifier := [A-Za-z_] [A-Za-z0-9_-]*

operator   := [<>!=] =? | 'any character'

integer    := 0 | [+-]? [1-9] [0-9]*
decimal    := [+-]? [0-9]+ \. [0-9]+

character  := ^' ([^'\n\r\\] | 'escape') '$
string     := ^" ([^"\n\r\\] | 'escape')* "$

escape     := ^\\ [bnrt'"\\]$
```

### Notes

* Keywords (`LET`, `DEF`, `IF`, etc.) are lexed as identifiers and promoted to keyword tokens during parsing
* Whitespace and comments are not preserved in the AST
* All tokens retain **source position metadata** for error reporting

---

## Program Structure

The **non-terminal symbols (N)** of FreshlyGround can be broken down into three categories:
* **Top level forms** — define program structure and global declarations.
* **Statements** — define control flow and state transitions; they do not evaluate to values.
* **Expressions** — define value-producing constructs; every expression reduces to a single value.

These categories partition the grammar into compositional layers of structure.

### Start Symbol
The start symbol `source` represents a complete program.

```ebnf
non-terminal symbol ::= production rule
---                           ---
source              ::= { field } { method }
```
>**Legend:**
>- `{ … }` = zero or more
>- `[ … ]` = optional (zero or one)
>- `|` = alternative
>- Keywords (`"LET"`, `"DEF"`, etc.) are case-sensitive

### Top-Level Forms

``` ebnf
field  ::= "LET" [ "CONST" ] identifier ":" identifier
           [ "=" expression ] ";"

method ::= "DEF" identifier
           "(" [ identifier ":" identifier { "," identifier ":" identifier } ] ")" [ ":" identifier ]
           "DO"
             { statement }
           "END"
```

### Statements

``` ebnf
statement ::=
      "LET" identifier [ ":" identifier ] [ "=" expression ] ";"
    | "IF" expression "DO" { statement } [ "ELSE" { statement } ] "END"
    | "FOR" "("[ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")"
                { statement }
              "END"
    | "WHILE" expression "DO" { statement } "END"
    | "RETURN" expression ";"
    | expression [ "=" expression ] ";"
```

### Expressions
Expressions are defined using a precedence hierarchy.

``` ebnf
expression ::= logical_expression

logical_expression ::=
    comparison_expression
    { ( "AND" | "OR" ) comparison_expression }

comparison_expression ::=
    additive_expression
    { ( "<" | "<=" | ">" | ">=" | "==" | "!=" )
      additive_expression }

additive_expression ::=
    multiplicative_expression
    { ( "+" | "-" ) multiplicative_expression }

multiplicative_expression ::=
    secondary_expression
    { ( "*" | "/" ) secondary_expression }

secondary_expression ::=
    primary_expression
    { "." identifier
      [ "(" [ expression { "," expression } ] ")" ] }

primary_expression ::=
      "NIL"
    | "TRUE"
    | "FALSE"
    | integer
    | decimal
    | character
    | string
    | "(" expression ")"
    | identifier
      [ "(" [ expression { "," expression } ] ")" ]
```

#### Secondary Expressions (Dot Operator Syntax)

The grammar permits chained member expressions using the dot (`.`) operator.

##### Forms

- `expression . identifier`
- `expression . identifier ( argument_list? )`

---

## Example Derivation
For example:

```fg
LET x: Integer = 10;
```

### Token Stream
This code would lex to the following token stream:

```text
{ "LET", "x", ":", "Integer", "=", "10", ";" }
```

Which would match with the following top level form:

```ebnf
field ::= "LET" identifier ":" identifier "=" expression ";"
```

---

## Navigation

* Index: [Overview & Index](./00_index.md)
* Previous: [Compiler Pipeline](./01_pipeline.md)
* Next: [Structural Representation](./03_struct_rep.md)