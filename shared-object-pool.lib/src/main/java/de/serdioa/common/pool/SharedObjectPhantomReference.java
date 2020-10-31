package de.serdioa.common.pool;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;


/**
 * A phantom reference on a shared object held by the pool to track shared objects abandoned by a client without
 * properly disposing of it.
 */
/* package private */ class SharedObjectPhantomReference<K, S extends SharedObject> extends PhantomReference<S> {

    // The key of the pooled object, and the ID of the shared object. Both are used only for logging.
    private final K key;
    private final long sharedObjectId;

    // The stack trace taken when the shared object has been allocated, to track abandoned shared objects.
    private final StackTrace stackTrace;

    // A callback to be invoked to dispose of the shared object by the abandoned objects monitor.
    // This callback is invoked when an abandoned objects monitor detects that the shared object has been GC'ed
    // without being explicitly disposed of.
    private final Runnable disposeCallback;

    // A key in the entry map pointing to this phantom reference, so that it may be removed after disposing
    // of the object.
    private final Object phantomReferenceKey;

    // The name of the thread which disposed of the shared object held by this phantom reference, if any.
    // We do not keep the thread object itself to prevent possible thread-related memory leaks, such as keeping
    // unnecessary thread-local variables after the thread stopped.
    // This variable is used to track double-dispose errors, when the same shared object is disposed of more than
    // once.
    // @GuardedBy synchronized(this)
    private String disposedByName;

    // Was the shared object held by this phantom reference disposed directly, that is by calling it's dispose
    // method (true) or indirectly, that is by the reaper thread cleaning up phantom references (false).
    // This variable is used to track double-dispose errors, when the same shared object is disposed of more than
    // once.
    // @GuardedBy synchronized(this)
    private SharedObjectDisposeType disposeType;


    SharedObjectPhantomReference(K key, long sharedObjectId, StackTrace stackTrace,
            S referent, Runnable disposeCallback,
            Object phantomReferenceKey, ReferenceQueue<? super S> queue) {
        super(referent, queue);
        this.key = Objects.requireNonNull(key);
        this.sharedObjectId = sharedObjectId;
        this.stackTrace = Objects.requireNonNull(stackTrace);
        this.disposeCallback = Objects.requireNonNull(disposeCallback);
        this.phantomReferenceKey = Objects.requireNonNull(phantomReferenceKey);
    }


    public K getKey() {
        return this.key;
    }


    public long getSharedObjectId() {
        return this.sharedObjectId;
    }


    public StackTrace getStackTrace() {
        return this.stackTrace;
    }


    public Object getPhantomReferenceKey() {
        return this.phantomReferenceKey;
    }


    public void disposeIfRequired() {
        this.disposeCallback.run();
    }

    // Subsequent methods must be called when synchronized on this phantom reference, each of them contains
    // an assertion. We are using assertions instead of checks with runtime exceptions, because all methods
    // may be used only inside the class ConcurrentSharedObjectPool (this file), so we are in a full control.
    // Assertions are just to check for possible programming errors, especially if this class is refactored later.

    public void markAsDisposed(SharedObjectDisposeType disposeType) {
        assert (Thread.holdsLock(this));

        // Clear this phantom reference.
        // If the shared object has been disposed of properly, we do not need to track when it is GC'ed anymore.
        // If the shared object has been GC'ed without being properly disposed of, and disposal was triggered
        // by the reaper thread, there is nothing to track anymore (the shared object already has been GC'ed).
        this.clear();
        this.disposedByName = Thread.currentThread().getName();
        this.disposeType = disposeType;
    }


    public boolean isDisposed() {
        assert (Thread.holdsLock(this));

        return (this.disposedByName != null);
    }


    public String getDisposedByName() {
        assert (Thread.holdsLock(this));

        return this.disposedByName;
    }


    public SharedObjectDisposeType getDisposeType() {
        assert (Thread.holdsLock(this));

        return this.disposeType;
    }


    /**
     * A simple mutable holder class. We may have used a 1-element array to hold the object, but arrays of generic
     * objects are not directly support and require casting, so a simple mutable object is more elegant.
     */
    public static class Holder<K, S extends SharedObject> {

        public SharedObjectPhantomReference<K, S> ref;
    }
}
