package com.phishguard.ai.detection;

public class InvalidInputException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidInputException(String message) { super(message); }
}
