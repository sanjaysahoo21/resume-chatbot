package com.example.resumeats.exception;

public class LlmServiceException extends RuntimeException {
    public LlmServiceException(String message) {
        super(message);
    }
    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
