# JMH Test Results

## SynchronizedSharedObjectPool

### Separate get() and increment(), 1 thread

Benchmark                                                  Mode  Cnt    Score   Error  Units
SynchronizedSharedObjectPoolBenchmark_01.measureGet        avgt   25  172.888 ± 2.267  ns/op
SynchronizedSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  173.634 ± 3.298  ns/op

### Separate get() and increment(), 2 threads

Benchmark                                                  Mode  Cnt    Score    Error  Units
SynchronizedSharedObjectPoolBenchmark_01.measureGet        avgt   25  809.566 ± 14.694  ns/op
SynchronizedSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  809.790 ±  8.809  ns/op

### Separate get() and increment(), 4 threads

Benchmark                                                  Mode  Cnt     Score    Error  Units
SynchronizedSharedObjectPoolBenchmark_01.measureGet        avgt   25  1433.068 ± 47.921  ns/op
SynchronizedSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  1460.565 ± 46.021  ns/op

### Common benchmark for get() and increment(), 2 threads

Benchmark                                                    Mode  Cnt    Score    Error  Units
SynchronizedSharedObjectPoolBenchmark_02.g                   avgt   25  802.951 ± 12.280  ns/op
SynchronizedSharedObjectPoolBenchmark_02.g:measureGet        avgt   25  780.553 ± 12.250  ns/op
SynchronizedSharedObjectPoolBenchmark_02.g:measureIncrement  avgt   25  825.348 ± 16.788  ns/op

### Common benchmark for get() and increment(), 4 threads

Benchmark                                                    Mode  Cnt     Score    Error  Units
SynchronizedSharedObjectPoolBenchmark_02.g                   avgt   25  1481.534 ± 60.143  ns/op
SynchronizedSharedObjectPoolBenchmark_02.g:measureGet        avgt   25  1474.789 ± 60.739  ns/op
SynchronizedSharedObjectPoolBenchmark_02.g:measureIncrement  avgt   25  1488.278 ± 59.855  ns/op

## LockingSharedObjectPool - locks only for pool itself, synchronization for entries.

### Separate get() and increment(), 1 thread

Benchmark                                             Mode  Cnt    Score   Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  169.630 ± 2.478  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  173.598 ± 3.232  ns/op

### Separate get() and increment(), 2 threads

Benchmark                                             Mode  Cnt     Score    Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  1186.036 ± 37.938  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  1229.278 ± 31.761  ns/op

### Separate get() and increment(), 4 threads

Benchmark                                             Mode  Cnt     Score    Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  1053.836 ± 52.465  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25   995.484 ± 17.353  ns/op[

## LockingSharedObjectPool - locks for pool and entries

### Separate get() and increment(), 1 thread

Benchmark                                             Mode  Cnt    Score   Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  291.223 ± 4.781  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  290.024 ± 3.738  ns/op

### Separate get() and increment(), 2 threads

Benchmark                                             Mode  Cnt     Score    Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  1590.004 ± 97.425  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  1680.849 ± 67.728  ns/op

### Separate get() and increment(), 4 threads

Benchmark                                             Mode  Cnt     Score    Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  1310.935 ± 70.364  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  1243.855 ± 57.730  ns/op

## LockingSharedObjectPool - locks for pool and entries, read-lock-first for pool when getting an entry.

### Separate get() and increment(), 1 thread

Benchmark                                             Mode  Cnt    Score   Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  302.500 ± 4.233  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  308.240 ± 1.142  ns/op

### Separate get() and increment(), 2 threads

Benchmark                                             Mode  Cnt     Score    Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  1775.984 ± 47.921  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  1748.650 ± 38.942  ns/op

### Separate get() and increment(), 4 threads

Benchmark                                             Mode  Cnt     Score     Error  Units
LockingSharedObjectPoolBenchmark_01.measureGet        avgt   25  2363.511 ± 133.880  ns/op
LockingSharedObjectPoolBenchmark_01.measureIncrement  avgt   25  2251.914 ± 142.642  ns/op

