# 04 — Semantic Model & Bindings Specification

This document specifies the **FreshlyGround semantic layer**: the rules that assign *meaning* to
syntactically valid programs by resolving names, enforcing types, and constructing a scoped binding environment
over the Abstract Syntax Tree (AST).

Semantics define **what programs mean and how they are interpreted**. This layer
guarantees that all identifiers, expressions, and control structures are well-typed, well-scoped, and resolvable
before any backend lowers the program into an executable form.

---

## Scope Model

FreshlyGround enforces **lexical (static) scoping**.

A Scope represents a lexical region of name visibility. Scopes form a parent-linked 
chain corresponding to nested program structure.

### Conceptual Structure

```yaml
Scope
 ├─ parent    : Scope | null
 ├─ variables : Map<String, Environment.Variable>
 └─ functions : Map<String, Environment.Function>
```

**Note:** The function must be searched by both identifier and arity, "name + '/' + arity".

### Scope Rules
1. Declarations 
   * Bind a `Environment.Variable` or `Environment.Function` in the **current scope**
   * **Shadowing is allowed** across nested scopes
   * **Redeclaration in the same scope is forbidden**

2. Resolutions
   * Lookup `Environment.Variable` or `Environment.Function` in the **current scope**
   * If not found, resolution proceeds recursively through **parent scopes**
   * Failure to resolve results in a compile-time error

3. Nesting Scopes
   * Occurs within: method bodies, conditional blocks, loop blocks 
---

## Environment Model
The `Environment` defines the semantic entities used during analysis. At its core the model enforces one 
key guarantee: 

> After analysis every `Ast.Expression` node will resolve to exactly one `Environment.Type`

This invariant ensures that all expressions are well-typed and fully bound prior to lowering.

### Conceptual Structure

```yaml
Environment
 ├─ types    : Map<String, Type>
 ├─ type     : Class { name : String, scope : Scope }
 ├─ function : Class { name : String, parameterTypes : List<Type>, returnType : Type }
 └─ variable : Class { name : String, constant : boolean, type : Type }
```

### Environment Rules
1. All types are singletons
2. Types and subtype sets are closed, and fixed at initialization
3. Each `Type` owns a `Scope`
   * This scope stores type-associated member functions and variables
   * Member access is performed using the dot (`.`) operator
   * Member resolution proceeds through the owning type’s scope, then iteratively through its scope chain

#### Member Access / Resolution (Dot Operator)
Each `Environment.Variable` associated with an `Environment.Type`. That type contains a `Scope` populated
at Design Time, and set with builtin member variables and functions. To access these member builtins, FreshlyGround 
uses a dot operator to shift the resolution context from the current lexical scope to the scope owned by the expression’s 
resolved type.

Member resolution is purely static. The dot operator performs compile-time lookup within the owning type’s
scope and resolves to the `Environment.Type` found.

Assume there exists a variable:
```text
Message : String = "Hello World"
```

The `Environment.Type.STRING` contains:
- variable `length` with type `Integer`
- function `slice(start, end)` with return type `String`

So calling... 
- `Message.length` resolves to type `Integer`
- `Message.slice(0,5)` resolves to type `String`

#### Scope Chain
```yaml
Any
├─ Primitive
│   ├─ Integer
│   ├─ Decimal
│   ├─ Character
│   └─ Boolean
├─ String
└─ Nil
```
Assume there exists a variable:
```text
Number : Integer = 1234
```

The `Environment.Type.PRIMITIVE` contains:
- function `stringify` with return type `String`

So calling...
- `Number.stringify` resolves to type `String`

---

## Bindings Model

Bindings store the results of semantic analysis. They associate specific AST nodes with the 
semantic entities produced during resolution.

### Conceptual Model

```yaml
Bindings
 ├─ fieldBindings       : Map<Ast.Field, Environment.Variable>
 ├─ declarationBindings : Map<Ast.Statement.Declaration, Environment.Variable>
 ├─ accessBindings      : Map<Ast.Expression.Access, Environment.Variable>
 ├─ methodBindings      : Map<Ast.Method, Environment.Function>
 ├─ functionBindings    : Map<Ast.Expression.Function, Environment.Function>
 └─ typeBindings        : Map<Ast.Expression, Environment.Type>
```

