package de.serdioa.common.pool;

import java.util.Objects;
import java.util.function.Supplier;


public class AbstractStackTraceProviderTest {

    /**
     * A simple wrapper to simulate method calls with different call depth.
     */
    public static class Wrapper implements Supplier<StackTrace> {

        private final Supplier<StackTrace> supplier;
        private final int depth;


        public Wrapper(Supplier<StackTrace> supplier) {
            this(supplier, 1);
        }


        public Wrapper(Supplier<StackTrace> supplier, int depth) {
            if (depth < 1) {
                throw new IllegalArgumentException("depth < 1");
            }

            this.supplier = Objects.requireNonNull(supplier);
            this.depth = depth;
        }


        @Override
        public StackTrace get() {
            if (this.depth <= 1) {
                return this.supplier.get();
            } else {
                return this.get(this.depth - 1);
            }
        }


        private StackTrace get(int depth) {
            if (depth <= 1) {
                return this.supplier.get();
            } else {
                return this.get(depth - 1);
            }
        }
    }
}
