package com.chatapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an invalid message status is provided.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMessageStatusException extends RuntimeException {
    public InvalidMessageStatusException(String message) {
        super(message);
    }
}
