package com.chatapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a message exceeds the maximum allowed length.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MessageTooLongException extends RuntimeException {
    public MessageTooLongException(String message) {
        super(message);
    }
}
