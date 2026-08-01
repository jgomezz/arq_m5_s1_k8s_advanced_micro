package com.tecsup.app.micro.product.domain.exception;

/**
 * Excepción cuando no se encuentra un producto
 */
public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException(String message) {
        super(message);
    }
    
    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }
}
