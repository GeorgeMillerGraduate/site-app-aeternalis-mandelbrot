# Distributed renderer · 0.11

## Integration

`RenderEngine.render` still returns a cancellable Job. When NetworkService selects a hosted network session, it forwards numerical tile work to MasterServer/DistributedJob; otherwise the existing local CPU/OpenCL renderer runs. DocumentRenderer continues background palette/histogram mapping, layer composition and throttled JavaFX presentation. ZoomSequence therefore inherits network rendering without duplicating its persistence/export logic. RenderRequest permits large geometry, while the interactive retained-sample limits stay in DocumentRenderer and Studio.

`TileKernel` calls the original PixelEvaluator and formula/precision implementations. The local CPU path and Worker path both use it. CPU transcendental functions in the numerical path use StrictMath to avoid platform-dependent library variations. GPU comparison retains its documented tolerance; distributed rendering is CPU-based and is checked against local CPU output.

## Authoritative generation and precision

DistributedJob owns an immutable job UUID, changing generation, exact RenderRequest, a shared PixelEvaluator.Context/CoordinateRegistry, pending tile IDs, leases with fresh assignment UUIDs, a completion BitSet and per-slot counters. Claiming each task registers its axis coordinates in the Master's existing runtime HashMaps. This catches collisions across tiles/computers, including pairs that are unique within each tile. It starts Double for Automatic, honours manual minimums, and never promotes from zoom level or an iteration/ULP margin.

A collision invalidates every lease in that generation, clears accepted tiles, emits the engine's reset event and restarts in DD, QD or AP. Image/disk consumers discard earlier stage data. TASK_CANCEL identifies the exact old assignment; it cannot cancel a newer task accidentally. Every result is checked against job/generation/tile/assignment and its owning slot. Late/duplicate results are ignored; disconnected leases return to the pending queue. Workers cannot choose an independent precision. Exact BigDecimal centre/span values are sent in the once-per-generation configuration; Workers construct numerical components with the same code, without double transport conversion.

Tile size adapts down on higher-precision stages (base/2 for DD, base/4 for QD/AP, minimum16), enlarging if necessary to respect the one-million-tile ceiling. Final placement and numerical samples are independent from reveal/scheduling order.

## Transport and scheduling

NetworkProtocol frames TCP messages as 4-byte magic, 4-byte protocol version, 1-byte type, 4-byte payload length and bounded payload (maximum8MiB). UTF-8 strings have their own validated byte lengths. Binary task/result fields use explicit primitive encoding; no Java native object deserialization is used. Shared request JSON uses the project's existing Gson with validated records and a 2MB cap. Task dimensions, ranges, sample counts, counters, generation and ownership are validated before accepting values. Infinite numerical result values are rejected; NaN retains the existing uncolourable-sample convention.

HELLO identifies the engine/application version and optional token. WORKER_CAPABILITIES reports group identity, logical cores, configured slots, memory and all four precision modes; incompatible versions/capabilities are rejected. HELLO_ACK assigns the connection identity. JOB_CONFIGURATION is sent once per slot per job generation; TASK_ASSIGNMENT then contains compact identity/bounds. TASK_RESULT carries lossless double sample indices plus iteration/classification counters. TASK_PROGRESS reports active assignment progress. TASK_REQUEST, TASK_CANCEL, HEARTBEAT, JOB_COMPLETE, ERROR and DISCONNECT complete the protocol. A heartbeat payload can wake idle pull slots without permitting multiple assignments.

One TCP slot per Worker compute thread has at most one in-flight tile. Java virtual threads handle the bounded set of socket readers; a bounded reusable CPU executor performs calculations. Master local threads pull from the same queue by default. Worker CPUs are never assumed to be proportional to advertised core count: faster task completion simply produces more pull requests. Per-computer reports aggregate the slots. CPU queues and heartbeat/control submissions are bounded; connection count is capped from heap size, up to512. Ordinary EOF, timeout, malformed replies and disconnects close a slot and release its lease. Workers reconnect automatically. A private token is admission control, not TLS or proof of honest numerical work.

## Automatic selection and diagnostics

Local/Distributed/Automatic modes are explicit. Automatic considers connected Workers and a default60s estimated-local threshold. A bounded4×4 probe (up to512 iterations each) estimates actual orbit work; recent local tile iteration throughput updates an exponentially weighted rate. This heuristic does not choose precision and can be inaccurate, particularly before runtime collisions establish a wider backend. Manual Distributed is available to override it. Nothing waits a minute before starting.

Statistics include wall duration, accepted local/remote task-seconds, completed tiles, per-computer share, bytes, average tile duration, throughput, estimated remaining time, task occupancy and an estimated-local baseline/speedup. Noncompute task round trip includes network transfer and queueing/scheduling; it is not an isolated NIC measurement. The live chart shows average accepted sample throughput. The benchmark runner separately reports measured speedup against its actual Master-only baseline. No linear scaling is promised.

## Out-of-core image export

DiskPngExport renders each visible layer to numerical tile files. Generation resets remove old stage files. Histogram mapping makes streaming min/max and4096-bin passes over the layer's numerical data, then colours each tile with the original ColourRenderer. Straight-alpha ARGB layers are stored on disk. During PNG output, one scanline from every visible layer is read and passed through the original Compositor together; intermediate layer quantization is not introduced. The writer streams8-bit RGBA rows through zlib and bounded IDAT chunks with CRCs. It never creates a full-resolution BufferedImage. Only a fully completed temporary PNG is renamed to the destination. Scratch files are cleaned on normal cancellation/error; process crashes may leave clearly named scratch directories/temporary output, never a successfully named partial PNG.

Master owns file output; Workers need no shared filesystem. Image geometry is bounded to131072 per axis and one million tasks. Disk capacity is checked before work. Existing interactive memory limits remain, and very high precision/thread counts still require appropriate CPU/RAM. Image export itself is not restartable after a process crash; the existing XML zoom-frame checkpoints are unchanged.

## Scope of validation

Real TCP loopback tests cover all three formulas and Double/DD/QD/AP, cross-tile precision detection, output equality, heterogeneous slots, Worker loss with local continuation, reconnection after server restart, cancellation/new job traffic, invalid framing/versions and PNG composition. Separate-JVM loopback benchmarks cover Master-only and one/two/three Worker processes with matching hashes. A4096² image was rendered with128MiB heap, decoded/verified and atomically saved. Physical multi-machine LAN and Windows firewall deployment are not available in this environment. Distributed GPUs, public-internet transport/TLS and Byzantine worker validation are outside this implementation.
