package de.serdioa.common.pool;

import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * An implementation of the {@link StackTraceProvider} which uses a {@link SecurityManager} to obtain the current call
 * stack. This implementation is faster than {@link ThrowableStackTraceProvider}, but it provides only class names in
 * the call stack, method names are not available.
 */
public class SecurityManagerStackTraceProvider implements StackTraceProvider {

    private final StackFillingSecurityManager securityManager;


    public SecurityManagerStackTraceProvider() {
        this.securityManager = AccessController
                .doPrivileged((PrivilegedAction<StackFillingSecurityManager>) StackFillingSecurityManager::new);
    }


    @Override
    public StackTrace provide() {
        return this.securityManager.provideStackTrace(0);
    }


    @Override
    public StackTrace provide(int skipFrames) {
        return this.securityManager.provideStackTrace(skipFrames);
    }


    private class StackFillingSecurityManager extends SecurityManager {

        public StackTrace provideStackTrace(int skipFrames) {
            Class<?>[] classContext = this.getClassContext();
            int length = classContext.length;

            // We expect that the context has more than 2 elements: this method,
            // the method SecurityManagerStackTrace.provide(), and the caller.
            // If our expectation is wrong, return an empty array.
            if (length < 3 + skipFrames) {
                return StackTrace.empty();
            }

            // Create a stack trace based on the context, removing top 2 elements described above.
            StackTraceElement[] elements = new StackTraceElement[length - 2 - skipFrames];

            for (int i = length - 3 - skipFrames; i >= 0; --i) {
                // Only class names are available, method names are not available.
                elements[i] =
                        new StackTraceElement(classContext[i + 2 + skipFrames].getName(), "<unavailable>", null, -1);
            }

            return new StackTrace(elements);
        }
    }
}
