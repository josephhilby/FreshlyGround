# Semantic Model
This document specifies the **FreshlyGround semantic layer**. This layer sets the rules that assign *meaning* to
syntactically valid programs by enforcing types, and constructing a scoped binding environment
over the Abstract Syntax Tree (AST).

Semantics define **what programs mean and how they are interpreted**. This layer guarantees that all 
identifiers, expressions, and control structures are well-typed, well-scoped, and resolvable before 
any backend lowers the program into an executable form.

Practically what this means is that after semantic analysis every `Ast.Expression` node 
will resolve directly or indirectly to exactly one `Environment.Type`.

## Environment Model
The `Environment` defines the available semantic entities for use during analysis, and acts as a facade for their use.
Everything mentioned below will be a manifestation of or reference to one of these three entities.

::: tip Semantic Entities
```yaml
Environment
 ├─ type     : Class {
 │                name: String,
 │                internalType: boolean,
 │                scope: Scope }
 ├─ function : Class {
 │                name: String,
 │                parameterTypes: List<Type>,
 │                returnType: Type }
 └─ variable : Class { 
                  name: String, 
                  constant: boolean, 
                  type: Type }
```
:::

## Bindings Model
Bindings provide the primary connection between specific syntax and its referenced semantic entity. They store the 
results of semantic analysis by associating specific AST nodes with their corresponding semantic meaning. 
FreshlyGround follows a static early-binding model, similar to Java, where bindings are determined
as early as possible and recorded during compilation.

::: warning Binding Times
1. **Design Time** — Definition of builtin types and standard library functions
2. **Source Time** — Introduction of user-declared variables and functions within the program
3. **Compile Time** — Resolution of identifiers to variables, functions, and types
:::

::: tip Binding Records
```yaml
Bindings
 ├─ typeBindings        : Map<Ast.Expression, Environment.Type>
 │
 ├─ methodBindings      : Map<Ast.Method, Environment.Function>
 ├─ functionBindings    : Map<Ast.Expression.Function, Environment.Function>
 │
 ├─ fieldBindings       : Map<Ast.Field, Environment.Variable>
 ├─ declarationBindings : Map<Ast.Statement.Declaration, Environment.Variable>
 └─ accessBindings      : Map<Ast.Expression.Access, Environment.Variable>
```
:::

## Scope Model
Scope represents the primary means of looking up a semantic entity for variable access or function calls; 
or, in more technical terms, the region of the program where the bindings are visible. This interplay is 
important as the structure for scopes and bindings are very similar. The key is that Java passes values by reference.

Starting with the semantic entity 'type'. These must be seen throughout the program, so their scope is not limited,
and their looked up occurs directly from the bindings. As such, they are not included in the scope object.

Moving on to the 'variable' and 'function' semantic entities. Both entities have similar lifecycles, they are first
declared then referenced. When they are declared they are recorded in both the bindings and scope. Because their recordings
are done by reference, both records point to the same literal object in memory. So when they are referenced by name (string) in
the scope, then changed, that change is reflected in the binding as well.

Additionally, if a new 'variable' or 'function' is declared with the same name as an existing one, so long as it is in a 
different scope (even a nested scope, a.k.a., shadowing), it will be recorded. This is because, even though it shares
a name (scope lookup), it is a whole new object.

FreshlyGround enforces **Lexical (static)** as well as **Type** scoping. Additionally, both scopes form a parent-linked 
chain corresponding to nested program structure.

::: tip Scope Structure

```yaml
Scope
 ├─ parent    : Optional<Scope>
 ├─ variables : Map<String, Environment.Variable>
 └─ functions : Map<String, Environment.Function>
```

:::

::: info Note 
The function must be searched by both identifier and arity, "name + '/' + arity".
:::

::: warning Scope Rules
1. Declarations 
   * Bind a `Environment.Variable` or `Environment.Function` in the **current lexical scope**
   * **Shadowing is allowed** across nested scopes
   * **Redeclaration in the same scope is forbidden**

2. Lexical Resolutions
   * Lookup `Environment.Variable` or `Environment.Function` in the **current lexical scope**
   * If not found, resolution proceeds recursively through **parent scopes**
   * Failure to resolve results in a compile-time error

3. Member Resolutions
   * Lookup an `Environment.Function` in the **current type scope**
   * See, Member Function Calls below

4. Nesting Lexical Scopes
   * Occurs within: method bodies, and conditional blocks

:::

### Builtin Types and Standard Library Functions
The FreshlyGround language contains multiple builtin types. These types are defined as singletons and organized in a
nested scope chain. This allows standard library member functions to be defined in a specific type scope while still
allowing access to additional types (see, Member Function Calls below).

::: tip Type Scope Chain
```yaml
Any
├─ Primitive (internal type)
│   ├─ Integer
│   ├─ Decimal
│   ├─ Character
│   └─ Boolean
├─ String
└─ Nil
```
:::

::: warning Standard Library Functions
- `Environment.Type.ANY`
   - `print(message)` - outputs a message to the terminal
   - `input(message)` - outputs a message to the terminal, then waits to collect user input
