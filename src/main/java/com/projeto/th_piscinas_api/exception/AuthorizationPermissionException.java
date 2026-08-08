package com.projeto.th_piscinas_api.exception;

public class AuthorizationPermissionException extends RuntimeException {
    public AuthorizationPermissionException(String message) {
        super(message);
    }
}
