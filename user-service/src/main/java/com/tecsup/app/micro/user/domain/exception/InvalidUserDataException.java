package com.tecsup.app.micro.user.domain.exception;

/**
 * Excepción cuando los datos del usuario son inválidos
 */
public class InvalidUserDataException extends RuntimeException {
    
    public InvalidUserDataException(String message) {
        super(message);
    }
}
