# JMH Test Results

## SharedCounterBenchmark

### 1 Thread

Benchmark                                           (type)  Mode  Cnt   Score   Error  Units
SharedCounterBenchmark.getShared                    pooled  avgt    5  15.605 ± 0.059  ns/op
SharedCounterBenchmark.getShared                   locking  avgt    5  28.220 ± 0.903  ns/op
SharedCounterBenchmark.getShared                      sync  avgt    5  16.720 ± 0.053  ns/op
SharedCounterBenchmark.getShared        reflection-locking  avgt    5  30.265 ± 0.008  ns/op
SharedCounterBenchmark.getShared           reflection-sync  avgt    5  24.297 ± 0.020  ns/op
SharedCounterBenchmark.getThread                    pooled  avgt    5  15.806 ± 0.021  ns/op
SharedCounterBenchmark.getThread                   locking  avgt    5  28.737 ± 0.027  ns/op
SharedCounterBenchmark.getThread                      sync  avgt    5  17.137 ± 0.008  ns/op
SharedCounterBenchmark.getThread        reflection-locking  avgt    5  34.771 ± 0.028  ns/op
SharedCounterBenchmark.getThread           reflection-sync  avgt    5  25.805 ± 0.009  ns/op
SharedCounterBenchmark.incrementShared              pooled  avgt    5  19.155 ± 0.033  ns/op
SharedCounterBenchmark.incrementShared             locking  avgt    5  30.522 ± 0.043  ns/op
SharedCounterBenchmark.incrementShared                sync  avgt    5  19.144 ± 0.002  ns/op
SharedCounterBenchmark.incrementShared  reflection-locking  avgt    5  34.964 ± 6.357  ns/op
SharedCounterBenchmark.incrementShared     reflection-sync  avgt    5  32.530 ± 2.140  ns/op
SharedCounterBenchmark.incrementThread              pooled  avgt    5  19.390 ± 0.011  ns/op
SharedCounterBenchmark.incrementThread             locking  avgt    5  30.938 ± 0.001  ns/op
SharedCounterBenchmark.incrementThread                sync  avgt    5  19.368 ± 0.008  ns/op
SharedCounterBenchmark.incrementThread  reflection-locking  avgt    5  37.809 ± 3.232  ns/op
SharedCounterBenchmark.incrementThread     reflection-sync  avgt    5  33.304 ± 8.471  ns/op

### 4 Threads

Benchmark                                           (type)  Mode  Cnt    Score    Error  Units
SharedCounterBenchmark.getShared                    pooled  avgt    5  564.543 ±  3.311  ns/op
SharedCounterBenchmark.getShared                   locking  avgt    5  644.041 ±  5.778  ns/op
SharedCounterBenchmark.getShared                      sync  avgt    5  230.737 ± 17.411  ns/op
SharedCounterBenchmark.getShared        reflection-locking  avgt    5  778.680 ±  4.755  ns/op
SharedCounterBenchmark.getShared           reflection-sync  avgt    5  259.306 ±  5.276  ns/op
SharedCounterBenchmark.getThread                    pooled  avgt    5   15.449 ±  0.117  ns/op
SharedCounterBenchmark.getThread                   locking  avgt    5   28.643 ±  0.079  ns/op
SharedCounterBenchmark.getThread                      sync  avgt    5   16.778 ±  0.049  ns/op
SharedCounterBenchmark.getThread        reflection-locking  avgt    5   35.140 ±  0.122  ns/op
SharedCounterBenchmark.getThread           reflection-sync  avgt    5   21.137 ±  0.079  ns/op
SharedCounterBenchmark.incrementShared              pooled  avgt    5  643.378 ± 22.185  ns/op
SharedCounterBenchmark.incrementShared             locking  avgt    5  671.338 ±  2.708  ns/op
SharedCounterBenchmark.incrementShared                sync  avgt    5  219.840 ± 20.920  ns/op
SharedCounterBenchmark.incrementShared  reflection-locking  avgt    5  682.614 ±  5.047  ns/op
SharedCounterBenchmark.incrementShared     reflection-sync  avgt    5  267.186 ± 11.649  ns/op
SharedCounterBenchmark.incrementThread              pooled  avgt    5   19.319 ±  0.073  ns/op
SharedCounterBenchmark.incrementThread             locking  avgt    5   30.674 ±  0.109  ns/op
SharedCounterBenchmark.incrementThread                sync  avgt    5   19.052 ±  0.169  ns/op
SharedCounterBenchmark.incrementThread  reflection-locking  avgt    5   36.629 ±  1.677  ns/op
SharedCounterBenchmark.incrementThread     reflection-sync  avgt    5   28.868 ±  4.311  ns/op

### 8 Threads

Benchmark                                           (type)  Mode  Cnt     Score    Error  Units
SharedCounterBenchmark.getShared                    pooled  avgt    5  1103.268 ± 13.872  ns/op
SharedCounterBenchmark.getShared                   locking  avgt    5  1213.942 ±  2.723  ns/op
SharedCounterBenchmark.getShared                      sync  avgt    5   272.565 ± 34.653  ns/op
SharedCounterBenchmark.getShared        reflection-locking  avgt    5  1276.789 ±  8.989  ns/op
SharedCounterBenchmark.getShared           reflection-sync  avgt    5   337.229 ± 31.673  ns/op
SharedCounterBenchmark.getThread                    pooled  avgt    5    31.310 ±  0.109  ns/op
SharedCounterBenchmark.getThread                   locking  avgt    5    56.356 ±  0.148  ns/op
SharedCounterBenchmark.getThread                      sync  avgt    5    34.225 ±  0.074  ns/op
SharedCounterBenchmark.getThread        reflection-locking  avgt    5    66.764 ±  0.282  ns/op
SharedCounterBenchmark.getThread           reflection-sync  avgt    5    46.237 ±  0.394  ns/op
SharedCounterBenchmark.incrementShared              pooled  avgt    5  1304.755 ±  6.178  ns/op
SharedCounterBenchmark.incrementShared             locking  avgt    5  1495.463 ±  9.196  ns/op
SharedCounterBenchmark.incrementShared                sync  avgt    5   349.626 ± 19.339  ns/op
SharedCounterBenchmark.incrementShared  reflection-locking  avgt    5  1467.483 ±  7.048  ns/op
SharedCounterBenchmark.incrementShared     reflection-sync  avgt    5   423.030 ±  7.381  ns/op
SharedCounterBenchmark.incrementThread              pooled  avgt    5    35.879 ±  0.303  ns/op
SharedCounterBenchmark.incrementThread             locking  avgt    5    61.827 ±  0.154  ns/op
SharedCounterBenchmark.incrementThread                sync  avgt    5    38.789 ±  0.172  ns/op
SharedCounterBenchmark.incrementThread  reflection-locking  avgt    5    72.631 ±  0.640  ns/op
SharedCounterBenchmark.incrementThread     reflection-sync  avgt    5    53.575 ±  0.061  ns/op

