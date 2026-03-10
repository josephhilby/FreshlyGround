# Backend Specification

This document specifies the **FreshlyGround WebAssembly (WAT) backend**: the lowering rules, runtime 
interface (host ABI), and representation strategy for compiling semantically-analyzed FreshlyGround programs 
into WebAssembly Text Format (WAT).

The WAT backend is one concrete implementation of the modular backend interface defined in the compiler pipeline.

It:

* consumes **AST + Bindings**
* assumes semantic correctness has already been proven
* performs mechanical lowering only
* emits **WAT source**
* does not perform name resolution or type checking

---

## Backend Overview

The WAT backend targets a **browser-hosted execution environment**.

Execution model:

* The module exports a single function: `main`
* `main` returns `i32` (matching `Integer`)
* Output is routed through host-provided imports
* All state is stored in:

    * WASM locals
    * WASM globals
    * Linear memory (for strings)

---

## Type Mapping

FreshlyGround types lower into WASM value types as follows:

| FreshlyGround | WASM Representation    |
| ------------- | ---------------------- |
| `Integer`     | `i32`                  |
| `Boolean`     | `i32` (`0` or `1`)     |
| `Decimal`     | `f64`                  |
| `Nil`         | no value / omitted     |
| `String`      | `(ptr: i32, len: i32)` |

### Boolean Convention

Booleans are represented as `i32`:

* `0` → false
* `1` → true

Logical operations should normalize to `0/1`.

---

## String Representation

Strings are represented using linear memory:

```
String := (ptr: i32, len: i32)
```

* `ptr` → byte offset in linear memory
* `len` → number of UTF-8 bytes

String literals are emitted using **data segments**.

Example:

```wat
(data (i32.const 1024) "hello")
```

The backend assigns fixed offsets for literals.

---

## Host ABI (Imports)

The WAT backend requires the following imports:

```wat
(import "env" "print_i32" (func $print_i32 (param i32)))
(import "env" "print_str" (func $print_str (param i32 i32)))
```

### Contracts

**print_i32**

* prints signed integer

**print_str**

* reads `len` bytes from memory starting at `ptr`
* decodes as UTF-8
* prints to console

The host environment (browser runtime) must implement these.

---

## Module Skeleton

Every compiled module follows this structure:

```wat
(module
  (import "env" "print_i32" (func $print_i32 (param i32)))
  (import "env" "print_str" (func $print_str (param i32 i32)))

  (memory (export "memory") 1)

  ;; optional data segments for string literals
  ;; (data (i32.const ...) "...")

  (func $main (export "main") (result i32)
    ;; emitted body
    (i32.const 0))
)
```

---

## Lowering Rules

Lowering is mechanical and driven entirely by semantic bindings.

---

### Variables

| Source Construct | WAT              |
| ---------------- | ---------------- |
| local variable   | `local.get/set`  |
| field/global     | `global.get/set` |

Fields may be lowered as module globals:

```wat
(global $x (mut i32) (i32.const 0))
```

---

### Integer Literals

```
Ast.Expression.Literal(Integer)
→ (i32.const N)
```

---

### String Literals

```
Ast.Expression.Literal(String)
→ (i32.const ptr)
  (i32.const len)
```

Where `(ptr,len)` references a data segment.

---

### Arithmetic (Integer)

| Operator | WAT         |
| -------- | ----------- |
| `+`      | `i32.add`   |
| `-`      | `i32.sub`   |
| `*`      | `i32.mul`   |
| `/`      | `i32.div_s` |

---

### Comparisons

All comparisons produce `i32` boolean:

| Operator | WAT        |
| -------- | ---------- |
| `==`     | `i32.eq`   |
| `!=`     | `i32.ne`   |
| `<`      | `i32.lt_s` |
| `<=`     | `i32.le_s` |
| `>`      | `i32.gt_s` |
| `>=`     | `i32.ge_s` |

---

### Logical Operators

Lower as integer operations:

| Operator | WAT       |
| -------- | --------- |
| `AND`    | `i32.and` |
| `OR`     | `i32.or`  |

Operands should be canonicalized to `0/1`.

---

### Control Flow

WASM uses structured control flow.

#### If

```wat
(if (result i32)
  (then ...)
  (else ...))
```

#### While

Lowered using block/loop pattern:

```wat
(block
  (loop
    ;; condition
    br_if 1   ;; exit block if false
    ;; body
    br 0      ;; repeat loop
  )
)
```

---

### Function Calls

Resolved functions lower to direct calls:

```wat
call $function_name
```

If member call lowering inserted receiver as first argument during analysis, backend simply emits arguments in order.

---

## Printing

### Printing Integer

```
print(expr: Integer)
→ compile expr
→ call $print_i32
```

### Printing String

```
print(expr: String)
→ compile expr (ptr,len)
→ call $print_str
```

---

## Example

### FreshlyGround Source

```fg
DEF main() : Integer DO
  print(10);
  print("hello");
  RETURN 0;
END
```

### Lowered WAT

```wat
(module
  (import "env" "print_i32" (func $print_i32 (param i32)))
  (import "env" "print_str" (func $print_str (param i32 i32)))

  (memory (export "memory") 1)

  (data (i32.const 1024) "hello")

  (func $main (export "main") (result i32)

    (i32.const 10)
    call $print_i32

    (i32.const 1024)
    (i32.const 5)
    call $print_str

    (i32.const 0))
)
```

---


## Navigation

* Next: N/A
* Previous: [Semantic Model & Bindings](./04_semantics.md)
* Index: [Overview & Index](./00_index.md)
