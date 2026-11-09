# Architecture · 0.6.0

Immutable StudioDocument/LayerState objects retain independent formula, exact viewport, Calculation and colour settings. ProjectIO validates versioned JSON and atomically replaces files. Viewport preserves exact decimals and legacy approximate accessors.

DocumentRenderer plans visible layers, bounds numerical cache memory, reuses exact request keys, schedules asynchronous colour/composition and reports aggregate diagnostics. PrecisionController chooses a safe minimum backend; PixelEvaluator owns worker-local arithmetic and optional reference-orbit delta evaluation. PreciseFormula implements all six families through Arithmetic adapters for DD, QD and BigDecimal. RenderEngine dispatches shared tiles to a configurable CPU pool with cooperative cancellation and optional GpuCompute tiles. OpenCL probing/validation runs asynchronously; incompatible precision never crosses into a narrower GPU mode.

Layer colour samples remain independent, are averaged in linear light, then composited with the existing eight blend modes. Palette changes can reuse complete numerical caches. Exports preserve the completed viewport composition. OverviewNavigator has a separate lightweight single-worker reference renderer; Composite is a distinct thumbnail. Theme, Sections, NumericField, Icons and WorkspacePrefs centralize visual and interaction conventions. BuildInfo reads the shared application version resource.

## Animation (0.7)

ZoomJob is an immutable captured document and exact geometric path. ZoomJobIO persists self-contained version-1 XML with secure entity-disabled parsing. ZoomSequence owns a separate DocumentRenderer, streams one complete PNG at a time, uses a folder lock and XML fingerprint, and reconciles PNGs on resume. SequenceEncoder streams built-in GIF output or invokes optional FFmpeg through ProcessBuilder argument arrays; cancellation retains source frames and incomplete output stays temporary. ZoomWindow keeps this work off the JavaFX thread.

0.10: CoordinateRegistry supplies per-axis runtime HashMaps and checked-position flags. PrecisionController no longer chooses an elevated automatic mode by margins. RenderEngine Job owns a sequence of cancellable precision attempts; a step=-1 Tile is an internal reset event, followed by replacement tiles with their own completed/total counts. DocumentRenderer and BenchmarkMain handle resets/completion explicitly. Only final step=1 samples are retained, coloured for export or considered complete.

0.11 integrates MasterServer, WorkerClient, NetworkService, DistributedJob and DiskPngExport. TileKernel is shared by local CPU workers and network workers. See NETWORK-ARCHITECTURE.md for protocol, global precision, scheduling, bounds and streaming export.
