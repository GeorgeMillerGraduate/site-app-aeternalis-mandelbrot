# CPU/GPU rendering measurements

Actual measurements on Linux amd64, Java 21.0.13, 9 logical processors. One warm-up render, then median of three timed renders for each configuration. These are numerical tile timings; colour/composition/UI presentation are excluded. Worker-pool scheduling and OpenCL transfer costs, when applicable, are included. Reference-orbit cache is warm after the warm-up. No general speedup claim is inferred from this host.

| Scene | Workers | Backend | Precision | Median ms | Numerical pixels/s |
|---|---:|---|---|---:|---:|
| Mandelbrot full | 1 | CPU | DOUBLE | 117.526 | 5227796 |
| Seahorse detail | 1 | CPU | DOUBLE | 315.346 | 974168 |
| Julia 2x2 | 1 | CPU | DOUBLE | 294.505 | 1043106 |
| Deep 1e-30 perturbation | 1 | CPU | QUAD_DOUBLE | 63.966 | 96051 |
| Mandelbrot full | 4 | CPU | DOUBLE | 23.613 | 26019724 |
| Seahorse detail | 4 | CPU | DOUBLE | 90.195 | 3405957 |
| Julia 2x2 | 4 | CPU | DOUBLE | 71.933 | 4270642 |
| Deep 1e-30 perturbation | 4 | CPU | QUAD_DOUBLE | 35.167 | 174708 |
| Mandelbrot full | 9 | CPU | DOUBLE | 16.453 | 37342387 |
| Seahorse detail | 9 | CPU | DOUBLE | 53.815 | 5708414 |
| Julia 2x2 | 9 | CPU | DOUBLE | 37.290 | 8238221 |
| Deep 1e-30 perturbation | 9 | CPU | QUAD_DOUBLE | 31.800 | 193209 |

GPU report: No OpenCL GPU configured · Unknown · No OpenCL ICD registered · FP64 no · 0 compute units · 0 MiB global / 0 MiB allocation · Compute unavailable or not validated · A loader library alone is not a usable compute device. CPU retained. Vulkan compute is not implemented. Advanced direct-loader probing can be enabled with aeternalis.forceOpenCLProbe.

**GPU and hybrid timings were not measured:** no validated OpenCL GPU is exposed on this host. Missing GPU results are not zeros or projected speedups. Run the same benchmark on the target Windows system.

Full logical-core count can be slower on small/cheap scenes because tile scheduling, allocation and memory contention matter. Compare each scene's rows instead of treating logical processors as an automatic performance optimum. No Vulkan or GPU double-double implementation is claimed.
