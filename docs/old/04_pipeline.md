# 04 — Compiler Pipeline Specification

This document specifies the **FreshlyGround compiler pipeline**: the ordered sequence of single-responsibility 
passes that transform source text into backend-ready output.

The pipeline is designed to be:

* **Linear** — each pass consumes the output of the previous pass
* **Deterministic** — the same input yields the same intermediate forms
* **Modular** — backends can be swapped without rewriting front-end semantics

FreshlyGround intentionally separates:

* **Syntax** (what was written)
* **Semantics** (what it means)
* **Lowering** (how it executes)

---

## Navigation

* Index: [Overview & Index](00_index.md)
* Previous: [Semantic Model & Bindings](03_semantics.md)
* Next: [WebAssembly Backend](05_wasm_backend.md)

---

## Design Notes

### Pass Discipline (Single Responsibility)

Each pass must:

* have a single input representation and a single output representation
* enforce exactly one category of correctness constraints
* avoid "leaking" responsibilities across stages (e.g., no type checking during parsing)

This makes the pipeline easy to test and easy to extend.

### Immutable Syntax + External Semantics

FreshlyGround treats the AST as **syntactic truth**:

* the parser builds it once
* subsequent passes do not rewrite it to encode meaning

All semantic information is attached externally through **Bindings**. This enables:

* repeated semantic passes
* consistent backend behavior
* simpler code generation

---

## Pipeline Overview

```text
Source Text
  ↓
Lexer        → Token Stream
  ↓
Parser       → AST (syntax only)
  ↓
Analyzer     → Scope + Environment + Bindings
  ↓
Generator    → Backend Output (Java | JVM bytecode | WAT)
```

---

## Passes

### 1) Lexing (Tokenization)

**Input:** Source text (characters)

**Output:** Token stream

**Purpose:**

* classify characters into typed tokens
* preserve positional metadata for diagnostics

**Guarantees:**

* tokens are well-formed (integers, identifiers, strings, operators)
* invalid lexemes produce lexer errors

**Artifacts:**

* `Token(type, lexeme, position)`

---

### 2) Parsing (Syntactic Analysis)

**Input:** Token stream

**Output:** Abstract Syntax Tree (AST)

**Purpose:**

* enforce grammar correctness (EBNF)
* encode precedence and associativity in tree shape

**Guarantees:**

* the AST is structurally valid and internally consistent
* no semantic validation is performed

**Artifacts:**

* `Ast.Source` root containing declarations and statements

---

### 3) Analyzing (Semantic Analysis)

**Input:** AST

**Output:** Bindings + Scope hierarchy (semantic overlay)

**Purpose:**

* resolve names into variables/functions/types
* build lexical scope chains
* type-check expressions and statements
* attach meaning to AST nodes through Bindings

**Guarantees:**

* every identifier resolves to a valid binding
* every expression has a concrete static type
* control-flow guards are boolean-typed
* assignments and returns satisfy assignability rules

**Artifacts:**

* `Environment` (type/function/variable descriptors)
* `Scope` (lexical visibility model)
* `Bindings` (AST node → semantic descriptor mapping)

---

### 4) Generating (Lowering / Emitting)

**Input:** AST + Bindings

**Output:** Backend artifact (representation depends on target)

**Purpose:**

* deterministically lower analyzed programs into an execution format
* remain purely mechanical (no semantic decisions)

**Guarantees:**

* generator assumes semantic correctness has already been proven
* backend output faithfully preserves language semantics

**Backends:**

* Java source emission (current)
* JVM bytecode (planned via ASM)
* WebAssembly text (planned WAT) / WebAssembly binary (future)

---

## Example Walkthrough (Small to Big)

### Source

```fg
LET x: Integer = 10;
```

---

### 1) Tokenization

```text
[LET] [x] [:] [Integer] [=] [10] [;]
```

More explicitly:

```text
TokenStream = {
  (LET), (IDENT "x"), (:), (IDENT "Integer"), (=), (INT "10"), (;)
}
```

---

### 2) Parsing → AST

Grammar match:

```ebnf
field ::= "LET" identifier ":" identifier "=" expression ";"
```

AST shape:

```text
Ast.Source
 └─ fields
    └─ Ast.Field
       ├─ name      = "x"
       ├─ typeName  = "Integer"
       ├─ constant  = false
       └─ value
          └─ Ast.Expression.Literal(10)
```

At this point:

* `Integer` is just a name
* `10` is just a literal node
* no types/scopes exist yet

---

### 3) Analysis → Scope + Bindings

The analyzer constructs semantic meaning in three visible steps:

#### (a) Resolve types

```text
"Integer" → Environment.Type.INTEGER
literal(10) → Environment.Type.INTEGER
```

#### (b) Declare symbols in scope

```text
Scope(global)
  declare variable x : Integer (constant=false)
```

#### (c) Bind meaning to AST nodes

```text
Bindings:
  Ast.Field("x")                ↦ Environment.Variable(x : Integer)
  Ast.Expression.Literal(10)     ↦ Environment.Type.INTEGER
  Ast.Expression.Access("x")    ↦ Environment.Variable(x : Integer)   (if referenced)
```

The AST is unchanged; meaning exists entirely in **Scope + Bindings**.

---

### 4) Emitting → Backend Output

Once analysis succeeds, emitting becomes a direct lowering.

#### Java Source (current backend)

Conceptual output:

```text
int x = 10;
```

#### WebAssembly (planned backend)

Conceptual lowering goal:

```text
(local $x i32)
(i32.const 10)
(local.set $x)
```

---

## Diagnostics Policy

Errors are reported at the earliest responsible pass:

* **Lexer**: invalid lexemes (malformed numbers/strings)
* **Parser**: grammar violations and structural mismatches
* **Analyzer**: unresolved names, invalid types, forbidden assignments, invalid control guards
* **Generator**: should not introduce new user-facing errors (it may still fail on internal invariants)

---

## Forward Links

* For the formal grammar and precedence structure: **[Language Syntax](01_syntax.md)**
* For the node taxonomy used by all passes: **[AST Map](02_ast_map.md)**
* For semantic rules and binding invariants: **[Semantics](03_semantics.md)**