- `Environment.Type.PRIMITIVE`
   - `stringify(data)` - converts a primitive datatype to a string
- `Environment.Type.STRING`
   - `length()` - returns the length of a string in characters
   - `slice(a, b)` - slices string from character a to b, inclusive distance
:::

#### Member Function Calls
As discussed, each `Environment.Variable` contains an `Environment.Type`. That type in turn contains a `Scope` defined
and set with builtin member functions at Design Time. To access these member builtins, FreshlyGround
uses a dot operator (`.`) to shift the resolution context from the current lexical scope to the type scope owned by the
variable expression’s resolved type or parent types.

To illustrate this, assume there exists a variable:
```yaml
Number : Integer = 1234
```

The `Environment.Type.PRIMITIVE` contains:
- function `stringify()` with return type `String`

So calling...
- First looks for `stringify()` in the `Environment.Type.INTEGER` scope and fails to find it
- Then recursively moves to look for `stringify()` in the `Environment.Type.PRIMITIVE` scope and finds it
- Now, `Number.stringify` can be resolved to type `String`

## Semantic Model

### Root

::: tip **Ast.Source**
AST mapping:

```yaml
Ast.Source
 ├─ fields  : List<Ast.Field>
 └─ methods : List<Ast.Method>
```

Rules:

* **[Rule]** `main/0` (`method/arity`) must exist in the methods
* **[Rule]** `main/0` must return `Integer`

>**Legend:**
>- **[Rule]** — semantic restriction
>- **(T: …)** — type constraint

:::

### Top-Level

::: tip **Ast.Field**
AST mapping:

```yaml
Ast.Field
 ├─ name     : String
 ├─ typeName : String
 ├─ constant : boolean
 └─ value    : Optional<Ast.Expression>
```

Rules:

* **[Rule]** If `constant=true`, `value` must be present
* **[Rule]** If `value` is present, **(T: `value.type` assignable to `typeName`)**
* **[Rule]** Declares an `Environment.Variable` in the current scope

:::

::: tip **Ast.Method**
AST mapping:

```yaml
Ast.Method
 ├─ name           : String
 ├─ parameters     : List<String>
 ├─ parameterTypes : List<String>
 ├─ returnTypeName : Optional<String>
 └─ statements     : List<Ast.Statement>
```

Rules:

* **[Rule]** Method body is analyzed in a **new nested scope**
* **[Rule]** `parameterTypes` must resolve to `Environment.Type`
* **[Rule]** `returnTypeName` defaults to `Nil` if omitted
* **[Rule]** Each `RETURN` must satisfy **(T: `value.type` assignable to `returnType`)**
* **[Rule]** Declares an `Environment.Function` in the current scope

:::

### Statements
#### Variables

::: tip **Ast.Statement.Declaration**
AST mapping:

```yaml
Ast.Statement.Declaration
 ├─ name     : String
 ├─ typeName : Optional<String>
 └─ value    : Optional<Ast.Expression>
```

Rules:

* **[Rule]** If `value` present and valid, `typeName` is inferred from `value.type`
* **[Rule]** If `typeName` present, it must resolve to `Environment.Type`
* **[Rule]** If both present, **(T: `value.type` assignable to `typeName`)**
* **[Rule]** Declares an `Environment.Variable` in the current scope

:::

::: tip **Ast.Statement.Assignment**
AST mapping:

```yaml
Ast.Statement.Assignment
 ├─ receiver : Ast.Expression.Access
 └─ value    : Ast.Expression
```

Rules:

* **[Rule]** Receiver must not be constant
* **[Rule]** **(T: `value.type` assignable to `receiver.type`)**

:::

#### Functions

::: tip **Ast.Statement.Expression**
AST mapping:

```yaml
Ast.Statement.Expression
 └─ expression : Ast.Expression.Function
```

**Note:** Function calls are modeled as expressions because they evaluate to a value. `Ast.Statement.Expression`
exists to permit an expression — specifically a function call — to appear in statement position when its resulting 
value is not used. This allows calls that exist purely for their side effects (e.g., `print(String)`) to stand 
alone as complete statements.

Rules: See, `Ast.Expression.Function`

:::

#### Conditional

::: tip **Ast.Statement.If**
AST mapping:

```yaml
Ast.Statement.If
 ├─ condition      : Ast.Expression
 ├─ thenStatements : List<Ast.Statement>
 └─ elseStatements : List<Ast.Statement>
```

Rules:

* **[Rule]** **(T: `condition.type` must resolve to `Boolean`)**
* **[Rule]** `thenStatements` must be non-empty
* **[Rule]** Then/Else bodies analyzed in **new nested scopes**

:::

::: tip **Ast.Statement.For**
AST mapping:

```yaml
Ast.Statement.For
 ├─ initialization : Optional<Ast.Statement.Assignment>
 ├─ condition      : Ast.Expression
 ├─ increment      : Optional<Ast.Statement.Assignment>
 └─ statements     : List<Ast.Statement>
```

Rules:

