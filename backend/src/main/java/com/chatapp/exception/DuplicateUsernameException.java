package com.chatapp.exception;

/**
 * Exception thrown when attempting to register with an existing username
 */
public class DuplicateUsernameException extends RuntimeException {
    
    public DuplicateUsernameException(String message) {
        super(message);
    }
    
    public DuplicateUsernameException(String message, Throwable cause) {
        super(message, cause);
    }
}