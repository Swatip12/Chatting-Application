package com.chatapp.exception;

/**
 * Exception thrown when WebSocket operations fail
 */
public class WebSocketException extends RuntimeException {
    
    public WebSocketException(String message) {
        super(message);
    }
    
    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }
}