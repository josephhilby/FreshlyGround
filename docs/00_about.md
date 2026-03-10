# About the Project

FreshlyGround is an educational programming language and compiler designed to explore modern compiler 
architecture from first principles.

The system separates compilation into distinct stages, each stage is documented independently 
in this reference.

## Where to Start

::: tip
Recommended reading order If you're new to the project, read the documentation
in this order:

1. **Compiler Pipeline** — overview of the compilation architecture
2. **Language Syntax** — grammar and lexical structure
3. **Program Model** — AST specification and internal program model
4. **Semantics** — binding rules and type checking
5. **Backend** — lowering to an executable target

:::

## Terminology

::: info
Core terminology used throughout the documentation...

>**Token Stream:**
>Ordered sequence of lexical units produced by the lexer.
>
>**AST (Abstract Syntax Tree):**
>Tree representation produced by the parser that mirrors grammar structure.
>
>**Environment:**
>Global catalog of known types, functions, and variables.
>
>**Bindings:**
>Semantic mappings that associate AST nodes with their resolved entities.
>
>**Scope:**
>Visibility chain used for resolving identifiers.
>
>**Lowering:**
>Deterministic emission from analyzed AST to backend representation.

:::
