# 02 — Abstract Syntax Tree (AST) Specification

This document specifies the **FreshlyGround Abstract Syntax Tree (AST)**: the structured, intermediate representation 
produced by the parser and consumed by the semantic analyzer and code generators.

The AST captures **program form without program meaning**. It encodes hierarchy, precedence, and grammatical structure, 
but does not resolve symbols, assign types, or enforce semantic rules. Those responsibilities are externalized into 
the **Bindings** and **Semantic Model** layers.

---

## Related

* [Language Syntax Specification](./01_syntax.md)
* [Semantic Model & Bindings](./03_semantics.md)
* [Compiler Pipeline](./04_pipeline.md)

---

## Design Notes

### Syntax Trees vs. Semantic Models

* The **AST** represents *what was written* — structure and nesting derived directly from grammar rules.
* The **Semantic Model** represents *what it means* — types, variables, scopes, and function resolution.

FreshlyGround enforces a strict separation:

* AST nodes are **pure data structures**
* All semantic information is stored externally in the **Bindings** table

This ensures that:

* The same AST can be reused across multiple backends
* Semantic passes can be rerun or replaced without rebuilding syntax
* Code generation remains a mechanical lowering step

#### Resolution Model

Given:

    object.member

1. Evaluate `object` to type **T**
2. Resolve `member` inside the **type scope of T**

If resolution fails, this is a semantic error.

#### Invocation Semantics

For method calls:

    object.method(a, b)

The expression is lowered to:

    method(object, a, b)

The implicit receiver (`object`) is inserted as the first argument.

This implies:

- Declared method arity = N
- Call-site arity = N − 1 (receiver is implicit)

---

## AST Root

All programs are parsed from a single root node: `Ast.Source`.

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
field ::= "LET" [ "CONST" ] identifier ":" identifier
          [ "=" expression ] ";"
```

AST mapping:

```text
Ast.Field
 ├─ name       : String
 ├─ typeName  : String
 ├─ constant  : boolean
 └─ value     : Ast.Expression | null
```

### Methods

Grammar:

```ebnf
method ::= "DEF" identifier
           "(" [ identifier ":" identifier
           { "," identifier ":" identifier } ] ")"
           [ ":" identifier ]
           "DO"
             { statement }
           "END"
```

AST mapping:

```text
Ast.Method
 ├─ name              : String
 ├─ parameters       : String[]
 ├─ parameterTypes  : String[]
 ├─ returnTypeName : String | null
 └─ statements      : Ast.Statement[]
```

---

## Statements

Statements encode **control flow and state mutation**. They do not produce values.

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
 ├─ typeName : String | null
 └─ value    : Ast.Expression | null
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
 ├─ thenStatements : Ast.Statement[]
 └─ elseStatements : Ast.Statement[]
```

### For Loop

Grammar:

```ebnf
"FOR" "("
  [ identifier "=" expression ] ";"
  expression ";"
  [ identifier "=" expression ]
")"
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

Expressions form a **tree of value-producing operations**. All expressions reduce to a single value.

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

### Function Call

Grammar fragment:

```ebnf
primary "." identifier "(" [ expression { "," expression } ] ")"
identifier "(" [ expression { "," expression } ] ")"
```

AST mapping:

```text
Ast.Expression.Function
 ├─ receiver  : Ast.Expression | null
 ├─ name      : String
 └─ arguments : Ast.Expression[]
```

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

## Structural Guarantees

The AST enforces the following invariants:

* All operator precedence is **fully encoded in the tree shape**
* Parentheses do not survive beyond the `Group` node
* All identifiers are preserved as **raw names** (unresolved)
* No semantic information is embedded in any node

These guarantees allow the semantic analyzer to operate as a **pure decoration pass** rather than a structural rewrite.

---

## Forward Links

* For symbol resolution, scope, and type attachment, see: **[Semantic Model & Bindings](./03_semantics.md)**
* For how AST nodes flow through compiler passes, see: **[Compiler Pipeline](./04_pipeline.md)**
* For execution lowering rules, see: **[JVM Backend](./05_jvm_backend.md)** and **[WebAssembly Backend](./06_wasm_backend.md)**
