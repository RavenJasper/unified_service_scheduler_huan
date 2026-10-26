package com.unified.scheduler.exception;

/**
 * Base abstract exception for all business domain exceptions in USS.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
