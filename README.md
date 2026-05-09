<!-- PROJECT LOGO -->
<br />
<div align="center">

  [![Issues][issues-shield]][issues-url] [![Gradle Tests][test-shield]][test-url]

  <img src="assets/fg_banner.png" alt="Logo" style="max-width:100%; border-radius:12px;">

</div>

---
<!-- ABOUT THE PROJECT -->
## About The Project
FreshlyGround is a **novel programming language and compiler** designed around a clean, multi-pass architecture
that separates syntax, semantics, and code generation into explicit, single-responsibility components. It can be
run via a web UI or command line compiler (see, Quick Start).

The project originated as an academic transpiler for **COP 4020** at the **University of Florida**. It has since been
refactored into a full compiler toolchain, and web environment. Its design is strongly influenced by the `lox`
programming language in [*Crafting Interpreters*](https://www.craftinginterpreters.com/), with an emphasis on explicit intermediate representations,
externalized semantic bindings, and multiple pluggable backends.

FreshlyGround currently targets the JVM and is being extended by means of additional WebAssembly and Hack VM (nand2tetris CPU) backends.

<br>
<p align="center">
  <img src="assets/front_end.png"
       alt="FreshlyGround Web UI"
       style="max-width:80%; border-radius:12px;">
  <br>
  <em>FreshlyGround Web Compiler User Interface</em>
</p>


### Requirements

* Docker
* Docker Compose

### Quick Start

1. Clone this repository
2. Navigate to the project root
3. Build and start the development containers:

```bash
docker compose up --build
```

1. Open your browser to:

* Frontend: [http://localhost:5173/](http://localhost:5173/)
* Backend: [http://localhost:7070/](http://localhost:7070/)

1. Stop and remove containers:

```bash
docker compose down
```

### Run Tests

1. Ensure Docker is running
2. Navigate to the project root
3. Run the backend test suite:

```bash
docker compose run --rm \
  -e GRADLE_USER_HOME=/tmp/gradle-test \
  backend sh -c 'cp -a /app /tmp/app-test && cd /tmp/app-test && ./gradlew test --no-daemon --console=plain'
```

1. Open the generated report:

```bash
open compiler/build/reports/tests/test/index.html
```


### Repository Structure
```text
./
 ├─ assets/              # Repo/docs Images
 ├─ compiler/
 │   ├─ src/
 │  ...  ├─ main/java... # Core compiler (logic, api, server)
 │       └─ test/java... # Gradle tests
 │
 ├─ docs/                # Technical reference
 ├─ web/                 # Web UI
 └─ README.md
```

### Roadmap
#### Core Implementation

- [x] Complete COP 4020 baseline implementation
- [x] Enforce Java 21 via Gradle toolchain
- [x] Redesign and refactor compiler architecture
    - [x] Centralize error handling via `CompilerException`
    - [x] Enforce semantic / syntactic seporation of concerns
        - [x] Enforce syntactic validation within `AST` construction
        - [x] Remove syntactic validation logic from `Analyzer`
        - [x] Remove semantic information from `AST` nodes
        - [x] Remove semantic validation logic from `Parser`
    - [x] Remove all references to `jvmName` in `Environment` and `Builtins`
    - [x] Remap Type Model Scope chain
        - [x] Move `String` under `Any`
        - [x] Change `Compariable` to `Primitive`
        - [x] Update comparable expression semantic rules
    - [x] Introduce `Bindings` for semantic attachments
        - [x] Move `AST` semantic attachements into `Bindings`
    - [x] Introduce `Types` as singletons
        - [x] Refactor type singletons into `Types`
    - [x] Introduce `StandardLibrary` for member functions and variables
        - [x] Move `Environment` member functions and variables into `StandardLibrary`
        - [x] Use generator or host ABI to implement functionality
    - [x] Refactor `Environment` to hold semantic classes, and relivent helpers only
    - [x] Ensure consistency with `nullable` values
        - [x] Update `Ast.Statement.For`, `initialization` and `increment` to `Optional<Ast.Statement.Assignment>`
        - [x] Update `Scope`, `parent` to `Optional<Scope>`
        - [x] Update parsing, generating, and testing to match
    - [x] Rework Directory to match future goals for plugable backends and web UI

#### Compiler Architecture

- [x] Establish compiler-oriented architecture
    - [x] Enforce single-responsibility across all compiler layers

#### Command-Line Interface

- [x] Implement unified CLI entrypoint (`CompilerMain`)
- [x] Configure Gradle installation target as `fgc` (FreshlyGround Compiler)

#### Testing Improvements

- [ ] Refactor and expand test suite
    - [x] Split existing tests by intermediate representation class
    - [ ] Ensure unit tests validate only class-level responsibilities
    - [x] Reclassify current end-to-end tests as interaction tests
      - [x] Standardize integration tests with the use of a wrapper
    - [ ] Introduce CLI-driven end-to-end tests

#### Compiler Backends

- [ ] Add WebAssembly backend
    - [ ] Generate WAT (WebAssembly Text) from AST + Bindings
    - [ ] Define minimal host ABI for runtime interaction (e.g., `print_i32`)

#### Web Execution Environment

- [x] Build containerized web execution platform
    - [x] Develop lightweight web IDE frontend
    - [x] Implement minimal API service
- [x] Refactor `CompilerMain` into reusable entrypoint
    - [x] CLI becomes thin wrapper over shared entrypoint
    - [x] API becomes thin wrapper over shared entrypoint
- [ ] Implement `/compile` API endpoint
    - [x] POST source code → return Java output
    - [x] Display all IR representations of code in UI
    - [ ] Change POST from Java to WAT
- [ ] Containerize through Docker

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

---

## The /docs
Read the [docs](https://freshlyground-docs.onrender.com) to get a better understanding of the project.

---

## Acknowledgments

### Robert Nystrom
Based on the book [**Crafting Interpreters**](https://www.craftinginterpreters.com/) by Robert Nystrom.

If you are interested in programming languages, I strongly recommend the book — it provided the scaffolding for
everything implemented here.

### Christian Vallentin
EBNF syntax highlighting file authored by Christian Vallentin, licensed under the MIT License. The original
author retains full credit for their work.

Christian, V. (2020-06-03). bnf.tmLanguage.json — File used in FreshlyGround documentation syntax.
- Original source repository: https://github.com/vallentin/vscode-bnf


<!-- MARKDOWN LINKS & IMAGES -->

<!-- Tests Shield -->
[test-shield]: https://github.com/josephhilby/FreshlyGround/actions/workflows/gradle_test_ci.yml/badge.svg
[test-url]: https://github.com/josephhilby/FreshlyGround/actions/workflows/gradle_test_ci.yml

<!-- Issues Shield -->
[issues-shield]: https://img.shields.io/github/issues/josephhilby/FreshlyGround.svg
[issues-url]: https://github.com/josephhilby/FreshlyGround/issues
