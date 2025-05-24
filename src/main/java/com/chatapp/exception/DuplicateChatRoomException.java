package com.chatapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a duplicate private chat room between the same users.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateChatRoomException extends RuntimeException {
    public DuplicateChatRoomException(String message) {
        super(message);
    }
}
