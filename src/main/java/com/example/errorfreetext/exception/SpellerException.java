package com.example.errorfreetext.exception;

public class SpellerException extends RuntimeException {

    public SpellerException(String message) {
        super(message);
    }

    public SpellerException(String message, Throwable cause) {
        super(message, cause);
    }
}