* **[Rule]** **(T: `condition.type` must resolve to `Boolean`)**
* **[Rule]** If `initialization` exists, **(T: `init.receiver` must resolve to `Integer`)**
* **[Rule]** If `increment` exists, **(T: `inc.receiver` must resolve to `Integer`)**
* **[Rule]** If `increment` and `initialization` exist, **(T: `init.receiver` must be the same variable as `inc.receiver`)**
* **[Rule]** Statements body analyzed in a **new nested scope**

:::

::: tip **Ast.Statement.While**
AST mapping:

```yaml
Ast.Statement.While
 ├─ condition  : Ast.Expression
 └─ statements : List<Ast.Statement>
```

Rules:

* **[Rule]** **(T: `condition.type` assignable to `Boolean`)**
* **[Rule]** Statements body analyzed in a **new nested scope**

:::

#### Return

::: tip **Ast.Statement.Return**
AST mapping:

```yaml
Ast.Statement.Return
 └─ value : Ast.Expression
```

Rules:

* **[Rule]** **(T: `value.type` assignable to current method `returnType`)**

:::

### Expressions
#### Binary

::: tip **Ast.Statement.Binary**
##### Logical

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "AND" | "OR"
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** **(T: `left.type` and `right.type` assignable to `Boolean`)**
* **[Rule]** `result.type` must resolve to `Boolean`

##### Comparison (Equality)

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "==" | "!="
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** **(T: `left.type` must be the same as `right.type`)**
* **[Rule]** `result.type` must resolve to `Boolean`

##### Comparison (Inequality)

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "<" | "<=" | ">" | ">="
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** **(T: `left.type` and `right.type` assignable to `Primitive`)**
* **[Rule]** **(T: `left.type` and `right.type` must not be `Boolean`)**
* **[Rule]** **(T: `left.type` must be the same as `right.type`)**
* **[Rule]** `result.type` must resolve to `Boolean`

##### Arithmetic

AST mapping:

```yaml
Ast.Expression.Binary
├─ operator : "+" | "-" | "*" | "/"
├─ left     : Ast.Expression
└─ right    : Ast.Expression
```

Rules:

* **[Rule]** If operator is `+` and either operand is `String`, `result.type` will be `String`
* **[Rule]** Otherwise, **(T: both operands must be `Integer` or both `Decimal`)**
* **[Rule]** `result.type` must resolve to the numeric operand type (`Integer` or `Decimal`)

:::

#### Member Access and Function Calls
##### Resolution and Lowering

FreshlyGround defaults to resolving fields and functions within the current lexical scope, however
it also supports a dot (`.`) operator for accessing type-associated variables and functions. The dot 
expressions are resolved against the static type of the left operand (i.e., receiver).

Although the language is not object-oriented it's not unfair to think of the type singletons as an almost 
proto-object for the FreshlyGround language.

##### Member Access

::: tip **Ast.Expression.Access**

AST mapping:

```yaml
Ast.Expression.Access
 ├─ receiver : Optional<Ast.Expression>
 └─ name     : String
```

Rules:

* **[Rule]** If `receiver` = `null`, resolve name in the **current lexical scope**
* **[Rule]** If `receiver` != `null`, resolve name in the **receiver’s type scope**

###### Access Resolution Model
Given:

    receiver.member

1. Evaluate `receiver` to type **T**
2. Resolve `member` inside the **type scope of T**

:::

##### Function Call

::: tip **Ast.Expression.Function**
AST mapping:

```yaml
Ast.Expression.Function
 ├─ receiver  : Optional<Ast.Expression>
 ├─ name      : String
 └─ arguments : List<Ast.Expression>
```

Rules:

* **[Rule]** Resolve function via scope (lexical or type scope)
* **[Rule]** Arguments analyzed **left-to-right**
* **[Rule]** Each argument must be **assignable to the corresponding parameter type**
* **[Rule]** If `receiver` != `null`, implicit receiver is prepended to argument list

###### Function Resolution Model
Given:

    receiver.function(a, b)

1. Evaluate `receiver` to type **T**
2. Increment arity for lookup
2. Resolve `function/3` inside the **type scope of T**

The expression is then lowered to:

    function(receiver, a, b)

As the receiver is inserted as the first argument.

:::

#### Primary Expressions

::: tip **Ast.Expression.Group**

AST mapping:

```yaml
Ast.Expression.Group
 └─ expression : Ast.Expression
```

Rules:

* **[Rule]** `group.type` must be the same as `expression.type`

:::

::: tip **Ast.Expression.Literal**

AST mapping:

```yaml
Ast.Expression.Literal
 └─ literal : Object
```

:::

::: warning Type Map (java object → Environment.Type):

* `null`       → `Environment.Type.NIL`
* `Boolean`    → `Environment.Type.BOOLEAN`
* `BigInteger` → `Environment.Type.INTEGER`
* `BigDecimal` → `Environment.Type.DECIMAL`
* `Character`  → `Environment.Type.CHARACTER`
* `String`     → `Environment.Type.STRING`

Note: Integer and Decimal are bounded within a 32-bit int and 64-bit double

:::