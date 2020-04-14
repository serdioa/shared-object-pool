# JMH Test Results

## SharedObjectBenchmark

### 1 Thread
Benchmark                         (tokens)              (type)  Mode  Cnt      Score    Error  Units
SharedObjectBenchmark.testShared         0              pooled  avgt    5      2.455 ±  0.013  ns/op
SharedObjectBenchmark.testShared         0             locking  avgt    5     15.085 ±  0.576  ns/op
SharedObjectBenchmark.testShared         0                sync  avgt    5      4.583 ±  0.076  ns/op
SharedObjectBenchmark.testShared         0  reflection-locking  avgt    5     16.616 ±  0.092  ns/op
SharedObjectBenchmark.testShared         0     reflection-sync  avgt    5     13.491 ±  0.298  ns/op
SharedObjectBenchmark.testShared       100              pooled  avgt    5    149.168 ±  0.270  ns/op
SharedObjectBenchmark.testShared       100             locking  avgt    5    159.546 ±  0.211  ns/op
SharedObjectBenchmark.testShared       100                sync  avgt    5    151.784 ±  0.189  ns/op
SharedObjectBenchmark.testShared       100  reflection-locking  avgt    5    163.999 ±  0.111  ns/op
SharedObjectBenchmark.testShared       100     reflection-sync  avgt    5    159.932 ±  0.075  ns/op
SharedObjectBenchmark.testShared     10000              pooled  avgt    5  15582.321 ±  6.545  ns/op
SharedObjectBenchmark.testShared     10000             locking  avgt    5  15579.887 ± 12.214  ns/op
SharedObjectBenchmark.testShared     10000                sync  avgt    5  15576.399 ± 23.571  ns/op
SharedObjectBenchmark.testShared     10000  reflection-locking  avgt    5  15589.227 ± 22.573  ns/op
SharedObjectBenchmark.testShared     10000     reflection-sync  avgt    5  15584.791 ± 28.051  ns/op
SharedObjectBenchmark.testThread         0              pooled  avgt    5      2.447 ±  0.001  ns/op
SharedObjectBenchmark.testThread         0             locking  avgt    5     14.954 ±  0.001  ns/op
SharedObjectBenchmark.testThread         0                sync  avgt    5      4.608 ±  0.007  ns/op
SharedObjectBenchmark.testThread         0  reflection-locking  avgt    5     18.848 ±  0.013  ns/op
SharedObjectBenchmark.testThread         0     reflection-sync  avgt    5     13.427 ±  0.011  ns/op
SharedObjectBenchmark.testThread       100              pooled  avgt    5    149.116 ±  0.092  ns/op
SharedObjectBenchmark.testThread       100             locking  avgt    5    159.566 ±  0.113  ns/op
SharedObjectBenchmark.testThread       100                sync  avgt    5    151.779 ±  0.164  ns/op
SharedObjectBenchmark.testThread       100  reflection-locking  avgt    5    161.342 ±  0.236  ns/op
SharedObjectBenchmark.testThread       100     reflection-sync  avgt    5    159.732 ±  0.158  ns/op
SharedObjectBenchmark.testThread     10000              pooled  avgt    5  15571.799 ±  7.995  ns/op
SharedObjectBenchmark.testThread     10000             locking  avgt    5  15578.103 ±  1.814  ns/op
SharedObjectBenchmark.testThread     10000                sync  avgt    5  15572.450 ± 10.234  ns/op
SharedObjectBenchmark.testThread     10000  reflection-locking  avgt    5  15590.734 ± 21.312  ns/op
SharedObjectBenchmark.testThread     10000     reflection-sync  avgt    5  15580.021 ±  1.090  ns/op


