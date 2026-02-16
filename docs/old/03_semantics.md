# 03 — Semantic Model & Bindings Specification

This document specifies the **FreshlyGround semantic layer**: the rules that assign *meaning* to 
syntactically valid programs by resolving names, enforcing types, and constructing a scoped binding environment 
over the Abstract Syntax Tree (AST).

Semantics define **what programs mean and how they are interpreted**, not how they are written. This layer 
guarantees that all identifiers, expressions, and control structures are **well-typed, well-scoped, and resolvable** 
before any backend lowers the program into an executable form.

---

## Navigation

* Index: [Overview & Index](00_index.md)
* Previous: [Abstract Syntax Tree (AST Map)](02_ast_map.md)
* Next: [Compiler Pipeline](04_pipeline.md)

---

## Design Notes

### Syntax vs. Semantics

* **Syntax** defines *structure* — which sequences of tokens form valid programs.
* **Semantics** defines *meaning* — what identifiers refer to, what types expressions produce, and whether operations are valid.

FreshlyGround enforces a strict separation:

* The **AST is immutable and purely syntactic**
* All meaning is attached externally through **Bindings**

This allows:

* Multiple semantic passes over the same AST
* Deterministic lowering into different backends (JVM, WASM)
* Independent evolution of syntax and execution targets

### Early Binding Model

FreshlyGround follows a **static, early-binding semantic model** similar to Java:

* Names, types, and scopes are resolved at **compile time**
* All expressions are assigned concrete static types
* Runtime execution is delegated entirely to the target platform (JVM or host ABI)

This guarantees that generated code is **portable, deterministic, and free of dynamic name resolution**.

---

## Semantic Model

### Core Definitions

* **Name** — A symbolic identifier in the source program (e.g., `x`)
* **Value** — The runtime data a name refers to (e.g., `10`)
* **Binding** — The association between an AST node and a semantic descriptor
* **Binding Time** — The phase at which a binding is created
* **Lifetime** — The duration from binding creation to destruction
* **Scope** — A region of the program in which a binding is visible
* **Environment** — The global definition of types and symbols

### Binding Times

Bindings are established progressively:

1. **Design Time** — Grammar rules and primitive types
2. **Implementation Time** — Mapping language types to backend representations (`Environment.Type`)
3. **Source Time** — Variable and function declarations in user code
4. **Compile Time** — Scope construction, name resolution, and type checking

Runtime value binding is owned entirely by the execution platform.

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
 ├─ parent   : Scope | null
 ├─ variables: Map<String, Environment.Variable>
 └─ functions: Map<String, Environment.Function>
```

Example:

```text
Scope{ parent=null,
       variables={
           "x" -> Environment.Variable(
               name="x",
               jvmName="x",
               constant=false,
               type=Environment.Type(
                   name="Integer",
                   jvmName="int",
                   scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
           )
       },
       functions={}
}
```

---

## Bindings Table

Bindings form a **global association** between AST nodes and their resolved semantic meaning.

### Conceptual Model

```text
Bindings
 ├─ Ast.Node → Environment.Type
 ├─ Ast.Node → Environment.Variable
 └─ Ast.Node → Environment.Function
```

Example:

```text
Bindings{
  Ast.Field("x") -> Environment.Variable(
      name="x",
      jvmName="x",
      constant=false,
      type=Environment.Type(
          name="Integer",
          jvmName="int",
          scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
  ),

  Ast.Expression.Literal(10) -> Environment.Type(
      name="Integer",
      jvmName="int",
      scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
}
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

---

## Structural Guarantees

The semantic layer enforces:

* All names in the AST resolve to a **unique binding**
* All expressions have a **concrete static type**
* All assignments and returns satisfy **assignability rules**
* All control-flow conditions are **Boolean-typed**

Once these invariants hold, backend lowering becomes a **pure mechanical transformation**.

---

## Forward Links

* For AST structure and node taxonomy, see: **[AST Specification](02_ast_map.md)**
* For pass ordering and IR flow, see: **[Compiler Pipeline](04_pipeline.md)**
* For execution lowering rules, see: **[WebAssembly Backend](05_wasm_backend.md)**
