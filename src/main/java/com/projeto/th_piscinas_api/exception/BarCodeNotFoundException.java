package com.projeto.th_piscinas_api.exception;

public class BarCodeNotFoundException extends RuntimeException {
    public BarCodeNotFoundException(String message) {
        super(message);
    }
}
