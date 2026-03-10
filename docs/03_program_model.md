# Program Model

This document specifies FreshlyGround's program model. This model is used to map the previously discussed
context-free grammar onto an encoded **Abstract Syntax Tree (AST)**. This AST will serve as the primary structured, 
intermediate representation for any code written in the FreshlyGround language. It will capture that 
**program's form without semantic meaning**. It encodes hierarchy, conditional logic, and overall grammatical structure.

::: info Note
Some of the EBNF definitions have been modified to better show how the map to their respective AST class.
:::

## Root

At the top most level is `Ast.Source`. This will serve as the programs entry point in any target representation.

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

* **Fields** define global storage
* **Methods** define executable entry points

:::

## Top-Level

lorem

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
statement_declaration ::= "LET" name [ ":" type ] [ "=" value ] ";"
```

AST mapping:

```yaml
Ast.Statement.Declaration
 ├─ name     : String
 ├─ typeName : Optional<String>
 └─ value    : Optional<Ast.Expression>
```

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

:::

### Return

Grammar:

```ebnf
statement_return ::= "RETURN" value ";"
```

AST mapping:

```yaml
Ast.Statement.Return
 └─ value : Ast.Expression
```

---

## Expressions
### Binary Operations
All infix operators are normalized into a single binary node type.

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

### Member Access and Function Calls
Please note that in the CFG, these were split among primary- and secondary-expressions. However, 
both are unified here under a **receiver-based model**, where:

* `receiver = null` represents an unqualified name
* `receiver != null` represents member access

Semantic resolution of this construct is defined in the next section.

#### Member Access
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

#### Function Call

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

### Primary Expressions
#### Grouping

Grammar:

```ebnf
expression_group ::= "(" expression ")"
```

AST mapping:

```yaml
Ast.Expression.Group
 └─ expression : Ast.Expression
```

#### Literals

```ebnf
expression_literal ::= literal
```

AST mapping:

```yaml
Ast.Expression.Literal
 └─ literal : Object
```

Type Map (literal → java object):

* `"NIL"`                → `null`
* (`"TRUE"` | `"FALSE"`) → `Boolean`
* `integer`              → `BigInteger`
* `decimal`              → `BigDecimal`
* `character`            → `Character`
* `string`               → `String`
