package com.chatapp.exception;

/**
 * Exception thrown when message delivery fails
 */
public class MessageDeliveryException extends RuntimeException {
    
    public MessageDeliveryException(String message) {
        super(message);
    }
    
    public MessageDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}