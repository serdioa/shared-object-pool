package de.serdioa.common.pool;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;


/**
 * Micrometer performance metrics for a {@link SharedObjectPool}.
 */
public class SharedObjectPoolMetrics {

    private final SharedObjectPoolStats stats;
    private final SharedObjectPoolStatsListener statsListener = new StatsListener();

    private final MeterRegistry meterRegistry;

    private final Gauge pooledObjectsCount;
    private final Gauge unusedPooledObjectsCount;
    private final Gauge sharedObjectsCount;

    private final Timer hit;
    private final Timer miss;
    private final Timer createdSuccess;
    private final Timer createdFailed;
    private final Timer initializedSuccess;
    private final Timer initializedFailed;
    private final Timer disposedSuccess;
    private final Timer disposedFailed;


    public SharedObjectPoolMetrics(SharedObjectPoolStats stats, MeterRegistry meterRegistry) {
        this.stats = Objects.requireNonNull(stats);
        this.meterRegistry = Objects.requireNonNull(meterRegistry);

        List<Tag> tags = buildTags(this.stats);

        this.pooledObjectsCount = Gauge.builder("sharedObjectPool.pooled", this.stats,
                SharedObjectPoolStats::getPooledObjectsCount)
                .description("Number of objects in the pool")
                .tags(tags)
                .register(this.meterRegistry);
        this.unusedPooledObjectsCount = Gauge.builder("sharedObjectPool.pooledUnused", this.stats,
                SharedObjectPoolStats::getUnusedPooledObjectsCount)
                .description("Number of unused objects in the pool eligible for eviction")
                .tags(tags)
                .register(this.meterRegistry);
        this.sharedObjectsCount = Gauge.builder("sharedObjectPool.shared", this.stats,
                SharedObjectPoolStats::getSharedObjectsCount)
                .description("Number of shared objects provided by the pool")
                .tags(tags)
                .register(this.meterRegistry);

        this.hit = Timer.builder("sharedObjectPool.get")
                .description("Cache hit when getting object from the pool")
                .tags(tags)
                .tag("result", "hit")
                .register(this.meterRegistry);
        this.miss = Timer.builder("sharedObjectPool.get")
                .description("Cache miss when getting object from the pool")
                .tags(tags)
                .tag("result", "miss")
                .register(this.meterRegistry);

        this.createdSuccess = Timer.builder("sharedObjectPool.created")
                .description("Created pooled objects")
                .tags(tags)
                .tag("result", "success")
                .register(this.meterRegistry);
        this.createdFailed = Timer.builder("sharedObjectPool.created")
                .description("Created pooled objects")
                .tags(tags)
                .tag("result", "failed")
                .register(this.meterRegistry);

        this.initializedSuccess = Timer.builder("sharedObjectPool.initialized")
                .description("Initialized pooled objects")
                .tags(tags)
                .tag("result", "success")
                .register(this.meterRegistry);
        this.initializedFailed = Timer.builder("sharedObjectPool.initialized")
                .description("Initialized pooled objects")
                .tags(tags)
                .tag("result", "failed")
                .register(this.meterRegistry);

        this.disposedSuccess = Timer.builder("sharedObjectPool.disposed")
                .description("Disposed pooled objects")
                .tags(tags)
                .tag("result", "success")
                .register(this.meterRegistry);
        this.disposedFailed = Timer.builder("sharedObjectPool.disposed")
                .description("Disposed pooled objects")
                .tags(tags)
                .tag("result", "failed")
                .register(this.meterRegistry);

        this.stats.addSharedObjectPoolStatsListener(this.statsListener);
    }


    private List<Tag> buildTags(SharedObjectPoolStats stats) {
        return Arrays.asList(Tag.of("type", stats.getClass().getSimpleName()));
    }


    public void dispose() {
        this.stats.removeSharedObjectPoolStatsListener(this.statsListener);

        this.meterRegistry.remove(this.pooledObjectsCount);
        this.meterRegistry.remove(this.unusedPooledObjectsCount);
        this.meterRegistry.remove(this.sharedObjectsCount);

        this.meterRegistry.remove(this.hit);
        this.meterRegistry.remove(this.miss);
        this.meterRegistry.remove(this.createdSuccess);
        this.meterRegistry.remove(this.createdFailed);
        this.meterRegistry.remove(this.initializedSuccess);
        this.meterRegistry.remove(this.initializedFailed);
        this.meterRegistry.remove(this.disposedSuccess);
        this.meterRegistry.remove(this.disposedFailed);
    }


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append(toString(this.pooledObjectsCount)).append("\n");
        b.append(toString(this.unusedPooledObjectsCount)).append("\n");
        b.append(toString(this.sharedObjectsCount)).append("\n");

        b.append(toString(this.hit)).append("\n");
        b.append(toString(this.miss)).append("\n");
        b.append(toString(this.createdSuccess)).append("\n");
        b.append(toString(this.createdFailed)).append("\n");
        b.append(toString(this.initializedSuccess)).append("\n");
        b.append(toString(this.initializedFailed)).append("\n");
        b.append(toString(this.disposedSuccess)).append("\n");
        b.append(toString(this.disposedFailed)).append("\n");

        return b.toString();
    }


    private String toString(Meter meter) {
        return meter.getId() + ": " + meter.measure();
    }


    private class StatsListener implements SharedObjectPoolStatsListener {

        @Override
        public void onSharedObjectGet(long durationNanos, boolean hit) {
            if (hit) {
                SharedObjectPoolMetrics.this.hit.record(durationNanos, TimeUnit.NANOSECONDS);
            } else {
                SharedObjectPoolMetrics.this.miss.record(durationNanos, TimeUnit.NANOSECONDS);
            }
        }


        @Override
        public void onPooledObjectCreated(long durationNanos, boolean success) {
            if (success) {
                SharedObjectPoolMetrics.this.createdSuccess.record(durationNanos, TimeUnit.NANOSECONDS);
            } else {
                SharedObjectPoolMetrics.this.createdFailed.record(durationNanos, TimeUnit.NANOSECONDS);
            }
        }


        @Override
        public void onPooledObjectInitialized(long durationNanos, boolean success) {
            if (success) {
                SharedObjectPoolMetrics.this.initializedSuccess.record(durationNanos, TimeUnit.NANOSECONDS);
            } else {
                SharedObjectPoolMetrics.this.initializedFailed.record(durationNanos, TimeUnit.NANOSECONDS);
            }
        }


        @Override
        public void onPooledObjectDisposed(long durationNanos, boolean success) {
            if (success) {
                SharedObjectPoolMetrics.this.disposedSuccess.record(durationNanos, TimeUnit.NANOSECONDS);
            } else {
                SharedObjectPoolMetrics.this.disposedFailed.record(durationNanos, TimeUnit.NANOSECONDS);
            }
        }
    }
}