### Binding Resolution
```text
AST node
  ↓
Name / construct extracted
  ↓
Lexical or type-based scope lookup
  ↓
Environment entity resolved
  ↓
Binding recorded
```

---

## Semantic Model
### AST Root

AST mapping:

```yaml
Ast.Source
 ├─ fields  : List<Ast.Field>
 └─ methods : List<Ast.Method>
```

Rules:

* **[Rule]** `main/0` (`method/arity`) must exist in the methods
* **[Rule]** `main/0` must return `Integer`

>**Legend:**
>- **[Rule]** — semantic restriction
>- **(T: …)** — type constraint

---

## Top-Level Declarations
### Fields

AST mapping:

```yaml
Ast.Field
 ├─ name     : String
 ├─ typeName : String
 ├─ constant : boolean
 └─ value    : Optional<Ast.Expression>
```

Rules:

* **[Rule]** If `constant=true`, `value` must be present
* **[Rule]** If `value` is present, **(T: `value.type` assignable to declared `typeName`)**
* **[Rule]** Declares an `Environment.Variable` in the current scope

### Method

AST mapping:

```yaml
Ast.Method
 ├─ name           : String
 ├─ parameters     : List<String>
 ├─ parameterTypes : List<String>
 ├─ returnTypeName : Optional<String>
 └─ statements     : List<Ast.Statement>
```

Rules:

* **[Rule]** Method body is analyzed in a **new nested scope**
* **[Rule]** `parameterTypes` must resolve to `Environment.Type`
* **[Rule]** `returnTypeName` defaults to `Nil` if omitted
* **[Rule]** Each `RETURN` must satisfy **(T: `value.type` assignable to method `returnType`)**
* **[Rule]** Declares an `Environment.Function` in the current scope

---

## Statements
### Declaration

AST mapping:

```yaml
Ast.Statement.Declaration
 ├─ name     : String
 ├─ typeName : Optional<String>
 └─ value    : Optional<Ast.Expression>
```

Rules:

* **[Rule]** If `value` present and valid, `typeName` is inferred from `value.type`
* **[Rule]** If `typeName` present, it must resolve to `Environment.Type`
* **[Rule]** If both present, **(T: `value.type` assignable to declared `typeName`)**
* **[Rule]** Declares an `Environment.Variable` in the current scope

### Assignment

AST mapping:

```yaml
Ast.Statement.Assignment
 ├─ receiver : Ast.Expression.Access
 └─ value    : Ast.Expression
```

Rules:

* **[Rule]** Receiver must not be constant
* **[Rule]** **(T: `value.type` assignable to `receiver.type`)**

### Expression

AST mapping:

```yaml
Ast.Statement.Expression
 └─ expression : Ast.Expression.Function
```

**Note:** Function calls are modeled as expressions because they evaluate to a value. `Ast.Statement.Expression`
exists to permit an expression — specifically a function call — to appear in statement position when its resulting 
value is not used. This allows calls that exist purely for their side effects (e.g., `print(String)`) to stand 
alone as complete statements.

Rules: See, `Ast.Expression.Function`

### Conditional

AST mapping:

```yaml
Ast.Statement.If
 ├─ condition      : Ast.Expression
 ├─ thenStatements : List<Ast.Statement>
 └─ elseStatements : List<Ast.Statement>
```

Rules:

* **[Rule]** **(T: `condition.type` assignable to `Boolean`)**
* **[Rule]** `thenStatements` must be non-empty
* **[Rule]** Then/Else bodies analyzed in **new nested scopes**

### For Loop

AST mapping:

```yaml
Ast.Statement.For
 ├─ initialization : Ast.Statement.Assignment | null
 ├─ condition      : Ast.Expression
 ├─ increment      : Ast.Statement.Assignment | null
 └─ statements     : List<Ast.Statement>
```

Rules:

* **[Rule]** **(T: `condition.type` assignable to `Boolean`)**
* **[Rule]** If `initialization` exists, **(T: `init.receiver` assignable to `Comparable`)**
* **[Rule]** If `increment` and `initialization` exist, **(T: `init.receiver` assignable to `inc.receiver`)**
* **[Rule]** Statements body analyzed in a **new nested scope**

