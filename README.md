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
FreshlyGround is a **novel programming language compiler** whose source code is compiled into bytecode
for execution on the **Java Virtual Machine (JVM)**. This project was adapted from an academic compiler
project created in **COP 4020** at the **University of Florida**, and loosely follows the methodology 
outlined in the book [*Crafting Interpreters*](https://www.craftinginterpreters.com/).

The compiler is structured as a sequence of well-defined, ordered, single-responsibility passes. Each pass performs 
a distinct transformation on the program representation, following a clear separation of concerns between tokenization, 
syntactic analysis, semantic analysis, and bytecode generation.

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

The lexer (`Lexer.java`) performs lexical analysis (or tokenization), converting raw characters into a stream of typed tokens while preserving 
positional information. The parser (`Parser.java`) iterates over the token stream, validating the program syntax against the 
language grammar and constructs a hierarchical Abstract Syntax Tree (AST) that captures the program's syntactic structure, 
without interpreting semantics or types. 

The analyzer (`Analyzer.java`) then traverses the AST and applies the languages scoping and environment rules,
performing semantic analysis such as name resolution, type checking, and local type inference for untyped declarations. 
This pass 'decorates' the AST with resolved symbols and concrete types without altering the original syntax. 

Finally, the generator (`Generator.java`) lowers the fully analyzed program into executable JVM bytecode, relying on 
the decorated AST to ensure all identifiers, scopes, and types are resolved prior to code generation.

## Example
```
LET x: Integer = 10;
```
While the above snippet is valid, by itself it would fail to compile. This is because FreshlyGround requires a `main()` 
method as an entry point in all source code files. For simplicity, we will not include a `main()` method in this example.

### Tokenization & Syntactic Analysis (Lexing and Parsing)
The lexer tokenizes the source code into a typed token stream. For this input, the relevant tokens correspond to:

`token stream = { "LET", "x", ":", "Integer", "=", "10", ";" }`

Because the token stream is syntactically valid, the parser matches it to the field production: 

```ebnf
field ::= "LET" [ CONST ] identifier ":" identifier [ "=" expression ] ";"
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
 └─ fields: [
    Ast.Field
     ├─ name: "x"
     ├─ typeName: "Integer"
     ├─ constant: false
     └─ value:
        Ast.Expression.Literal
         └─ literal: 10
     ]
 └─ methods: []
```

### Semantic Analysis (Analyzing)
The analyzer performs a pre-order traversal over the AST and applies the language’s scope and environment rules,
using:
- Environment to provide compile-time semantic descriptors of `Type`, `Variable`, and `Function`
- Bindings to match AST nodes to their semantic descriptors
- Scope to model lexical visibility of those descriptors at any point

During this specific pass, it:
- Resolves the field node's declared type to a concrete `Environment.Type`
- Infers and resolves the literal node's type to a concrete `Environment.Type`
- Ensures the two types are compatible according to the languages semantic rules
- Declares `x` in the current scope as an `Environment.Variable`
- Binds the variable and type metadata to the respective AST nodes (i.e., "decorates the AST via external bindings")

```text
// Current Scope
Scope{ parent=null, 
       variables={ 
           "x" -> Environment.Variable(
               name="x",
               jvmName="x",
               constant=false,
               type=Environment.Type(
                   name="Integer", 
                   jvmName="int",
                   // triple nested as `Integer` has a scope chain (Integer → Comparable → Any)
                   scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
       )},
       functions={}
}
```

```text
// Global Bindings
Binding{ 
    Ast.Field("x") -> Environment.Variable(
        name="x",
        jvmName="x",
        constant=false,
        type=Environment.Type(
            name="Integer",
            jvmName="int",
            // triple nested as `Integer` has a scope chain (Integer → Comparable → Any)
            scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
    
    Ast.Expression.Literal(10) -> Environment.Type(
        name="Integer",
        jvmName="int",
        scope=Integer.scope ⊆ Comparable.scope ⊆ Any.scope)
    
}
```

## Documentation 
(TODO - fill out docs files and add links with small descriptions)