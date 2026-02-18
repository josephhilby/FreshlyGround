# 05 — WebAssembly (WASM) Backend Specification

This document specifies the **FreshlyGround WebAssembly backend**: the lowering rules, runtime interface 
(host ABI), and representation strategy for compiling analyzed FreshlyGround programs into WebAssembly.

The WASM backend targets a **browser-hosted execution environment** where programs are compiled to WebAssembly and 
executed client-side, with output routed to a web console via a minimal set of host-provided imports.

---

## Navigation

* Index: [Overview & Index](00_index.md)
* Previous: [Compiler Pipeline](04_pipeline.md)
* Next: N/A

---

## Design Notes

### Why WebAssembly

* WebAssembly is a **portable, sandboxed execution target** supported natively by modern browsers.
* It provides a deterministic runtime for a language playground without shipping a custom VM.
* It cleanly separates execution from I/O via host imports.

### Emission Strategy

The backend emits:

* **WAT** (WebAssembly Text) as a human-readable intermediate (recommended during development)
* optionally **WASM binary** as a later optimization step

WAT provides:

* easy inspection and debugging
* straightforward test fixtures (golden outputs)
* rapid iteration on lowering rules

### Backend Contract

The WASM backend is a **mechanical lowering** pass.

* It consumes **AST + Bindings**
* It assumes semantic correctness has already been proven
* It does not perform name resolution or type checking

---

## Execution Model

FreshlyGround lowers into a single exported function entrypoint:

* `main` is exported and invoked by the host
* `main` returns an `i32` (matching the language requirement that `main/0` returns `Integer`)

```text
(export "main" (func $main))
```

All program state is represented using:

* WASM **locals** (function locals)
* WASM **linear memory** (for heap-backed data such as strings)

---

## Type Mapping

FreshlyGround types are mapped into WASM value types.

### Primitive Types

| FreshlyGround | WASM Type | Notes                                                             |
| ------------- | --------- | ----------------------------------------------------------------- |
| `Integer`     | `i32`     | 32-bit signed integer                                             |
| `Boolean`     | `i32`     | `0` = false, non-zero = true (canonicalize to `0/1` for prints)   |
| `Decimal`     | `f64`     | optional; can be added later                                      |
| `Nil`         | —         | represented as absence of value; or `i32 0` sentinel where needed |

### Strings

Strings are represented as:

* a pointer to bytes in linear memory
* a length in bytes

```text
String := (ptr: i32, len: i32)
```

This is the canonical ABI shape used at the host boundary.

---

## Host ABI (Imports)

The backend relies on a minimal set of host-provided functions. The host is typically the web UI runtime.

### Printing

The only required observable output for the web environment is printing.

#### `print_i32`

```wat
(import "env" "print_i32" (func $print_i32 (param i32)))
```

Contract:

* prints a signed integer value to the console
* implementation is owned by the host

#### `print_str`

```wat
(import "env" "print_str" (func $print_str (param i32 i32)))
;; (ptr, len)
```

Contract:

* reads `len` bytes starting at `ptr` from the module’s linear memory
* decodes as UTF-8 (recommended)
* prints to the console

### Optional Helpers

These are not required for a minimal playground, but are common extensions:

* `print_ln()` to print a newline
* `read_line(ptr, cap) -> len` for interactive input

---

## Memory Layout

The WASM module defines a single linear memory and uses a simple bump allocator for strings.

### Minimum Definition

```wat
(memory (export "memory") 1)
(global $heap (mut i32) (i32.const 1024))
```

Notes:

* memory page size is 64KiB; `1` page is sufficient for early programs
* `$heap` points to the next free byte

### String Allocation (Conceptual)

To emit a string literal:

1. reserve `len` bytes at `$heap`
2. write the UTF-8 bytes into memory
3. return `(ptr, len)`
4. advance `$heap += len`

The exact writing mechanism depends on whether the backend emits:

* inline data segments (`(data (i32.const ...) "...")`) for literals, or
* stores bytes procedurally (slower; not recommended)

Recommended for literals:

```wat
(data (i32.const 1024) "hello")
```

---

## Module Skeleton (Reference)

This skeleton represents the minimum structure expected from the WASM backend.

```wat
(module
  (import "env" "print_i32" (func $print_i32 (param i32)))
  (import "env" "print_str" (func $print_str (param i32 i32)))

  (memory (export "memory") 1)

  ;; optional: heap pointer for dynamic strings
  (global $heap (mut i32) (i32.const 1024))

  ;; optional: data segments for string literals
  ;; (data (i32.const 1024) "...")

  (func $main (export "main") (result i32)
    ;; body emitted here
    (i32.const 0))
)
```

