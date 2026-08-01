package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.exception.InvalidProductDataException;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Crear un nuevo producto
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateProductUseCase {
    
    private final ProductRepository productRepository;
    
    public Product execute(Product product) {
        log.debug("Executing CreateProductUseCase for product: {}", product.getName());
        
        // Validar datos del producto
        if (!product.isValid()) {
            throw new InvalidProductDataException("Invalid product data. Name, valid price and stock are required.");
        }
        
        // Guardar producto
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        
        return savedProduct;
    }
}
