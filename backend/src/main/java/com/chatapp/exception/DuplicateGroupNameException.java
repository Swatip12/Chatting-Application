package com.chatapp.exception;

/**
 * Exception thrown when attempting to create a group with a name that already exists
 */
public class DuplicateGroupNameException extends RuntimeException {
    
    public DuplicateGroupNameException(String message) {
        super(message);
    }
    
    public DuplicateGroupNameException(String message, Throwable cause) {
        super(message, cause);
    }
}