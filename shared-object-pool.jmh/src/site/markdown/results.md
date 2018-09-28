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
