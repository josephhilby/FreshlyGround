# 04 — Semantic Model & Bindings Specification (draft)

This document specifies the **FreshlyGround semantic layer**: the rules that assign *meaning* to
syntactically valid programs by resolving names, enforcing types, and constructing a scoped binding environment
over the Abstract Syntax Tree (AST).

Semantics define **what programs mean and how they are interpreted**. This layer
guarantees that all identifiers, expressions, and control structures are **well-typed, well-scoped, and resolvable**
before any backend lowers the program into an executable form.

---

## Scope Model

FreshlyGround enforces **lexical (static) scoping**.

### Rules

1. Declarations bind names in the **current scope**
2. References resolve by walking outward through **parent scopes**
3. **Shadowing is allowed** across nested scopes
4. **Redeclaration in the same scope is forbidden**

### Conceptual Structure

```text
Scope
 ├─ parent     : Scope | null
 ├─ variables  : Map<String, Environment.Variable>
 └─ functions  : Map<String, Environment.Function>
```
---

## Semantic Model


---

---

## Bindings Table

Bindings form a **global association** between AST nodes and their resolved semantic meaning.

### Conceptual Model

```text
Bindings
 ├─ field       : Map<Ast.Field: Environment.Variable>
 ├─ declaration : Map<Ast.Statement.Declaration: Environment.Variable>
 ├─ access      : Map<Ast.Expression.Access: Environment.Variable>
 │
 ├─ method      : Map<Ast.Method → Environment.Function>
 ├─ function    : Map<Ast.Expression.Function: Environment.Function>
 │
 └─ type        : Map<Ast.Expression: Environment.Type>
```

---

## Semantic Rules (Small to Large)

This section specifies **semantic constraints** enforced by the Analyzer. These rules are applied during a pre-order traversal of the AST and recorded in the Bindings table.

Notation:

* **[Rule]** — semantic restriction
* **(T: …)** — type constraint
* **(Node: …)** — AST shape constraint

---

## Program-Level Rules

### Source

```text
Ast.Source
 ├─ fields  : Ast.Field[]
 └─ methods : Ast.Method[]
```

Rules:

* **[Rule]** `main/0` must exist in the global scope
* **[Rule]** `main/0` must return `Integer`

---

## Declarations

### Field

```text
Ast.Field
 ├─ name      : String
 ├─ typeName : String
 ├─ constant : boolean
 └─ value    : Ast.Expression | null
```

Rules:

* **[Rule]** If `constant=true`, `value` must be present
* **[Rule]** If `value` is present, **(T: value.type assignable to declared typeName)**
* **[Rule]** Declares an `Environment.Variable` in the current scope
* **[Rule]** Attaches the variable binding to this AST node

### Method

```text
Ast.Method
 ├─ name              : String
 ├─ parameters       : String[]
 ├─ parameterTypes  : String[]
 ├─ returnTypeName : String | null
 └─ statements      : Ast.Statement[]
```

Rules:

* **[Rule]** `parameterTypes` must resolve to `Environment.Type`
* **[Rule]** `returnTypeName` defaults to `Nil` if omitted
* **[Rule]** Declares an `Environment.Function` in the current scope
* **[Rule]** Method body is analyzed in a **new nested scope**
* **[Rule]** Each `RETURN` must satisfy **(T: returnExpr.type assignable to method returnType)**

---

## Statements

### Assignment

```text
Ast.Statement.Assignment
 ├─ receiver : Ast.Expression
 └─ value    : Ast.Expression
```

Rules:

* **[Rule]** **(Node: receiver must be `Ast.Expression.Access`)**
* **[Rule]** Receiver must not be constant
* **[Rule]** **(T: value.type assignable to receiver.type)**

### Expression Statement

```text
Ast.Statement.Expression
 └─ expression : Ast.Expression
```

Rules:

* **[Rule]** **(Node: expression must be `Ast.Expression.Function`)**

### Declaration

```text
Ast.Statement.Declaration
 ├─ name      : String
 ├─ typeName : String | null
 └─ value    : Ast.Expression | null
```

Rules:

