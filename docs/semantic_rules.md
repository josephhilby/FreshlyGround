### Semantic Model
FreshlyGround follows a static, early-binding semantic model, similar to Java. The compiler resolves names,
types, and scopes at compile time in order to produce portable and efficient JVM bytecode. Runtime execution is
delegated entirely to the JVM.

#### Definitions
>- Name := a symbolic identifier in the source program (e.g., x)
>- Value := the data or object a name refers to at runtime (e.g., 10)
>- Binding := the association between a name and its meaning (i.e., Environment.Type/Variable/Function)
>- Binding Time := the point at which the binding is created
>- Lifetime := period of time from binding creation to destruction
>- Scope := region of the program where a binding is visible

#### Binding Times
Bindings are established progressively throughout the compilation process:

> 1. Design: language grammar, syntax rules, and primitive type definitions
> 2. Implementation: Mapping language-level types to JVM representations (Environment.Type)
> 3. Writing: Variable and function declarations in source code
> 4. Compile: Name resolution, scope construction, and type checking (Environment.Variable/Function)

Runtime value binding is left to the JVM.

#### Scope Rules
FreshlyGround enforces lexical (static) scoping with the following rules:

> 1. Declarations bind names in the current scope
> 2. References are resolved by walking outward through parent scopes
> 3. Redeclaration outside same scope (Shadowing) is allowed.
> 4. Redeclaration in same scope is not allowed.