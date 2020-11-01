package de.serdioa.common.pool.jmh;

import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.NoOpStackTraceProvider;
import de.serdioa.common.pool.SecurityManagerStackTraceProvider;
import de.serdioa.common.pool.StackTrace;
import de.serdioa.common.pool.StackTraceProvider;
import de.serdioa.common.pool.ThrowableStackTraceProvider;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


/**
 * Benchmark for {@link StackTradeProvider}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class StackTraceProviderBenchmark {

    @Param({"noop", "sec", "throwable"})
    public String type;

    @Param({"0", "1", "10", "20"})
    public int level;

    private StackTraceProvider provider;


    @Setup
    public void setup() {
        switch (this.type) {
            case "noop":
                this.provider = new NoOpStackTraceProvider();
                break;
            case "sec":
                this.provider = new SecurityManagerStackTraceProvider();
                break;
            case "throwable":
                this.provider = new ThrowableStackTraceProvider();
                break;
            default:
                throw new IllegalArgumentException("Unexpected type of the stack trace provider: " + this.type);
        }
    }


    @Benchmark
    public StackTrace testStackTrace() {
        return this.deep(this.level);
    }


    // A recursive method to create a realistic deep stack trace.
    private StackTrace deep(int level) {
        if (level == 0) {
            return this.provider.provide();
        } else {
            return this.deep(level - 1);
        }
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(StackTraceProviderBenchmark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
