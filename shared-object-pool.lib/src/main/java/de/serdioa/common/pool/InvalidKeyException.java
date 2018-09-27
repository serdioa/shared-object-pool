package de.serdioa.common.pool;


public class InvalidKeyException extends IllegalArgumentException {
    public InvalidKeyException(Object key) {
        super(buildDefaultMessage(key));
    }


    public InvalidKeyException(Object key, Throwable cause) {
        super(buildDefaultMessage(key), cause);
    }


    private static String buildDefaultMessage(Object key) {
        return "Invalid key " + key;
    }
}
