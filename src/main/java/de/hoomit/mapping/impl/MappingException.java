package de.hoomit.mapping.impl;

/**
 * Unchecked exception thrown when the mapping framework encounters an
 * unrecoverable error (e.g. destination class cannot be instantiated,
 * converter throws, type mismatch that cannot be coerced).
 */
public class MappingException extends RuntimeException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
