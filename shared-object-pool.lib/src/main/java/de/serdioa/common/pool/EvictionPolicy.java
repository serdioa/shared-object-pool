package de.serdioa.common.pool;


/**
 * Defines an eviction policy for pooled objects.
 */
public interface EvictionPolicy {
    Cancellable evict(Runnable evictionCallback);
    
    
    void dispose();
}
