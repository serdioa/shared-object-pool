#!/bin/bash

java -jar target/benchmarks.jar ConcurrentSharedObjectPoolBenchmark_01 -t 1 | tee target/ConcurrentSharedObjectPoolBenchmark_01_1.log
java -jar target/benchmarks.jar ConcurrentSharedObjectPoolBenchmark_01 -t 2 | tee target/ConcurrentSharedObjectPoolBenchmark_01_2.log
java -jar target/benchmarks.jar ConcurrentSharedObjectPoolBenchmark_01 -t 4 | tee target/ConcurrentSharedObjectPoolBenchmark_01_4.log
