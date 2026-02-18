<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/<your_repo>">
    <img src="assets/banner.png" alt="Logo" width="80%">
  </a>

  <h3>
    A novel programming language for the Java Virtual Machine
  </h3>
</div>

---

<!-- ABOUT THE PROJECT -->
## About The Project
FreshlyGround is a **novel programming language and compiler** designed around a clean, multi-pass architecture 
that separates syntax, semantics, and code generation into explicit, single-responsibility components. 

The project originated as an academic transpiler for **COP 4020** at the **University of Florida**. It has since been 
refactored into a full compiler toolchain, and web environment. Its design is strongly influenced by the `lox` 
programming language in [*Crafting Interpreters*](https://www.craftinginterpreters.com/), with an emphasis on explicit intermediate representations, 
externalized semantic bindings, and multiple pluggable backends.

FreshlyGround currently targets the JVM and is being extended by means of a WebAssembly backend, with a 
browser-based execution environment, and a Hack VM backend for compilation to the nand2tetris CPU.

### Requirements
- Java 21+
- Gradle (via wrapper)

### Quick Start
1. Ensure you have Java 21 or higher
2. Clone this repository
3. Place your code in `/examples/src`
   - Note: There is an existing sample file there to transpile (`hello.fg`)
4. Navigate to root (following paths will assume you are in root)
5. Transpile with the following:
> ```bash
> ./gradlew build
> ./build/install/FreshlyGround/bin/fgc examples/src/<src_name>.fg examples/dist/Main.java
> javac examples/dist/Main.java
> ```

6. Run with the following:
> ```bash
> cd examples/dist
> java Main
> ```

### Repository Structure
```text
./
 ├─ assets/            # Repo/docs Images
 ├─ api/               # REST adapter (POST /compile, file.fg → file.wat)
 ├─ docs/              # Technical reference and language specifications
 ├─ examples/          # Example programs
 ├─ gradle/            # Gradle wrapper
 ├─ src/
 │  ├─ main/           # Core compiler passes (cli (fgc), lexer, parser, analyzer, generator(s))
 │  └─ test/           # Generated outputs
 ├─ tests/             # Unit, interaction, and end-to-end tests
 ├─ web/               # Web UI
 └─ build.gradle
```

### Roadmap
- [x] Complete COP 4020 baseline implementation
- [x] Redesign and Refactor
  - [x] Centralize error handling with `CompilerException`
  - [x] Enforce syntactic error handling in AST
  - [x] Remove all syntax error handling from Analyzer
  - [x] Implement semantic `Bindings`
  - [x] Remove semantic info from AST
  - [x] Add `Builtins` for common use functions and variables
  - [ ] Remove JVM specific information from Environment class
  - [x] Enforce Java 21 via Gradle toolchain
  - [x] Update README
- [x] Transpiler architecture
  - [x] Ensure single-responsibility in all passes
- [x] Command Line Interface (CLI)
  - [x] Create `CompilerMain` class for single CLI
  - [x] Set Gradle to install CLI as `fgc` (FreshlyGround Compiler)
- [ ] Clean up and Expand Testing
  - [ ] Reduce overlap between test layers
  - [ ] Ensure unit tests only cover class responsibilities
  - [ ] Reclassify current End-to-End testing to Interaction Tests
  - [ ] Add CLI-driven End-to-End tests
- [ ] Compiler Backends
  - [ ] Add WebAssembly backend
    - [ ] Generate WAT (WebAssembly Text) from AST + Bindings 
    - [ ] Define minimal host ABI for output (e.g., `print_i32`)
  - [ ] Lower from Java source generation to direct Java Bytecode with ASM
- [ ] Web Execution Environment
  - [ ] Add `/compile` API (POST source → return WAT string)
  - [ ] Refactor `CompilerMain` into a reusable compiler entrypoint (shared by CLI + API)
    - [ ] CLI becomes a thin wrapper over the shared entrypoint
    - [ ] API becomes a thin wrapper over the shared entrypoint
- [ ] Expand Documentation
    - [ ] Finalize and link `/docs` files


## Project Architecture
FreshlyGround follows a linear, multi-pass compiler pipeline with explicit separation between syntax, semantics, 
and execution format. To do this it uses the following components:

### Compilation Pipeline (Passes)
>```text
>Source
>  ↓
>Lexer        → Token Stream
>  ↓
>Parser       → Abstract Syntax Tree (AST)
>  ↓
>Analyzer     → Bindings + Scoped Semantic Model
>  ↓
>Generator    → Backend Output (Java | Hack Bytecode | WAT/WASM)
>```

### Design Principles
- AST is purely syntactic — no embedded semantic metadata
- Bindings are external — all semantic meaning is attached via a separate mapping layer
- Backends are pluggable — generators change representation, not language semantics
- Passes are single-responsibility — each stage performs one transformation only

This structure allows new targets (Hack bytecode, WASM) to be added without modifying the language front-end.

### Technical Reference
The full language and compiler specification is maintained in /docs:
- Language Grammar — EBNF, tokens, and syntactic forms
- Abstract Syntax Tree (AST) — node taxonomy and syntactic structural model
- Semantic Model — scope, bindings, type system, and resolution of semantic rules
- Compiler Pipeline — single pass structure and intermediate representations
- Backends — WebAssembly (WAT/WASM) targets

#### /docs
| Topic               | Document                                        |
| ------------------- |-------------------------------------------------|
| Overview & Index    | [docs/00_index.md](./docs/00_index.md)          |
| Compiler Pipeline   | [docs/01_pipeline.md](./docs/01_pipeline.md)    |
| Language Grammar    | [docs/02_syntax.md](./docs/02_syntax.md)        |
| AST Specification   | [docs/03_ast_map.md](./docs/03_struct_rep.md)   |
| Semantic Rules      | [docs/04_semantics.md](./docs/04_semantics.md)  |
| WebAssembly Backend | [docs/05_wasm_backend.md](./docs/05_backend.md) |


## Acknowledgments

Based on the book **Crafting Interpreters** by Robert Nystrom.

If you are interested in programming languages, I strongly recommend the book — it provided the scaffolding for 
everything implemented here.