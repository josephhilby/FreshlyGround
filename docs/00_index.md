# 00 — FreshlyGround Language & Compiler Reference Index

This documentation set serves as a detailed reference for the FreshlyGround 
language and compiler stack, from:

<p align="center">
<strong>source text → analyzed program model → backend emission</strong>
</p>

Each document below owns a single layer of that process, with some shared vocabulary and design between them.

---

## What to read when
- **Compiler Ordering and Artifact Use:** see, *[Compiler Pipeline](./01_pipeline.md)*
- **Token and Grammar Definitions:** see, *[Syntactic Definitions](./02_syntax.md)*
- **AST Mapping and Layout:** see, *[Structural Representation](./03_struct_rep.md)*
- **Semantic Rules and Node Bindings:** use *[Semantic Model & Bindings](./04_semantics.md)*
- **WAT Emission:** use *[Modular Backends](./05_backend.md)*

---

## Vocabulary
- **Token Stream:** lexer output; ordered sequence of atomic lexical units
- **AST:** parser output; structural tree that matches grammar production rules
- **Environment:** global catalog of known types, variables, and functions
- **Bindings:** analyzer output; semantic mappings of AST nodes to environment types, variables, or functions
- **Scope:** visibility chain for resolving unqualified identifier names to environment variables or functions
- **Lowering:** generator output; deterministic emission from AST + Bindings to a backend format

---

## Design Notes
### Pass Discipline

Each pass must...
* accept exactly one well-defined input representation (i.e., artifact)
* produce exactly one well-defined output representation
* avoid "leaking" responsibilities across stages (e.g., no type checking during parsing)

This makes the pipeline easy to test and easy to extend.

### Syntax vs. Semantics

* **Syntax** defines *structure* — which sequences of tokens form valid programs
* **Semantics** defines *meaning* — what identifiers refer to, what types expressions produce, and whether operations are valid

FreshlyGround enforces a strict separation between the two, such that:

* The AST is immutable and encodes only structural (**syntactic**) information
* The External Bindings are mutable and store the **semantic** information needed for lowering

This allows:

* The same AST can be reused across multiple backends
* Code generation to remain a simple mechanical lowering step

### Binding Model

FreshlyGround follows a static, early-binding model similar to Java, where bindings are established progressively then turned over to the target platform:

#### FreshlyGround
1. **Design Time** — Grammar rules, builtins, and primitive types
2. **Implementation Time** — Mapping language primitive types, and builtins to backend representations
3. **Source Time** — Variable and function declarations in user code
4. **Compile Time** — Scope construction, name resolution, and type checking

#### Target Platform
5. **Run Time** — Storage allocation, and stack activation record