### While Loop

AST mapping:

```yaml
Ast.Statement.While
 ├─ condition  : Ast.Expression
 └─ statements : List<Ast.Statement>
```

Rules:

* **[Rule]** **(T: `condition.type` assignable to `Boolean`)**
* **[Rule]** Statements body analyzed in a **new nested scope**

### Return

AST mapping:

```yaml
Ast.Statement.Return
 └─ value : Ast.Expression
```

Rules:

* **[Rule]** **(T: `value.type` assignable to current method `returnType`)**

---

## Expressions
### Binary Operations
#### Logical

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "AND" | "OR"
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** **(T: `left.type` and `right.type` assignable to `Boolean`)**
* **[Rule]** `result.type` must resolve to `Boolean`

#### Comparison

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "<" | "<=" | ">" | ">=" | "==" | "!="
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** **(T: `left.type` and `right.type` assignable to `Comparable`)**
* **[Rule]** **(T: `left.type` must be the same as `right.type`)**
* **[Rule]** `result.type` must resolve to `Boolean`

#### Arithmetic

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "+" | "-" | "*" | "/"
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** If operator is `+` and either operand is `String`, `result.type` will be `String`
* **[Rule]** Otherwise, **(T: both operands must be `Integer` or both `Decimal`)**
* **[Rule]** `result.type` must resolve to the numeric operand type (`Integer` or `Decimal`)

### Member Access and Function Calls
#### Resolution and Lowering

FreshlyGround defaults to resolving fields and functions within the current lexical scope, however
it also supports a statically-dispatched dot (`.`) operator for accessing type-associated fields 
and functions.

Although the language is not object-oriented, dot expressions are resolved against the static type of 
the left operand (i.e., receiver).

#### Member Access

AST mapping:

```yaml
Ast.Expression.Access
 ├─ receiver : Optional<Ast.Expression>
 └─ name     : String
```

Rules:

* **[Rule]** If `receiver` = `null`, resolve name in the **current lexical scope**
* **[Rule]** If `receiver` != `null`, resolve name in the **receiver’s type scope**

##### Access Resolution Model
Given:

    receiver.member

1. Evaluate `receiver` to type **T**
2. Resolve `member` inside the **type scope of T**

#### Function Call

AST mapping:

```yaml
Ast.Expression.Function
 ├─ receiver  : Optional<Ast.Expression>
 ├─ name      : String
 └─ arguments : List<Ast.Expression>
```

Rules:

* **[Rule]** Resolve function via scope (lexical or type scope)
* **[Rule]** Arguments analyzed **left-to-right**
* **[Rule]** Each argument must be **assignable to the corresponding parameter type**
* **[Rule]** If `receiver` != `null`, implicit receiver is prepended to argument list

##### Function Resolution Model
Given:

    receiver.function(a, b)

1. Evaluate `receiver` to type **T**
2. Increment arity for lookup
2. Resolve `function/3` inside the **type scope of T**

The expression is then lowered to:

    function(receiver, a, b)

As the receiver is inserted as the first argument.

### Primary Expressions
#### Grouping

AST mapping:

```yaml
Ast.Expression.Group
 └─ expression : Ast.Expression
```

Rules:

* **[Rule]** `group.type` must be the same as `expression.type`

#### Literals

AST mapping:

```yaml
Ast.Expression.Literal
 └─ literal : Object
```

Type Map (java object → Environment.Type):

* `null`       → `Environment.Type.NIL`
* `Boolean`    → `Environment.Type.BOOLEAN`
* `BigInteger` → `Environment.Type.INTEGER`
* `BigDecimal` → `Environment.Type.DECIMAL`
* `Character`  → `Environment.Type.CHARACTER`
* `String`     → `Environment.Type.STRING`

Note: Integer and Decimal are bounded within a 32-bit int and 64-bit double

---

## Navigation

* Index: [Overview & Index](./00_index.md)
* Previous: [Structural Representation](./03_struct_rep.md)
* Next: [Modular Backends](./05_backend.md)