### 2 Threads
Benchmark                         (tokens)              (type)  Mode  Cnt      Score     Error  Units
SharedObjectBenchmark.testShared         0              pooled  avgt    5      2.449 ±   0.003  ns/op
SharedObjectBenchmark.testShared         0             locking  avgt    5    191.664 ±  22.128  ns/op
SharedObjectBenchmark.testShared         0                sync  avgt    5     53.757 ±   7.678  ns/op
SharedObjectBenchmark.testShared         0  reflection-locking  avgt    5    210.317 ±  14.473  ns/op
SharedObjectBenchmark.testShared         0     reflection-sync  avgt    5    107.147 ±  22.038  ns/op
SharedObjectBenchmark.testShared       100              pooled  avgt    5    149.363 ±   1.150  ns/op
SharedObjectBenchmark.testShared       100             locking  avgt    5    324.508 ±  14.328  ns/op
SharedObjectBenchmark.testShared       100                sync  avgt    5    619.299 ±  76.998  ns/op
SharedObjectBenchmark.testShared       100  reflection-locking  avgt    5    321.526 ±   4.033  ns/op
SharedObjectBenchmark.testShared       100     reflection-sync  avgt    5    649.724 ±  32.273  ns/op
SharedObjectBenchmark.testShared     10000              pooled  avgt    5  15599.401 ± 100.301  ns/op
SharedObjectBenchmark.testShared     10000             locking  avgt    5  15740.155 ± 245.712  ns/op
SharedObjectBenchmark.testShared     10000                sync  avgt    5  33548.452 ± 824.773  ns/op
SharedObjectBenchmark.testShared     10000  reflection-locking  avgt    5  15726.504 ± 180.741  ns/op
SharedObjectBenchmark.testShared     10000     reflection-sync  avgt    5  33646.874 ± 558.182  ns/op
SharedObjectBenchmark.testThread         0              pooled  avgt    5      2.457 ±   0.036  ns/op
SharedObjectBenchmark.testThread         0             locking  avgt    5     15.019 ±   0.130  ns/op
SharedObjectBenchmark.testThread         0                sync  avgt    5      4.477 ±   0.052  ns/op
SharedObjectBenchmark.testThread         0  reflection-locking  avgt    5     17.638 ±   0.136  ns/op
SharedObjectBenchmark.testThread         0     reflection-sync  avgt    5     13.578 ±   0.186  ns/op
SharedObjectBenchmark.testThread       100              pooled  avgt    5    149.695 ±   1.154  ns/op
SharedObjectBenchmark.testThread       100             locking  avgt    5    160.119 ±   2.162  ns/op
SharedObjectBenchmark.testThread       100                sync  avgt    5    151.786 ±   0.148  ns/op
SharedObjectBenchmark.testThread       100  reflection-locking  avgt    5    161.615 ±   1.078  ns/op
SharedObjectBenchmark.testThread       100     reflection-sync  avgt    5    159.875 ±   0.039  ns/op
SharedObjectBenchmark.testThread     10000              pooled  avgt    5  15573.102 ±  12.608  ns/op
SharedObjectBenchmark.testThread     10000             locking  avgt    5  15582.044 ±  11.918  ns/op
SharedObjectBenchmark.testThread     10000                sync  avgt    5  15576.707 ±   9.067  ns/op
SharedObjectBenchmark.testThread     10000  reflection-locking  avgt    5  15596.306 ±  16.459  ns/op
SharedObjectBenchmark.testThread     10000     reflection-sync  avgt    5  15585.727 ±  11.394  ns/op


