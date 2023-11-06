package dev.lydtech.dispatch.exception;


public class RetryableException extends RuntimeException {

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(Throwable e) {
        super(e);
    }
}
