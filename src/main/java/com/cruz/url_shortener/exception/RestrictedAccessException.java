package com.cruz.url_shortener.exception;

public class RestrictedAccessException extends RuntimeException {
    public RestrictedAccessException(String message) {
        super(message);
    }
}