### 4 Threads
Benchmark                         (tokens)              (type)  Mode  Cnt       Score        Error  Units
SharedObjectBenchmark.testShared         0              pooled  avgt    5       2.454 ±      0.002  ns/op
SharedObjectBenchmark.testShared         0             locking  avgt    5     519.460 ±      7.282  ns/op
SharedObjectBenchmark.testShared         0                sync  avgt    5     168.031 ±     10.200  ns/op
SharedObjectBenchmark.testShared         0  reflection-locking  avgt    5     621.231 ±      4.517  ns/op
SharedObjectBenchmark.testShared         0     reflection-sync  avgt    5     218.310 ±      9.635  ns/op
SharedObjectBenchmark.testShared       100              pooled  avgt    5     149.639 ±      0.364  ns/op
SharedObjectBenchmark.testShared       100             locking  avgt    5     486.804 ±      0.435  ns/op
SharedObjectBenchmark.testShared       100                sync  avgt    5    1143.384 ±     14.263  ns/op
SharedObjectBenchmark.testShared       100  reflection-locking  avgt    5     491.176 ±      1.537  ns/op
SharedObjectBenchmark.testShared       100     reflection-sync  avgt    5    1162.187 ±     11.704  ns/op
SharedObjectBenchmark.testShared     10000              pooled  avgt    5   15615.968 ±     17.172  ns/op
SharedObjectBenchmark.testShared     10000             locking  avgt    5   15726.257 ±     18.149  ns/op
SharedObjectBenchmark.testShared     10000                sync  avgt    5  125111.926 ± 206698.889  ns/op
SharedObjectBenchmark.testShared     10000  reflection-locking  avgt    5   15729.832 ±    108.521  ns/op
SharedObjectBenchmark.testShared     10000     reflection-sync  avgt    5   80621.615 ±  16922.908  ns/op
SharedObjectBenchmark.testThread         0              pooled  avgt    5       2.455 ±      0.004  ns/op
SharedObjectBenchmark.testThread         0             locking  avgt    5      14.984 ±      0.019  ns/op
SharedObjectBenchmark.testThread         0                sync  avgt    5       4.640 ±      0.302  ns/op
SharedObjectBenchmark.testThread         0  reflection-locking  avgt    5      20.199 ±      0.941  ns/op
SharedObjectBenchmark.testThread         0     reflection-sync  avgt    5      13.607 ±      0.106  ns/op
SharedObjectBenchmark.testThread       100              pooled  avgt    5     149.630 ±      0.663  ns/op
SharedObjectBenchmark.testThread       100             locking  avgt    5     160.015 ±      0.228  ns/op
SharedObjectBenchmark.testThread       100                sync  avgt    5     152.171 ±      0.479  ns/op
SharedObjectBenchmark.testThread       100  reflection-locking  avgt    5     161.635 ±      0.454  ns/op
SharedObjectBenchmark.testThread       100     reflection-sync  avgt    5     160.369 ±      0.243  ns/op
SharedObjectBenchmark.testThread     10000              pooled  avgt    5   15625.014 ±    110.846  ns/op
SharedObjectBenchmark.testThread     10000             locking  avgt    5   15622.445 ±     30.451  ns/op
SharedObjectBenchmark.testThread     10000                sync  avgt    5   15613.935 ±     24.450  ns/op
SharedObjectBenchmark.testThread     10000  reflection-locking  avgt    5   15638.445 ±     87.789  ns/op
SharedObjectBenchmark.testThread     10000     reflection-sync  avgt    5   15639.552 ±     70.739  ns/op


### 6 Threads
Benchmark                         (tokens)              (type)  Mode  Cnt        Score          Error  Units
SharedObjectBenchmark.testShared         0              pooled  avgt    5        3.823 ±        0.141  ns/op
SharedObjectBenchmark.testShared         0             locking  avgt    5      944.703 ±       30.491  ns/op
SharedObjectBenchmark.testShared         0                sync  avgt    5       96.352 ±        0.678  ns/op
SharedObjectBenchmark.testShared         0  reflection-locking  avgt    5      888.012 ±       52.111  ns/op
SharedObjectBenchmark.testShared         0     reflection-sync  avgt    5      132.998 ±        1.261  ns/op
SharedObjectBenchmark.testShared       100              pooled  avgt    5      231.213 ±        9.080  ns/op
SharedObjectBenchmark.testShared       100             locking  avgt    5      741.934 ±        5.131  ns/op
SharedObjectBenchmark.testShared       100                sync  avgt    5     1750.084 ±      449.008  ns/op
SharedObjectBenchmark.testShared       100  reflection-locking  avgt    5      872.029 ±       10.594  ns/op
SharedObjectBenchmark.testShared       100     reflection-sync  avgt    5     1610.224 ±       54.341  ns/op
SharedObjectBenchmark.testShared     10000              pooled  avgt    5    24063.821 ±      952.020  ns/op
SharedObjectBenchmark.testShared     10000             locking  avgt    5    24336.445 ±     1245.395  ns/op
SharedObjectBenchmark.testShared     10000                sync  avgt    5  4963460.030 ± 25966909.211  ns/op
SharedObjectBenchmark.testShared     10000  reflection-locking  avgt    5    24207.676 ±      398.591  ns/op
SharedObjectBenchmark.testShared     10000     reflection-sync  avgt    5   104933.741 ±     9177.063  ns/op
SharedObjectBenchmark.testThread         0              pooled  avgt    5        3.794 ±        0.150  ns/op
SharedObjectBenchmark.testThread         0             locking  avgt    5       23.031 ±        0.914  ns/op
SharedObjectBenchmark.testThread         0                sync  avgt    5        6.916 ±        0.307  ns/op
SharedObjectBenchmark.testThread         0  reflection-locking  avgt    5       30.912 ±        0.480  ns/op
SharedObjectBenchmark.testThread         0     reflection-sync  avgt    5       16.415 ±        0.139  ns/op
SharedObjectBenchmark.testThread       100              pooled  avgt    5      236.979 ±       39.424  ns/op
SharedObjectBenchmark.testThread       100             locking  avgt    5      246.973 ±        4.077  ns/op
SharedObjectBenchmark.testThread       100                sync  avgt    5      238.275 ±       26.990  ns/op
SharedObjectBenchmark.testThread       100  reflection-locking  avgt    5      248.533 ±        4.196  ns/op
SharedObjectBenchmark.testThread       100     reflection-sync  avgt    5      240.872 ±       10.274  ns/op
SharedObjectBenchmark.testThread     10000              pooled  avgt    5    24040.624 ±      533.339  ns/op
SharedObjectBenchmark.testThread     10000             locking  avgt    5    24034.847 ±      480.219  ns/op
SharedObjectBenchmark.testThread     10000                sync  avgt    5    24066.297 ±      574.380  ns/op
SharedObjectBenchmark.testThread     10000  reflection-locking  avgt    5    24095.255 ±      449.496  ns/op
SharedObjectBenchmark.testThread     10000     reflection-sync  avgt    5    24124.718 ±      652.325  ns/op


