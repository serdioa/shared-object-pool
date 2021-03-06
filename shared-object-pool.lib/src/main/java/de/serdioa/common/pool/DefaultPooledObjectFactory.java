package de.serdioa.common.pool;

import java.util.Objects;
import java.util.function.Consumer;


/**
 * An implementation of a {@link PooledObjectFactory} that allows to register external functions or lambda expressions
 * for each function, acting as an adapter for legacy code.
 *
 * @param <K> type of keys used to create pooled objects.
 * @param <P> type of pooled objects created by this factory.
 */
public class DefaultPooledObjectFactory<K, P> implements PooledObjectFactory<K, P> {

    private final PooledObjectCreator<K, P> creator;
    private final Consumer<P> initializer;
    private final Consumer<P> disposer;


    private DefaultPooledObjectFactory(PooledObjectCreator<K, P> creator,
            Consumer<P> initializer, Consumer<P> disposer) {
        this.creator = Objects.requireNonNull(creator);
        this.initializer = Objects.requireNonNull(initializer);
        this.disposer = Objects.requireNonNull(disposer);
    }


    @Override
    public P create(K key) throws InvalidKeyException {
        return this.creator.create(key);
    }


    @Override
    public void initialize(P pooledObject) {
        this.initializer.accept(pooledObject);
    }


    @Override
    public void dispose(P pooledObject) {
        this.disposer.accept(pooledObject);
    }


    /**
     * A builder for creating a {@link PooledObjectFactory} by registering external functions or lambda expressions for
     * each function. Only the creator function is mandatory. The initializer and the disposer functions are no-op by
     * default.
     *
     * @param <K> type of keys used to create pooled objects.
     * @param <P> type of pooled objects created by this factory.
     */
    public static class Builder<K, P> {

        private PooledObjectCreator<K, P> creator;
        private Consumer<P> initializer = p -> {}; // Default no-op initializer.
        private Consumer<P> disposer = p -> {}; // Default no-op disposer.


        public Builder<K, P> setCreator(PooledObjectCreator<K, P> creator) {
            this.creator = creator;
            return this;
        }


        public Builder<K, P> setInitializer(Consumer<P> initializer) {
            this.initializer = initializer;
            return this;
        }


        public Builder<K, P> setDisposer(Consumer<P> disposer) {
            this.disposer = disposer;
            return this;
        }


        public PooledObjectFactory<K, P> build() {
            if (this.creator == null) {
                throw new IllegalStateException("creator is required");
            }
            if (this.initializer == null) {
                throw new IllegalStateException("initializer is required");
            }
            if (this.disposer == null) {
                throw new IllegalArgumentException("disposer is not set");
            }

            return new DefaultPooledObjectFactory<>(creator, initializer, disposer);
        }
    }
}