* **[Rule]** Must specify a type or a value (cannot omit both)
* **[Rule]** If `value` present, inferred type = `value.type`
* **[Rule]** If `typeName` present, it must resolve to `Environment.Type`
* **[Rule]** If both present, **(T: value.type assignable to declared typeName)**
* **[Rule]** Declares an `Environment.Variable` in the current scope

### Conditional

```text
Ast.Statement.If
 ├─ condition       : Ast.Expression
 ├─ thenStatements : Ast.Statement[]
 └─ elseStatements : Ast.Statement[]
```

Rules:

* **[Rule]** **(T: condition.type must be `Boolean`)**
* **[Rule]** `thenStatements` must be non-empty
* **[Rule]** Then/Else bodies analyzed in **new nested scopes**

### For Loop

```text
Ast.Statement.For
 ├─ initialization : Ast.Statement.Assignment | null
 ├─ condition      : Ast.Expression
 ├─ increment      : Ast.Statement.Assignment | null
 └─ statements     : Ast.Statement[]
```

Rules:

* **[Rule]** `statements` must be non-empty
* **[Rule]** **(T: condition.type must be `Boolean`)**
* **[Rule]** If `initialization` present, **(Node: must be `Ast.Statement.Assignment`)**
* **[Rule]** If `increment` present, **(Node: must be `Ast.Statement.Assignment`)**
* **[Rule]** Loop body analyzed in a **new nested scope**

### While Loop

```text
Ast.Statement.While
 ├─ condition  : Ast.Expression
 └─ statements : Ast.Statement[]
```

Rules:

* **[Rule]** **(T: condition.type must be `Boolean`)**
* **[Rule]** Body analyzed in a **new nested scope**

### Return

```text
Ast.Statement.Return
 └─ value : Ast.Expression
```

Rules:

* **[Rule]** **(T: value.type assignable to current method returnType)**

---

## Expressions

### Logical

```text
Ast.Expression.Binary(operator = AND | OR)
```

Rules:

* **[Rule]** **(T: left.type == Boolean and right.type == Boolean)**
* **[Rule]** `result.type = Boolean`

### Comparison

```text
Ast.Expression.Binary(operator = < | <= | > | >= | == | !=)
```

Rules:

* **[Rule]** **(T: left.type and right.type assignable to `Comparable`)**
* **[Rule]** **(T: left.type == right.type)**
* **[Rule]** `result.type = Boolean`

### Arithmetic

```text
Ast.Expression.Binary(operator = + | - | * | /)
```

Rules:

* **[Rule]** If operator is `+` and either operand is `String`, `result.type = String`
* **[Rule]** Otherwise, **(T: both operands must be `Integer` or both `Decimal`)**
* **[Rule]** `result.type` is the numeric operand type

---

## Member Access and Function Calls

### Nominal Member Resolution and Lowering

FreshlyGround supports a statically-dispatched dot (`.`) operator
for accessing type-associated fields and functions.

Although the language is not object-oriented,
dot expressions are resolved against the static type of the left operand.

### Access

```text
Ast.Expression.Access
 ├─ receiver : Ast.Expression | null
 └─ name     : String
```

Rules:

* **[Rule]** If `receiver = null`, resolve name in the **current lexical scope**
* **[Rule]** If `receiver != null`, resolve name in the **receiver’s type scope**

### Function Call

```text
Ast.Expression.Function
 ├─ receiver  : Ast.Expression | null
 ├─ name      : String
 └─ arguments : Ast.Expression[]
```

Rules:

* **[Rule]** Resolve function via scope (lexical or type scope)
* **[Rule]** Arguments analyzed **left-to-right**
* **[Rule]** Each argument must be **assignable to the corresponding parameter type**
* **[Rule]** If `receiver != null`, implicit receiver is prepended to argument list

---

## Primary Expressions

### Literals

```text
Ast.Expression.Literal
 └─ literal : Object
```

Rules:

* `NIL` → `result.type = Nil`
* `TRUE` / `FALSE` → `result.type = Boolean`
* Numeric literals → `Integer` or `Decimal` (bounds-checked)
* `character` → `Character`
* `string` → `String`

### Grouping

```text
Ast.Expression.Group
 └─ expression : Ast.Expression
```

Rules:

* **[Rule]** Grouped expression must be binary (as implemented)
* **[Rule]** `group.type = inner.type`