### 8 Threads
Benchmark                         (tokens)              (type)  Mode  Cnt        Score         Error  Units
SharedObjectBenchmark.testShared         0              pooled  avgt    5        5.009 ±       0.084  ns/op
SharedObjectBenchmark.testShared         0             locking  avgt    5     1145.961 ±      41.830  ns/op
SharedObjectBenchmark.testShared         0                sync  avgt    5      155.898 ±       4.236  ns/op
SharedObjectBenchmark.testShared         0  reflection-locking  avgt    5     1128.277 ±       2.561  ns/op
SharedObjectBenchmark.testShared         0     reflection-sync  avgt    5      184.219 ±       0.628  ns/op
SharedObjectBenchmark.testShared       100              pooled  avgt    5      305.513 ±       3.368  ns/op
SharedObjectBenchmark.testShared       100             locking  avgt    5     1101.664 ±       4.258  ns/op
SharedObjectBenchmark.testShared       100                sync  avgt    5     2496.976 ±     147.489  ns/op
SharedObjectBenchmark.testShared       100  reflection-locking  avgt    5     1147.086 ±       9.549  ns/op
SharedObjectBenchmark.testShared       100     reflection-sync  avgt    5     2197.927 ±      55.965  ns/op
SharedObjectBenchmark.testShared     10000              pooled  avgt    5    31820.882 ±     237.365  ns/op
SharedObjectBenchmark.testShared     10000             locking  avgt    5    32037.266 ±     319.576  ns/op
SharedObjectBenchmark.testShared     10000                sync  avgt    5  1115337.122 ± 5904467.539  ns/op
SharedObjectBenchmark.testShared     10000  reflection-locking  avgt    5    32016.188 ±     202.593  ns/op
SharedObjectBenchmark.testShared     10000     reflection-sync  avgt    5   141197.015 ±   14444.848  ns/op
SharedObjectBenchmark.testThread         0              pooled  avgt    5        4.998 ±       0.022  ns/op
SharedObjectBenchmark.testThread         0             locking  avgt    5       30.391 ±       0.141  ns/op
SharedObjectBenchmark.testThread         0                sync  avgt    5        9.133 ±       0.374  ns/op
SharedObjectBenchmark.testThread         0  reflection-locking  avgt    5       35.210 ±       0.118  ns/op
SharedObjectBenchmark.testThread         0     reflection-sync  avgt    5       27.806 ±       0.171  ns/op
SharedObjectBenchmark.testThread       100              pooled  avgt    5      304.575 ±       2.493  ns/op
SharedObjectBenchmark.testThread       100             locking  avgt    5      326.061 ±       2.946  ns/op
SharedObjectBenchmark.testThread       100                sync  avgt    5      310.094 ±       1.776  ns/op
SharedObjectBenchmark.testThread       100  reflection-locking  avgt    5      346.191 ±      40.084  ns/op
SharedObjectBenchmark.testThread       100     reflection-sync  avgt    5      326.174 ±       9.093  ns/op
SharedObjectBenchmark.testThread     10000              pooled  avgt    5    31862.311 ±     459.678  ns/op
SharedObjectBenchmark.testThread     10000             locking  avgt    5    32265.737 ±    2726.331  ns/op
SharedObjectBenchmark.testThread     10000                sync  avgt    5    31781.635 ±     188.522  ns/op
SharedObjectBenchmark.testThread     10000  reflection-locking  avgt    5    32594.144 ±    6334.774  ns/op
SharedObjectBenchmark.testThread     10000     reflection-sync  avgt    5    31884.482 ±     547.345  ns/op


