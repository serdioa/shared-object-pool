# Shared Object Pool

This library provide a pool of objects accessible by keys, where each object
may be accessed by multiple clients simultaneously. The pool supports objects
with a complicated life cycle, which are expensive to create, to keep and to
destroy. A typical example is a pool of FX exchange rates connected to a remote
market which provides live prices: it is slow and expensive to create (requires
network calls to subscribe on a remote data source), expensive to keep (it
consumes the network capacity, receiving price updates all the time), and
expensive to destroy (requires network calls to properly unsubscribe from the
remote data source). Pools provided by this library handle the complexity behind
such objects.

## Basic concepts

The main concepts used by this library are `pooled object`, `shared object`
and `shared object pool`.

A `pooled object` is an object which contains all the business logic. In our
example with FX exchange rates, a `pooled object` is an FX rate object for a
particular currency, capable of connecting to a remote market, receiving live
"ticking" prices over network, and providing this prices to it's clients.

A `shared object` is a wrapper around a `pooled object` provided by the pool
to a particular client, where a "client" is a piece of code (a class, a Spring
bean etc). A client owns a particular `shared object`, but multiple
`shared objects` may be backed by the same `pooled object`. When a client is
done using the `shared object`, the client explicitly disposes of it, informing
the pool that it does not require the `shared object` anymore.

A `shared object pool` is a cache of `pooled objects` mapped to keys,
responsible for providing `shared object` wrappers and managing a life cycle
of `pooled objects`, that is creating and destroying them, when required.
Since it could be expensive to keep a `pooled object` (for example, it may
consume network traffic), a pool may dispose of a `pooled object` if no client
is using a `shared object` backed by it. A pool may implement various policies,
for example it may keep a `pooled object` for some time even if no client is
using it, to reduce an overhead of creating a new `pooled object` when some
client requests it again.

Let us consider our example (a pool with FX exchange rates) in more details.

* A new pool is created. Initially the pool does not contain any
`pooled objects` or `shared objects`.

* A client A requests the FX rate for USD. Since there is no `pooled object`
for the USD FX rate in the cache, the pool creates a new `pooled object` for the
USD FX rate and adds it to the cache. The pool creates a new `shared object` for
the USD FX rate and returns it to the client.

* A client B requests the FX rate for EUR. The same as above, since there is no
`pooled object` for the EUR FX rate in the cache, the pool creates a new
`pooled object` for the EUR FX rate and adds it to the cache. The pool creates a
new `shared object` for the EUR FX rate and returns it to the client.

* The A client A requests the FX rate for EUR. The pool finds a `pooled object`
for the EUR FX rate in the cache, so no new `pooled object` is created. The pool
creates a new `shared object` for the EUR FX rate and returns it to the client.
Note that at this moment for the EUR FX rate there are two different
`shared objects` backed by the same `pooled object`.

* The client B disposes of the `shared object` for the EUR FX rate. The pool
tracks `shared objects`, so it knows that now there is only one active
`shared object` for the EUR FX rate remaining. The cached `pooled object` for
the EUR FX rate is still required, because it is backing the remaining
`shared object` for the EUR FX rate.

* The client A disposes of the `shared object` for the EUR FX rate as well.
Now the pool knows that there are no active `shared objects` for the EUR FX
rate. To prevent unnecessary network traffic caused by the cached
`pooled object` for the EUR FX rate, the pool may dispose of the `pooled object`
and remove it from the cache. Alternatively, the pool may decide to keep the
`pooled object` for some more time (configurable property of the pool), so that
if a client request the EUR FX rate again, it will get it faster, because the
`pooled object` is still available in the pool.

The following code snippet shows a typical usage from the client's point of
view:

```java
// Obtain the pool from the application environment, for example inject it
// from the Spring context.
SharedObjectPool<String, SharedFxRate> pool = ...;

// Obtain FX rates from the pool.
SharedFxRate eur = pool.get("EUR");
SharedFxRate usd = pool.get("USD");

// ... some code using FX rates ...

// We do not require FX rate objects anymore. Dispose of them, so that the pool
// may remove backing pooled objects, if there are no other shared objects which
// rely on them.
eur.dispose();
usd.dispose();
```

## Implementing a shared object pool

This library provides 3 implementations of a `shared object pool`:

* `ConcurrentSharedObjectPool` - a generic-purpose implementation based on
non-blocking data structures. This implementation is recommended in most cases.

* `SynchronizedSharedObjectPool` - an implementation based plain Java
synchronization. This implementation is significantly simpler than the
concurrent implementation, so it is faster when a pool may be simultaneously
used only by a few threads, but the performance of this implementation
degrades dramatically as the number of threads grows. In my tests this
implementation is faster than the concurrent one if only 1 or 2 threads may
access the pool simultaneously. This implementation is not recommended, except
of very special cases when, on one hand, performance is critical, and on another
hand you could guarantee that the pool is never accessed simultaneously by
multiple threads.

