#!/bin/bash

BENCHMARK=SharedCounterBenchmark
MAX_THREADS=10
for THREADS in $(echo 1; seq 2 2 $MAX_THREADS) ; do
    echo "Running $BENCHMARK with $THREADS threads"
    java -jar target/benchmarks.jar $BENCHMARK -f 1 -t $THREADS -w 5s -r 5s \
        -o target/${BENCHMARK}_${THREADS}.txt \
        -rff target/${BENCHMARK}_${THREADS}.csv 2>&1 \
        | tee target/${BENCHMARK}_${THREADS}.log
done

