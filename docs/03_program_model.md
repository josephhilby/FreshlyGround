# Program Model

This document specifies FreshlyGround's program model. This model is used to map the previously discussed
context-free grammar onto an encoded **Abstract Syntax Tree (AST)**. This AST will serve as the primary structured, 
intermediate representation for any code written in the FreshlyGround language. It will capture that 
**program's form without semantic meaning**. It encodes hierarchy, conditional logic, and overall grammatical structure.

::: info Note
Some of the EBNF definitions have been modified to better show how the map to their respective AST class.
:::

## Root

At the top most level is `Ast.Source`. This will serve as the programs entry point for any target representation.

::: tip **Ast.Source**

Grammar:

```ebnf
source ::= { fields } { methods }
```

AST mapping:

```yaml
Ast.Source
 ├─ fields  : List<Ast.Field>
 └─ methods : List<Ast.Method>
```

:::

## Top-Level

* **Fields** define global storage
* **Methods** define executable entry points

::: tip **Ast.Field**

Grammar:

```ebnf
field ::= "LET" [ "CONST" ] name ":" typeName [ "=" value ] ";"
```

AST mapping:

```yaml
Ast.Field
 ├─ name     : String
 ├─ typeName : String
 ├─ constant : boolean
 └─ value    : Optional<Ast.Expression>
```

:::

::: tip **Ast.Method**

Grammar:

```ebnf
method ::= 
"DEF" name "(" [ parameter(s) ":" parameterType(s) ] ")" [ ":" returnType ] 
"DO"
    { statements }
"END"
```

AST mapping:

```yaml
Ast.Method
 ├─ name           : String
 ├─ parameters     : List<String>
 ├─ parameterTypes : List<String>
 ├─ returnTypeName : Optional<String>
 └─ statements     : List<Ast.Statement>
```

:::

## Statements

### Variables

::: tip **Ast.Statement.Declaration**

Grammar:

```ebnf
statement_declaration ::= "LET" name [ ":" typeName ] [ "=" value ] ";"
```

AST mapping:

```yaml
Ast.Statement.Declaration
 ├─ name     : String
 ├─ typeName : Optional<String>
 └─ value    : Optional<Ast.Expression>
```

Rules:
- Must have `value` or `typeName`

:::

::: tip **Ast.Statement.Assignment**

Grammar:

```ebnf
statement_assignment ::= receiver "=" value ";"
```

AST mapping:

```yaml
Ast.Statement.Assignment
 ├─ receiver : Ast.Expression.Access
 └─ value    : Ast.Expression
```

:::

### Functions

::: tip **Ast.Statement.Expression**

Grammar:

```ebnf
statement_expression ::= expression ";"
```

AST mapping:

```yaml
Ast.Statement.Expression
 └─ expression : Ast.Expression.Function
```

:::

### Conditional Logic

::: tip **Ast.Statement.If**

Grammar:

```ebnf
statement_if ::=
"IF" condition "DO"
    { thenStatements }
[ "ELSE"
    { elseStatements } ]
"END"
```

AST mapping:

```yaml
Ast.Statement.If
 ├─ condition      : Ast.Expression
 ├─ thenStatements : List<Ast.Statement>
 └─ elseStatements : List<Ast.Statement>
```

Rules:
- `thenStatements` must be non-empty

:::

### Loops

::: tip **Ast.Statement.For**

Grammar:

```ebnf
statement_for ::=
"FOR" "(" [ initialization ] ";" condition ";" [ increment ] ")"
    { statements }
"END"
```

AST mapping:

```yaml
Ast.Statement.For
 ├─ initialization : Optional<Ast.Statement.Assignment>
 ├─ condition      : Ast.Expression
 ├─ increment      : Optional<Ast.Statement.Assignment>
 └─ statements     : List<Ast.Statement>
```

Rules:
- `condition` must be present

:::

::: tip **Ast.Statement.While**

Grammar:

```ebnf
statement_while ::=
"WHILE" condition "DO"
  { statements }
"END"
```

AST mapping:

```yaml
Ast.Statement.While
 ├─ condition  : Ast.Expression
 └─ statements : List<Ast.Statement>
```

Rules:
- `condition` must be present

:::

### Return

::: tip **Ast.Statement.Return**

Grammar:

```ebnf
statement_return ::= "RETURN" value ";"
```

AST mapping:

```yaml
Ast.Statement.Return
 └─ value : Ast.Expression
```

:::

## Expressions
### Binary
All infix operators are normalized into a single binary node type.

::: tip **Ast.Expression.Binary**
Grammar:

```ebnf
expression_binary ::= left operator right
```

AST mapping:

```yaml
Ast.Expression.Binary
 ├─ operator : String
 ├─ left     : Ast.Expression
 └─ right    : Ast.Expression
```

Infix Operators:

* Logical operators: { `AND`, `OR` }
* Comparison operators: { `<`, `<=`, `>`, `>=`, `==`, `!=` }
* Arithmetic operators: { `+`, `-`, `*`, `/` }

:::

### Member Access and Function Calls
Recall that in the CFG, variable access and function calls were split between 
**primary-** and **secondary-expressions**. Where the primary-expression allowed for 
a direct reference to the variable or function and the secondary-expression allowed
for recursive chained-member access call(s). Here both are unified here under the 
**receiver-based model**.

::: warning Receiver-Based Model
* No receiver, represents an access/call to the referenced variable or function, in the current or parent lexical scope(s)
* A receiver, represents a member access/call in the receiver's type scope
* This allows for calling specialized functions like `"string".length()`
  * Or recursively calling specialized functions like `"string".slice(1,3).length()`
:::

Semantic resolution of this construct and discussion of these specialized variables/functions (Standard Library) 
can be found in the [next section](04_semantics.md).

#### Member Access

::: tip **Ast.Expression.Access**
Grammar fragment:

```ebnf
expression_access ::= [ receiver "." ] name
```

AST mapping:

```yaml
Ast.Expression.Access
 ├─ receiver : Optional<Ast.Expression>
 └─ name     : String
```

:::

#### Function Call

::: tip **Ast.Expression.Function**
Grammar fragment:

```ebnf
expression_function ::= [ receiver "." ] name "(" [ arguments ] ")"
```

AST mapping:

```yaml
Ast.Expression.Function
 ├─ receiver  : Optional<Ast.Expression>
 ├─ name      : String
 └─ arguments : List<Ast.Expression>
```

:::

### Primary Expressions

::: tip **Ast.Expression.Group**

Grammar:

```ebnf
expression_group ::= "(" expression ")"
```

AST mapping:

```yaml
Ast.Expression.Group
 └─ expression : Ast.Expression
```

:::

::: tip **Ast.Expression.Literal**

```ebnf
expression_literal ::= literal
```

AST mapping:

```yaml
Ast.Expression.Literal
 └─ literal : Object
```

:::

::: warning Type Map (token → java object):

* `identifier`
    * `"NIL"`                → `null`
    * (`"TRUE"` | `"FALSE"`) → `Boolean`
* `integer`              → `BigInteger`
* `decimal`              → `BigDecimal`
* `character`            → `Character`
* `string`               → `String`

:::