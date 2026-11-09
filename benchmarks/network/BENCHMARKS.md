# Actual loopback multi-process benchmarks

Java 21.0.13, Linux amd64, 9 logical processors. Each worker is a separate JVM with one compute thread; the Master uses two compute threads. One warm-up then median of three numerical renders. TCP, serialization, scheduling and process execution are timed; colour/UI/PNG encoding are excluded. All processes share one physical host and its CPU/memory; these are **not physical-LAN benchmarks**. Speedup is measured against the same scheduler with only the two-thread Master. Noncompute task round trip includes queueing/scheduling, not just network transmission. Output SHA-256 hashes match across all configurations.

| Scene | Worker JVMs | Median ms | Measured speedup | Remote tiles | TX bytes | RX bytes |
|---|---:|---:|---:|---:|---:|---:|
| Seahorse | 0 | 211.521 | 1.000× | 0 | 0 | 0 |
| Newton n8 | 0 | 123.637 | 1.000× | 0 | 0 | 0 |
| Seahorse | 1 | 178.044 | 1.188× | 16 | 1623 | 510394 |
| Newton n8 | 1 | 100.115 | 1.235× | 17 | 1651 | 543303 |
| Seahorse | 2 | 138.779 | 1.524× | 31 | 3145 | 955072 |
| Newton n8 | 2 | 79.531 | 1.555× | 33 | 3229 | 1037300 |
| Seahorse | 3 | 124.460 | 1.700× | 38 | 4111 | 1185552 |
| Newton n8 | 3 | 92.319 | 1.339× | 27 | 3201 | 823397 |

Faster completion is workload-dependent. This host's loopback results do not predict performance on separate home computers, Wi-Fi or Ethernet.
