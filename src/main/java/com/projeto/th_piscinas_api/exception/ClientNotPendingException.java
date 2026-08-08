package com.projeto.th_piscinas_api.exception;

public class ClientNotPendingException extends RuntimeException {
    public ClientNotPendingException(String message) {
        super(message);
    }
}
