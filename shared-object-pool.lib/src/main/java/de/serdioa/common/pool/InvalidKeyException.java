package de.serdioa.common.pool;


/**
 * An exception thrown when a key provided to the pool is invalid, and the pool can not provide a shared object for this
 * key.
 */
public class InvalidKeyException extends IllegalArgumentException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 8251620653593899311L;


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
