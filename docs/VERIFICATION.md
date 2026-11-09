# Verification · 0.12.0

On 9 November 2026, Java 21 / Linux: **92 automated tests, 91 passed, zero failures/errors, 1 conditional GPU test skipped** (no usable OpenCL GPU exposed). The current run includes the complete existing precision, collision escalation, formula, palette, layers, animation, persistence, disk export and TCP network regression suite. New tests validate all 15 offline JPEG thumbnails, widest-to-deepest catalogue ordering, the exact final Islands coordinates, all destination project round trips, long decimal strings beyond double precision, typed iteration validation and the new cubic default palette.

Actual JavaFX headless integration checks passed:

- All 15 native gallery buttons apply their exact destination to the selected layer; layer count is retained; the typed 32768 iteration value is used; invalid input is reported; long exact coordinate entry works; the sidebar collapses at 1280px and Focus hides the atlas. The header close button removes the atlas from layout, View restores it, and leaving Focus does not reopen an atlas you explicitly closed.
- Custom cubic palette editing, Fine sampling, physical render dimensions, rectangle zoom/cancel, floating/docked gradient editor, independent Julia layer, opacity/blend controls, project save/reload with identical image and Newton family.
- An actual TCP Master/Worker interactive render, the Network menu and all six network window tabs, including the shared illustration header and app icon.
- Separate frame-render and video-conversion windows, owner containment, 90% monitor cap, oversized drag/resize, owner shrink and themed dialogs.

The screenshot checks used JavaFX Monocle software rendering, not a shipped dependency. Native Windows dialogs, real DPI scaling, physical multi-computer LAN throughput and GPU hardware require verification on the target machines. The existing numerical/network benchmark results remain in `benchmarks/`; this presentation/catalogue release does not claim new render-engine speedups. The UI has actual functional controls, not a flattened concept image.

## Earlier validation record

### 0.10.0

`mvn test`: 69 tests, 68 passed, zero failures/errors, one conditional GPU test skipped because no usable OpenCL GPU was exposed. Tests cover legacy formulas/palettes/layers/export data, project persistence including all four old v0.4 examples, DD/QD arithmetic against BigDecimal, all precision transitions, adjacent axes at spans down to 1e-1000, subnormal double escalation, exact navigation/persistence, six wide formula families, perturbation/fallback, independent subpixel counts, cancellation and worker configuration. Quad-double division retains a 1e-60 oracle threshold.

Headless JavaFX smoke workflows exercised custom palettes, rectangle navigation, cancellation, floating/docked gradient editor, layers, Julia, blending/opacity, project save/reopen pixel identity and Newton. A second visual matrix covered 1280×720, 1366×768, 1600×900, 1920×1080 and 2560×1440 windows, Performance, persistent inline validation, Focus, fullscreen, exact deep-coordinate input and themed About artwork. Selected actual screenshots are included here. Monocle was a test harness, not a shipped runtime dependency. Headless logical-size checks do not replace testing Windows native dialogs or real 125/150/200% display scaling.

Representative numerical rendering benchmarks ran on Java 21, Linux amd64, nine logical processors, 2 GiB maximum heap, one warm-up and median of three timed runs. Full raw CSV/JSON, hardware inventory and scene settings are in ../benchmarks/. Nine workers were fastest among 1/4/9 for these four scenes; this is not a universal core-count recommendation. UI/colour-composition time is excluded and the reference cache is warm. GPU/hybrid execution, native-driver recovery and GPU correctness/performance remain unverified on hardware in this environment; the shipped conditional tests and benchmark runner enable target-machine verification.

Animation adds eight tests and a real 27-frame PNG/GIF/MP4 smoke. See ZOOM-ANIMATION.md for details. The GPU test remains the only skipped test on this host.

0.7.1: ZoomWorkflowSmoke passed the empty-folder regression and create/render/export button-state checks, including a frame removed after rendering. Maven remains 66 tests: 65 passed, one GPU-only skip.

0.7.2: three window-bound geometry tests plus SeparateWindowsSmoke verify separate controls, 90% limits, main-window containment, parent shrinking, oversized resizing/dragging and themed alert containment. Headless QA does not certify native Windows decorations/file pickers.

0.8 OpeningSmoke verifies the real production launcher: splash visible with intermediate progress, main window not yet shown, automatic handover after the three-second timeline, splash closed at progress 1 and cream-themed workspace rendered. Actual screenshots are included. Existing Maven suite remains 69 tests, 68 passed and one hardware-dependent GPU skip.

0.8.1: production startup smoke passed with the revised black-strip artwork and live left-to-right gold bar. The three-second handover remains unchanged.

0.9: 71 tests total, 70 passed, zero failures/errors, one GPU-only skip. All Newton degrees 2–12 are checked near every unit root across Double, DD, QD and arbitrary precision; exact-root colour tests distinguish every root. MP4 encoding is tested using the project-managed FFmpeg runtime without a PATH probe. UI integration passed with Newton n=8, palette/docking, independent layers and pixel-identical project reload. Separate-window checks now also assert nonempty icon lists for both animation stages and themed alerts. Two old Mandelbrot/Julia examples remain compatibility inputs; removed-family gallery files were intentionally replaced with Newton examples.

0.9.1: 74 tests, 73 passed, one GPU skip. New checks cover wide-precision 16×16 tiles retaining every final subpixel, a first coarse DD preview before final tiles, and incomplete finer previews preserving already computed coarse pixels. The preview-order test now checks 64→32→16→8→4→2. Historical v0.6 benchmark numbers predate the tile change; run the benchmark again for current comparisons.

0.10: 77 tests total, 76 passed and one GPU-only skip. RuntimeCollisionTest verifies no premature promotion for unique Double coordinates, actual restart chains to DD/QD/AP (including span 1e-400), exact subpixel counts, smooth values against AP reference, repeated axis values across rows without false collisions, and document completion after tile-size changes with old data discarded. Earlier margin-based expectations were replaced by runtime policy tests. GPU hardware remains unavailable; GPU dispatch shares the validated registry checks.

The fixed-size JavaFX integration smoke passed after the collision changes: custom palettes, supersampling, rectangle navigation/cancellation, floating editor/docking, independent layers, blend/opacity, pixel-identical project reload and Newton. The separate rapid-resize visual matrix completed its assertions but logged a Monocle pixel-buffer resize exception; that headless harness run is not claimed as clean native-display validation.

0.11 LAN extension: see NETWORK-ARCHITECTURE.md and ../benchmarks/network/. The new suite covers TCP formula/precision equality, heterogeneous Workers, live disconnect continuation, reconnect, cancellation/new traffic, stale/duplicate lease rejection, bounded framing/version rejection, cross-tile coordinate collisions and disk-backed layered/histogram pixel equality. Separate JVM benchmarks measure Master-only and one/two/three Worker processes over loopback with identical hashes. A4096² distributed PNG completed under a128MiB JVM heap; the PNG chunks and a decoded pixel were verified. NetworkUiSmoke passed the new menu, distributed viewport, six settings tabs, icon and owned-window screenshots. Actual multi-machine LAN, Windows firewall deployment and GPU hardware execution are not claimed.

Final v0.11 build: **86 tests run, 85 passed, zero failures/errors, one GPU-only skip**. The TCP tests include arbitrary precision and a collision that occurs only between two individually unique tiles. Final NetworkUiSmoke and the pre-existing full UiSmoke both passed. A final4096² distributed export completed in128MiB heap; its complete PNG chunk stream was checked with Pillow.verify, and ImageIO verified dimensions/decoded a source-region pixel. Raw loopback benchmark data were regenerated with the final scheduler.
