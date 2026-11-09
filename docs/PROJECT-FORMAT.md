# Project format · schema 1

`.aeternalis` is JSON with schemaVersion, applicationVersion and document. Layers store identity/order, name, visibility, opacity/blend, formula, viewport, Calculation and complete colour/palette settings. Application version is now 0.6.0; the backward-compatible schema remains 1. Future unsupported schemas are rejected.

Viewport writes approximate real/imaginary/span plus BigDecimal realExact/imaginaryExact/spanExact. Exact values are authoritative; older three-double files derive exact decimal equivalents. Deep positive spans can have an approximate span of zero without losing their decimal value. Four original v0.4 projects remain in examples and are load-tested.

Calculation now includes precision (AUTOMATIC/DOUBLE/DOUBLE_DOUBLE/QUAD_DOUBLE/ARBITRARY) and perturbation. Old absent precision defaults to Automatic; old absent perturbation defaults false and can be enabled in Performance. New calculations default to Automatic with perturbation enabled. Precision is a minimum request and cannot override collapse protection.

Images, hardware selection, runtime caches and workspace preferences are not embedded in project JSON. Save commits the current immutable document; apply pending typed edits before saving. Atomic replacement is attempted with a filesystem fallback. No crash recovery/autosave is claimed.

0.11: saved document and zoom XML formats are unchanged. Network requests transport exact decimal coordinates as validated JSON inside versioned binary frames; transient session/token/lease data is not stored in artwork files.
