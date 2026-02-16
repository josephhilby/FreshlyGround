# 05 — WebAssembly Backend Specification (TODO)

This document specifies the **FreshlyGround WebAssembly backend**: the lowering rules, runtime interface
(host ABI), and representation strategy for compiling analyzed FreshlyGround programs into WebAssembly.

The WASM backend targets a **browser-hosted execution environment** where programs are compiled to WebAssembly and
executed client-side, with output routed to a web console via a minimal set of host-provided imports.

---

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
