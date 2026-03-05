# 01 — Compiler Pipeline Specification

This document explains the overall architecture of the FreshlyGround compiler. More detailed rules for each stage 
live in their respective documents. As a quick note, I will be using the terms 'stage' and 'pass' to talk about the physical 
implementation, the use of 'layer' refers to the more theoretical level that stage or pass belongs to.

This roadmap will cover the following:

- System Overview of stages and their artifact production
- Responsibilities of each stage
- Guarantees each stage has with the next

---

## System Overview
FreshlyGround follows a layered, single-direction pipeline:

```text
Source Text
   ↓
 Lexer      (Syntax Layer)
   ↓
Parser      (Syntax Layer)
   ↓
Analyzer    (Semantic Layer)
   ↓
Emitter     (Backend Layer)
```

Each stage transforms one artifact into another and enforces only the rules it owns.

### Artifact Production

| Stage    | Produces              | Consumed By |
| -------- |-----------------------| ----------- |
| Lexer    | Token Stream          | Parser      |
| Parser   | AST                   | Analyzer    |
| Analyzer | AST + Bindings        | Emitter     |
| Emitter  | Target Representation | Runtime     |

---

## Compilation Stages
### 1) Lexing (Tokenization)

**Input:** Source Text

**Output:** Token Stream

**Responsibilities:**

* classify characters into typed tokens
* preserve positional metadata for diagnostics

**Guarantees:**

* tokens are well-formed (integers, identifiers, strings, operators)
* invalid lexemes produce compiler errors

### 2) Parsing (Syntactic Analysis)

**Input:** Token Stream

**Output:** Abstract Syntax Tree (AST)

**Responsibilities:**

* enforce grammar correctness (EBNF)
* encode precedence and associativity in tree shape

**Guarantees:**

* the AST is structurally valid and internally consistent
* all syntactic rules are satisfied
* invalid syntax produces compiler errors

### 3) Analyzing (Semantic Analysis)

**Input:** AST

**Output:** AST + Bindings

**Responsibilities:**

* build lexical scope chains
* resolve identifiers into variables, functions, or types
* attach semantic meaning to AST nodes through Bindings

**Guarantees:**

* every identifier resolves to a valid binding
* every expression has a concrete static type
* all semantic rules are satisfied
* invalid semantics produce compiler errors

### 4) Generating (Lowering / Emitting)

**Input:** AST + Bindings

**Output:** Target Representation

**Responsibilities:**

* deterministically lower analyzed programs into target representation
* remain purely mechanical (no semantic decisions)

**Guarantees:**

* backend output faithfully preserves language semantics

---

## Navigation

* Index: [Overview & Index](./00_index.md)
* Previous: N/A
* Next: [Syntactic Definitions](./02_syntax.md)
