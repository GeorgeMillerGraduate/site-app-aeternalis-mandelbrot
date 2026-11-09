# Aeternalis LAN rendering — quick start

Use the same v0.11 source project and Java 21 build on every computer. Open `pom.xml` as a Maven project in NetBeans, then run `javafx:run` as before. The splash, existing formulas, palettes, precision modes, layers, zoom jobs and project-managed FFmpeg remain available.

## Master computer

1. Open **Network → Start Master Server**.
2. Leave bind address `0.0.0.0` to accept LAN connections. Use TCP port `5050` (or another unused port).
3. Optionally enter a session token; enter exactly the same token on Workers.
4. Choose local compute threads, click **Start Master**, and note the displayed LAN IP. `127.0.0.1` works only for Workers on the same machine.
5. In **Settings**, choose **DISTRIBUTED** to force network use while trying it. **AUTOMATIC** uses a bounded workload probe and recent local throughput, with a default 60-second estimated-local threshold. It does not wait for 60 seconds. **LOCAL** retains the existing local CPU/GPU path.
6. Leave **Master also computes tiles** enabled unless you want a coordinator-only machine. Apply settings.

## Worker computers

1. Open **Network → Connect as Worker**.
2. Enter the Master's LAN IP, port and optional token. Set compute threads and click **Connect Worker**.
3. Keep the application running. No project, source image or shared drive is needed on the Worker; formula and exact coordinates arrive with each new job generation.
4. Workers automatically retry lost connections. Disconnect/reconnect controls are also available without restarting the app. Connecting as Worker replaces any Master session on that same app instance.

## Render

- Navigate/configure the Master normally. Use **Render current viewport** in the Master window to force a fresh numerical pass; completed tiles appear progressively in the main canvas. Already cached identical renders can otherwise be reused without needing network work.
- **Network → Connected Computers / Network Rendering Statistics** shows capabilities, threads/slots, contributions, bytes, task timings, throughput, estimated remaining time and a live chart. The overlay setting adds computer-coloured tile boundaries (and short labels where tiles are large enough) to the main image. It is a debug overlay and is not baked into exports.
- **Network → Render high-resolution PNG** renders the complete applied visible-layer document at independent dimensions. It stores numerical tiles and ARGB layers on the Master's disk, then streams the PNG scanlines. Choose 4096, 8192, 16384 or custom dimensions if disk space/resources permit. A temporary output is renamed only after success. The final file is saved only on the Master.
- Zoom-frame rendering also uses the session through the same engine. Its existing XML/PNG pause/resume workflow is unchanged. Workers never write those files.
- Cancel in the main studio to stop interactive rendering while retaining the session. Use Cancel export for disk export. Disconnect/Stop ends the network role. If local Master computation is disabled and all Workers leave, the job waits for a Worker to reconnect or for cancellation.

## Private LAN firewall access

On the Master, allow the selected Java runtime/application or chosen TCP port on the **Private** network profile. Connect all computers to a mutually reachable LAN; guest Wi-Fi and client isolation can prevent connections. No router port forwarding is required. Do not expose this server port to the public internet. Tokens control admission but do not encrypt traffic; only use trusted private computers. Native OS firewall prompts and exact menus vary by installation.

## Useful settings and limits

Each Worker has one bounded in-flight task per configured CPU slot. Faster slots request more tiles; there is no permanent equal-sized region per computer. Default network tile edge is 64; 128–256 can reduce overhead for large, inexpensive exports. Precision escalation reduces the edge for DD/QD/AP to improve load balance, subject to the one-million-tile cap. A hard task lease timeout (default 600 seconds, configurable 1–3600) requeues tasks whose peer stalls; 2-second heartbeats and 12-second socket read timeouts handle lost connections sooner. Increase the lease limit for genuinely very expensive tiles.

Supported image axes are 1–131072 pixels, with at most one million tiles per generation. Disk use is roughly `width × height × (4 × visibleLayers + 8 × largestSamplesPerPixel)`, plus temporary PNG and filesystem overhead. Four-sample and nine-sample antialiasing retain independent samples. The interactive memory budget remains separate; use the disk-backed export for large images. Exports are atomic but not crash-resumable jobs; existing zoom PNG jobs remain resumable.

Distributed computation uses the common CPU tile kernel in Double/DD/QD/AP. Local-only rendering keeps the existing optional OpenCL backend. GPU aggregation across computers is not implemented. The Master remains authoritative for the runtime HashMap coordinate-collision policy; no zoom-level precision threshold was added.

## Verification and benchmarks

See `docs/NETWORK-ARCHITECTURE.md`, `docs/VERIFICATION.md` and `benchmarks/network/BENCHMARKS.md`. Benchmarks actually run separate Worker JVMs with TCP on loopback. They share one physical host and are **not measurements of separate home computers**. Real LAN/firewall and native Windows network deployment still need validation on your machines. All nodes must use the exact compatible application/engine version.

For an optional headless Worker from the same project, run Maven goal:

```
compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.jenga.aeternalis.network.WorkerMain -Dexec.args="MASTER_IP 5050 TOKEN THREADS"
```

Use a token without spaces for this command, or use the GUI. Stop the headless process to disconnect. For multi-process benchmarks, use `run-network-benchmarks.bat`; it resolves a runtime classpath and launches actual child JVMs.