### 10 Threads
Benchmark                         (tokens)              (type)  Mode  Cnt        Score         Error  Units
SharedObjectBenchmark.testShared         0              pooled  avgt    5        6.292 ±       0.047  ns/op
SharedObjectBenchmark.testShared         0             locking  avgt    5     1527.372 ±      13.561  ns/op
SharedObjectBenchmark.testShared         0                sync  avgt    5      162.562 ±       1.139  ns/op
SharedObjectBenchmark.testShared         0  reflection-locking  avgt    5     1463.261 ±      11.906  ns/op
SharedObjectBenchmark.testShared         0     reflection-sync  avgt    5      226.624 ±      91.096  ns/op
SharedObjectBenchmark.testShared       100              pooled  avgt    5      383.336 ±       3.629  ns/op
SharedObjectBenchmark.testShared       100             locking  avgt    5     1280.501 ±       5.706  ns/op
SharedObjectBenchmark.testShared       100                sync  avgt    5     3096.517 ±     341.238  ns/op
SharedObjectBenchmark.testShared       100  reflection-locking  avgt    5     1331.767 ±       5.237  ns/op
SharedObjectBenchmark.testShared       100     reflection-sync  avgt    5     2809.893 ±      63.183  ns/op
SharedObjectBenchmark.testShared     10000              pooled  avgt    5    39904.709 ±     372.270  ns/op
SharedObjectBenchmark.testShared     10000             locking  avgt    5    40205.665 ±     497.223  ns/op
SharedObjectBenchmark.testShared     10000                sync  avgt    5  1000730.379 ± 2344855.025  ns/op
SharedObjectBenchmark.testShared     10000  reflection-locking  avgt    5    40106.868 ±     385.244  ns/op
SharedObjectBenchmark.testShared     10000     reflection-sync  avgt    5   178338.158 ±   23561.326  ns/op
SharedObjectBenchmark.testThread         0              pooled  avgt    5        6.274 ±       0.140  ns/op
SharedObjectBenchmark.testThread         0             locking  avgt    5       39.194 ±       0.124  ns/op
SharedObjectBenchmark.testThread         0                sync  avgt    5       11.389 ±       0.081  ns/op
SharedObjectBenchmark.testThread         0  reflection-locking  avgt    5       53.436 ±       0.741  ns/op
SharedObjectBenchmark.testThread         0     reflection-sync  avgt    5       23.455 ±       0.531  ns/op
SharedObjectBenchmark.testThread       100              pooled  avgt    5      382.523 ±       5.587  ns/op
SharedObjectBenchmark.testThread       100             locking  avgt    5      410.600 ±       5.837  ns/op
SharedObjectBenchmark.testThread       100                sync  avgt    5      389.127 ±       2.610  ns/op
SharedObjectBenchmark.testThread       100  reflection-locking  avgt    5      422.739 ±       3.497  ns/op
SharedObjectBenchmark.testThread       100     reflection-sync  avgt    5      418.092 ±       3.222  ns/op
SharedObjectBenchmark.testThread     10000              pooled  avgt    5    40006.318 ±     614.846  ns/op
SharedObjectBenchmark.testThread     10000             locking  avgt    5    40242.978 ±    1331.207  ns/op
SharedObjectBenchmark.testThread     10000                sync  avgt    5    40062.133 ±     734.664  ns/op
SharedObjectBenchmark.testThread     10000  reflection-locking  avgt    5    40065.231 ±     600.671  ns/op
SharedObjectBenchmark.testThread     10000     reflection-sync  avgt    5    40093.220 ±     185.331  ns/op
