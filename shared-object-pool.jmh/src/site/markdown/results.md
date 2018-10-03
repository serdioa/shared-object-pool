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

# DynamicSharedObject

Benchmark                                                     Mode  Cnt   Score   Error  Units
DynamicSharedObjectBenchmark.testLockingDynamicSharedCounter  avgt   10  28.327 ± 0.126  ns/op
DynamicSharedObjectBenchmark.testPooledCounter                avgt   10  15.721 ± 0.497  ns/op
DynamicSharedObjectBenchmark.testSharedCounter                avgt   10  27.229 ± 0.108  ns/op

# ConcurrentSharedObjectPool

### 1 thread

Benchmark                                                   (disposeUnusedEntries)  Mode  Cnt     Score     Error  Units
ConcurrentSharedObjectPoolBenchmark_01.measureGet                             true  avgt    5   426.348 ±  48.122  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGet                            false  avgt    5   171.036 ±  59.408  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                    true  avgt    5   459.975 ± 127.384  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                   false  avgt    5  1776.641 ± 516.553  ns/op

### 2 threads

Benchmark                                                   (disposeUnusedEntries)  Mode  Cnt     Score     Error  Units
ConcurrentSharedObjectPoolBenchmark_01.measureGet                             true  avgt    5  1110.625 ±  56.414  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGet                            false  avgt    5   742.622 ±  58.139  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                    true  avgt    5   464.769 ±  27.456  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                   false  avgt    5  2188.693 ± 609.138  ns/op

### 4 threads

Benchmark                                                   (disposeUnusedEntries)  Mode  Cnt     Score      Error  Units
ConcurrentSharedObjectPoolBenchmark_01.measureGet                             true  avgt    5  1604.821 ±  697.263  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGet                            false  avgt    5  1818.959 ±  357.490  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                    true  avgt    5   646.703 ±   95.195  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                   false  avgt    5  6323.872 ± 4947.114  ns/op

# ConcurrentSharedObjectPool with week refs

### 1 thread

Benchmark                                                   (disposeUnusedEntries)  Mode  Cnt    Score   Error  Units
ConcurrentSharedObjectPoolBenchmark_01.measureGet                             true  avgt   25  468.073 ± 1.852  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                    true  avgt   25  494.874 ± 2.579  ns/op

### 2 threads

Benchmark                                                   (disposeUnusedEntries)  Mode  Cnt     Score    Error  Units
ConcurrentSharedObjectPoolBenchmark_01.measureGet                             true  avgt   25  1985.305 ± 37.721  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                    true  avgt   25   608.875 ± 13.127  ns/op

### 4 threads

Benchmark                                                   (disposeUnusedEntries)  Mode  Cnt     Score    Error  Units
ConcurrentSharedObjectPoolBenchmark_01.measureGet                             true  avgt   25  2385.353 ± 27.371  ns/op
ConcurrentSharedObjectPoolBenchmark_01.measureGetUniqueKey                    true  avgt   25   750.102 ± 21.390  ns/op



