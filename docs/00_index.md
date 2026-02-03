# FreshlyGround Language & Compiler Reference

This section documents the complete FreshlyGround language and compiler stack, from raw source text to executable 
targets (JVM and WebAssembly). Each layer is built strictly from the layer below it, forming a clear abstraction 
ladder from syntax to semantics to machine execution.

Language layers define surface structure and meaning (grammar, AST, scope, and types). Backend layers define execution 
representation (bytecode, WASM, or JVM artifacts) that are deterministically lowered into runtime behavior.

## Abstraction Ladder (Construction vs. Conceptual Role)
| Construction             | Conceptual Role                                 |
| ------------------------ | ----------------------------------------------- |
| **Source Text**          | Human-facing program representation             |
| **Tokens** (`Lexer`)     | Lexical units (typed symbols)                   |
| **Grammar / Parse Tree** | Formal syntax structure                         |
| **AST**                  | Syntactic abstraction (tree model)              |
| **Scope**                | Lexical visibility model                        |
| **Bindings**             | Semantic attachment (types, variables, symbols) |
| **Semantic Model**       | Meaning (type system, compatibility, rules)     |
| **Intermediate Form**    | Backend-neutral program representation          |
| **Backend** (JVM / WASM) | Execution abstraction                           |
| **Runtime Environment**  | Machine behavior (JVM / Browser / Host ABI)     |

*Note:* The ladder is bidirectional. Language layers are constructed upward from source text to semantic meaning, 
while backend layers are lowered downward from semantic form into executable machine artifacts.

## Sections
1. [Language Grammar (EBNF)](./01_syntax.md)
2. [Abstract Syntax Tree (AST Map)](./02_ast_map.md)
3. [Semantic Model & Bindings](./03_semantics.md)
4. [Compiler Pipeline](./04_pipeline.md)
5. [WebAssembly Backend](./05_wasm_backend.md)

## Reading Guide
- Start at **Language Grammar → AST Map** to understand the formal structure of FreshlyGround programs
- Move to **Semantic Model & Bindings** to see how meaning, scope, and types are assigned
- Use **Compiler Pipeline** to understand how representations flow between passes
- Refer to **WebAssembly Backend** to see how high-level semantics are lowered into concrete execution environments
