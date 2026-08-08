package com.projeto.th_piscinas_api.exception;

public class CodeAlreadyInUseException extends RuntimeException {
    public CodeAlreadyInUseException(String message) {
        super(message);
    }
}
