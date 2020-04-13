#!/bin/bash

# java -jar target/benchmarks.jar ConcurrentSharedObjectPoolBenchmark_01 -t 1 2>&1 | tee target/ConcurrentSharedObjectPoolBenchmark_01_1.log
# java -jar target/benchmarks.jar ConcurrentSharedObjectPoolBenchmark_01 -t 2 2>&1 | tee target/ConcurrentSharedObjectPoolBenchmark_01_2.log
# java -jar target/benchmarks.jar ConcurrentSharedObjectPoolBenchmark_01 -t 4 2>&1 | tee target/ConcurrentSharedObjectPoolBenchmark_01_4.log

java -jar target/benchmarks.jar SharedCounterBenchmark -f 1 -t 1 2>&1 | tee target/SharedCounterBenchmark_1.log
#java -jar target/benchmarks.jar SharedCounterBenchmark -f 1 -t 2 2>&1 | tee target/SharedCounterBenchmark_2.log
java -jar target/benchmarks.jar SharedCounterBenchmark -f 1 -t 4 2>&1 | tee target/SharedCounterBenchmark_4.log
#java -jar target/benchmarks.jar SharedCounterBenchmark -f 1 -t 6 2>&1 | tee target/SharedCounterBenchmark_6.log
java -jar target/benchmarks.jar SharedCounterBenchmark -f 1 -t 8 2>&1 | tee target/SharedCounterBenchmark_8.log
#java -jar target/benchmarks.jar SharedCounterBenchmark -f 1 -t 10 2>&1 | tee target/SharedCounterBenchmark_10.log

