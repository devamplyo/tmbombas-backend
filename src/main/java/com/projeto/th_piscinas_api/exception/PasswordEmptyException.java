package com.projeto.th_piscinas_api.exception;

public class PasswordEmptyException extends RuntimeException {
    public PasswordEmptyException(String message) {
        super(message);
    }
}
