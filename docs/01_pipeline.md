# Compiler Pipeline

This document describes the architecture of the FreshlyGround compiler.

The compiler is organized as a sequence of **stages**, each responsible for transforming one program 
representation into another. From a conceptual perspective these stages belong to three broader **layers**.

::: tip **Layers**:
- **Syntax layer** — lexing and parsing
- **Semantic layer** — analysis and binding
- **Backend layer** — target emission
:::

The layers group stages by the level of interpretation they operate on: syntactic structure,
semantic meaning, or target (backend) representation. Within their respective level each 
stage performs a single transformation and produces a well-defined artifact consumed by the next stage.

## System Overview
At a high level, FreshlyGround follows a layered, single-direction pipeline. The diagram below shows the 
compilation pipeline and the artifacts produced between stages.

```mermaid
flowchart LR

Source@{ shape: doc, label: "Source\nCode" }

subgraph syntax["Syntax Layer"]
    Lexer["Lexer:\nTokens"]
    Parser["Parser:\nAST"]
end

subgraph semantic["Semantic Layer"]
    Analyzer["Analyzer:\nAST+Bindings"]
end

subgraph backend["Backend Layer"]
    Generator["Generator"]
end

Target@{ shape: lin-doc, label: "Target\nRep." }

Source --> Lexer
Lexer --> Parser
Parser --> Analyzer
Analyzer --> Generator
Generator --> Target

%% Layer coloring

classDef syntax fill:#1f2937,stroke:#6b7280,color:#e5e7eb
classDef semantic fill:#2f3352,stroke:#6366f1,color:#e5e7eb
classDef backend fill:#4a3a12,stroke:#f59e0b,color:#e5e7eb

class Lexer,Parser syntax
class Analyzer semantic
class Generator backend
```

## Individual Stages

Each stage in the pipeline consumes a specific artifact and produces the artifact required by the next stage.
The guarantees established by one stage become the assumptions relied upon by the next.

<div class="callout-grid">

::: info Lexing (Tokenization)

**Input:** Source Code

**Output:** Token Stream

**Responsibilities:**

* classify valid character groupings (lexemes) into typed tokens
* preserve positional metadata for diagnostics

**Guarantees:**

* tokens are well-formed (integers, identifiers, strings, operators)
* invalid lexemes produce compiler errors

:::

::: info Parsing (Syntactic Analysis)

**Input:** Token Stream

**Output:** Abstract Syntax Tree (AST)

**Responsibilities:**

* enforce context free grammar (syntactic) rules
* encode precedence and associativity in AST shape

**Guarantees:**

* the AST is structurally valid and internally consistent
* all syntactic rules are satisfied
* invalid syntaxes produce compiler errors

:::

::: tip Analyzing (Semantic Analysis)

**Input:** AST

**Output:** AST + Bindings

**Responsibilities:**

* build lexical scope chains to record binding visibility
* resolve identifiers into variables, functions, or types
* attach semantic meaning to AST through Bindings

**Guarantees:**

* every identifier resolves to a valid binding
* every expression has a concrete static type
* all semantic rules are satisfied
* invalid semantics produce compiler errors

:::

::: warning Generating (Lowering / Emitting)

**Input:** AST + Bindings

**Output:** Target Representation

**Responsibilities:**

* deterministically lower analyzed programs into target representation
* remain purely mechanical (no semantic decisions)

**Guarantees:**

* backend output faithfully preserves language semantics

:::

</div>