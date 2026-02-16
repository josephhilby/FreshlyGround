# 01 — Language Syntax Specification

This document specifies the **FreshlyGround language syntax layer**: the formal grammar and token model that define 
how raw source text is transformed into a structured **Abstract Syntax Tree (AST)**.

Syntax defines **form, not meaning**. This layer guarantees that programs are *well-formed* according to the grammar, 
but does not assign types, resolve symbols, or enforce semantic correctness. Those responsibilities belong to the 
**Semantic Model** and **Bindings** stages.

---

## Navigation

* Index: [Overview & Index](00_index.md)
* Previous: N/A
* Next: [Abstract Syntax Tree (AST Map)](02_ast_map.md)

---

## Design Notes

### Syntax vs. Semantics

* **Syntax** answers: *Is this program structurally valid?*
* **Semantics** answers: *What does this program mean?*

The lexer and parser are intentionally blind to:

* type compatibility
* symbol resolution
* scope visibility
* constant evaluation

They only generate a token stream and ensure it can be reduced to a valid **AST shape** under the grammar rules.

### Grammar-Driven AST Construction

FreshlyGround uses **Extended Backus–Naur Form (EBNF)** to define a **context-free grammar (CFG)**  (i.e., the AST 
shape) that supports a top-down, recursive-descent parsing strategy.

The lexer:

* Consumes individual characters of source code to produce tokens


The parser:

* Consumes **terminal symbols (Σ)** produced by the lexer
* Expands **non-terminals (N)** using **production rules (P)**
* Builds a hierarchical tree rooted at the **start symbol (S = `source`)**

This yields an AST that preserves **program structure without semantic decoration**.

---

## Lexical Model (Tokens)

The lexer transforms raw characters into a typed token stream. These tokens form the terminal alphabet **Σ** of the grammar.

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

## Grammar Model (EBNF)

### Formal Definition

The grammar is defined as a 4-tuple:

**G = (Σ, N, P, S)**

Where:

* **Σ** — Terminal symbols (tokens)
* **N** — Non-terminal symbols (syntactic categories)
* **P** — Production rules (rule that defines a non-terminal)
* **S** — Start symbol (`source`; the starting point for all programs)

---

## Program Structure

### Top-Level Forms

```ebnf
non-terminal symbol       ::= production rule
---                           ---

source                    ::= { field } { method }

field                     ::= "LET" [ "CONST" ] identifier ":" identifier
                              [ "=" expression ] ";"

method                    ::= "DEF" identifier
                              "(" [ identifier ":" identifier
                              { "," identifier ":" identifier } ] ")"
                              [ ":" identifier ]
                              "DO"
                                { statement }
                              "END"
```

### Interpretation

* A program consists of **zero or more fields** followed by **zero or more methods**
* Fields declare global variables
* Methods define executable code blocks with optional return types

---

## Statements

Statements define **control flow and state transitions**. They do not evaluate to values.

```ebnf
statement ::= "LET" identifier [ ":" identifier ] [ "=" expression ] ";"

            | "IF" expression "DO"
                { statement }
              [ "ELSE"
                { statement } ]
              "END"

            | "FOR" "("
                [ identifier "=" expression ] ";"
                expression ";"
                [ identifier "=" expression ]
              ")"
              { statement }
              "END"

            | "WHILE" expression "DO"
                { statement }
              "END"

            | "RETURN" expression ";"

            | expression [ "=" expression ] ";"
```

### Notes

* Assignment is syntactically allowed as an **expression statement**
* Type enforcement is deferred to semantic analysis
* Control structures define **nested lexical scopes**

---

## Expressions

Expressions define **value computation**. All expressions reduce to a single value.

### Precedence Model

```ebnf
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

---

## Secondary Expressions (Dot Operator Syntax)

The grammar permits chained member expressions using the dot (`.`) operator.

### Forms

- `expression . identifier`
- `expression . identifier ( argument_list? )`

This rule defines only the syntactic structure of member access and invocation.
No validity checking or name resolution occurs at this stage.

---

## Example Derivation

### Source

```fg
LET x: Integer = 10;
```

### Token Stream

```text
{ "LET", "x", ":", "Integer", "=", "10", ";" }
```

### Grammar Match

```ebnf
field ::= "LET" identifier ":" identifier "=" expression ";"
```

### AST Shape

```text
Ast.Source
 └─ fields:
    Ast.Field
     ├─ name: "x"
     ├─ typeName: "Integer"
     ├─ constant: false
     └─ value:
        Ast.Expression.Literal
         └─ literal: 10
 └─ methods: []
```

At this stage:

* The type name `Integer` is **not resolved**
* The literal `10` is **not typed**
* No scope or symbol table exists

Those transformations occur in the **Semantic Model & Bindings** pass.

---

## Constraints

* Grammar is **case-sensitive**
* Keywords cannot be used as identifiers
* Operator precedence is strictly defined by grammar structure, not semantic rules

---

## Forward Links

* For AST node taxonomy and structural guarantees, see: **[AST Specification](02_ast_map.md)**
* For symbol resolution, scope, and type rules, see: **[Semantic Model & Bindings](03_semantics.md)**
* For how syntax flows through compiler passes, see: **[Compiler Pipeline](04_pipeline.md)**
