package de.serdioa.common.pool;


public class StackTrace {

    // A shared empty array of stack trace elements.
    private static final StackTraceElement[] EMPTY_ELEMENTS = new StackTraceElement[0];

    // A shared empty stack trace.
    private static final StackTrace EMPTY = new StackTrace(EMPTY_ELEMENTS);

    private final StackTraceElement[] elements;


    public StackTrace(StackTraceElement[] elements) {
        this.elements = (elements != null ? elements : EMPTY_ELEMENTS);
    }


    public StackTraceElement[] getElements() {
        // Return a defensive copy.
        return this.elements.clone();
    }


    @Override
    public String toString() {
        // Fast-track if stack trace is not available.
        if (this.elements.length == 0) {
            return "\tunavailable\n";
        }

        StringBuilder sb = new StringBuilder();

        for (StackTraceElement element : this.elements) {
            sb.append("\tat ").append(element).append("\n");
        }

        return sb.toString();
    }


    public static StackTrace empty() {
        return EMPTY;
    }
}