* `LockingSharedObjectPool` - an implementation based on locks. Contrary to
an intuitive expectation, this implementation is even slower as the
`SynchronizedSharedObjectPool` and is not recommended in any scenario. It is
provided only for a comparison.

Performance tests for available `shared object pool` implementations
are available in a
[separate document](shared-object-pool.jmh/src/main/R/SharedObjectPool.html).

In order to implement a pool for a particular object type, you must provide
to a pool implementation two factories:

* A `PooledObjectFactory` is responsible for creating new `pooled objects`, and
for destroying `pooled objects` which are not required anymore.

* A `SharedObjectFactory` is responsible for creating `shared object` wrappers
from `pooled objects`.

This library provides two generic-purpose reflection-based implementations
of the `SharedObjectFactory`:

* `SynchronizedSharedObject` - an implementation based on plain Java
synchronization.

* `LockingSharedObject` - an implementation based on locks.

The `SynchronizedSharedObject` is marginally faster (3 nanoseconds per method
call) compared to the `LockingSharedObject`, when each shared object is used
only by a single thread. If the same shared object may be used by multiple
threads, performance of the `SynchronizedSharedObject` quickly degrades. Still,
you may consider using the `SynchronizedSharedObject` in time-critical code
if you could guarantee that each shared object will be used only in a single
thread.

An alternative to using reflection-based shared objects provided by the library,
is to use a hand-crafted shared object implementation which does not rely on
reflection, instead using a normal method calls to delegate to a backing `pooled
object`. In my tests an overhead of a reflection-based shared object compared
to a hand-crafted shared object is about 5 nanoseconds per method call, so in
most cases an overhead of maintaining a separate hand-crafted `shared object`
implementation for each `pooled object` type is not worth the trouble.

Performance tests for available `shared object` implementations
are available in a
[separate document](shared-object-pool.jmh/src/main/R/SharedObject.html).


The following code snippet shows how a `shared object pool` could be created.

```java

// Declare an interface with business methods.
public interface FxRate {
    String getSymbol();
    double getPrice();
    // ... various other business methods ...
}

// Declare an interface for the shared object, extending the business interface.
// There are no additional methods in the interface.
public interface SharedFxRate extends FxRate, SharedObject {}

// Implement the pooled object, that is the object with all the real business logic.
public class FxRateImpl implements FxRate {
    public String getSymbol() {
        // Implement the method declared in the interface FxRate.
    }
    
    public double getPrice() {
        // Implement the method declared in the interface FxRate.
    }
    
    // Implement all other methods declared in the interface FxRate.
}

// Implement the factory for managing lifecycle of pooled objects.
public class PooledFxRateFactory implements PooledObjectFactory<String, FxRate> {
    public FxRate create(String key) {
        // Create a new FxRateImpl and return it.
        //
        // Note that the Javadoc of PooledObjectFactory highly recommends to
        // keep this method as fast as possible, in particular it shall not
        // perform any complicated initialization and no remote calls.
        // All slow initialization shall be implemented in the method
        // initialize() below.
        FxRate fxRate = ...

        return fxRate;
    }

    public void initialize(FxRate fxRate) {
        // Initialize the provided pooled object. In particular, this method
        // may connect to a remote data source and load some data, or register
        // listeners.
    }
    
    public void dispose(FxRate fxRate) {
        // Destroy the provided pooled object. In particular, this method
        // may disconnect from remote data sources, remove previously registered
        // listeners, or dispose of used resources.
    }
}

// We will use a reflection-based factory for creating shared objects, so
// we do not require any special class for it.

// Create the shared object pool.
PooledObjectFactory<String, FxRate> pooledObjectFactory =
        new PooledFxRateFactory(...);
SharedObjectFactory<FxRate, SharedFxRate> sharedObjectFactory =
        LockingSharedObject.factory(SharedFxRate.class);
SharedObjectPool<String, FxRate> pool =
        new ConcurrentSharedObjectPool.Builder<String, SharedFxRate, FxRate>()
                .setPooledObjectFactory(pooledObjectFactory)
                .setSharedObjectFactory(sharedObjectFactory)
                .build();
```

## Extended configuration

Besides the basic functionality, implementations of the shared object pool
provided by this library have additional optional capabilities.

### Disposing of `pooled objects`

By default all pool implementations provided by this library dispose of the
`pooled object` immediately when there are no `shared objects` backed by it.
All pool implementations support several configuration parameters which allows
to modify when `pooled objects` are disposed of:

