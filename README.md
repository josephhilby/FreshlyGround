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
FreshlyGround is a **novel programming language** whose source code is transpiled into Java. 
This project was refactored and expanded from an academic transpiler project created in **COP 4020** at the 
**University of Florida**, and follows the methodology outlined in the book [*Crafting Interpreters*](https://www.craftinginterpreters.com/).

### Requirements
- Java 21+

### Get Started
1. Ensure you have Java 21 or higher
2. Clone this repository
3. Place your code in `/examples/src`
   - Note: There is an existing sample file there to transpile (`hello.fg`)
4. Navigate to root (following paths will assume you are in root)
5. Transpile with the following:
> ```bash
> ./gradlew build
> ./build/install/FreshlyGround/bin/fgc examples/src/<file_name>.fg examples/dist/Main.java
> javac examples/dist/Main.java
> ```

6. Run with the following:
> ```bash
> cd examples/dist
> java Main
> ```

## Roadmap
- [x] Complete COP 4020 baseline implementation
- [x] Redesign and Refactor
    - [x] Remove semantic info from AST
    - [x] Enforce syntactic correctness in AST constructors
    - [x] Implement a dedicated `Bindings` class
    - [x] Add `Builtins` for common use functions and variables
    - [x] Centralize error handling with `CompilerException`
    - [x] Remove all syntax error handling from Analyzer
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
- [ ] Compiler
    - [ ] Lower from Java source generation to direct Java Bytecode
- [ ] Expand Documentation
    - [ ] Finalize and link /docs files

## Architecture
The transpiler is structured as a sequence of well-defined, ordered, single-responsibility passes. Each pass performs
a distinct transformation on the program representation, following a clear separation of concerns between tokenization,
syntactic analysis, semantic analysis, and code generation.

### Compilation Pipeline (Passes)

> - **Tokenization**  
>   *Source Code* → `Lexer.java` → *Token Stream*
>
> - **Syntactic Analysis**  
>   *Token Stream* → `Parser.java` → *Abstract Syntax Tree (AST)*
>
> - **Semantic Analysis**  
>   *AST* → `Analyzer.java` → *Decorated AST*
>
> - **Bytecode Generation**  
>   *Decorated AST* → `Generator.java` → *Java Bytecode*

Digging a bit deeper:

The lexer (`Lexer.java`) performs lexical analysis (or tokenization), converting raw characters into a stream of 
typed tokens while preserving positional information. The parser (`Parser.java`) iterates over that token stream, 
validating the program syntax against the language grammar and constructs a hierarchical Abstract Syntax Tree (AST) 
that captures the program's syntactic structure, without interpreting semantics or types. 

The analyzer (`Analyzer.java`) then traverses the AST and applies the bindings by means of the languages scoping and 
environment rules, performing semantic analysis such as name resolution, type checking, and local type inference for 
untyped declarations. This pass 'decorates' the AST with resolved symbols and concrete types without altering the 
original syntax. 

Finally, the generator (`Generator.java`) translates the fully analyzed program into executable Java, relying on 
the decorated AST to ensure all identifiers, scopes, and types are resolved prior to code generation.

## Example
```
LET x: Integer = 10;
```

### Tokenization & Syntactic Analysis (Lexing and Parsing)
The lexer tokenizes the provided source code into a typed token stream. For this input, the relevant tokens would 
correspond to:

`token stream = { "LET", "x", ":", "Integer", "=", "10", ";" }`

Because the token stream is syntactically valid, the parser would match it to the field production: 

```ebnf
field ::= "LET" identifier ":" declared_type "=" expression ";"
``` 

With the field identifier mapping to `x`, the declared type mapping to `Integer`, and the initialized expression 
to the literal `10`, through the following precedence chain:

> `expression` → `logical_expression` → `comparison_expression` → `additive_expression` → `multiplicative_expression` 
> → `secondary_expression` → `primary_expression` → `integer`

A simplified tree view of this mapping would look like:

```text
field
 ├─ "LET"
 ├─ identifier("x")
 ├─ ":"
 ├─ identifier("Integer")
 ├─ "="
 ├─ expression
 │   └─ logical_expression
 │       └─ ...
 │           └─ integer("10")
 └─ ";"
```

At this time the parser has enforced only syntax. While the field type (`Integer`) and expression literal type 
(`integer`) do match -- that is to say the statement is semantically correct -- if they did not, the parser would 
not care. Semantics will be checked later.

All programs in FreshlyGround parse from the source entry point. Consequently, this example becomes a `source` 
node containing a single field and no methods:

```yaml
Ast.Source
 └─ fields:
    Ast.Field
     ├─ name: "x"
     ├─ typeName: "Integer"
     ├─ constant: false
     └─ value:
        Ast.Expression.Literal
         └─ literal: 10
 └─ methods: []
```

### Semantic Analysis (Analyzing)
The analyzer performs a pre-order traversal over the AST and applies the language’s scope and environment rules,
using:
- Environment to provide compile-time semantic descriptors of `Type`, `Variable`, and `Function`
- Bindings to match AST nodes to their semantic descriptors
- Scope to model lexical visibility of those descriptors at any point

During this specific pass, it:
- Resolves the field node's declared type to a concrete `Environment.Type.INTEGER`
- Infers and resolves the literal node's type to a concrete `Environment.Type.INTEGER`
- Ensures the two types are compatible according to the languages semantic rules
- Declares `x` in the current scope as an `Environment.Variable`
- Binds the semantic metadata to the respective AST nodes (i.e., "decorates the AST via external bindings")

This results in:
- A populated scope hierarchy
- A global bindings table mapping AST nodes to resolved semantics

### Code Generation (emitting)
The generator traverses the fully analyzed AST and emits Java source code. Because all identifiers, scopes, and 
types are resolved prior to this pass, code generation is a purely mechanical transformation.

For the example above, the generator emits Java equivalent code targeting the JVM, which can then be compiled using 
javac or executed as part of a larger Java program.

Future work will replace Java source emission with direct JVM bytecode generation, eliminating the Java code 
representation entirely.
