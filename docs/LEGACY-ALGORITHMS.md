# Supplied renderer audit and integration

Source reviewed: `mandelbrot-java-intermediate-main(2).zip`, archive commit comment `8674168da5067d9962e977de0ec4bc218c7133b5`. Its Java sources carry George Miller’s 2025 MIT notice. The complete original notice is preserved in LEGACY-LICENSE.txt.

| Original implementation | Integration in Aeternalis 0.3 |
|---|---|
| MandelbrotProcessorDouble: cached zx² and zy² | Adapted in Mandelbrot evaluator |
| Cardioid and period-two bulb shortcuts | Retained, with existing inward comparison margins |
| Adaptive interval checkpoint plus spaced checkpoint | Adapted with exact double-state equality; the original fixed absolute tolerance is removed |
| Input bailout compared against zx²+zy² | Exposed as “Squared bailout |z|²”, default 10, validated ≥4 |
| AntiAliasing: regular grid per pixel | Existing centred subpixel grids retained; colours averaged in linear light, rather than the original encoded-channel integer average |
| MandelbrotColoring: normalized escape count and sqrt mapping | Existing refined smooth escape value plus Square root transfer; avoids the original integer index quantisation |
| SmoothGradient: four-stop Catmull–Rom polynomial | Selectable Catmull–Rom/Hermite form, position-aware tangents for uneven stops, linear-light channels and output clamping |
| Blue / pale / gold preset in ZoomInSimple | Added as “George’s 2025 blue & gold” |
| Ten scan-order classes | Adapted priority equations to 64-pixel tiles; explicit control in Render properties |
| RenderUtils.makeLayoutMap: large-to-small square reveal | Adapted as independent 8 → 4 → 2 coarse square preview passes, behind exact final tiles |
| MandelbrotOutput.HowFound and showType | Classification counts shown in Render telemetry: cardioid, bulb, exact cycle and iteration limit |
| ZoomInSimple frame sequence, VideoCreator and bundled FFmpeg | Not imported: animation remains a later workspace |

The supplied code contains a double-precision evaluator, not double-double arithmetic, arbitrary-precision iteration or perturbation. Those features are not implied by generic type names or Javadoc. This release remains double precision.

## Correctness changes

Exact repeated double states imply a repeating deterministic floating-point orbit for this recurrence. This justifies terminating the finite double evaluator, but does not prove mathematical membership in the real Mandelbrot set. A close state within 10⁻¹⁷ does not establish such a cycle, so the original near-repeat rule is deliberately replaced.

Squared bailout and radius are distinct: threshold 10 means radius √10. The control avoids the ambiguity of the old input’s “length” description. Post-escape colour refinement preserves classification at the original iteration limit. The telemetry counts escape iterations, excluding refinement and preview evaluation work.

Scan patterns affect only reveal order. Every final tile calculates exactly the same pixel/subpixel coordinates for every order. Preview data are not reused as final values, so enlarged coarse blocks cannot survive a finished render. A per-render generation and per-colour version stop old jobs from publishing into the current image.

Catmull–Rom can overshoot channel values; results are explicitly clamped into linear RGB gamut. For irregular stop positions, tangents are expressed per unit gradient position. The previous cubic smoothstep is still useful for monotone segment transitions and remains selectable.

The old project’s JavaFX SDK binaries, FFmpeg executable, Windows paths and machine-specific configuration are not bundled or executed. Maven supplies the target platform’s JavaFX components.

## Workspace reference

The supplied screenshots and the manufacturer’s page https://www.ultrafractal.com/whatsnew.html informed the general workflow: a dominant fractal image, compact property tabs, navigation/history and a floating gradient tool. All UI code, styling and artwork here are Aeternalis’s own; no proprietary graphics or exact layout were copied. Layers, formula editing and animation are still outside this release.
