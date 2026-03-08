# 03 — Structural Representation Specification

This document specifies the **FreshlyGround Abstract Syntax Tree (AST)**: the structured, intermediate representation
produced by the parser according to the previously discussed context-free grammar. The AST captures 
**program form without semantic meaning**. It encodes hierarchy, precedence, and grammatical structure.

Note: Some of the EBNF definitions have been modified to better show how the map to their respective AST class.

---

## AST Root
All programs are parsed from a single root node: `Ast.Source`.

Grammar:

```ebnf
source ::= { fields } { methods }
```
>**Legend:**
>- `{ … }` = zero or more
>- `[ … ]` = optional (zero or one)
>- `|` = alternative
>- Keywords (`"LET"`, `"DEF"`, etc.) are case-sensitive

AST mapping:

```yaml
Ast.Source
 ├─ fields  : List<Ast.Field>
 └─ methods : List<Ast.Method>
```

* **Fields** define global storage
* **Methods** define executable entry points

---

## Top-Level Declarations

### Fields

Grammar:

```ebnf
field ::= "LET" [ "CONST" ] name ":" type [ "=" value ] ";"
```

AST mapping:

```yaml
Ast.Field
 ├─ name     : String
 ├─ typeName : String
 ├─ constant : boolean
 └─ value    : Optional<Ast.Expression>
```

### Methods

Grammar:

```ebnf
method ::= 
"DEF" name "(" [ param ":" paramType ] ")" [ ":" returnType ] "DO"
    { statements }
"END"
```

AST mapping:

```yaml
Ast.Method
 ├─ name           : String
 ├─ parameters     : List<String>
 ├─ parameterTypes : List<String>
 ├─ returnTypeName : Optional<String>
 └─ statements     : List<Ast.Statement>
```

---

## Statements
### Declaration

Grammar:

```ebnf
statement_declaration ::= "LET" name [ ":" type ] [ "=" value ] ";"
```

AST mapping:

```yaml
Ast.Statement.Declaration
 ├─ name     : String
 ├─ typeName : Optional<String>
 └─ value    : Optional<Ast.Expression>
```

### Assignment

Grammar:

```ebnf
statement_assignment ::= receiver "=" value ";"
```

AST mapping:

```yaml
Ast.Statement.Assignment
 ├─ receiver : Ast.Expression.Access
 └─ value    : Ast.Expression
```

### Expression

Grammar:

```ebnf
statement_expression ::= expression ";"
```

AST mapping:

```yaml
Ast.Statement.Expression
 └─ expression : Ast.Expression.Function
```

### Conditional

Grammar:

```ebnf
statement_if ::=
"IF" condition "DO"
    { thenStatements }
[ "ELSE"
    { elseStatements } ]
"END"
```

AST mapping:

```yaml
Ast.Statement.If
 ├─ condition      : Ast.Expression
 ├─ thenStatements : List<Ast.Statement>
 └─ elseStatements : List<Ast.Statement>
```

### For Loop

Grammar:

```ebnf
statement_for ::=
"FOR" "(" [ initialization ] ";" condition ";" [ increment ] ")"
    { statements }
"END"
```

AST mapping:

```yaml
Ast.Statement.For
 ├─ initialization : Optional<Ast.Statement.Assignment>
 ├─ condition      : Ast.Expression
 ├─ increment      : Optional<Ast.Statement.Assignment>
 └─ statements     : List<Ast.Statement>
```

### While Loop

Grammar:

```ebnf
statement_while ::=
"WHILE" condition "DO"
  { statements }
"END"
```

AST mapping:

```yaml
Ast.Statement.While
 ├─ condition  : Ast.Expression
 └─ statements : List<Ast.Statement>
```

### Return

Grammar:

```ebnf
statement_return ::= "RETURN" value ";"
```

AST mapping:

```yaml
Ast.Statement.Return
 └─ value : Ast.Expression
```

---

## Expressions
### Binary Operations
All infix operators are normalized into a single binary node type.

Grammar:

```ebnf
expression_binary ::= left operator right
```

AST mapping:

```yaml
Ast.Expression.Binary
 ├─ operator : String
 ├─ left     : Ast.Expression
 └─ right    : Ast.Expression
```

Infix Operators:

* Logical operators: { `AND`, `OR` }
* Comparison operators: { `<`, `<=`, `>`, `>=`, `==`, `!=` }
* Arithmetic operators: { `+`, `-`, `*`, `/` }

### Member Access and Function Calls
Please note that in the CFG, these were split among primary- and secondary-expressions. However, 
both are unified here under a **receiver-based model**, where:

* `receiver = null` represents an unqualified name
* `receiver != null` represents member access

Semantic resolution of this construct is defined in the next section.

#### Member Access
Grammar fragment:

```ebnf
expression_access ::= [ receiver "." ] name
```

AST mapping:

```yaml
Ast.Expression.Access
 ├─ receiver : Optional<Ast.Expression>
 └─ name     : String
```

#### Function Call

Grammar fragment:

```ebnf
expression_function ::= [ receiver "." ] name "(" [ arguments ] ")"
```

AST mapping:

```yaml
Ast.Expression.Function
 ├─ receiver  : Optional<Ast.Expression>
 ├─ name      : String
 └─ arguments : List<Ast.Expression>
```

### Primary Expressions
#### Grouping

Grammar:

```ebnf
expression_group ::= "(" expression ")"
```

AST mapping:

```yaml
Ast.Expression.Group
 └─ expression : Ast.Expression
```

#### Literals

```ebnf
expression_literal ::= literal
```

AST mapping:

```yaml
Ast.Expression.Literal
 └─ literal : Object
```

Type Map (literal → java object):

* `"NIL"`                → `null`
* (`"TRUE"` | `"FALSE"`) → `Boolean`
* `integer`              → `BigInteger`
* `decimal`              → `BigDecimal`
* `character`            → `Character`
* `string`               → `String`

---

## Navigation

* Next: [Semantic Model & Bindings](./04_semantics.md)
* Previous: [Syntactic Definitions](./02_syntax.md)
* Index: [Overview & Index](./00_index.md)
