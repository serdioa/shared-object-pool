package de.serdioa.common.pool;

import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * An implementation of the {@link StackTraceProvider} which uses a {@link SecurityManager} to obtain the current call stack.
 * This implementation is faster than {@link ThrowableStackTrace}, but it provides only class names in the call stack,
 * method names are not available.
 */
public class SecurityManagerStackTraceProvider implements StackTraceProvider {

    private StackFillingSecurityManager securityManager;


    public SecurityManagerStackTraceProvider() {
        this.securityManager = AccessController
                .doPrivileged((PrivilegedAction<StackFillingSecurityManager>) StackFillingSecurityManager::new);
    }


    @Override
    public StackTraceElement[] provide() {
        return this.securityManager.provideStackTrace();
    }


    private class StackFillingSecurityManager extends SecurityManager {

        public StackTraceElement[] provideStackTrace() {
            Class<?>[] classContext = this.getClassContext();
            int length = classContext.length;

            // We expect that the context has more than 2 elements: this method,
            // the method SecurityManagerStackTrace.provide(), and the caller.
            // If our expectation is wrong, return an empty array.
            if (length < 3) {
                return new StackTraceElement[0];
            }

            // Create a stack trace based on the context, removing top 2 elements described above.
            StackTraceElement[] stackTrace = new StackTraceElement[length - 2];

            for (int i = length - 3; i >= 0; --i) {
                // Only class names are available, method names are not available.
                stackTrace[i] = new StackTraceElement(classContext[i + 2].getName(), "<unavailable>", null, -1);
            }

            return stackTrace;
        }
    }
}
