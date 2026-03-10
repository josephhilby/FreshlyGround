---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: "FreshlyGround"
  text: "Compiler & Language Reference"
  tagline: A programming language brewed from first principles
  actions:
    - theme: brand
      text: Read the Docs
      link: /01_pipeline
    - theme: alt
      text: Try the Live Compiler
      link: https://freshlyground.onrender.com

features:
  - title: About
    details: A quick overview of the project and related terms.
    link: /00_about
  - title: Compiler Pipeline
    details: Understand how FreshlyGround transforms source text into executable output through a disciplined multi-pass architecture.
    link: /01_pipeline
  - title: Language Syntax
    details: Formal grammar definitions and token structure for the FreshlyGround language.
    link: /02_syntax
  - title: Program Model
    details: How parsed programs are represented internally through AST structures and intermediate artifacts.
    link: /03_program_model
  - title: Semantic Analysis
    details: Name resolution, type checking, scope construction, and binding rules that define program meaning.
    link: /04_semantics
  - title: Backend Generation
    details: Deterministic lowering of analyzed programs into backend targets such as WebAssembly and other runtimes.
    link: /05_backend
---