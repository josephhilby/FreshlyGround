# 03 — Structural Representation Specification

This document specifies the **FreshlyGround Abstract Syntax Tree (AST)**: the structured, intermediate representation
produced by the parser according to the previously discussed context-free grammar. The AST captures 
**program form without semantic meaning**. It encodes hierarchy, precedence, and grammatical structure.

---

## AST Root
All programs are parsed from a single root node: `Ast.Source`.

Grammar:
```ebnf
source ::= { field } { method }
```

AST mapping:
```text
Ast.Source
 ├─ fields   : Ast.Field[]
 └─ methods  : Ast.Method[]
```

* **Fields** define global storage
* **Methods** define executable entry points

---

## Top-Level Declarations

### Fields

Grammar:

```ebnf
field ::= "LET" [ "CONST" ] identifier ":" identifier [ "=" expression ] ";"
```

AST mapping:

```text
Ast.Field
 ├─ name       : String
 ├─ typeName   : String
 ├─ constant   : boolean
 └─ value      : Ast.Expression | null
```

### Methods

Grammar:

```ebnf
method ::= 
"DEF" identifier "(" [ identifier ":" identifier { "," identifier ":" identifier } ] ")" [ ":" identifier ] "DO"
    { statement }
"END"
```

AST mapping:

```text
Ast.Method
 ├─ name              : String
 ├─ parameters        : String[]
 ├─ parameterTypes    : String[]
 ├─ returnTypeName    : String | null
 └─ statements        : Ast.Statement[]
```

---

## Statements
### Assignment

Grammar:

```ebnf
expression "=" expression ";"
```

AST mapping:

```text
Ast.Statement.Assignment
 ├─ receiver : Ast.Expression
 └─ value    : Ast.Expression
```

### Expression Statement

Grammar:

```ebnf
expression ";"
```

AST mapping:

```text
Ast.Statement.Expression
 └─ expression : Ast.Expression
```

### Declaration

Grammar:

```ebnf
"LET" identifier [ ":" identifier ] [ "=" expression ] ";"
```

AST mapping:

```text
Ast.Statement.Declaration
 ├─ name      : String
 ├─ typeName  : String | null
 └─ value     : Ast.Expression | null
```

### Conditional

Grammar:

```ebnf
"IF" expression "DO"
    { statement }
[ "ELSE"
    { statement } ]
"END"
```

AST mapping:

```text
Ast.Statement.If
 ├─ condition       : Ast.Expression
 ├─ thenStatements  : Ast.Statement[]
 └─ elseStatements  : Ast.Statement[]
```

### For Loop

Grammar:

```ebnf
"FOR" "(" [ identifier "=" expression ] ";" expression ";" [ identifier "=" expression ] ")"
    { statement }
"END"
```

AST mapping:

```text
Ast.Statement.For
 ├─ initialization : Ast.Statement.Assignment | null
 ├─ condition      : Ast.Expression
 ├─ increment      : Ast.Statement.Assignment | null
 └─ statements     : Ast.Statement[]
```

### While Loop

Grammar:

```ebnf
"WHILE" expression "DO"
  { statement }
"END"
```

AST mapping:

```text
Ast.Statement.While
 ├─ condition  : Ast.Expression
 └─ statements : Ast.Statement[]
```

### Return

Grammar:

```ebnf
"RETURN" expression ";"
```

AST mapping:

```text
Ast.Statement.Return
 └─ value : Ast.Expression
```

---

## Expressions
### Binary Operations
All infix operators are normalized into a single binary node type.

AST mapping:

```text
Ast.Expression.Binary
 ├─ operator : Operator
 ├─ left     : Ast.Expression
 └─ right    : Ast.Expression
```

This node is used for:

* Logical operators: `AND`, `OR`
* Comparison operators: `<`, `<=`, `>`, `>=`, `==`, `!=`
* Arithmetic operators: `+`, `-`, `*`, `/`

---

## Member Access and Function Calls
These constructs unify under a **receiver-based model**.

### Access
Grammar fragment:

```ebnf
identifier
```
or
```ebnf
primary "." identifier
```

AST mapping:

```text
Ast.Expression.Access
 ├─ receiver : Ast.Expression | null
 └─ name     : String
```

* `receiver = null` represents an unqualified name
* `receiver != null` represents member access

Semantic resolution of this construct is defined in the next section.

### Function Call

Grammar fragment:

```ebnf
identifier "(" [ expression { "," expression } ] ")"
```
or
```ebnf
primary "." identifier "(" [ expression { "," expression } ] ")"
```

AST mapping:

```text
Ast.Expression.Function
 ├─ receiver  : Ast.Expression | null
 ├─ name      : String
 └─ arguments : Ast.Expression[]
```

* `receiver = null` represents an unqualified invocation
* `receiver != null` represents a qualified invocation

Semantic resolution of this construct is defined in the next section.

---

## Primary Expressions

### Literals

```text
Ast.Expression.Literal
 └─ literal : Object
```

Represents:

* `NIL` → `null`
* `TRUE` / `FALSE` → `Boolean`
* `integer` → `Integer`
* `decimal` → `Double`
* `character` → `Character`
* `string` → `String`

### Grouping

Grammar:

```ebnf
"(" expression ")"
```

AST mapping:

```text
Ast.Expression.Group
 └─ expression : Ast.Expression
```

---

## Navigation

* Index: [Overview & Index](./00_index.md)
* Previous: [Syntactic Definitions](./02_syntax.md)
* Next: [Semantic Model & Bindings](./04_semantics.md)