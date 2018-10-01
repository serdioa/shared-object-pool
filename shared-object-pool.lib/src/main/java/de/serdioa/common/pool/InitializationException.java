package de.serdioa.common.pool;


public class InitializationException extends RuntimeException {

    /** Serial version UID. */
    private static final long serialVersionUID = 7353897128474452193L;

    public InitializationException(Object key) {
        super(buildDefaultMessage(key));
    }


    public InitializationException(Object key, Throwable cause) {
        super(buildDefaultMessage(key), cause);
    }


    public InitializationException(Object key, String message) {
        super(buildDefaultMessage(key) + ": " + message);
    }


    private static String buildDefaultMessage(Object pooledObject) {
        return "Can't initialize pooled object " + pooledObject;
    }


    public static InitializationException wrap(Object key, Exception ex) {
        if (ex instanceof InitializationException) {
            return (InitializationException) ex;
        } else {
            return new InitializationException(key, ex);
        }
    }
}
