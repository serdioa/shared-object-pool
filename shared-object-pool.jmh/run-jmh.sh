#!/bin/bash

SharedObjectBenchmark() {
    local BENCHMARK=SharedObjectBenchmark
    local MAX_THREADS=4

    for THREADS in $(echo 1; seq 2 2 $MAX_THREADS) ; do
        echo "Running $BENCHMARK with $THREADS threads"
        $JAVA_HOME/bin/java -jar target/benchmarks.jar $BENCHMARK -f 1 -t $THREADS -w 5s -r 5s \
            -p tokens="0,10,100,200,300,400,500,600,700,800,900,1000" \
            -o target/${BENCHMARK}_${THREADS}.txt \
            -rff target/${BENCHMARK}_${THREADS}.csv 2>&1
    done
}

SharedObjectBenchmark

