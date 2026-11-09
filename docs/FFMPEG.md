# Project-managed FFmpeg · 0.9

The NetBeans/Maven project declares org.bytedeco:ffmpeg:7.1-1.5.11, a platform-specific GPL native artifact, and JavaCPP 1.5.11. Windows x64 is the default native.platform; Linux x64/arm64 and macOS Intel/Apple Silicon have auto-activated profiles. A manual override is available as -Dnative.platform=windows-x86_64 (or a matching supported Bytedeco classifier). Use a 64-bit JDK matching the platform. Windows ARM may use x64 emulation with an x64 JDK; a native Windows ARM artifact is not claimed.

The first Maven build downloads these dependencies into the normal local Maven cache. The source ZIP does not duplicate those large native JARs. FfmpegRuntime resolves the executable with JavaCPP Loader, extracting the versioned binary and libraries automatically. No external install or PATH dependency remains in the UI. Once dependencies are resolved, the app does not download FFmpeg during export. The default and explicit bundled encoding paths both use this runtime.

The subprocess remains isolated from the JVM and receives argument arrays (no shell). Cancellation terminates the process and retains PNG inputs. Output is committed atomically only on success. H.264 uses libx264, yuv420p, CRF18 and faststart. GIF still uses JDK ImageIO.

Actual Linux validation used the project-managed FFmpeg 7.1 GPL build (libx264 enabled), including an MP4 test that never probes or invokes a system ffmpeg. Native Windows execution is not available on this host; Maven's Windows classifier is configured for the user's target. The separate converter now reports the integrated runtime and omits the executable picker.

JavaCPP and preset wrappers use Apache-2.0/GPLv2 with Classpath Exception licensing as provided upstream. The selected FFmpeg GPL build reports GNU GPL version 3 or later and includes separately licensed codec components. The source application licence does not replace dependency licences. Preserve runtime notices and corresponding-source obligations when redistributing native binaries or installers. No FFmpeg binaries are copied into this source ZIP.

Upstream integration: https://bytedeco.org/download/ ; https://github.com/bytedeco/javacpp-presets/tree/1.5.11/ffmpeg ; https://ffmpeg.org/legal.html
