# Remaining work

Implemented in 0.6: Victorian observatory theme/artwork, exact decimal navigation, automatic Double/DD/QD/AP planning, all-family CPU wide arithmetic, guarded Mandelbrot perturbation, configurable tile workers, optional OpenCL FP64 quadratic kernel, diagnostics, tests and measured CPU pool comparisons.

Next: actual Windows GPU validation and profiling; Vulkan and GPU DD only if correctness/performance justify them; FloatExp/series perturbation; adaptive sampling; independent-resolution export queues; formula DSL; orbit traps and distance-estimation colouring; multi-keyframe/eased animation; image layers/masks/groups; full document undo/autosave; recent-project persistence; richer colour spaces/opacity curves. Native high-DPI and accessibility QA on target Windows hardware is still needed. These are not presented as working features.

0.7 implements resumable single-segment geometric zoom PNG sequences, self-contained XML jobs, built-in looping GIF export and external FFmpeg H.264 MP4 export.

0.11 implements trusted-LAN distributed CPU rendering, streaming large PNG export, network UI and measured loopback multi-process benchmarks. Next validation: separate physical Windows/Linux LAN machines, Ethernet/Wi-Fi scaling and real firewall setup. Future optional work: distributed GPU kernels, TLS/public-network transport and resumable out-of-core still-image jobs.