---

## Lowering Rules (Small to Large)

This section defines how analyzed AST nodes lower into WASM constructs.

### Names and Locals

* Each variable binding is assigned a WASM local index
* Global fields may be lowered as:

    * module globals (`(global $x (mut i32) ...)`) for simple primitives, or
    * linear memory slots for uniformity

Recommended early strategy:

* fields as module globals for `Integer/Boolean`
* locals as function locals

### Integer Literals

```text
Ast.Expression.Literal(Integer)
  → (i32.const N)
```

### String Literals

String literals lower to `(ptr,len)`.

Conceptually:

```text
Ast.Expression.Literal(String)
  → (i32.const <ptr>) (i32.const <len>)
```

Where `<ptr>` and `<len>` reference a module data segment.

### Variable Access

* local variable: `local.get $x`
* field/global: `global.get $x` (if represented as globals)

### Assignment

* local: `local.set $x`
* global: `global.set $x`

### Arithmetic (Integer)

```text
+  → i32.add
-  → i32.sub
*  → i32.mul
/  → i32.div_s
```

### Comparisons

All comparisons produce an `i32` boolean (`0/1`).

```text
== → i32.eq
!= → i32.ne
<  → i32.lt_s
<= → i32.le_s
>  → i32.gt_s
>= → i32.ge_s
```

### Logical Operators

FreshlyGround booleans lower as `i32`.

Recommended canonicalization:

* treat any non-zero as true
* normalize results to `0/1`

Operators:

```text
AND → i32.and   (if canonicalized)
OR  → i32.or
```

If you require strict boolean semantics (0/1), canonicalize operands first.

### Control Flow

WASM uses structured control flow.

* `if` lowers to `(if (then ...) (else ...))`
* `while` lowers to a `(block (loop ... br_if ...))` pattern

The backend is responsible for structured lowering but not for semantic decisions.

---

## Printing Semantics

Printing is a host-side effect.

### Printing Integers

```text
print(expr: Integer)
  compile expr → i32
  call $print_i32
```

### Printing Strings

```text
print(expr: String)
  compile expr → (ptr,len)
  call $print_str
```

If `print` is a builtin function in the language, it should be resolved during analysis and lowered here.

---

## Example Walkthrough

### FreshlyGround Source

```fg
DEF main() : Integer DO
  print(10);
  print("hello");
  RETURN 0;
END
```

### Conceptual WASM Lowering

```wat
(module
  (import "env" "print_i32" (func $print_i32 (param i32)))
  (import "env" "print_str" (func $print_str (param i32 i32)))

  (memory (export "memory") 1)

  ;; string literal
  (data (i32.const 1024) "hello")

  (func $main (export "main") (result i32)
    ;; print(10)
    (i32.const 10)
    call $print_i32

    ;; print("hello")
    (i32.const 1024)
    (i32.const 5)
    call $print_str

    ;; return 0
    (i32.const 0))
)
```

---

## Constraints and Assumptions

* `main/0` must exist and returns `i32`
* `print_i32` and `print_str` must be provided by the host
* string encoding is assumed UTF-8
* memory layout is stable for the duration of execution

---

## Forward Links

* For pass ordering and invariants: **[Compiler Pipeline](04_pipeline.md)**
* For the semantic rules that resolve builtins like `print`: **[Semantics](03_semantics.md)**

# 05 — WebAssembly Backend Specification

This document specifies the FreshlyGround WebAssembly (WAT) backend: the lowering rules, runtime interface
(host ABI), and representation strategy for compiling semantically-analyzed FreshlyGround programs into
WebAssembly Text Format (WAT).

The WAT backend is one concrete implementation of the modular backend interface defined in the compiler pipeline, that:

* consumes AST + Bindings
* assumes semantic correctness has already been proven
* performs mechanical lowering only
* emits WAT source
* does not perform name resolution or type checking

---

## Backend Overview



## Type Mapping

FreshlyGround   WASM
  --------------- -----------
Integer         i32
Boolean         i32
Decimal         f64
String          (ptr,len)

## Imports

``` wat
(import "env" "print_i32" (func $print_i32 (param i32)))
(import "env" "print_str" (func $print_str (param i32 i32)))
```

## Example Module Skeleton

``` wat
(module
  (memory (export "memory") 1)
  (func (export "main") (result i32)
    (i32.const 0))
)
```

