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

### Run Tests
1. Finish the 'Quick Start' steps
2. Run the tests:
> ```bash
> ./gradlew test
> ```
3. Open report:
>```bash
> open build/reports/tests/test/index.html
>```

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
#### Core Implementation

- [x] Complete COP 4020 baseline implementation
- [x] Enforce Java 21 via Gradle toolchain
- [ ] Redesign and refactor compiler architecture
    - [x] Centralize error handling via `CompilerException`
    - [x] Enforce semantic / syntactic seporation of concerns
        - [x] Enforce syntactic validation within AST construction
        - [x] Remove syntactic validation logic from `Analyzer`
        - [x] Implement semantic `Bindings` layer
        - [x] Eliminate semantic state from AST nodes
        - [x] Remove semantic validation logic from `Parser`
    - [x] Remap Scope chain
        - [x] Move `String` under `Any`
        - [x] Change `Compariable` to `Primitive`
        - [x] Update comparable expression semantic rules
        - [x] Update `Analyzer`
    - [x] Introduce `Builtins` for member functions and variables
        - [x] Move current member functions and variables into `Builtins`
        - [x] Restrict `Builtins` file to in scope declarations only
        - [x] Use generator or host ABI to implement functionality
    - [x] Remove JVM-specific concerns from `Environment` and member functions and variables
        - [x] Map builtin symbols to target representation (e.g., `jvmName`) in current generator
        - [x] Remove all references to `jvmName` in `Environment` and `Builtins`
    - [ ] Ensure consistency with `nullable` values
        - [ ] Update For loop `initialization` and `increment` to `Optional<Ast.Statement.Assignment>`
        - [ ] Update Scope `parent` to `Optional<Scope>`
        - [ ] Update parsing, generating, and testing to match

#### Compiler Architecture

- [x] Establish compiler-oriented architecture
    - [x] Enforce single-responsibility across all compiler layers

#### Command-Line Interface

- [x] Implement unified CLI entrypoint (`CompilerMain`)
- [x] Configure Gradle installation target as `fgc` (FreshlyGround Compiler)

#### Testing Improvements

- [ ] Refactor and expand test suite
    - [ ] Ensure unit tests validate only class-level responsibilities
    - [ ] Reclassify current end-to-end tests as interaction tests
    - [ ] Introduce CLI-driven end-to-end tests

#### Compiler Backends

- [ ] Add WebAssembly backend
    - [ ] Generate WAT (WebAssembly Text) from AST + Bindings
    - [ ] Define minimal host ABI for runtime interaction (e.g., `print_i32`)

#### Web Execution Environment

- [ ] Build containerized web execution platform
    - [ ] Develop lightweight web IDE frontend
    - [ ] Implement minimal API service
- [ ] Refactor `CompilerMain` into reusable compiler entrypoint
    - [ ] CLI becomes thin wrapper over shared entrypoint
    - [ ] API becomes thin wrapper over shared entrypoint
- [ ] Implement `/compile` API endpoint
    - POST source code → return WAT output

#### Documentation

- [ ] Update README
- [ ] Expand and finalize `/docs` documentation set
    - [x] Cross-link all documentation sections
    - [x] Layout documents by compiler layer
    - [ ] Write generator to be WebAssembly specific

---

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
| Topic               | Document                                         |
| ------------------- |--------------------------------------------------|
| Overview & Index    | [docs/00_index.md](./docs/00_index.md)           |
| Compiler Pipeline   | [docs/01_pipeline.md](./docs/01_pipeline.md)     |
| Language Grammar    | [docs/02_syntax.md](./docs/02_syntax.md)         |
| AST Specification   | [docs/03_struct_rep.md](./docs/03_struct_rep.md) |
| Semantic Rules      | [docs/04_semantics.md](./docs/04_semantics.md)   |
| WebAssembly Backend | [docs/05_backend.md](./docs/05_backend.md)       |

---

## Acknowledgments

Based on the book **Crafting Interpreters** by Robert Nystrom.

If you are interested in programming languages, I strongly recommend the book — it provided the scaffolding for 
everything implemented here.
