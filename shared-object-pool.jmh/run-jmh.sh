#!/bin/bash

java -jar target/benchmarks.jar LockingSharedObjectPoolBenchmark_01 -t 1 | tee target/LockingSharedObjectPoolBenchmark_01_1.log
java -jar target/benchmarks.jar LockingSharedObjectPoolBenchmark_01 -t 2 | tee target/LockingSharedObjectPoolBenchmark_01_2.log
java -jar target/benchmarks.jar LockingSharedObjectPoolBenchmark_01 -t 4 | tee target/LockingSharedObjectPoolBenchmark_01_4.log
