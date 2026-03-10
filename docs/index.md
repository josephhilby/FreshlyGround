---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: "FreshlyGround"
  text: "Compiler & Language Reference"
  tagline: A programming language brewed from first principles
  actions:
    - theme: brand
      text: Start with the Compiler Pipeline
      link: /01_pipeline
    - theme: alt
      text: Try the Live Compiler
      link: https://freshlyground.onrender.com

features:
  - title: Compiler Pipeline
    details: Understand how FreshlyGround transforms source text into executable output through a disciplined multi-pass architecture.
    link: /01_pipeline
  - title: Language Syntax
    details: Formal grammar definitions and token structure for the FreshlyGround language.
    link: /02_syntax
  - title: Program Model
    details: How parsed programs are represented internally through AST structures and intermediate artifacts.
    link: /03_program_model
  - title: Semantic Model
    details: Name resolution, type checking, scope construction, and binding rules that define program meaning.
    link: /04_semantics
  - title: Backend Emissions
    details: Deterministic lowering of analyzed programs into backend targets such as WebAssembly and other runtimes.
    link: /05_backend
---

---

# About the Project

FreshlyGround is an educational programming language and compiler designed to explore modern compiler
architecture from first principles. The system separates compilation into distinct stages, each stage is documented independently
in this reference.

<div class="callout-grid">

::: tip Recommended reading order:
1. **Compiler Pipeline** — High-level overview of the compilation architecture.
2. **Language Syntax** — Formal grammar and lexical rules that define the structure of valid programs.
3. **Program Model** — Specification of the Abstract Syntax Tree (AST) and the internal structures used to represent programs.
4. **Semantic Model** — Rules governing name resolution, scope construction, type checking, and the binding of identifiers to variables and functions, along with the determination of static types for expressions and literals.
5. **Backend Emissions** — Deterministic lowering of analyzed programs into the WAT backend target.

:::

::: warning Core terminology used throughout the documentation...

- **Token Stream:** Ordered sequence of lexical units produced by the lexer.
- **AST (Abstract Syntax Tree):** Tree representation produced by the parser that mirrors grammar structure.
- **Environment:** Global catalog of known types, functions, and variables.
- **Bindings:** Semantic mappings that associate AST nodes with their resolved entities.
- **Scope:** Visibility chain used for resolving identifiers.
- **Lowering:** Deterministic emission from analyzed AST to backend representation.

:::

</div>
