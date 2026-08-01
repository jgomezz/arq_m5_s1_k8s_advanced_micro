package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Obtener producto por ID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetProductByIdUseCase {
    
    private final ProductRepository productRepository;
    
    public Product execute(Long id) {
        log.debug("Executing GetProductByIdUseCase for id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
