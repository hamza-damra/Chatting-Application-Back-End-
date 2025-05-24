package com.chatapp.exception;

/**
 * Exception thrown when there's an issue with WebSocket connections.
 */
public class WebSocketConnectionException extends RuntimeException {
    public WebSocketConnectionException(String message) {
        super(message);
    }

    public WebSocketConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
