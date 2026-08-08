package com.projeto.th_piscinas_api.exception;

public class OrderInAlreadyInProgressException extends RuntimeException {
    public OrderInAlreadyInProgressException(String message) {
        super(message);
    }
}