* `disposeUnused` - defaults to `false`. When set to `true`, the pool will never
dispose of `pooled objects`, keeping them in cache even if they are not backing
any active `shared object`. This mode may be useful when `pooled objects` are
expensive to create, but cheap to keep. For example, this mode makes sense
if creating a new `pooled object` loads a lot of data from an external slow data
source, but these data changes extremely seldom, so keeping a `pooled object`
in a cache consumes very little of a network traffic.

* `idleDisposeTimeMillis` - defaults to 0. Duration in milliseconds to keep
unused `pooled objects` before disposing of them. Using a non-zero keep-alive
time may be useful in cases when both creating and keeping a `pooled object` is
expensive, allowing a compromise: the pool keeps unused `pooled objects`in a
cache for some time, able to provide an object quickly if another client
requests it before the keep-alive duration expires, but eventually the pool
disposes of unused `pooled objects` to reduce the keep-alive overhead.

* `disposeThreads` - defaults to 0. A number of background threads responsible
for disposing of unused `pooled objects`. This parameter must be set to a
positive number when `idleDisposeTimeMillis` is configured. In most cases one
thread shall be enough, but it could be required to increase a number of threads
if `pooled objects` are created and destroyed very often, or if it takes a very
long time to properly dispose of a `pooled object`.

### Tracking abandoned `shared objects`

This library requires users to explicitly dispose of a `shared object` once the
client does not require it by calling the method `SharedObject.dispose()`, but
the library could not enforce correct usage. It is possible that a client
forgets to properly dispose of a `shared object` and just lets the GC to claim
it. If a pool is not notified that a `shared object` is not required anymore,
the pool considers related `pooled object` to be still used, and does not
dispose of it, keeping the `pooled object` in the cache.

The `ConcurrentSharedObjectPool` implementation contains a protection against
such cases, tracking abandoned `shared objects`. The implementation relies
on the `PhantomReference` class. When the `ConcurrentSharedObjectPool` detects
that a `shared object` has been claimed by the GC without being properly
dispose of, it writes a warning message to a log.

By default the warning message contains just a key of the `shared object` which
was not properly disposed of. The library allows to register on a pool a stack
trace provider to enrich the warning log message with an execution stack trace
taken when the client obtained the `shared object`. If there are multiple places
in the code which obtain `shared objects` from a particular pool, an execution
stack trace may help to identify the place in the code where a `shared object`
was obtained, but never properly disposed of.

The library provides the following stack trace providers:

* `NoOpStackTraceProvider` - default, does not collect the stack trace.

* `ThrowableStackTraceProvider` - uses a `Throwable` to obtain the current stack
trace. An execution stack trace created by this provider looks identical to a
familiar exception stack trace, that is it contains names of classes and
methods, as well as lines in the source code, if they are available.
This implementation is very slow, it takes 5 - 10 microseconds to provide
a stack trace. Remember that this performance penalty applies each time when
a `shared object` is provided by the pool, so it quickly accumulates.

* `SecurityManagerStackTraceProvider` - uses a `SecurityManager` to obtain the
current stack trace. A stack trace from this provider contain only names of
classes, but it does not contain names of functions or lines in the source code.
On the other hand, this implementation is faster then the
`ThrowableStackTraceProvider`, it takes 1-2 microseconds to provide a stack
trace.

The default mode (no stack traces) is recommended for the production usage.
Alternative stack trace providers could be used for troubleshooting.

There is one additional topic related to detecting abandoned objects, namely
of false positives. Due to Java runtime optimizations, in rare cases it is
possible that an object will be claimed by the GC before properly disposed of,
even if the client called the method `dispose()`. In particular, when JIT
compiles Java byte code into the native code, it may inline methods and re-order
operations, if they do not affect an execution. I have observed a case when 
GC claimed a `shared object` when the `dispose()` method of that shared object
was still being executed. This is a possible optimization: if the object is not
actually used by the method `dispose()` or any subsequent code, the JVM is free
to GC the object. If the thread executing the method `dispose()` is put on ice
by the JVM, and the GC thread runs instead, the application may observe that
first a phantom reference on the GC'ed object appears in the reference queue,
and only later the method `dispose()` continues. In such case the protection
against abandoned objects provided by the `SecurityManagerStackTraceProvider`
first will detect that an object is abandoned, and a short time later will
attempt to properly dispose of the object. `SecurityManagerStackTraceProvider`
writes a special log message when such case is detected, telling to ignore
the previous warning about an abandoned object.

Such false positive warnings happens very rarely. One could prevent them by
using a `shared object` even after it is disposed of (for example, by calling
the method `isDisposed()`), but it is not elegant and makes the code slower.
